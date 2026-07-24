# AGENTS for Glowbook

## Purpose
This file helps AI agents understand the Glowbook repository structure, build commands, and key conventions so they can be productive quickly.

## Repository layout
- Root contains a single Maven-based Spring Boot project under `glowbook/`.
- Main application module: `glowbook/pom.xml`.
- Primary source: `glowbook/src/main/java/glowbook/`.
- Tests: `glowbook/src/test/java/glowbook/`.
- Runtime configuration: `glowbook/src/main/resources/application.properties`.
- Test configuration: `glowbook/src/test/resources/application.properties`.

## Build and test commands
Use the Maven wrapper from the `glowbook/` directory.
- Windows: `cd glowbook && .\mvnw.cmd clean test`
- Windows compile only: `cd glowbook && .\mvnw.cmd clean test-compile`
- Cross-platform: `cd glowbook && ./mvnw clean test`

## Important technologies
- Java 25 (declared in `glowbook/pom.xml`)
- Spring Boot 4.1.0
- Lombok (annotation processing configured in `pom.xml`)
- Spring Security, Spring Data JPA, Spring MVC

## Key files
- `glowbook/src/main/java/glowbook/GlowbookApplication.java`
- `glowbook/src/main/java/glowbook/config/SecurityConfig.java`
- `glowbook/src/main/java/glowbook/controller/` (web controllers)
- `glowbook/src/main/java/glowbook/entity/` (JPA entities)
- `glowbook/src/main/java/glowbook/service/` (business logic)
- `glowbook/src/test/java/glowbook/GlowbookApplicationTests.java`
- `glowbook/HELP.md` for Spring Boot plugin and reference links

## Conventions
- Prefer small, targeted changes that preserve Spring Boot conventions.
- Do not bypass the project wrapper; use `mvnw`/`mvnw.cmd` so builds remain consistent.
- Keep Lombok annotations intact; update code only when required by compatibility or security.
- When changing config or dependency versions, also verify tests via wrapper commands.

## Notes
- No existing `.github/copilot-instructions.md` or `AGENTS.md` was present before this file.
- If you need more detail, inspect `glowbook/README.md` and `glowbook/HELP.md`.
