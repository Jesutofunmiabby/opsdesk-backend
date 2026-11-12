package com.opsdesk.opsdeskbackend.config;

import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// In production, stops the app at startup if a secret is missing, with a
// message that names it. Without this, a missing DB_PASSWORD is passed to
// PostgreSQL as the text "${DB_PASSWORD}" and fails later with a confusing
// login error (or, on a database that accepts any password, not at all).
// A BeanFactoryPostProcessor runs before any other bean is created, so this
// check happens before the app tries to reach the database.
@Component
@Profile("prod")
public class RequiredSecretsCheck implements BeanFactoryPostProcessor, EnvironmentAware {

    static final List<String> REQUIRED = List.of("JWT_SECRET", "DB_PASSWORD");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        List<String> missing = REQUIRED.stream()
                .filter(name -> isBlank(environment.getProperty(name)))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("The prod profile needs these environment variables, "
                    + "but they are missing or empty: " + String.join(", ", missing)
                    + ". Set them (see .env.example) and start again.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
