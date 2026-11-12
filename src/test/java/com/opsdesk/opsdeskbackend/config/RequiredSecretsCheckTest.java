package com.opsdesk.opsdeskbackend.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

class RequiredSecretsCheckTest {

    @Test
    void bothMissingStopsStartupAndNamesBoth() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> run(new MockEnvironment()));

        assertTrue(e.getMessage().contains("JWT_SECRET"));
        assertTrue(e.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void oneMissingNamesOnlyThatOne() {
        MockEnvironment env = new MockEnvironment().withProperty("JWT_SECRET", "a-secret-that-is-long-enough-for-tests");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> run(env));

        assertTrue(e.getMessage().contains("DB_PASSWORD"));
        assertFalse(e.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void anEmptyValueCountsAsMissing() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("JWT_SECRET", "   ")
                .withProperty("DB_PASSWORD", "test-db-password");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> run(env));

        assertTrue(e.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void bothSetLetsTheAppStart() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("JWT_SECRET", "a-secret-that-is-long-enough-for-tests")
                .withProperty("DB_PASSWORD", "test-db-password");

        assertDoesNotThrow(() -> run(env));
    }

    private static void run(MockEnvironment env) {
        RequiredSecretsCheck check = new RequiredSecretsCheck();
        check.setEnvironment(env);
        check.postProcessBeanFactory(new DefaultListableBeanFactory());
    }
}
