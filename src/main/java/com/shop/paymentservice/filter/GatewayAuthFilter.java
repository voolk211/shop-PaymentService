package com.shop.paymentservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${internal.internal-secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger")) {
            filterChain.doFilter(request, response);
            return;
        }


        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-Roles");
        String secretHeader = request.getHeader("X-Internal-Auth");

        if (!secret.equals(secretHeader)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid X-Internal-Auth header");
            return;
        }

        if (userId == null || rolesHeader == null || rolesHeader.isBlank()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing user roles or user id");
            return;
        }

        long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-Id");
            return;
        }

        List<? extends GrantedAuthority> authorities = Arrays
                .stream(rolesHeader.split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(userIdLong, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(token);

        filterChain.doFilter(request, response);
    }
}
