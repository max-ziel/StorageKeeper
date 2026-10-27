package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class StorageLocationServiceTest {

    @Mock
    private StorageLocationRepository repository;

    @InjectMocks
    private StorageLocationService service;

    @Test
    void addCreatesAndSavesLocation() {
        LocationName cellar = new LocationName("Keller");

        when(repository.existsByName(cellar)).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        StorageLocation added = service.add(cellar);

        assertThat(added.getLocationName()).isEqualTo(cellar);
        assertThat(added.isDefault()).isFalse();

        verify(repository).save(added);
    }

    @Test
    void addWithExistingNameFails() {
        LocationName cellar = new LocationName("Keller");

        when(repository.existsByName(cellar)).thenReturn(true);

        assertThatExceptionOfType(StorageLocationAlreadyExistsException.class).isThrownBy(() -> service.add(cellar));

        verify(repository, never()).save(any());
    }
}
