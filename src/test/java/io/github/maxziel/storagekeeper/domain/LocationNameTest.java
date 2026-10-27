package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LocationNameTest {

    @Test
    void stripsSurroundingWhitespace() {
        assertThat(new LocationName("  Keller  ").value()).isEqualTo("Keller");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankNames(String blank) {
        assertThatIllegalArgumentException().isThrownBy(() -> new LocationName(blank));
    }

    @Test
    void rejectsNamesLongerThanMaxLength() {
        String tooLong = "x".repeat(LocationName.MAX_LENGTH + 1);

        assertThatIllegalArgumentException().isThrownBy(() -> new LocationName(tooLong));
    }
}
