package com.opsdesk.opsdeskbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// The security filter chain: the checks every request passes through before it
// reaches a controller. @EnableMethodSecurity turns on the @PreAuthorize role
// checks on controller methods.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
            JsonAuthenticationEntryPoint entryPoint) throws Exception {
        http
                // Uses the rules in CorsConfig, so the React app can still call
                // the API, including when the answer is a 401.
                .cors(Customizer.withDefaults())
                // CSRF tricks a browser into sending its cookies. This API uses
                // no cookies or sessions: the app sends the token itself.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        // Spring's own error page, so an error is not turned into a 401.
                        .requestMatchers("/error").permitAll()
                        // The health check answers only UP or DOWN, so it needs no login.
                        .requestMatchers("/actuator/health").permitAll()
                        // The API documentation and the Swagger page. They
                        // describe the API but hold no data: every endpoint
                        // tried from the page still needs a token.
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // BCrypt is slow on purpose and adds a random salt to each hash, so a
    // leaked hash is very hard to turn back into the password.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
