package ru.vladislav.baksahsnkij.product;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

record HostnameSpanProcessor(String hostname) implements SpanProcessor {
    @Override
    public void onStart(Context parentContext, ReadWriteSpan readWriteSpan) {
        readWriteSpan.setAttribute("hostname", hostname);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {

    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
