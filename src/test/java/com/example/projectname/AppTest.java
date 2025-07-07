package com.example.projectname;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void applicationRunsWithoutException() {
        int exitCode = App.run(new String[] {"--name", "Test"});
        assertEquals(0, exitCode, "Application should exit with code 0 on success.");
    }

    @Test
    void applicationPrintsGreeting() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));

        try {
            App.run(new String[] {"--name", "JUnit"});
            String output = bos.toString();
            assertTrue(output.contains("Hello, JUnit!"), "Output should contain the greeting.");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void applicationPrintsCustomMessage() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));

        try {
            App.run(new String[] {"Custom Message Here"});
            String output = bos.toString();
            assertTrue(
                    output.contains("Custom Message Here"),
                    "Output should contain the custom message.");
        } finally {
            System.setOut(originalOut);
        }
    }
}
