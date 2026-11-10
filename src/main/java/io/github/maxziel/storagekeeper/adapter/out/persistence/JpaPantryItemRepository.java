package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.application.port.in.PantryItemFilter;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Implements the {@link PantryItemRepository} port with PostgreSQL via Spring Data JPA.
 * Translates between the domain class {@link PantryItem} and {@link PantryItemJpaEntity}.
 */
@Repository
class JpaPantryItemRepository implements PantryItemRepository {

    private final SpringDataPantryItemRepository jpaRepository;

    JpaPantryItemRepository(SpringDataPantryItemRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PantryItem save(PantryItem pantryItem) {
        jpaRepository.save(PantryItemJpaEntity.fromDomain(pantryItem));
        return pantryItem;
    }

    @Override
    public Optional<PantryItem> findById(PantryItemId pantryItemId) {
        return jpaRepository.findById(pantryItemId.value())
            .map(PantryItemJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public boolean deleteById(PantryItemId pantryItemId) {
        if (!jpaRepository.existsById(pantryItemId.value())) {
            return false;
        }

        jpaRepository.deleteById(pantryItemId.value());
        return true;
    }

    @Override
    public List<PantryItem> findWithExpiryDateUpTo(HouseholdId householdId, LocalDate latest) {
        return jpaRepository.findByHouseholdIdAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
                householdId.value(), latest).stream()
            .map(PantryItemJpaEntity::toDomain)
            .toList();
    }

    @Override
    public ResultPage<PantryItem> search(HouseholdId householdId, PantryItemFilter filter, PageQuery page) {
        Specification<PantryItemJpaEntity> specification = PantryItemSpecifications.inHousehold(householdId.value());

        if (filter.storageLocationId() != null) {
            specification = specification.and(
                PantryItemSpecifications.inStorageLocation(filter.storageLocationId().value())
            );
        }

        if (filter.onlyBelowMinimum()) {
            specification = specification.and(PantryItemSpecifications.belowMinimum());
        }

        // Sorting by id as well keeps the order stable when names are equal, so no item appears on two pages.
        PageRequest pageRequest = PageRequest.of(page.page(), page.size(), Sort.by("name", "id"));

        Page<PantryItemJpaEntity> result = jpaRepository.findAll(specification, pageRequest);

        return new ResultPage<>(
            result.getContent().stream().map(PantryItemJpaEntity::toDomain).toList(),
            page.page(),
            page.size(),
            result.getTotalElements()
        );
    }

    @Override
    public List<PantryItem> findAllById(Collection<PantryItemId> pantryItemIds) {
        return jpaRepository.findAllById(pantryItemIds.stream().map(PantryItemId::value).toList()).stream()
            .map(PantryItemJpaEntity::toDomain)
            .toList();
    }
}
