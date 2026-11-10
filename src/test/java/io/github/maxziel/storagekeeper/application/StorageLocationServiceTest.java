package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class StorageLocationServiceTest {

    private static final HouseholdId HOUSEHOLD = HouseholdId.newId();

    @Mock
    private StorageLocationRepository repository;
    @Mock
    private CurrentHousehold currentHousehold;

    private StorageLocationService service;

    @BeforeEach
    void setUp() {
        lenient().when(currentHousehold.require()).thenReturn(HOUSEHOLD);
        service = new StorageLocationService(repository, currentHousehold);
    }

    @Test
    void addCreatesAndSavesLocationInTheCurrentHousehold() {
        LocationName cellar = new LocationName("Keller");

        when(repository.existsByName(HOUSEHOLD, cellar)).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        StorageLocation added = service.add(cellar);

        assertThat(added.getLocationName()).isEqualTo(cellar);
        assertThat(added.isDefault()).isFalse();
        assertThat(added.getHouseholdId()).isEqualTo(HOUSEHOLD);

        verify(repository).save(added);
    }

    @Test
    void addWithExistingNameFails() {
        LocationName cellar = new LocationName("Keller");

        when(repository.existsByName(HOUSEHOLD, cellar)).thenReturn(true);

        assertThatExceptionOfType(StorageLocationAlreadyExistsException.class).isThrownBy(() -> service.add(cellar));

        verify(repository, never()).save(any());
    }

    @Test
    void listAllReturnsTheLocationsOfTheCurrentHousehold() {
        List<StorageLocation> locations = List.of(StorageLocation.createDefault(HOUSEHOLD));
        when(repository.findAll(HOUSEHOLD)).thenReturn(locations);

        assertThat(service.listAll()).isEqualTo(locations);
    }

    @Test
    void userWithoutHouseholdCannotListOrAdd() {
        when(currentHousehold.require()).thenThrow(new NoHouseholdException());

        assertThatExceptionOfType(NoHouseholdException.class).isThrownBy(() -> service.listAll());
        assertThatExceptionOfType(NoHouseholdException.class)
            .isThrownBy(() -> service.add(new LocationName("Keller")));
        verifyNoInteractions(repository);
    }
}
