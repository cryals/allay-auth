package dev.allayauth.util;

import java.security.SecureRandom;

public final class CodeGenerator {
    private final SecureRandom random = new SecureRandom();
    private final String alphabet;
    private final int length;
    private final String format;

    public CodeGenerator(String alphabet, int length, String format) {
        this.alphabet = alphabet;
        this.length = length;
        this.format = format;
    }

    public String generate() {
        StringBuilder raw = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            raw.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        StringBuilder formatted = new StringBuilder(format.length());
        int rawIndex = 0;
        for (int index = 0; index < format.length(); index++) {
            char c = format.charAt(index);
            if (c == 'X' && rawIndex < raw.length()) {
                formatted.append(raw.charAt(rawIndex++));
            } else {
                formatted.append(c);
            }
        }
        while (rawIndex < raw.length()) {
            formatted.append(raw.charAt(rawIndex++));
        }
        return formatted.toString();
    }
}
