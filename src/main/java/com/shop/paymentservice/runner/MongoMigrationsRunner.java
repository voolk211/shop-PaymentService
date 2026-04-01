package com.shop.paymentservice.runner;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MongoMigrationsRunner implements CommandLineRunner {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    public void run(String... args) throws Exception {
        var mongoLiquibaseDatabase = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, null);
        try (Liquibase liquibase = new Liquibase("liquibase/db.changelog.yaml",
                new ClassLoaderResourceAccessor(), mongoLiquibaseDatabase)) {
            liquibase.update();
        }
    }
}
