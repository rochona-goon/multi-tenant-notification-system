package io.github.rochona.notificationsystem.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
@Validated
public class RateLimitProperties {

    @Positive
    private long capacity;

    @Positive
    private long refillTokens;

    @NotNull
    private Duration refillDuration;
}
