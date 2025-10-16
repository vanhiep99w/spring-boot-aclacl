# Spring Boot ACL Demo

## Project Goal

This project demonstrates the implementation of Access Control List (ACL) functionality in a Spring Boot 3.x application. The goal is to showcase fine-grained authorization and security controls using Spring Security ACL, allowing for object-level permissions and access management.

## Overview

The application provides a foundation for building secure REST APIs with granular access control. It uses:
- Spring Boot 3.x
- Spring Security for authentication and authorization
- Spring Data JPA for data persistence
- H2 in-memory database for development
- Lombok for reducing boilerplate code
- Spring Boot Actuator for monitoring

## Project Structure

```
src/main/java/com/example/acl/
├── config/          - Configuration classes
├── domain/          - Domain entities
├── repository/      - Data repositories
├── service/         - Business logic services
├── web/             - REST controllers
└── security/        - Security configurations and ACL components
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Running the Application

Build and run the application using Maven:

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

### H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:acldb`
- Username: `sa`
- Password: (leave empty)

### Actuator Endpoints

Health and metrics endpoints are available at:
- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

## Development

This is a scaffold project. Future development will include:
- User and role management
- ACL-based authorization
- Domain objects with permission controls
- REST API endpoints
- Integration tests

## License

This is a demo project for educational purposes.
