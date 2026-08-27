package com.novamart.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novamart.common.api.ErrorResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Establishes the caller identity for a servlet request.
 *
 * <p>Two credentials are accepted:
 *
 * <ul>
 *   <li>{@code Authorization: Bearer <jwt>} for a person, verified by signature</li>
 *   <li>{@code X-Internal-Token} for another Nova Mart service, compared in
 *       constant time against the shared secret</li>
 * </ul>
 *
 * <p>Every service verifies the token itself rather than trusting the gateway to
 * have done it. The gateway is the first line of defence, not the only one: if a
 * service port is ever reachable directly, it still refuses unauthenticated
 * traffic.
 *
 * <p>A request that presents no credential is left anonymous and allowed to
 * continue, so public endpoints keep working. A request that presents a
 * <em>broken</em> credential is rejected here with a precise error code, because
 * a client needs to tell "expired, go refresh" apart from "invalid, sign in
 * again".
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final byte[] internalToken;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper, String internalToken) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.internalToken = internalToken == null ? new byte[0] : internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                authenticate(jwtService.verifyAccessToken(token), request);
            } catch (ApiException ex) {
                writeError(response, request, ex.errorCode(), ex.getMessage());
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String internal = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (StringUtils.hasText(internal)) {
            if (!matchesInternalToken(internal)) {
                writeError(response, request, ErrorCode.UNAUTHORIZED, "Invalid internal service credential");
                return;
            }
            authenticate(AuthenticatedUser.service("internal"), request);
        }

        chain.doFilter(request, response);
    }

    private void authenticate(AuthenticatedUser user, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = user.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Constant-time comparison. A byte-by-byte early exit would leak the shared
     * secret to anyone able to time enough requests.
     */
    private boolean matchesInternalToken(String presented) {
        if (internalToken.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(internalToken, presented.getBytes(StandardCharsets.UTF_8));
    }

    private void writeError(HttpServletResponse response,
                            HttpServletRequest request,
                            ErrorCode code,
                            String message) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(code, message, request.getRequestURI()));
    }
}
