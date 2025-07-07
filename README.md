# Java Project Template

<div align="center">
  <picture>
    <img alt="template-java-project logo" src="logo.svg" height="50%" width="50%">
  </picture>
</div>
<br>

[![Tests](https://img.shields.io/github/actions/workflow/status/habedi/template-java-project/tests.yml?label=tests&style=flat&labelColor=282c34&color=4caf50&logo=github)](https://github.com/habedi/template-java-project/actions/workflows/tests.yml)
[![Code Coverage](https://img.shields.io/codecov/c/github/habedi/template-java-project?style=flat&labelColor=282c34&color=ffca28&logo=codecov)](https://codecov.io/gh/habedi/template-java-project)
[![Code Quality](https://img.shields.io/codefactor/grade/github/habedi/template-java-project?style=flat&labelColor=282c34&color=4caf50&logo=codefactor)](https://www.codefactor.io/repository/github/habedi/template-java-project)
[![Docs](https://img.shields.io/badge/docs-latest-007ec6?style=flat&labelColor=282c34&logo=readthedocs)](docs)
[![License](https://img.shields.io/badge/license-MIT%2FApache--2.0-007ec6?style=flat&labelColor=282c34&logo=open-source-initiative)](https://github.com/habedi/template-java-project)
[![Release](https://img.shields.io/github/release/habedi/template-java-project.svg?style=flat&labelColor=282c34&color=f46623&logo=github)](https://github.com/habedi/template-java-project/releases/latest)

This is a template for Java projects.
It provides a minimalistic project structure with pre-configured GitHub Actions, Makefile, and a few useful
configuration files.
I share it here in case it might be useful to others.

## Features

- **Modern Java Development**: Built for Java 21 with Maven as the build system
- **Well-organized Project Structure**: Standard Maven directory layout with separate source and test directories
- **Pre-configured Tools**:
    - **Code Quality**: Spotless (Google Java Style) and Checkstyle for consistent code style
    - **Testing**: JUnit Jupiter for unit testing with JaCoCo for code coverage
    - **Logging**: Log4j configured and ready to use
    - **CLI Support**: Picocli for building command-line applications
    - **Benchmarking**: JMH for performance testing
- **CI/CD Integration**: Pre-configured GitHub Actions for running tests and making releases
- **Development Workflow**: Makefile for managing common tasks (build, test, format, lint)
- **Quality Assurance**: Pre-commit hooks to ensure code quality before commits
- **Documentation**: Ready-to-use templates for project documentation
- **Licensing**: Dual-licensed under MIT and Apache 2.0

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.8+ (or use the included Maven wrapper)
- Git
- (Optional) Python 3.6+ for pre-commit hooks

### Using This Template

1. Click the "Use this template" button on GitHub or clone the repository:
   ```bash
   git clone https://github.com/habedi/template-java-project.git my-project
   cd my-project
   ```

2. Update project information in `pom.xml`:
    - Change `groupId`, `artifactId`, and `version`
    - Update `name`, `description`, and `url`
    - Modify the main class path in the Maven Shade Plugin configuration

3. Build the project:
   ```bash
   make build
   ```

4. Run the application:
   ```bash
   java -jar target/project-name-0.1.0-SNAPSHOT.jar
   ```

## Project Structure

```
├── src/
│   ├── main/java/         # Application source code
│   ├── main/resources/    # Application resources
│   ├── test/java/         # Test source code
│   └── jmh/java/          # Benchmarking code
├── docs/                  # Documentation
├── examples/              # Example code
├── scripts/               # Utility scripts
├── pom.xml                # Maven configuration
├── Makefile               # Common tasks
└── README.md              # This file
```

## Development Workflow

This project uses `make` to manage common development tasks:

```bash
make build        # Compile, test, and package the application
make test         # Run all tests
make format       # Format source code using Spotless
make lint         # Check code style with Checkstyle
make clean        # Remove build artifacts
make help         # Show all available commands
```

### Pre-commit Hooks

This project uses [pre-commit](https://pre-commit.com/) to run checks before each commit, ensuring code quality and
consistency. The hooks include:

- Code formatting checks
- Code style checks
- Basic Git checks (trailing whitespace, merge conflicts, etc.)
- Tests (on push)

To set up the pre-commit hooks:

1. Install pre-commit:
   ```bash
   pip install pre-commit
   ```

2. Set up the hooks:
   ```bash
   make setup-hooks
   ```

Once installed, the hooks will run automatically on `git commit` and `git push`.

You can also manually test the hooks on all files without committing:

```bash
make test-hooks
```

## Dependencies

This template includes the following key dependencies:

- **Logging**: [Log4j 2](https://logging.apache.org/log4j/2.x/) for application logging
- **CLI**: [Picocli](https://picocli.info/) for command-line interface support
- **Testing**: [JUnit 5](https://junit.org/junit5/) for unit testing
- **Benchmarking**: [JMH](https://github.com/openjdk/jmh) for performance benchmarking

Additional development dependencies:

- **Code Style**: [Spotless](https://github.com/diffplug/spotless) with Google Java Style
- **Code Quality**: [Checkstyle](https://checkstyle.org/) for static code analysis
- **Code Coverage**: [JaCoCo](https://www.jacoco.org/jacoco/) for test coverage reporting

## Continuous Integration

This template includes GitHub Actions workflows for:

- Running tests on multiple Java versions
- Checking code style and quality
- Building and publishing releases
- Generating code coverage reports

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for details on:

- How to report bugs or suggest features
- Development workflow and code style guidelines
- Pull request process

## License

This project is available under either of the following licenses:

* MIT License ([LICENSE-MIT](LICENSE-MIT) or https://opensource.org/licenses/MIT)
* Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or https://www.apache.org/licenses/LICENSE-2.0)

You can choose which license you want to use.
