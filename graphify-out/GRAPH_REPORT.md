# Graph Report - StorageKeeper  (2026-11-02)

## Corpus Check
- Corpus is ~11,204 words - fits in a single context window. You may not need a graph.

## Summary
- 544 nodes · 1471 edges · 29 communities (14 shown, 15 thin omitted)
- Extraction: 81% EXTRACTED · 19% INFERRED · 0% AMBIGUOUS · INFERRED: 276 edges (avg confidence: 0.85)
- Token cost: 99,458 input · 0 output

## Community Hubs (Navigation)
- Persistence Adapter Core
- Domain Modeling Concepts
- Pantry Item REST Features
- Persistence & Schema Design
- Qodana CI Workflow
- Filtering & Paging
- Expiring Items API
- Unit Test Infrastructure
- Roadmap & Architecture Rules
- Web Layer Tests
- CI Build Workflow
- API Error Handling
- Storage Location Use Cases
- Spring App Config
- Clock & Testcontainers Config
- Name Value Object Tests
- Docker Compose Postgres
- Qodana Config
- Application Entry Point

## God Nodes (most connected - your core abstractions)
1. `PantryItem` - 66 edges
2. `LocationName` - 40 edges
3. `Quantity` - 37 edges
4. `StorageLocation` - 35 edges
5. `StorageLocationId` - 34 edges
6. `PantryItemId` - 32 edges
7. `ItemName` - 28 edges
8. `PantryItemService` - 24 edges
9. `PantryItemControllerTest` - 21 edges
10. `PantryItemServiceTest` - 21 edges

## Surprising Connections (you probably didn't know these)
- `M1 Vorratsartikel` --conceptually_related_to--> `PantryItem`  [INFERRED]
  docs/ROADMAP.md → src/main/java/io/github/maxziel/storagekeeper/domain/PantryItem.java
- `REST API /api/storage-locations` --references--> `StorageLocationController`  [INFERRED]
  README.md → src/main/java/io/github/maxziel/storagekeeper/adapter/in/web/StorageLocationController.java
- `Table storage_location` --shares_data_with--> `JpaStorageLocationRepository`  [INFERRED]
  docs/DOMAIN_MODEL.md → src/main/java/io/github/maxziel/storagekeeper/adapter/out/persistence/JpaStorageLocationRepository.java
- `Aggregate Root` --references--> `PantryItem`  [EXTRACTED]
  docs/DOMAIN_MODEL.md → src/main/java/io/github/maxziel/storagekeeper/domain/PantryItem.java
- `M3 Lagerorte & Ablauf` --conceptually_related_to--> `StorageLocation`  [INFERRED]
  docs/ROADMAP.md → src/main/java/io/github/maxziel/storagekeeper/domain/StorageLocation.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Change-quantity request flow through hexagonal layers** — src_main_java_io_github_maxziel_storagekeeper_adapter_in_web_pantryitemcontroller_pantryitemcontroller, src_main_java_io_github_maxziel_storagekeeper_application_pantryitemservice_pantryitemservice, src_main_java_io_github_maxziel_storagekeeper_application_port_out_pantryitemrepository_pantryitemrepository, src_main_java_io_github_maxziel_storagekeeper_domain_pantryitem_pantryitem [EXTRACTED 1.00]
- **PantryItem persistence adapter mapping** — src_main_java_io_github_maxziel_storagekeeper_adapter_out_persistence_jpapantryitemrepository_jpapantryitemrepository, src_main_java_io_github_maxziel_storagekeeper_adapter_out_persistence_springdatapantryitemrepository_springdatapantryitemrepository, src_main_java_io_github_maxziel_storagekeeper_adapter_out_persistence_pantryitemjpaentity_pantryitemjpaentity, docs_domain_model_pantry_item_table, src_main_java_io_github_maxziel_storagekeeper_adapter_out_persistence_pantryitemspecifications_pantryitemspecifications [EXTRACTED 1.00]
- **M3 features: storage locations, expiry query, minimum amount, filter/paging** — docs_roadmap_m3_locations_and_expiry, docs_domain_model_expiry_and_clock, docs_domain_model_minimum_amount, docs_domain_model_filter_and_paging, docs_domain_model_default_storage_location [INFERRED 0.85]

## Communities (29 total, 15 thin omitted)

### Community 0 - "Persistence Adapter Core"
Cohesion: 0.09
Nodes (9): JpaPantryItemRepository, AddPantryItemCommand, ItemName, PantryItem, Quantity, JpaPantryItemRepositoryTest, PantryItemServiceTest, PantryItemTest (+1 more)

