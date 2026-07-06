package com.fundmatrix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundmatrix.common.exception.ApiError;
import com.fundmatrix.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c->c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

//                        // CORS preflight
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── PUBLIC — no login needed ──
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ── ADMIN only ──
                        .requestMatchers("/users/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/schemes", "/schemes/*/options").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/schemes/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/distributors").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/distributors/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/dashboard/admin").hasAuthority("ADMIN")

                        // ── INVESTOR only ──
                        .requestMatchers(HttpMethod.GET, "/dashboard/investor").hasAuthority("INVESTOR")
                        .requestMatchers(HttpMethod.GET, "/kyc/mine").hasAuthority("INVESTOR")
                        .requestMatchers(HttpMethod.POST, "/kyc").hasAuthority("INVESTOR")
                        .requestMatchers(HttpMethod.GET, "/dividends/entitlements/mine").hasAuthority("INVESTOR")

                        // ── DISTRIBUTOR only ──
                        .requestMatchers(HttpMethod.GET, "/dashboard/distributor").hasAuthority("DISTRIBUTOR")
                        .requestMatchers(HttpMethod.GET, "/commissions/mine").hasAuthority("DISTRIBUTOR")

                        // ── Place transactions / folios / SIPs (investor, distributor, fund ops, admin) ──
                        .requestMatchers(HttpMethod.POST,
                                "/transactions/subscriptions", "/transactions/redemptions", "/transactions/switches")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/folios").hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips").hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips/*/pause", "/sips/*/resume", "/sips/*/cancel")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/swp-mandates").hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/swp-mandates/*", "/swp-mandates/*/status")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")

                        // ── FUND OPS (+ admin) ──
                        .requestMatchers(HttpMethod.PATCH, "/folios/*/status").hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/transactions/queue").hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/allot-batch").hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/transactions/*/accept", "/transactions/*/allot", "/transactions/*/reject")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/sips/due").hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips/*/run").hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/swp-mandates/*/process").hasAnyAuthority("FUND_OPS", "ADMIN")

                        // ── KYC verification (fund ops, compliance, admin) ──
                        .requestMatchers(HttpMethod.GET, "/kyc", "/kyc/investor/**").hasAnyAuthority("FUND_OPS", "COMPLIANCE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/kyc/*/status").hasAnyAuthority("FUND_OPS", "COMPLIANCE", "ADMIN")

                        // ── FUND ACCOUNTANT (+ admin) ──
                        .requestMatchers(HttpMethod.POST, "/nav", "/nav/*/publish").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers("/accruals/**").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/dividends", "/dividends/*/compute", "/dividends/*/approve",
                                "/dividends/*/process", "/dividends/*/cancel")
                            .hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/commissions/distributor/**").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/commissions/compute", "/commissions/*/approve", "/commissions/*/pay")
                            .hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")

                        // ── COMPLIANCE (+ admin) ──
                        .requestMatchers(HttpMethod.GET, "/transactions/flagged").hasAnyAuthority("COMPLIANCE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/dashboard/compliance").hasAnyAuthority("COMPLIANCE", "ADMIN")
                        .requestMatchers("/compliance/**").hasAnyAuthority("COMPLIANCE", "ADMIN")


                        .requestMatchers(HttpMethod.GET, "/nav/aum-summary").hasAnyAuthority("FUND_ACCOUNTANT", "COMPLIANCE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/dividends", "/dividends/*/entitlements").hasAnyAuthority("FUND_ACCOUNTANT", "COMPLIANCE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/distributors", "/distributors/**").hasAnyAuthority("FUND_OPS", "FUND_ACCOUNTANT", "COMPLIANCE", "ADMIN")

                        // ── Everything else (profile, notifications, scheme/folio/txn reads…) just needs a valid token ──
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"message\":\"You do not have permission\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
