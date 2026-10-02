# Ticketline 4.0

**Ticketline 4.0** is a full-stack event ticketing platform: browse venues and events, pick seats on an interactive seat map, reserve or purchase tickets, and receive QR-code tickets and PDF invoices by e-mail.

The project was developed as part of the *Software Engineering Project* course at TU Wien by a team of six, following an agile workflow with merge requests, code reviews, a Definition of Done, and a three-stage CI pipeline. This repository contains the complete application — backend, frontend, and end-to-end tests.

## Key Features

- **Event discovery** — venues, halls, events, artists and genres with a fast search
- **Seat map editor** — admins can design hall layouts, sectors and seats
- **Seat holds** — selected seats are held for 15 minutes with an automatic cleanup job
- **Booking flows** — purchase, time-limited reservation, and purchase from an existing reservation
- **Documents** — PDF tickets with QR codes, invoices and cancellation invoices
- **Authentication** — JWT-based login with `USER` / `ADMIN` roles, password reset via e-mail, account locking
- **E-mail notifications** — order confirmations, expiring reservations, cancellations (async SMTP)
- **News & statistics** — news feed plus top-10 event sales charts per month and genre
- **Demo data** — a full sample dataset can be seeded via the `generateData` Spring profile
- **API documentation** — Swagger UI via springdoc-openapi

## Tech Stack

| Layer     | Technologies |
|-----------|--------------|
| Backend   | Java 25, Spring Boot 4, Spring Security (JJWT), Spring Data JPA, H2, MapStruct, springdoc-openapi, OpenPDF + QRCodeGen |
| Frontend  | Angular 21, TypeScript 6, Bootstrap 5 / ng-bootstrap, RxJS |
| Testing   | JUnit 5, Karma + Jasmine, Cypress (E2E) |
| Quality   | Checkstyle (Google Java Style), ESLint (angular-eslint), Conventional Commits |
| Ops       | Docker, Docker Compose, # Ticketline 4.0

**Ticketline 4.0** is a full-stack event ticketing platform: browse venues and events, pick seats on an interactive seat map, reserve or purchase tickets, and receive QR-code tickets and PDF invoices by e-mail.

The project was developed as part of the *Software Engineering Practical Research* course at TU Wien by a team of seven, following an agile workflow with merge requests, code reviews, a Definition of Done, and a three-stage CI pipeline. This repository contains the complete application — backend, frontend, and end-to-end tests.

## Key Features

- **Event discovery** — venues, halls, events, artists and genres with a fast search
- **Seat map editor** — admins can design hall layouts, sectors and seats
- **Seat holds** — selected seats are held for 15 minutes with an automatic cleanup job
- **Booking flows** — purchase, time-limited reservation, and purchase from an existing reservation
- **Documents** — PDF tickets with QR codes, invoices and cancellation invoices
- **Authentication** — JWT-based login with `USER` / `ADMIN` roles, password reset via e-mail, account locking
- **E-mail notifications** — order confirmations, expiring reservations, cancellations (async SMTP)
- **News & statistics** — news feed plus top-10 event sales charts per month and genre
- **Demo data** — a full sample dataset can be seeded via the `generateData` Spring profile
- **API documentation** — Swagger UI via springdoc-openapi

## Tech Stack

| Layer     | Technologies |
|-----------|--------------|
| Backend   | Java 25, Spring Boot 4, Spring Security (JJWT), Spring Data JPA, H2, MapStruct, springdoc-openapi, OpenPDF + QRCodeGen |
| Frontend  | Angular 21, TypeScript 6, Bootstrap 5 / ng-bootstrap, RxJS |
| Testing   | JUnit 5, Karma + Jasmine, Cypress (E2E) |
| Quality   | Checkstyle (Google Java Style), ESLint (angular-eslint), Conventional Commits |
| Ops       | Docker, Docker Compose, GitLab CI (automated testing), Spring Actuator + Prometheus metrics |


## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branching model (`master` / `dev` / `feat/<issue>-<description>`), commit conventions, and Definition of Done.
, Spring Actuator + Prometheus metrics |