### Community 1 - "Domain Modeling Concepts"
Cohesion: 0.07
Nodes (12): Aggregate Root, Entity (identity-based equality), Value Object (record, immutable), AddStorageLocationRequest, StorageLocationResponse, JpaStorageLocationRepository, StorageLocationJpaEntity, LocationName (+4 more)

### Community 2 - "Pantry Item REST Features"
Cohesion: 0.07
Nodes (19): Request flow: change quantity, Item list filter and paging, Minimum amount (Mindestbestand), M3 Lagerorte & Ablauf, REST API /api/pantry-items, ChangeMinimumAmountRequest, ChangeQuantityRequest, PantryItemController (+11 more)

### Community 3 - "Persistence & Schema Design"
Cohesion: 0.08
Nodes (13): Default storage location 'Vorrat', Error handling (ProblemDetail, RFC 9457), Flyway migrations V1-V4, Table pantry_item, Table storage_location, M2 Echte Persistenz (PostgreSQL), Build & Start (Gradle, Docker Compose, Testcontainers), Errors as application/problem+json (+5 more)

### Community 4 - "Qodana CI Workflow"
Cohesion: 0.07
Nodes (27): QODANA_ENDPOINT, QODANA_TOKEN, jobs, qodana, name, on, pull_request, push (+19 more)

### Community 5 - "Filtering & Paging"
Cohesion: 0.19
Nodes (4): PageResponse, PantryItemFilter, PageQuery, ResultPage

### Community 6 - "Expiring Items API"
Cohesion: 0.11
Nodes (11): AddPantryItemRequest, ExpiringPantryItemResponse, PantryItemResponse, ExpiringPantryItem, Unit, GRAM, KILOGRAM, LITER (+3 more)

### Community 8 - "Roadmap & Architecture Rules"
Cohesion: 0.14
Nodes (11): Domain-Modell Document, M0 Walking Skeleton, M1 Vorratsartikel, M4 Einkaufsliste, M5 API & Sicherheit, M6 Betrieb, Optional: Web-Frontend, Barcode-Scan (Open Food Facts), expiry notifications, Roadmap (+3 more)

### Community 10 - "CI Build Workflow"
Cohesion: 0.10
Nodes (19): runs-on, steps, jobs, build, name, on, pull_request, push (+11 more)

### Community 11 - "API Error Handling"
Cohesion: 0.18
Nodes (3): ApiExceptionHandler, StorageLocationAlreadyExistsException, StorageLocationNotFoundException

### Community 12 - "Storage Location Use Cases"
Cohesion: 0.22
Nodes (6): StorageLocationController, AddStorageLocationUseCase, ListStorageLocationsUseCase, StorageLocationRepository, StorageLocationService, StorageLocationControllerTest

### Community 13 - "Spring App Config"
Cohesion: 0.11
Nodes (17): name, web, include, ddl-auto, hibernate, open-in-view, management, endpoints (+9 more)

### Community 18 - "Docker Compose Postgres"
Cohesion: 0.22
Nodes (8): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, services, postgres

### Community 19 - "Qodana Config"
Cohesion: 0.29
Nodes (6): include, name, linter, profile, name, version

## Knowledge Gaps
- **9 isolated node(s):** `PIECE`, `GRAM`, `KILOGRAM`, `MILLILITER`, `LITER` (+4 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 107 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PantryItem` connect `Persistence Adapter Core` to `Domain Modeling Concepts`, `Pantry Item REST Features`, `Persistence & Schema Design`, `Filtering & Paging`, `Expiring Items API`, `Roadmap & Architecture Rules`, `API Error Handling`, `Storage Location Use Cases`, `Value Object Validation`, `Quantity & Command Types`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `LocationName` (e.g. with `.itemCanBeStoredInUserDefinedLocation()` and `.searchFiltersByLocationAndMinimumAndSortsByName()`) actually correct?**
  _`LocationName` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PIECE`, `GRAM`, `KILOGRAM` to the rest of the system?**
  _9 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Persistence Adapter Core` be split into smaller, more focused modules?**
  _Cohesion score 0.09364649202047576 - nodes in this community are weakly interconnected._
- **Why does `Domain-Modell Document` connect `Roadmap & Architecture Rules` to `Persistence Adapter Core`, `Domain Modeling Concepts`, `Pantry Item REST Features`, `Expiring Items API`, `Storage Location Use Cases`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._
- **Are the 23 inferred relationships involving `Quantity` (e.g. with `.toDomain()` and `.addReturnsCreatedItemWithLocation()`) actually correct?**
  _`Quantity` has 23 INFERRED edges - model-reasoned connections that need verification._
- **Should `Domain Modeling Concepts` be split into smaller, more focused modules?**
  _Cohesion score 0.06625258799171843 - nodes in this community are weakly interconnected._