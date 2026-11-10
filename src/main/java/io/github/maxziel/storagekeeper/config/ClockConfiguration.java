package io.github.maxziel.storagekeeper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Provides the {@link Clock} that the application asks for "now" and "today". The time zone
 * ({@code storagekeeper.time-zone}) decides when a new day begins; tests pass a fixed clock instead.
 */
@Configuration
class ClockConfiguration {

    @Bean
    Clock clock(@Value("${storagekeeper.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
