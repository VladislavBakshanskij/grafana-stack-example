package ru.vladislav.baksahsnkij.product.infra;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Optional;

public record MdcToTraceTaskDecorator(Tracer tracer) implements TaskDecorator {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String SAMPLED_KEY = "sampled";
    private static final String PARENT_ID_KEY = "parentId";

    @Override
    public Runnable decorate(Runnable runnable) {
        String traceId = MDC.get(TRACE_ID_KEY);
        String spanId = MDC.get(SPAN_ID_KEY);
        boolean sampled = Optional.of(SAMPLED_KEY)
                .map(MDC::get)
                .map(Boolean::parseBoolean)
                .orElse(false);
        String parentId = MDC.get(PARENT_ID_KEY);

        return () -> {
            TraceContext traceContext = tracer.traceContextBuilder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .sampled(sampled)
                    .parentId(parentId)
                    .build();

            CurrentTraceContext currentTraceContext = tracer.currentTraceContext();
            try (var scope = currentTraceContext.maybeScope(traceContext)) {
                Span span = tracer.currentSpan();
                runnable.run();
            }
        };
    }

}
