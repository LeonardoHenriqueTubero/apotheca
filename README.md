# Apotheca

[![Backend CI](https://github.com/LeonardoHenriqueTubero/apotheca/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/LeonardoHenriqueTubero/apotheca/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/LeonardoHenriqueTubero/apotheca/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/LeonardoHenriqueTubero/apotheca/actions/workflows/frontend-ci.yml)

Home medication inventory manager: track the medicines your household owns, how much is left, where they are stored and when they expire. Web app and installable PWA.

> Work in progress.

## Architecture

```mermaid
flowchart LR
    subgraph device["User device (phone / browser)"]
        pwa["Angular PWA"]
    end

    subgraph firebase["Firebase (free plan)"]
        hosting["Firebase Hosting<br/>static files"]
        auth["Firebase Authentication"]
        fcm["Firebase Cloud Messaging"]
    end

    subgraph render["Render (free plan)"]
        api["Spring Boot API<br/>Docker container"]
    end

    db[("Neon<br/>PostgreSQL")]
    cron["GitHub Actions<br/>daily cron"]

    hosting -->|serves app files| pwa
    pwa -->|sign in| auth
    pwa -->|REST + JWT| api
    api -->|validates JWT| auth
    api -->|SQL, Flyway migrations| db
    cron -->|POST alert endpoint + secret token| api
    api -->|send push| fcm
    fcm -->|push notification| pwa
```

Details: [docs/diagrams/architecture.md](docs/diagrams/architecture.md).
