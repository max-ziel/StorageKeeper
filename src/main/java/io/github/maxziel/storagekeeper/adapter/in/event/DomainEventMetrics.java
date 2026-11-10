package io.github.maxziel.storagekeeper.adapter.in.event;

import io.github.maxziel.storagekeeper.domain.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Counts the domain events by type, e.g. how often an item fell below its minimum stock.
 * Prometheus shows them as {@code storagekeeper_domain_events_total{type="PantryItemFellBelowMinimum"}}.
 * Counted only after the commit: a change that was rolled back did not happen.
 */
@Component
class DomainEventMetrics {

    static final String METRIC_NAME = "storagekeeper.domain.events";

    private final MeterRegistry metricRegistry;

    DomainEventMetrics(MeterRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(DomainEvent event) {
        // register() returns the existing counter if there already is one with this name and tag.
        Counter.builder(METRIC_NAME)
            .description("Domain events whose transaction was committed")
            .tag("type", event.getClass().getSimpleName())
            .register(metricRegistry)
            .increment();
    }
}
