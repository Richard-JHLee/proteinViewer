#!/bin/bash

# macOS Android Build Script - Ultimate Version
# This script completely prevents macOS metadata file creation and handles all edge cases

echo "🔧 Setting up macOS environment for Android build..."

# Set environment variables to completely prevent macOS metadata creation
export COPYFILE_DISABLE=1
export COPY_EXTENDED_ATTRIBUTES_DISABLE=1
export TMPDIR=/tmp
export TEMP=/tmp

# Additional macOS-specific settings
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# Clean up any existing metadata files recursively
echo "🧹 Cleaning up macOS metadata files..."
find . -name "._*" -type f -delete 2>/dev/null || true
find . -name ".DS_Store" -type f -delete 2>/dev/null || true

# Clean build directories completely
echo "🗑️  Cleaning build directories..."
rm -rf .gradle build app/build 2>/dev/null || true

# Create a clean environment
echo "🌱 Creating clean build environment..."
mkdir -p .gradle
mkdir -p app/build

# Set file permissions
echo "🔐 Setting file permissions..."
chmod -R 755 .gradle 2>/dev/null || true
chmod -R 755 app/build 2>/dev/null || true

# Run Gradle build with metadata prevention
echo "🚀 Starting Android build with metadata prevention..."
./gradlew clean compileDebugKotlin --no-daemon --no-build-cache --stacktrace

echo "✅ Build completed!"
