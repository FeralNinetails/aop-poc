package com.gm.aoppoc.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;

@Aspect
@Component
@Slf4j
public class RestTemplateAspect {

    private final MeterRegistry meterRegistry;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Expression filterExpression;
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RestTemplateAspect(MeterRegistry meterRegistry,
                              @Value("${resttemplate.aspect.filter:true}") String filter) {
        this.meterRegistry = meterRegistry;
        this.filterExpression = parser.parseExpression(filter);
    }

    @Around("execution(* org.springframework.web.client.RestTemplate.*(..)) && args(url, ..)")
    public Object intercept(ProceedingJoinPoint joinPoint, Object url) throws Throwable {
        String urlString = (url instanceof URI) ? ((URI) url).toString() : url.toString();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, args, parameterNameDiscoverer);

        // We can use #url directly because of args(url, ..) and MethodBasedEvaluationContext
        Boolean shouldLogAndMetric = filterExpression.getValue(context, Boolean.class);

        if (Boolean.TRUE.equals(shouldLogAndMetric)) {
            log.info("Interception! RestTemplate request to: {}", urlString);
            meterRegistry.counter("rest.template.requests",
                    Collections.singletonList(Tag.of("url", urlString)))
                    .increment();
        }

        return joinPoint.proceed();
    }
}
