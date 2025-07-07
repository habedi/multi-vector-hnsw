package com.example.projectname.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** The main command for the application, handled by picocli. */
@Command(
        name = "app",
        mixinStandardHelpOptions = true,
        version = "App 1.0",
        description = "Prints a greeting message.")
public class MainCommand implements Callable<Integer> {

    @Option(
            names = {"-n", "--name"},
            description = "The name to greet.",
            defaultValue = "World")
    private String name;

    @Parameters(index = "0", description = "An optional message to display.", arity = "0..1")
    private String customMessage;

    private String greeting;

    @Override
    public Integer call() {
        if (customMessage != null && !customMessage.isEmpty()) {
            greeting = customMessage;
        } else {
            greeting = "Hello, " + name + "!";
        }
        return 0; // Success
    }

    public String getGreeting() {
        return greeting;
    }
}
