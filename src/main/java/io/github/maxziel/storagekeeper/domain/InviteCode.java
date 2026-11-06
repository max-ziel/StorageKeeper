package io.github.maxziel.storagekeeper.domain;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Code with which a user joins a {@link Household}, e.g. {@code K7M2-QX9P}. Whoever knows it can join,
 * so it is random and long enough not to be guessed: 8 characters from an alphabet of 32 give about 10^12
 * possible codes.
 *
 * <p>Invariants: two groups of four characters separated by a dash; only upper-case letters and digits
 * without the easily confused {@code 0}, {@code O}, {@code 1} and {@code I}. Input is converted to upper case
 * and trimmed, so {@code " k7m2-qx9p "} is accepted.
 */
public record InviteCode(String value) {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern FORMAT = Pattern.compile("[" + ALPHABET + "]{4}-[" + ALPHABET + "]{4}");
    private static final RandomGenerator RANDOM = new SecureRandom();

    public InviteCode {
        if (value == null) {
            throw new IllegalArgumentException("invite code must not be null");
        }
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invite code must look like XXXX-XXXX");
        }
    }

    /** Creates a new random code. */
    public static InviteCode generate() {
        return generate(RANDOM);
    }

    static InviteCode generate(RandomGenerator random) {
        StringBuilder code = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                code.append('-');
            }
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new InviteCode(code.toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
