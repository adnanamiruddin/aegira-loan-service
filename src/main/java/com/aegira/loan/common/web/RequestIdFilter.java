package com.aegira.loan.common.web;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = request.getHeader("X-Request-Id");
        }


        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }


        MDC.put("correlation_id", correlationId);
        MDC.put("requestId", correlationId);
        
        response.setHeader("X-Correlation-Id", correlationId);
        response.setHeader("X-Request-Id", correlationId);


        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlation_id");
            MDC.remove("requestId");
            MDC.remove("business_correlation_id");
        }
    }
}
