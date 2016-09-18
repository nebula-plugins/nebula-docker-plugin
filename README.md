# Gradle Resolution Rules Plugin


Gradle resolution strategies and module metadata provide an effective way to solve the most common dependency issues, however sharing these rules between projects is cumbersome, and requires custom plugins or `apply from` calls. This plugin provides general purpose rule types, allowing rules to be published, versioned, shared between projects, and optionally [dependency locked](https://github.com/nebula-plugins/gradle-dependency-lock-plugin).

These rule types solve the most common cause of dependency issues in projects, including:

- Duplicate classes caused by changes to group or artifact ids, without renaming packages
- Duplicate classes caused by bundle dependencies, which do not conflict resolve against the normal dependencies for that library
- Lack of version alignment between libraries, where version alignment is needed for compatibility
- Ensuring a minimum version of a library

# Quick Start

Refer to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/nebula.resolution-rules) for instructions on how to apply the plugin.

## Rules

We produce a rules for dependencies found in Maven Central and other public repositories, to use those rules in your project add:

```groovy
dependencies {
    resolutionRules 'com.netflix.nebula:gradle-resolution-rules:latest.release'
}
```

See the [gradle-resolution-rules](https://github.com/nebula-plugins/gradle-resolution-rules) project for details of the rules, and instructions on how to enable optional rule sets.

# Documentation

The project wiki contains the [full documentation](https://github.com/nebula-plugins/gradle-resolution-rules-plugin/wiki) for the plugin.
