package ru.vladislav.baksahsnkij.product;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.pyroscope.labels.LabelsSet;
import io.pyroscope.labels.Pyroscope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
class AgentLabelFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public AgentLabelFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ThrowingRunnable<Exception> action = ThrowingRunnable.throwingRunnable(() -> filterChain.doFilter(request, response));
        Optional.of(tracer)
                .map(Tracer::currentSpan)
                .map(Span::context)
                .ifPresentOrElse(
                        context -> {
                            Map<String, String> labels = Map.of(
                                    "traceId", context.traceId(),
                                    "spanId", context.spanId()
                            );

                            Pyroscope.LabelsWrapper.run(new LabelsSet(labels), action);
                        },
                        action
                );

    }
}
