# jpa-bundle
A simple [Dropwizard](http://www.dropwizard.io) bundle for providing [JPA](http://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html) persistence support.

The implementation is provided by [Hibernate](http://hibernate.org).

### Usage
Add the following to your `build.gradle`:
```
repositories {
	jcenter()
}

dependencies {
	compile "com.cognodyne.dw:jpa-bundle:$bundleVersion"
}
```

### Examples
```
@ApplicationScoped
public class ExampleServer extends Application<ExampleConfiguration> {
    @Inject
    private CdiBundle            cdiBundle;
    @Inject
    private JtaBundle            jtaBundle;
    @Inject
    private JpaBundle            jpaBundle;
        
	@Override
    public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
        bootstrap.addBundle(this.cdiBundle);
        bootstrap.addBundle(this.jtaBundle);
        bootstrap.addBundle(this.jpaBundle);
    }
    
	public static void main(String... args) {
        try {
            CdiBundle.application(ExampleServer.class, args)//
                    .with(ResourceInjectionServiceProvider.getInstance())// to support @Resource 
                    .with(TransactionServiceProvider.getInstance())// to add jta support
                    .with(JpaServiceProvider.getInstance())// Hibernate backed jpa support
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public interface UserService {
    @GET
    @Timed
    public List<User> getUsers();

    @GET
    @Path("/{id}")
    @Timed
    public User getUser(@PathParam("id") String id);

    @POST
    @Path("/{userId}")
    @Timed
    public void createUser(@PathParam("userId") String userId);

    @PUT
    @Path("/{id}/{userId}")
    @Timed
    public void updateUser(@PathParam("id") String id, @PathParam("userId") String userId);

    @DELETE
    @Path("/{id}")
    @Timed
    public void delete(@PathParam("id") String id);
}

public class UserResource implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);
    @PersistenceContext(unitName = "exampleUnit")
    private EntityManager       em;

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public List<User> getUsers() {
        return ((List<CdiUser>) em.createQuery("select u from CdiUser u")//
                .getResultList()).stream()//
                        .map(u -> new User(u.getId(), u.getUserId()))//
                        .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public User getUser(String id) {
        CdiUser user = em.find(CdiUser.class, id);
        return user == null ? null : new User(user.getId(), user.getUserId());
    }

    @Override
    @Transactional
    public void createUser(String userId) {
        CdiUser user = new CdiUser();
        user.setUserId(userId);
        em.persist(user);
    }

    @Override
    @Transactional
    public void updateUser(String id, String userId) {
        CdiUser user = em.find(CdiUser.class, id);
        if (user == null) {
            logger.warn("user:{} does not exist", id);
        } else {
            user.setUserId(userId);
            em.merge(user);
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        CdiUser user = em.find(CdiUser.class, id);
        if (user == null) {
            logger.warn("user:{} does not exist", id);
        } else {
            em.remove(user);
        }
    }
}

```

And, the following should be in `src/main/resources/META-INF/` directory:
```
persistence.xml

<?xml version="1.0" encoding="UTF-8"?>
<persistence version="2.1"
	xmlns="http://xmlns.jcp.org/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd">
	<persistence-unit name="exampleUnit" transaction-type="JTA">
		<provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
		<jta-data-source>java:comp/env/jdbc/exampleDS</jta-data-source>
		<jar-file>build/classes/java/main</jar-file>
		<properties>
			<property name="hibernate.session_factory_name" value="java:comp/exampleEMF" />
			<property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect" />
			<property name="hibernate.hbm2ddl.auto" value="create-drop" />
			<!-- * validate: validate the schema, makes no changes to the database. 
				* update: update the schema. * create: creates the schema, destroying previous 
				data. * create-drop: drop the schema at the end of the session. -->

			<property name="hibernate.show_sql" value="false" />
			<property name="hibernate.format_sql" value="true" />
			<property name="hibernate.temp.use_jdbc_metadata_defaults"
				value="false" />
		</properties>
	</persistence-unit>
</persistence>

beans.xml
<beans xmlns="http://xmlns.jcp.org/xml/ns/javaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
        http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd"
       bean-discovery-mode="all">
       <interceptors>
       	<!-- class>com.cognodyne.dw.jta.TransactionInterceptor</class-->
       </interceptors>
</beans>
```

And, the `server.yml` should include something like the following:
```
jpa:
 -jndiName: 'java:comp/env/jdbc/exampleDS'
  database:
    driverClass: org.postgresql.Driver
    user: postgres
    password: postgres
    url: jdbc:postgresql://localhost/postgres
    validationQuery: select 1
```