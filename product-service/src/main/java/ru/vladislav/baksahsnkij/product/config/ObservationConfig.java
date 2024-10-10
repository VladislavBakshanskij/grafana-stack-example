package ru.vladislav.baksahsnkij.product.config;

import io.micrometer.tracing.Tracer;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import ru.vladislav.baksahsnkij.product.infra.MdcToTraceTaskDecorator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Configuration
public class ObservationConfig {
    @Bean
    public TaskDecorator taskDecorator(Tracer tracer) {
        return new CompositeTaskDecorator(List.of(
                new MdcToTraceTaskDecorator( tracer),
                new ContextPropagatingTaskDecorator()
        ));
    }
}
