package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ItemNameTest {

    @Test
    void stripsSurroundingWhitespace() {
        assertThat(new ItemName("   Reis   ").value()).isEqualTo("Reis");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankNames(String blank) {
        assertThatIllegalArgumentException().isThrownBy(() -> new ItemName(blank));
    }

    @Test
    void rejectsNamesLongerThanMaxLength() {
        String tooLong = "x".repeat(ItemName.MAX_LENGTH + 1);
        assertThatIllegalArgumentException().isThrownBy(() -> new ItemName(tooLong));
    }
}
