package io.github.maxziel.storagekeeper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
class ClockConfiguration {

    @Bean
    Clock clock(@Value("${storagekeeper.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
