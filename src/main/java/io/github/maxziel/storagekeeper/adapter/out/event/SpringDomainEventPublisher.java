package io.github.maxziel.storagekeeper.adapter.out.event;

import io.github.maxziel.storagekeeper.application.port.out.DomainEventPublisher;
import io.github.maxziel.storagekeeper.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes domain events as Spring application events. Spring calls all matching
 * {@code @EventListener} methods right away, in the same thread and transaction.
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(applicationEventPublisher::publishEvent);
    }
}
