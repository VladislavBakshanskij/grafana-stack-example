package ru.vladislav.baksahsnkij.product.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceparentResponseFilter extends OncePerRequestFilter {
    private final Tracer tracer;

    public TraceparentResponseFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Span span = tracer.currentSpan();
        TraceContext context = span.context();
        String traceId = context.traceId();
        String spanId = context.spanId();
        boolean sampled = context.sampled();

        response.addHeader("traceparent", "00-%s-%s-%s".formatted(traceId, spanId, (sampled ? "01" : "00")));
        System.out.println();
        filterChain.doFilter(request, response);

    }
}
