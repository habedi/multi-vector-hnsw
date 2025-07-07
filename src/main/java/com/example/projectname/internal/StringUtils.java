package com.example.projectname.internal;

/** A utility class for string manipulation. */
public final class StringUtils {

    private StringUtils() {}

    /**
     * Checks if a string is null, empty, or consists only of whitespace characters.
     *
     * @param str The string to check.
     * @return {@code true} if the string is null, empty, or whitespace; {@code false} otherwise.
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Reverses a given string.
     *
     * @param str The string to reverse.
     * @return The reversed string, or the original string if it was null or blank.
     */
    public static String reverse(String str) {
        if (isNullOrBlank(str)) {
            return str;
        }
        return new StringBuilder(str).reverse().toString();
    }
}
