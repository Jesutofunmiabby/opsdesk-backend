package com.opsdesk.opsdeskbackend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.test.context.support.WithSecurityContext;

import com.opsdesk.opsdeskbackend.user.UserRole;

// Runs a test as if this user had sent a valid token, the same way
// JwtAuthFilter logs them in. Put it on a test class or a test method.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = WithAuthUserSecurityContextFactory.class)
public @interface WithAuthUser {

    long id() default 1L;

    UserRole role() default UserRole.ADMIN;
}
