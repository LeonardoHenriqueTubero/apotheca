# ADR 0001: Backend package structure and entity/DTO modeling

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #22, #23, #24, #25

## Context

The backend has no code yet. Before writing the first class we need to decide how
packages are organized and how data moves between the database and the API.

Apotheca is a small MVP and a portfolio project for a junior developer looking for
a first job. The priorities are: a structure that recruiters recognize immediately,
that is easy to explain in an interview, and that does not add concepts the
project does not need yet.

## Decision

### 1. Package-by-layer

Code is organized by technical layer, not by feature/domain. The base package is
`br.dev.leonardo.apotheca` (decision log #26), with eight packages under it:

```
br.dev.leonardo.apotheca
├── entity
├── dto
├── mapper
├── repository
├── service
├── controller
├── config
└── exception
```

| Package | Contents |
|---|---|
| `entity` | JPA entities (`@Entity`), one per table created by Flyway |
| `dto` | Request and response records exposed by the API |
| `mapper` | MapStruct mappers between entities and DTOs |
| `repository` | Spring Data JPA interfaces |
| `service` | Business rules (e.g. effective expiry) and transactions |
| `controller` | REST endpoints (`@RestController`); receive and return DTOs only |
| `config` | Spring configuration (security, OpenAPI, CORS) |
| `exception` | Custom exceptions and the global `@RestControllerAdvice` handler |

Controllers never return entities. The API contract is defined by DTOs, so a change
in the database schema does not leak into the API by accident.

Mappers get their own package instead of living in `dto`, because a mapper depends
on both `entity` and `dto`. Keeping it in `dto` would make the DTO package depend on
the persistence model.

### 2. DTOs as Java records

DTOs are Java 21 `record`s, not classes with Lombok. A record is immutable and
already provides a constructor, accessors, `equals`, `hashCode` and `toString`,
which is exactly what a data carrier needs.

```java
package br.dev.leonardo.apotheca.dto;

public record MedicationResponse(Long id, String name, String activeIngredient, String form) {}
```

### 3. MapStruct for entity ↔ DTO mapping

Mapping is done by MapStruct interfaces in the `mapper` package, with
`componentModel = "spring"`, so each mapper is a Spring bean that services can
inject. MapStruct generates the mapping code at compile time: no reflection at
runtime, and a compile error if a field cannot be mapped.

```java
package br.dev.leonardo.apotheca.mapper;

@Mapper(componentModel = "spring")
public interface MedicationMapper {
    MedicationResponse toResponse(Medication medication);
}
```

### 4. Lombok on JPA entities, with specific annotations only

Entities and DTOs have different needs. Hibernate needs entities to be mutable and
to have a no-args constructor, so records do not fit there. Lombok removes that
boilerplate on entities only.

Allowed on entities: `@Getter`, `@Setter`, `@NoArgsConstructor`, and
`@AllArgsConstructor` when it is actually needed.

Not allowed on entities: `@Data`, `@EqualsAndHashCode` and `@ToString`. The
`equals`/`hashCode`/`toString` they generate read every field, including lazy
relationships. That can trigger unexpected queries, `LazyInitializationException`,
or infinite recursion between two related entities. If an entity ever needs
`equals`/`hashCode`, it is written by hand.

```java
package br.dev.leonardo.apotheca.entity;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
public class Medication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // ...
}
```

## Alternatives considered

- **Package-by-feature** (`medication/`, `batch/`, `household/`, each with its own
  controller, service and repository). It scales better and keeps a feature in one
  place, but it is less common at junior level and harder to explain for a project
  this size.
- **DTOs as Lombok classes** (`@Data`, `@Value`). It works, but records do the same
  thing with plain Java, so they need no extra dependency and no annotations.
- **Manual mapping** (hand-written `toResponse` methods). It is simple, but repetitive
  and easy to forget when a field is added. **ModelMapper** was also discarded: it maps
  through reflection at runtime, so mapping errors only appear while the app is running.
- **Mappers inside `dto`.** This means one package fewer, but it mixes the API contract
  with code that depends on entities.
- **Records or plain Java for entities.** Records cannot be JPA entities, because they
  are immutable and have no no-args constructor. Plain getters and setters work, but
  add a lot of boilerplate to every entity.

## Consequences

- ✅ Familiar layout: anyone who has seen a Spring tutorial or job test can navigate it.
- ✅ Clear separation: API contract (records) vs persistence model (entities),
  with mapping isolated in its own package.
- ✅ Mapping errors are caught at compile time.
- ⚠️ As the project grows, each layer package gets large and a feature is spread
  across several packages. If that becomes painful, moving to package-by-feature
  will be recorded in a new ADR that supersedes this one.
- ⚠️ MapStruct and Lombok are both annotation processors. The `maven-compiler-plugin`
  must list both, plus `lombok-mapstruct-binding`, so that MapStruct can see the
  getters and setters Lombok generates. This is configured in the backend skeleton.
