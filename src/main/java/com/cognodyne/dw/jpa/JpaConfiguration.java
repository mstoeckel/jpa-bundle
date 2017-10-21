package com.cognodyne.dw.jpa;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.db.DataSourceFactory;

public class JpaConfiguration {
    @Valid
    @NotNull
    @JsonProperty("jndiName")
    private String            jndiName;
    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    public String getJndiName() {
        return jndiName;
    }

    public DataSourceFactory getDatabase() {
        return database;
    }
}
