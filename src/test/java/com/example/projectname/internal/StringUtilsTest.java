package com.example.projectname.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StringUtilsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    void isNullOrBlank_withNullEmptyOrWhitespace_shouldReturnTrue(String input) {
        assertTrue(StringUtils.isNullOrBlank(input));
    }

    @Test
    void isNullOrBlank_withNonEmptyString_shouldReturnFalse() {
        assertFalse(StringUtils.isNullOrBlank("text"));
    }

    @Test
    void isNullOrBlank_withNonEmptyStringWithSpaces_shouldReturnFalse() {
        assertFalse(StringUtils.isNullOrBlank("  text  "));
    }

    @ParameterizedTest
    @CsvSource({"hello,olleh", "Java,avaJ", "racecar,racecar", "A,A"})
    void reverse_withValidStrings_shouldReturnReversed(String input, String expected) {
        assertEquals(expected, StringUtils.reverse(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void reverse_withNullOrEmpty_shouldReturnOriginal(String input) {
        assertSame(input, StringUtils.reverse(input));
    }
}
