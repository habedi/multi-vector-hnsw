package com.example.projectname;

import com.example.projectname.cli.MainCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

/** The main entry point for the application. */
public final class App {
    private static final Logger LOGGER = LogManager.getLogger(App.class);

    private App() {}

    /**
     * The main method that serves as the application's entry point.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        int exitCode = run(args);
        System.exit(exitCode);
    }

    /**
     * Runs the application logic without exiting the JVM. This is the primary method to be used for
     * testing.
     *
     * @param args Command line arguments.
     * @return The exit code of the application.
     */
    public static int run(String[] args) {
        LOGGER.info("Application logic starting.");
        MainCommand mainCommand = new MainCommand();
        CommandLine cmd = new CommandLine(mainCommand);

        int exitCode = cmd.execute(args);

        String greeting = mainCommand.getGreeting();
        if (greeting != null) {
            System.out.println(greeting);
        }

        LOGGER.info("Application logic finished with exit code: {}", exitCode);
        return exitCode;
    }
}
