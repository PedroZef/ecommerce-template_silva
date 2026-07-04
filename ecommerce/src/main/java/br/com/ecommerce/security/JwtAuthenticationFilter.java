package br.com.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawJwt = authHeader.substring(7).trim();
        if (rawJwt.toLowerCase().startsWith("bearer ")) {
            rawJwt = rawJwt.substring(7).trim();
        }
        
        // Remove aspas caso venha formatado erroneamente
        if (rawJwt.startsWith("\"") && rawJwt.endsWith("\"")) {
            rawJwt = rawJwt.substring(1, rawJwt.length() - 1).trim();
        }
        jwt = rawJwt;
        
        try {
            username = jwtService.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var roles = jwtService.extractRoles(jwt);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (!userDetails.getUsername().equals(username)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                boolean tokenRolesMatch = roles.stream()
                        .allMatch(r -> userDetails.getAuthorities().contains(r));

                var authorities = tokenRolesMatch && !roles.isEmpty() ? roles : userDetails.getAuthorities();

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Usuário '" + username + "' autenticado via Token JWT em " + request.getRequestURI());
                }
            }
        } catch (Exception e) {
            logger.error("Falha ao analisar token JWT: " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
