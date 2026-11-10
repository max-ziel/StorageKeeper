package io.github.maxziel.storagekeeper.adapter.in.event;

import io.github.maxziel.storagekeeper.domain.PantryItemFellBelowMinimum;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.PantryItemReachedMinimum;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventMetricsTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final DomainEventMetrics metrics = new DomainEventMetrics(meterRegistry);

    @Test
    void countsEventsByType() {
        metrics.on(new PantryItemFellBelowMinimum(PantryItemId.newId()));
        metrics.on(new PantryItemFellBelowMinimum(PantryItemId.newId()));
        metrics.on(new PantryItemReachedMinimum(PantryItemId.newId()));

        assertThat(count("PantryItemFellBelowMinimum")).isEqualTo(2.0);
        assertThat(count("PantryItemReachedMinimum")).isEqualTo(1.0);
    }

    @Test
    void hasNoCounterForEventsThatDidNotHappen() {
        assertThat(meterRegistry.find(DomainEventMetrics.METRIC_NAME).counter()).isNull();
    }

    private double count(String type) {
        return meterRegistry.get(DomainEventMetrics.METRIC_NAME)
                .tag("type", type)
                .counter()
                .count();
    }
}
