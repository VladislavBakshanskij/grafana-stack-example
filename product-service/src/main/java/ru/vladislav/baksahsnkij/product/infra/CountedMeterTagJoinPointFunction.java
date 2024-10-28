package ru.vladislav.baksahsnkij.product.infra;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import io.micrometer.common.annotation.NoOpValueResolver;
import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.common.annotation.ValueResolver;
import io.micrometer.core.aop.MeterTag;
import io.micrometer.core.instrument.Tag;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CountedMeterTagJoinPointFunction implements Function<ProceedingJoinPoint, Iterable<Tag>> {
    private static final Converter<String, String> LOWER_CAMEL_TO_SNAKE = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE);

    private final Function<Class<? extends ValueResolver>, ValueResolver> valueResolverProvider;
    private final Function<Class<? extends ValueExpressionResolver>, ? extends ValueExpressionResolver> valueExpressionResolver;

    public CountedMeterTagJoinPointFunction(
            Function<Class<? extends ValueResolver>, ValueResolver> valueResolverProvider,
            Function<Class<? extends ValueExpressionResolver>, ? extends ValueExpressionResolver> valueExpressionResolver
    ) {
        this.valueResolverProvider = valueResolverProvider;
        this.valueExpressionResolver = valueExpressionResolver;
    }

    @Override
    public Iterable<Tag> apply(ProceedingJoinPoint pjp) {
        Signature signature = pjp.getSignature();
        List<Tag> tags = new ArrayList<>();
        if (signature instanceof MethodSignature methodSignature) {
            Method method = methodSignature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = pjp.getArgs();
            int parameterIndex = 0;
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (Annotation[] parameterAnnotation : parameterAnnotations) {
                for (Annotation annotation : parameterAnnotation) {
                    if (annotation instanceof MeterTag meterTag) {
                        tags.add(Tag.of(resolveTagKey(meterTag, parameters[parameterIndex]),
                                resolveTagValue(meterTag, args[parameterIndex])));
                    }
                }
                parameterIndex++;
            }
        }
        return tags;
    }

    private String resolveTagValue(MeterTag meterTag, Object object) {
        if (StringUtils.hasText(meterTag.expression())) {
            return valueExpressionResolver.apply(ValueExpressionResolver.class)
                    .resolve(meterTag.expression(), object);
        }

        if (meterTag.resolver() == NoOpValueResolver.class) {
            return "";
        }

        return valueResolverProvider.apply(meterTag.resolver()).resolve(object);
    }

    private String resolveTagKey(MeterTag meterTag, Parameter parameter) {
        if (StringUtils.hasText(meterTag.key())) {
            return meterTag.key();
        }

        if (StringUtils.hasText(meterTag.value())) {
            return meterTag.value();
        }

        return LOWER_CAMEL_TO_SNAKE.convert(parameter.getName());
    }
}
