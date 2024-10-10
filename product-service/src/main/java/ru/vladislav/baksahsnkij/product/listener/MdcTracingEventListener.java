package ru.vladislav.baksahsnkij.product.listener;

import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.opentelemetry.api.trace.Span;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public record MdcTracingEventListener(@Lazy Tracer tracer) implements EventListener {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(MdcTracingEventListener.class);
    private static final String parentIdKey = "parentId";
    private static final String sampledKey = "sampled";


    private void onScopeAttached(EventPublishingContextWrapper.ScopeAttachedEvent event) {
        log.trace("Got scope changed event [{}]", event);
        Span span = event.getSpan();
        if (span != null) {
            MDC.put(parentIdKey, tracer.currentSpan().context().parentId());
            MDC.put(sampledKey, String.valueOf(span.getSpanContext().isSampled()));
        }
    }

    private void onScopeRestored(EventPublishingContextWrapper.ScopeRestoredEvent event) {
        log.trace("Got scope restored event [{}]", event);
        Span span = event.getSpan();
        if (span != null) {
            MDC.put(parentIdKey, tracer.currentSpan().context().parentId());
            MDC.put(sampledKey, String.valueOf(span.getSpanContext().isSampled()));
        }
    }

    private void onScopeClosed(EventPublishingContextWrapper.ScopeClosedEvent event) {
        log.trace("Got scope closed event [{}]", event);
        MDC.remove(sampledKey);
        MDC.remove(parentIdKey);
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof EventPublishingContextWrapper.ScopeAttachedEvent) {
            onScopeAttached((EventPublishingContextWrapper.ScopeAttachedEvent) event);
        } else if (event instanceof EventPublishingContextWrapper.ScopeClosedEvent) {
            onScopeClosed((EventPublishingContextWrapper.ScopeClosedEvent) event);
        } else if (event instanceof EventPublishingContextWrapper.ScopeRestoredEvent) {
            onScopeRestored((EventPublishingContextWrapper.ScopeRestoredEvent) event);
        }
    }

}
