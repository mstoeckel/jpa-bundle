package com.cognodyne.dw.jpa;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognodyne.dw.common.DeployableWeldService;
import com.google.common.collect.Maps;

public class JpaServiceProvider implements JpaInjectionServices, DeployableWeldService {
    private static final Logger               logger   = LoggerFactory.getLogger(JpaServiceProvider.class);
    private static final JpaServiceProvider instance = new JpaServiceProvider();
    private ReentrantLock                     lock     = new ReentrantLock();
    private Map<String, EntityManagerFactory> emfs     = Maps.newHashMap();

    private JpaServiceProvider() {
    }

    public static JpaServiceProvider getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Service> getType() {
        try {
            return (Class<Service>) Class.forName(JpaInjectionServices.class.getName());
        } catch (ClassNotFoundException e) {
            logger.error("this should not happen");
            return null;
        }
    }

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void cleanup() {
        logger.debug("cleanup called");
        for (Entry<String, EntityManagerFactory> entry : emfs.entrySet()) {
            entry.getValue().close();
        }
    }

    @Override
    public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(InjectionPoint ip) {
        return new ResourceReferenceFactory<EntityManager>() {
            @Override
            public ResourceReference<EntityManager> createResource() {
                return new ResourceReference<EntityManager>() {
                    @Override
                    public EntityManager getInstance() {
                        return resolvePersistenceContext(ip);
                    }

                    @Override
                    public void release() {
                        //noop
                    }
                };
            }
        };
    }

    @Override
    public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(InjectionPoint ip) {
        return new ResourceReferenceFactory<EntityManagerFactory>() {
            @Override
            public ResourceReference<EntityManagerFactory> createResource() {
                return new ResourceReference<EntityManagerFactory>() {
                    @Override
                    public EntityManagerFactory getInstance() {
                        return resolvePersistenceUnit(ip);
                    }

                    @Override
                    public void release() {
                        //noop
                    }
                };
            }
        };
    }

    @Override
    public EntityManager resolvePersistenceContext(InjectionPoint ip) {
        return this.resolvePersistenceUnit(ip).createEntityManager();
    }

    @Override
    public EntityManagerFactory resolvePersistenceUnit(InjectionPoint ip) {
        String name = ip.getAnnotated().getAnnotation(PersistenceContext.class).unitName();
        logger.debug("resolving persistence unit for {}...", name);
        lock.lock();
        try {
            EntityManagerFactory emf = emfs.get(name);
            if (emf == null) {
                emf = Persistence.createEntityManagerFactory(name);
                emfs.put(name, emf);
            }
            return emf;
        } finally {
            lock.unlock();
        }
    }
}
