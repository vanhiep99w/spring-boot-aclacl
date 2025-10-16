# Spring Boot ACL Demo

## Overview

This project is a scaffold for exploring Access Control Lists (ACL) in a Spring Boot 3.x application. It will demonstrate how to apply fine-grained authorization rules to domain objects using Spring Security.

## Tech Stack

- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- H2 in-memory database
- Bean Validation (Jakarta Validation)
- Lombok
- Spring Boot Actuator

## Modules

The application is structured using the `com.example.acl` base package with the following sub-packages:

- `config` – application-wide configuration classes
- `domain` – domain entities and value objects
- `repository` – data access components
- `service` – business logic services
- `web` – REST controllers and web entry points
- `security` – security and ACL components

## Running the Application

Ensure Java 17+ and Maven 3.8+ are available locally, then execute:

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

### Helpful Endpoints

- Actuator health: `http://localhost:8080/actuator/health`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:acldb`, username: `sa`, password: ` `)

## Next Steps

This scaffold is ready for implementing ACL-backed security, domain modeling, and API endpoints.
