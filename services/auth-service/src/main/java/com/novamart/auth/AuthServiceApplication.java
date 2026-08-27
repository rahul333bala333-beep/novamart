package com.novamart.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Nova Mart identity service.
 *
 * <p>The only service permitted to issue tokens and the only one that stores
 * credentials. Every other service verifies tokens but can neither mint nor read
 * a password hash, which keeps the blast radius of a compromise in one place.
 *
 * <p>{@code UserDetailsServiceAutoConfiguration} is excluded because this service
 * authenticates from a signed token, not from a Spring {@code UserDetailsService}.
 * Left in place, Boot would register an in-memory user and print a generated
 * password at every startup, which is noise at best and a real credential to
 * misread at worst.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
