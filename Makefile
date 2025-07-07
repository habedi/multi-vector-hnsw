# Use the Maven wrapper if it exists, otherwise fall back to a system-wide mvn
MVN := $(if $(wildcard ./mvnw),./mvnw,mvn)
SHELL := /bin/bash

# Default target executed when 'make' is run without arguments
.DEFAULT_GOAL := help

# Phony targets don't represent files
.PHONY: help build package test format format-check lint clean setup-hooks test-hooks

help: ## Show this help message
	@echo "Usage: make <target>"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' Makefile | \
	awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Run the full Maven build lifecycle (compile, check, test, and package)
	@echo "Building project and running all checks..."
	@$(MVN) -B verify

package: ## Compile and package the application into a JAR file
	@echo "Packaging application..."
	@$(MVN) -B package

test: ## Run all the tests
	@echo "Running tests..."
	@$(MVN) -B test

format: ## Format Java source files
	@echo "Formatting source code..."
	@$(MVN) -B spotless:apply

format-check: ## Check code formatting without applying changes
	@echo "Checking code formatting..."
	@$(MVN) -B spotless:check

lint: ## Check code style
	@echo "Checking code style..."
	@$(MVN) -B checkstyle:check

clean: ## Remove all build artifacts
	@echo "Cleaning project..."
	@$(MVN) -B clean

setup-hooks: ## Set up pre-commit hooks
	@echo "Setting up pre-commit hooks..."
	@if ! command -v pre-commit &> /dev/null; then \
		echo "pre-commit not found. Please install it using 'pip install pre-commit'"; \
		exit 1; \
	fi
	@pre-commit install --install-hooks

test-hooks: ## Test pre-commit hooks on all files
	@echo "Testing pre-commit hooks..."
	@./scripts/test-pre-commit.sh
