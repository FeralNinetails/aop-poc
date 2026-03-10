package com.gm.aoppoc;

import com.gm.aoppoc.service.ApiService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@SpringBootApplication
@Slf4j
@EnableScheduling
public class AoppocApplication {

    private final MeterRegistry meterRegistry;

    public AoppocApplication(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static void main(String[] args) {
        SpringApplication.run(AoppocApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public CommandLineRunner runner(ApiService apiService) {
        return args -> {
            String url = "https://catfact.ninja/fact";
            log.info("Calling public API: {}", url);
            try {
                String response = apiService.callApi(url);
                log.info("API call successful! Response: {}", response);
            } catch (Exception e) {
                log.error("API call failed: {}", e.getMessage());
            }
        };
    }

    @Scheduled(fixedRate = 10000)
    public void reportMetrics() {
        meterRegistry.find("rest.template.requests").counters().forEach(counter -> {
            log.info("Metric [rest.template.requests] for URL {}: {}", 
                counter.getId().getTag("url"), counter.count());
        });
    }

}
