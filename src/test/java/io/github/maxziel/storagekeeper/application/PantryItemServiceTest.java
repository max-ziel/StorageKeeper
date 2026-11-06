package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.application.port.in.AddPantryItemCommand;
import io.github.maxziel.storagekeeper.application.port.in.ExpiringPantryItem;
import io.github.maxziel.storagekeeper.application.port.in.PantryItemFilter;
import io.github.maxziel.storagekeeper.application.port.out.DomainEventPublisher;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;

import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PantryItemServiceTest {

    @Mock
    private PantryItemRepository repository;

    @Mock
    private StorageLocationRepository storageLocationRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private CurrentHousehold currentHousehold;

    private static final HouseholdId HOUSEHOLD = HouseholdId.newId();
    private static final HouseholdId OTHER_HOUSEHOLD = HouseholdId.newId();
    private static final StorageLocation PANTRY = StorageLocation.createDefault(HOUSEHOLD);

    /** "Today" is always 26 February 2027 in these tests. */
    private final Clock clock = Clock.fixed(Instant.parse("2027-02-26T10:00:00Z"), ZoneOffset.UTC);

    private PantryItemService service;

    @BeforeEach
    void setUp() {
        // Every test runs as a member of HOUSEHOLD, unless it says otherwise.
        lenient().when(currentHousehold.require()).thenReturn(HOUSEHOLD);
        service = new PantryItemService(repository, storageLocationRepository, clock, eventPublisher, currentHousehold);
    }

    private final PantryItem rice = PantryItem.create(HOUSEHOLD,
            new ItemName("Reis"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), LocalDate.of(2027, 3, 1),
            PANTRY.getStorageLocationId());

    @Test
    void addWithoutLocationUsesDefaultLocation() {
        when(storageLocationRepository.findDefault(HOUSEHOLD)).thenReturn(Optional.of(PANTRY));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var command = new AddPantryItemCommand(rice.getItemName(), rice.getQuantity(), null, null, null);

        PantryItem added = service.add(command);

        assertThat(added.getItemName()).isEqualTo(rice.getItemName());
        assertThat(added.getQuantity()).isEqualTo(rice.getQuantity());
        assertThat(added.getStorageLocationId()).isEqualTo(PANTRY.getStorageLocationId());
        assertThat(added.getHouseholdId()).isEqualTo(HOUSEHOLD);
        verify(repository).save(added);
    }

    @Test
    void addWithGivenLocationUsesIt() {
        StorageLocation cellar = StorageLocation.create(HOUSEHOLD, new LocationName("Keller"));
        when(storageLocationRepository.findById(cellar.getStorageLocationId())).thenReturn(Optional.of(cellar));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PantryItem added = service.add(new AddPantryItemCommand(
                rice.getItemName(), rice.getQuantity(), null, cellar.getStorageLocationId(), null));

        assertThat(added.getStorageLocationId()).isEqualTo(cellar.getStorageLocationId());
    }

    @Test
    void addWithLocationOfAnotherHouseholdFails() {
        StorageLocation foreignCellar = StorageLocation.create(OTHER_HOUSEHOLD, new LocationName("Keller"));
        when(storageLocationRepository.findById(foreignCellar.getStorageLocationId()))
                .thenReturn(Optional.of(foreignCellar));
        var command = new AddPantryItemCommand(
                rice.getItemName(), rice.getQuantity(), null, foreignCellar.getStorageLocationId(), null);

        assertThatExceptionOfType(StorageLocationNotFoundException.class)
                .isThrownBy(() -> service.add(command));
        verify(repository, never()).save(any());
    }

    @Test
    void addWithUnknownLocationFails() {
        StorageLocationId unknown = StorageLocationId.newId();
        when(storageLocationRepository.findById(unknown)).thenReturn(Optional.empty());
        var command = new AddPantryItemCommand(rice.getItemName(), rice.getQuantity(), null, unknown, null);

        assertThatExceptionOfType(StorageLocationNotFoundException.class)
                .isThrownBy(() -> service.add(command));
        verify(repository, never()).save(any());
    }

    @Test
    void changeQuantityUpdatesAndSavesItem() {
        when(repository.findById(rice.getPantryItemId())).thenReturn(Optional.of(rice));
        when(repository.save(rice)).thenReturn(rice);

        PantryItem changed = service.changeQuantity(rice.getPantryItemId(), new BigDecimal("0.5"));

        assertThat(changed.getQuantity()).isEqualTo(new Quantity(new BigDecimal("0.5"), Unit.KILOGRAM));
    }

    @Test
    void changeQuantityOfUnknownItemFails() {
        PantryItemId unknown = PantryItemId.newId();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.changeQuantity(unknown, BigDecimal.ONE));
    }

    @Test
    void removeOfUnknownItemFails() {
        PantryItemId unknown = PantryItemId.newId();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.remove(unknown));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void removeDeletesItemOfTheHousehold() {
        when(repository.findById(rice.getPantryItemId())).thenReturn(Optional.of(rice));

        service.remove(rice.getPantryItemId());

        verify(repository).deleteById(rice.getPantryItemId());
    }

    @Test
    void itemOfAnotherHouseholdCountsAsNotFound() {
        PantryItem foreign = PantryItem.create(OTHER_HOUSEHOLD, new ItemName("Fremder Reis"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, StorageLocationId.newId());
        when(repository.findById(foreign.getPantryItemId())).thenReturn(Optional.of(foreign));

        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.changeQuantity(foreign.getPantryItemId(), BigDecimal.TEN));
        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.remove(foreign.getPantryItemId()));
        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.restock(foreign.getPantryItemId(), BigDecimal.ONE));
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
        assertThat(foreign.getQuantity().amount()).isEqualByComparingTo("1");
    }

    @Test
    void userWithoutHouseholdCannotReadOrChangeAnything() {
        when(currentHousehold.require()).thenThrow(new NoHouseholdException());

        assertThatExceptionOfType(NoHouseholdException.class)
                .isThrownBy(() -> service.search(new PantryItemFilter(null, false), new PageQuery(0, 20)));
        assertThatExceptionOfType(NoHouseholdException.class)
                .isThrownBy(() -> service.add(new AddPantryItemCommand(
                        rice.getItemName(), rice.getQuantity(), null, null, null)));
        verifyNoInteractions(repository, storageLocationRepository, eventPublisher);
    }

    @Test
    void listExpiringWithinLooksAheadFromToday() {
        when(repository.findWithExpiryDateUpTo(HOUSEHOLD, LocalDate.of(2027, 3, 5))).thenReturn(List.of(rice));

        List<ExpiringPantryItem> expiring = service.listExpiringWithin(7);

        assertThat(expiring).singleElement().satisfies(entry -> {
            assertThat(entry.item()).isEqualTo(rice);
            assertThat(entry.daysUntilExpiry()).isEqualTo(3);
            assertThat(entry.expired()).isFalse();
        });
    }

    @Test
    void listExpiringWithinMarksExpiredItems() {
        PantryItem oldMilk = PantryItem.create(HOUSEHOLD, new ItemName("Milch"), new Quantity(BigDecimal.ONE, Unit.LITER),
                LocalDate.of(2027, 2, 24), PANTRY.getStorageLocationId());
        when(repository.findWithExpiryDateUpTo(HOUSEHOLD, LocalDate.of(2027, 2, 26))).thenReturn(List.of(oldMilk));

        List<ExpiringPantryItem> expiring = service.listExpiringWithin(0);

        assertThat(expiring).singleElement().satisfies(entry -> {
            assertThat(entry.daysUntilExpiry()).isEqualTo(-2);
            assertThat(entry.expired()).isTrue();
        });
    }

    @Test
    void listExpiringWithinRejectsNegativeDays() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.listExpiringWithin(-1));
    }

    @Test
    void addWithMinimumAmountSetsIt() {
        when(storageLocationRepository.findDefault(HOUSEHOLD)).thenReturn(Optional.of(PANTRY));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PantryItem added = service.add(new AddPantryItemCommand(
                rice.getItemName(), rice.getQuantity(), null, null, new BigDecimal("2")));

        assertThat(added.getMinimumQuantity()).isEqualTo(new Quantity(new BigDecimal("2"), Unit.KILOGRAM));
        assertThat(added.isBelowMinimum()).isTrue();
    }

    @Test
    void changeMinimumAmountUpdatesAndSavesItem() {
        when(repository.findById(rice.getPantryItemId())).thenReturn(Optional.of(rice));
        when(repository.save(rice)).thenReturn(rice);

        PantryItem changed = service.changeMinimumAmount(rice.getPantryItemId(), new BigDecimal("3"));

        assertThat(changed.getMinimumQuantity().amount()).isEqualByComparingTo("3");
    }

    @Test
    void changeMinimumAmountOfUnknownItemFails() {
        PantryItemId unknown = PantryItemId.newId();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.changeMinimumAmount(unknown, BigDecimal.ONE));
    }

    @Test
    void searchPassesFilterAndPageToRepository() {
        PantryItemFilter filter = new PantryItemFilter(PANTRY.getStorageLocationId(), true);
        PageQuery page = new PageQuery(1, 10);
        ResultPage<PantryItem> expected = new ResultPage<>(List.of(rice), 1, 10, 11);
        when(repository.search(HOUSEHOLD, filter, page)).thenReturn(expected);

        assertThat(service.search(filter, page)).isEqualTo(expected);
    }

    @Test
    void changeQuantityPublishesEventsOfTheItem() {
        rice.changeMinimumAmount(new BigDecimal("0.5"));
        rice.pullDomainEvents();
        when(repository.findById(rice.getPantryItemId())).thenReturn(Optional.of(rice));
        when(repository.save(rice)).thenReturn(rice);

        service.changeQuantity(rice.getPantryItemId(), new BigDecimal("0.2"));

        verify(eventPublisher).publish(List.of(new PantryItemFellBelowMinimum(rice.getPantryItemId())));
    }

    @Test
    void addBelowMinimumPublishesEvent() {
        when(storageLocationRepository.findDefault(HOUSEHOLD)).thenReturn(Optional.of(PANTRY));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PantryItem added = service.add(new AddPantryItemCommand(
                rice.getItemName(), rice.getQuantity(), null, null, new BigDecimal("2")));

        verify(eventPublisher).publish(List.of(new PantryItemFellBelowMinimum(added.getPantryItemId())));
    }

    @Test
    void restockAddsAmountAndPublishesEvents() {
        rice.changeMinimumAmount(new BigDecimal("2"));
        rice.pullDomainEvents();
        when(repository.findById(rice.getPantryItemId())).thenReturn(Optional.of(rice));
        when(repository.save(rice)).thenReturn(rice);

        PantryItem restocked = service.restock(rice.getPantryItemId(), BigDecimal.ONE);

        assertThat(restocked.getQuantity().amount()).isEqualByComparingTo("2");
        verify(eventPublisher).publish(List.of(new PantryItemReachedMinimum(rice.getPantryItemId())));
    }

    @Test
    void restockOfUnknownItemFails() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.restock(PantryItemId.newId(), BigDecimal.ONE));
    }
}
