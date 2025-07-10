#!/bin/bash
# Script to test pre-commit hooks without committing

set -e

echo "Testing pre-commit hooks..."

# Check if pre-commit is installed
if ! command -v pre-commit &> /dev/null; then
    echo "Error: pre-commit is not installed. Please install it with 'pip install pre-commit'"
    exit 1
fi

# Check if pre-commit hooks are installed
if [ ! -f .git/hooks/pre-commit ]; then
    echo "Pre-commit hooks are not installed. Installing now..."
    pre-commit install --install-hooks
fi

# Run pre-commit on all files
echo "Running pre-commit on all files..."
pre-commit run --all-files

echo "All pre-commit hooks passed!"
