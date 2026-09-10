package org.golftripbooker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the Authorization header once per request and, if it holds a valid token,
 * puts the caller into the SecurityContext. It never rejects anything itself --
 * an anonymous request simply continues with an empty context, and the
 * authorization rules in SecurityConfig decide what happens next.
 *
 * Deliberately NOT a @Component. Spring Boot auto-registers every bean of type
 * Filter with the servlet container, so annotating this would install it twice:
 * once in the security chain where it belongs, and once outside it, running on
 * static resources and error dispatches for no reason. SecurityConfig constructs
 * it instead, which keeps it in exactly one chain.
 */
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtConverter converter;

    public JwtRequestFilter(JwtConverter converter) {
        this.converter = converter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        UsernamePasswordAuthenticationToken authentication =
                converter.getAuthentication(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
