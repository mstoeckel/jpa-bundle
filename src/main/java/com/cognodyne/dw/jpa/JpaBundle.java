package com.cognodyne.dw.jpa;

import com.cognodyne.dw.common.JndiSupport;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class JpaBundle implements ConfiguredBundle<JpaConfigurable> {
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // nothing to do
    }

    @Override
    public void run(JpaConfigurable configuration, Environment environment) throws Exception {
        for (JpaConfiguration config : configuration.getJpaConfigurations()) {
            JndiSupport.bind(config.getJndiName(), config.getDatabase().build(environment.metrics(), config.getJndiName()));
        }
    }
}
