# Contribution Guidelines

Thank you for considering contributing to this project.
All contributions are welcome.

## How to Contribute

Please check the [issue tracker](https://github.com/habedi/template-java-project/issues) to see if there is a relevant
issue you would like to work on.

### Reporting Bugs

1. Open an issue on the issue tracker.
2. Include steps to reproduce the behavior and any relevant logs or screenshots.

### Suggesting Features

1. Open an issue on the issue tracker.
2. Provide details about the feature, its purpose, and potential implementation ideas.

## Development Workflow

This project uses `make` to manage common tasks.

### Code Style

- This project uses **Spotless** with the Google Java Style guide.
- Run `make format` to automatically format the code before committing.
- The CI build will fail if the code is not formatted correctly. You can check this with `make format-check`.

### Running Tests

- Use `make test` to run all unit tests.
- Make sure all tests pass before submitting a pull request.

### Running Linters

- Use `make lint` to run the Checkstyle static analysis tool.
- Address any reported issues before submitting.

### Available Commands

- Run `make help` to see all available commands.

> [!IMPORTANT]
> Unless you explicitly state otherwise, any contribution you intentionally submit for inclusion in the work, as defined
> in the Apache-2.0 license, shall be dual-licensed, without any additional terms or conditions.

## Code of Conduct

We adhere to the [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/) version 2.1.
