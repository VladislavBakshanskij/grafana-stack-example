package ru.vladislav.baksahsnkij.product.config;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import ru.vladislav.baksahsnkij.product.infra.CountedMeterTagJoinPointFunction;
import ru.vladislav.baksahsnkij.product.infra.MdcToTraceTaskDecorator;
import ru.vladislav.baksahsnkij.product.infra.ProductIdMeterTagResolver;

import java.util.List;

@Configuration
public class ObservationConfig {
    @Bean
    public TaskDecorator taskDecorator(Tracer tracer) {
        return new CompositeTaskDecorator(List.of(
                new MdcToTraceTaskDecorator(tracer),
                new ContextPropagatingTaskDecorator()
        ));
    }

    @Bean
    public CountedAspect countedAspect(MeterRegistry meterRegistry) {
        return new CountedAspect(meterRegistry,
                new CountedMeterTagJoinPointFunction(
                        resolverClass -> {
                            if (resolverClass.equals(ProductIdMeterTagResolver.class)) {
                                return new ProductIdMeterTagResolver();
                            }
                            return null;
                        },
                        _ -> spelExpressionResolver()));
    }

    @Bean
    public MeterTagAnnotationHandler meterTagAnnotationHandler() {
        return new MeterTagAnnotationHandler(
                resolverClass -> {
                    if (resolverClass.equals(ProductIdMeterTagResolver.class)) {
                        return new ProductIdMeterTagResolver();
                    }
                    return null;
                },
                _ -> spelExpressionResolver()
        );
    }

    private ValueExpressionResolver spelExpressionResolver() {
        return (expression, parameter) -> {
            try {
                SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
                ExpressionParser expressionParser = new SpelExpressionParser();
                Expression expressionToEvaluate = expressionParser.parseExpression(expression);
                return expressionToEvaluate.getValue(context, parameter, String.class);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to evaluate SpEL expression '%s'".formatted(expression), ex);
            }
        };
    }
}
