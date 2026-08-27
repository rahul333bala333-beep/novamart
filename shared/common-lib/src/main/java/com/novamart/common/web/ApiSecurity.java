package com.novamart.common.web;

import com.novamart.common.security.JwtAuthenticationFilter;
import com.novamart.common.security.RestAuthenticationEntryPoint;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The security posture shared by every Nova Mart service.
 *
 * <p>Each service still declares its own route rules, because only it knows
 * which of its endpoints are public. What is identical everywhere lives here so
 * that a service cannot accidentally omit it:
 *
 * <ul>
 *   <li><b>Stateless.</b> No HTTP session is created. Identity comes from the
 *       token on every request, which is what lets any instance serve any
 *       request without sticky sessions.</li>
 *   <li><b>CSRF disabled.</b> Correct here and only here: there is no cookie the
 *       browser attaches automatically, so there is no cross-site request to
 *       forge. Re-enabling it would break every client for no gain.</li>
 *   <li><b>JSON error rendering</b> for 401 and 403.</li>
 * </ul>
 *
 * <p>CORS is deliberately <em>not</em> configured in the services. The browser
 * only ever talks to the gateway, so the gateway is the single place that needs
 * an origin allow-list; duplicating it here would create a second, easily
 * forgotten place to get wrong.
 */
public final class ApiSecurity {

    private ApiSecurity() {
    }

    public static HttpSecurity applyDefaults(HttpSecurity http,
                                             JwtAuthenticationFilter jwtFilter,
                                             RestAuthenticationEntryPoint entryPoint) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(entryPoint))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
