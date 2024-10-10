package ru.vladislav.baksahsnkij.analytic.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;

import java.util.List;

@Configuration
public class TracingConfig {

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoMetricsSynchronousContextProvider(ObservationRegistry registry) {
        return clientSettingsBuilder -> clientSettingsBuilder.contextProvider(ContextProviderFactory.create(registry))
                .addCommandListener(new MongoObservationCommandListener(registry));
    }

    @Bean
    public SdkTracerProviderBuilderCustomizer sdkTracerProviderBuilderCustomizer(@Value("${hostname}") String hostname) {
        return builder -> builder.addSpanProcessor(new BaggageTaggingSpanProcessor(List.of("rqid")))
                .addSpanProcessor(new SpanProcessor() {
                    @Override
                    public void onStart(Context context, ReadWriteSpan readWriteSpan) {
                        readWriteSpan.setAttribute("hostname", hostname);
                    }

                    @Override
                    public boolean isStartRequired() {
                        return true;
                    }

                    @Override
                    public void onEnd(ReadableSpan readableSpan) {

                    }

                    @Override
                    public boolean isEndRequired() {
                        return false;
                    }
                });
    }



}
