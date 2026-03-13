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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Aspect // ASPECT: A class that modularizes a cross-cutting concern (logging/metrics).
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

    /**
     * POINTCUT: The expression inside @Around. It defines *where* to intercept.
     * Here it matches all methods in RestTemplate and binds the first argument to 'url'.
     *
     * ADVICE: This whole 'intercept' method. It's the logic that runs *around* the Join Point.
     *
     * JOIN POINT: Represented by 'ProceedingJoinPoint joinPoint'. It's the specific
     * execution of the method that matched the Pointcut (e.g., a call to restTemplate.getForObject).
     */
    @Around("execution(* org.springframework.web.client.RestTemplate.*(..)) && args(url, ..)")
    public Object intercept(ProceedingJoinPoint joinPoint, Object url) throws Throwable {
        String urlString = (url instanceof URI) ? ((URI) url).toString() : url.toString();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, args, parameterNameDiscoverer);

        Boolean shouldLogAndMetric = filterExpression.getValue(context, Boolean.class);



        Object result;
        HttpStatusCode status = HttpStatus.OK;
        try {
            result = joinPoint.proceed();
            if (result instanceof ResponseEntity) {
                status = ((ResponseEntity<?>) result).getStatusCode();
            }
        } catch (HttpStatusCodeException e) {
            status = e.getStatusCode();
            throw e;
        } catch (Throwable e) {
            status = HttpStatus.BAD_REQUEST;
            throw e;
        } finally {
            if (Boolean.TRUE.equals(shouldLogAndMetric)) {
                List<Tag> tags = new ArrayList<>();
                tags.add(Tag.of("url", urlString));
                tags.add(Tag.of("status", String.valueOf(status.value())));
                
                meterRegistry.counter("rest.template.requests", tags).increment();
                log.info("Recorded metric for {} with status {}", urlString, status);
            }
        }

        return result;
    }
}
