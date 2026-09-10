package org.golftripbooker.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtConverter jwtConverter;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtConverter jwtConverter,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtConverter = jwtConverter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // No cookies, no browser forms, so there is no CSRF vector to protect.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // Nothing is kept server side. Every request carries its own proof.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // CORS preflight must never require authentication.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        /*
                         * "As a client, I can't reach host features." This one line is the
                         * outer wall; every method in BookingService checks the role again,
                         * so a routing mistake cannot quietly open the door.
                         */
                        .requestMatchers("/api/host/**").hasRole("HOST")

                        .anyRequest().authenticated())

                /*
                 * Constructed here rather than injected, so it lives in this chain and
                 * nowhere else. The reference class does not have to be present in the
                 * chain -- Spring Security uses it to look up a position, not a filter,
                 * so this lands where a form-login filter would have gone: after the
                 * SecurityContext is loaded, before authorization is decided.
                 */
                .addFilterBefore(new JwtRequestFilter(jwtConverter),
                        UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        // Without these, an unauthenticated API call redirects to a login
                        // page instead of returning a status the client can react to.
                        .authenticationEntryPoint((req, res, e) -> res.setStatus(401))
                        .accessDeniedHandler((req, res, e) -> res.setStatus(403)));

        return http.build();
    }

    /** Injected into AuthController so /api/auth/login can verify a password. */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Patterns rather than exact origins, so a phone on your wifi is covered
        // without hard-coding today's DHCP address.
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        // Credentials are the bearer token in a header, not a cookie, so the browser
        // does not need permission to send anything of its own.
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
