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

    /** Characters per group; a code has two groups. */
    public static final int GROUP_SIZE = 4;
    /** Length of a code including the dash, e.g. 9 for {@code K7M2-QX9P}. */
    public static final int LENGTH = 2 * GROUP_SIZE + 1;

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String GROUP = "[" + ALPHABET + "]{" + GROUP_SIZE + "}";
    private static final Pattern FORMAT = Pattern.compile(GROUP + "-" + GROUP);
    private static final RandomGenerator RANDOM = new SecureRandom();

    public InviteCode {
        if (value == null) {
            throw new DomainValidationException("invite code must not be null");
        }
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("invite code must look like XXXX-XXXX");
        }
    }

    /** Creates a new random code. */
    public static InviteCode generate() {
        return generate(RANDOM);
    }

    static InviteCode generate(RandomGenerator random) {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < 2 * GROUP_SIZE; i++) {
            if (i == GROUP_SIZE) {
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
