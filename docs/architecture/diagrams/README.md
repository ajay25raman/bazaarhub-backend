# Architectural Diagrams

## 1. Purpose
This section visualizes the architectural blueprints, layer interactions, and module boundary models for BazaarHub using standard Mermaid diagrams.

---

## 2. Clean Architecture Layer Ring Model

```mermaid
graph TD
    subgraph Presentation_Layer ["Presentation Layer (REST)"]
        Controllers["REST Controllers"]
        ReqResp["Request / Response DTOs"]
    end

    subgraph Application_Layer ["Application Layer (Use Cases)"]
        InputPorts["Input Ports (Use Case Interfaces)"]
        UseCases["Use Case Implementations"]
        OutputPorts["Output Ports (SPI)"]
        CommandsQueries["Commands / Queries / DTOs"]
    end

    subgraph Domain_Layer ["Domain Layer (Pure Business Core)"]
        Entities["Entities & Aggregates"]
        ValueObjects["Value Objects"]
        DomainServices["Domain Services"]
        DomainEvents["Domain Events"]
        DomainExceptions["Domain Exceptions"]
        RepoAbstractions["Repository Abstractions (SPI)"]
    end

    subgraph Infrastructure_Layer ["Infrastructure Layer (Technical Details)"]
        Persistence["Persistence Adapters & Repositories"]
        EntitiesDB["Database Entities (Future Phase)"]
        Adapters["External Service Adapters"]
        Config["Spring @Configuration Beans"]
    end

    Controllers -->|calls| InputPorts
    InputPorts -->|implemented by| UseCases
    UseCases -->|orchestrates| Entities
    UseCases -->|invokes| DomainServices
    UseCases -->|calls| OutputPorts
    Persistence -->|implements| OutputPorts
    Adapters -->|implements| OutputPorts
    Persistence -->|maps to/from| Entities
```

---

## 3. Dependency Inversion & Call Direction

```mermaid
flowchart LR
    subgraph Client ["Client / Outer World"]
        HTTP["HTTP Client (Flutter/Web)"]
    end

    subgraph Presentation ["Presentation Layer"]
        C["REST Controller"]
    end

    subgraph Application ["Application Layer"]
        IP["Input Port (Interface)"]
        UC["Use Case (Implementation)"]
        OP["Output Port (Interface)"]
    end

    subgraph Domain ["Domain Layer"]
        D["Domain Entity / Service"]
        RA["Repository Abstraction (SPI)"]
    end

    subgraph Infrastructure ["Infrastructure Layer"]
        A["Persistence Adapter / Gateway"]
    end

    HTTP -->|1. HTTP Request| C
    C -->|2. Invoke Input Port| IP
    IP -.->|implemented by| UC
    UC -->|3. Execute Domain Rules| D
    UC -->|4. Query Data via Port| OP
    A -.->|implements| OP
    A -.->|implements| RA
```

> **Notice**: The compile-time source code dependency points inward (`Infrastructure -> Application / Domain`), while runtime execution flows outward through the output ports. This is the essence of the **Dependency Inversion Principle (DIP)**.

---

## 4. Cross-Module Communication Model

```mermaid
sequenceDiagram
    autonumber
    participant OrderModule as Order Module
    participant InvPort as InventoryReservationPort
    participant InventoryModule as Inventory Module
    participant EventBus as Domain Event Bus
    participant NotificationModule as Notification Module

    Note over OrderModule: User initiates Checkout
    OrderModule->>InvPort: reserveStock(orderItems)
    activate InvPort
    InvPort->>InventoryModule: executeReservation(cmd)
    activate InventoryModule
    InventoryModule-->>InvPort: ReservationSuccessToken
    deactivate InventoryModule
    InvPort-->>OrderModule: ReservationConfirmed
    deactivate InvPort

    Note over OrderModule: Order transitions to CONFIRMED
    OrderModule->>EventBus: publish(OrderConfirmedEvent)
    activate EventBus
    EventBus-->>NotificationModule: handle(OrderConfirmedEvent)
    activate NotificationModule
    Note over NotificationModule: Dispatch push/email notification
    deactivate NotificationModule
    deactivate EventBus
```

---

## 5. Maintenance
Maintained synchronously with architecture decisions. Diagrams must be updated whenever communication topologies or layer responsibilities change.
