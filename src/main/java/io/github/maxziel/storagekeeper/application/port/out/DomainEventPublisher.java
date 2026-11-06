package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.DomainEvent;

import java.util.List;

/**
 * Hands domain events to whoever wants to react to them. Listeners run synchronously and inside the
 * caller's transaction, so a failing listener rolls back the whole change.
 */
public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
