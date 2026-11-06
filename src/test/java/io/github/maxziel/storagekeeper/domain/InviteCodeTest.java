package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InviteCodeTest {

    @Test
    void generatedCodesHaveTheExpectedFormat() {
        for (int i = 0; i < 100; i++) {
            assertThat(InviteCode.generate().value()).matches("[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}");
        }
    }

    @Test
    void generatedCodesDiffer() {
        Set<InviteCode> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(InviteCode.generate());
        }

        assertThat(codes).hasSize(1000);
    }

    @Test
    void everyCharacterOfTheAlphabetCanOccur() {
        RandomGenerator random = new Random(42);
        Set<Character> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            InviteCode.generate(random).value().replace("-", "").chars().forEach(c -> seen.add((char) c));
        }

        assertThat(seen).hasSize(InviteCode.ALPHABET.length());
    }

    @Test
    void inputIsTrimmedAndConvertedToUpperCase() {
        assertThat(new InviteCode(" k7m2-qx9p ").value()).isEqualTo("K7M2-QX9P");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "K7M2QX9P", "K7M2-QX9", "K7M2-QX9PP", "K0M2-QX9P", "KIM2-QX9P", "K7M2_QX9P"})
    void rejectsInvalidCodes(String code) {
        assertThatIllegalArgumentException().isThrownBy(() -> new InviteCode(code));
    }
}
