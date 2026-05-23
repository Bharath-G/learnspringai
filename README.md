# 🏦 NeoBank — AI-Powered Intelligent Banking Platform

> Event-driven microservices banking system built with Java 25, Spring Boot 4.0.x, Apache Kafka (KRaft), Apache Cassandra 5, Spring AI (Ollama), GraphQL, and Docker.

---

## 📋 Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Event Flow](#event-flow)
- [Kafka Topics](#kafka-topics)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Cassandra Schema](#cassandra-schema)
- [Dependencies Per Service](#dependencies-per-service)
- [API Reference](#api-reference)
- [Environment Variables](#environment-variables)
- [Key Design Decisions](#key-design-decisions)
- [Build Order](#build-order)
- [Dev Workflow](#dev-workflow)
- [Learning Milestones](#learning-milestones)

---

## 🏛️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    GraphQL API Gateway :8080                     │
│              Single endpoint · schema-first · type-safe         │
└───────────────────────────┬─────────────────────────────────────┘
                            │
     ┌──────────────────────┼──────────────────────────┐
     │                      │                          │
     ▼                      ▼                          ▼
┌──────────────┐   ┌─────────────────┐      ┌──────────────────┐
│Account       │   │Transaction      │      │AI Advisor        │
│Service :8081 │   │Service :8082    │      │Service :8084     │
│REST+Cassandra│   │REST+Cassandra   │      │REST+Spring AI    │
│+Kafka Produce│   │+Kafka Prod+Cons │      │+Cassandra        │
└──────┬───────┘   └────────┬────────┘      └──────────────────┘
       │                    │
       └──────────┬─────────┘
                  │  Kafka Events
     ┌────────────┼──────────────────────────┐
     ▼            ▼                          ▼
┌──────────┐ ┌──────────────┐      ┌──────────────────┐
│Fraud     │ │Notification  │      │Ledger / Audit    │
│Detection │ │Service :8085 │      │Service :8086     │
│:8083     │ │Kafka Consumer│      │Kafka+Cassandra   │
│Kafka+AI  │ └──────────────┘      └──────────────────┘
└──────────┘

Infrastructure:
  kafka-kraft   :9092
  cassandra     :9042
  ollama        :11434
  kafka-ui      :8090
```

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Language |
| Spring Boot | 4.0.x | Application framework |
| Apache Kafka | 3.7.0 (KRaft) | Event streaming — no Zookeeper |
| Apache Cassandra | 5.0 | Time-series database |
| Spring AI | 1.0.0 | AI integration framework |
| Ollama | latest | Local AI model runner — free |
| mistral | latest | Fraud detection AI (3.8GB) |
| llama3.2 | latest | Financial advisor AI (2.0GB) |
| Spring for GraphQL | 4.0.x | API Gateway |
| Docker | latest | Containerization |
| Local Registry | localhost:5000 | Private Docker registry |

---

## 🔧 Services

| Service | Port | Type | Responsibilities |
|---|---|---|---|
| graphql-gateway | 8080 | GraphQL | Single entry point for all clients |
| account-service | 8081 | REST + Kafka Producer | Create/manage accounts, publish account events |
| transaction-service | 8082 | REST + Kafka Producer + Consumer | Initiate transfers, update transaction status |
| fraud-detection-service | 8083 | Kafka Consumer + Producer | AI-based fraud analysis on every transaction |
| ai-advisor-service | 8084 | REST | Spending insights, transaction categorization |
| notification-service | 8085 | Kafka Consumer | Send email/SMS on all banking events |
| ledger-service | 8086 | Kafka Consumer | Immutable audit log for every event |

---

## ⚡ Event Flow

```
── Account Created ──────────────────────────────────────────────
POST /accounts (Account Service)
  ├── [account-events] ──► Notification Service  (welcome email)
  └── [audit-events]  ──► Ledger Service         (audit log)

── Money Transfer ───────────────────────────────────────────────
POST /transfers (Transaction Service)
  │
  ├── Save transaction as PENDING
  ├── Return { transactionId, status: PENDING } immediately (~50ms)
  │
  └── [transaction-initiated]
        │
        ├──► Fraud Detection Service
        │      │
        │      ├── LEGITIMATE ──► [transaction-approved]
        │      │                        │
        │      │                        ├──► Transaction Service
        │      │                        │    (PENDING → COMPLETED)
        │      │                        │    deduct balance
        │      │                        │
        │      │                        ├──► [transaction-completed]
        │      │                        │         │
        │      │                        │         ├──► Notification Service
        │      │                        │         └──► Ledger Service
        │      │
        │      ├── SUSPICIOUS ──► [fraud-alerts]
        │      │                        └──► Notification Service
        │      │                             (flag for review)
        │      │
        │      └── FRAUD ──► [transaction-blocked]
        │                          ├──► Transaction Service (FAILED)
        │                          ├──► [account-frozen]
        │                          │         └──► Account Service
        │                          └──► Notification Service
        │                               (account frozen alert)
        │
        └──► Ledger Service (every initiated event logged)
```

---

## 📨 Kafka Topics

| Topic | Events | Produced By | Consumed By |
|---|---|---|---|
| account-events | AccountCreatedEvent, AccountFrozenEvent | account-service | notification-service, ledger-service |
| transaction-initiated | TransactionInitiatedEvent | transaction-service | fraud-detection-service, ledger-service |
| transaction-approved | TransactionApprovedEvent | fraud-detection-service | transaction-service |
| transaction-completed | TransactionCompletedEvent | transaction-service | notification-service, ledger-service |
| transaction-failed | TransactionFailedEvent | transaction-service | notification-service, ledger-service |
| fraud-alerts | TransactionFlaggedEvent, TransactionBlockedEvent | fraud-detection-service | notification-service, ledger-service |
| account-frozen | AccountFrozenEvent | fraud-detection-service | account-service, notification-service |
| audit-events | All events | All services | ledger-service |

---

## 📁 Project Structure

```
~/projects/neobank/
├── infra/
│   ├── docker-compose.yml
│   ├── build-all.sh
│   └── init-cassandra.cql
│
├── shared-events/
│   ├── src/main/java/com/neobank/events/
│   │   ├── AccountCreatedEvent.java
│   │   ├── AccountFrozenEvent.java
│   │   ├── TransactionInitiatedEvent.java
│   │   ├── TransactionApprovedEvent.java
│   │   ├── TransactionCompletedEvent.java
│   │   ├── TransactionFailedEvent.java
│   │   ├── TransactionFlaggedEvent.java
│   │   └── TransactionBlockedEvent.java
│   └── pom.xml
│
├── account-service/
│   ├── src/main/java/com/neobank/account/
│   │   ├── AccountServiceApplication.java
│   │   ├── config/
│   │   │   ├── KafkaProducerConfig.java
│   │   │   └── KafkaTopicConfig.java
│   │   ├── controller/
│   │   │   └── AccountController.java
│   │   ├── service/
│   │   │   └── AccountService.java
│   │   ├── producer/
│   │   │   └── AccountEventProducer.java
│   │   ├── repository/
│   │   │   └── AccountRepository.java
│   │   └── model/
│   │       └── Account.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── transaction-service/
│   ├── src/main/java/com/neobank/transaction/
│   │   ├── TransactionServiceApplication.java
│   │   ├── config/
│   │   │   ├── KafkaProducerConfig.java
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   ├── KafkaTopicConfig.java
│   │   │   └── KafkaErrorHandlerConfig.java
│   │   ├── controller/
│   │   │   └── TransactionController.java
│   │   ├── service/
│   │   │   └── TransactionService.java
│   │   ├── producer/
│   │   │   └── TransactionEventProducer.java
│   │   ├── consumer/
│   │   │   └── TransactionEventConsumer.java
│   │   ├── repository/
│   │   │   └── TransactionRepository.java
│   │   └── model/
│   │       └── Transaction.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── fraud-detection-service/
│   ├── src/main/java/com/neobank/fraud/
│   │   ├── FraudDetectionApplication.java
│   │   ├── config/
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   ├── KafkaProducerConfig.java
│   │   │   └── KafkaErrorHandlerConfig.java
│   │   ├── consumer/
│   │   │   └── TransactionEventConsumer.java
│   │   ├── producer/
│   │   │   └── FraudEventProducer.java
│   │   ├── service/
│   │   │   └── FraudDetectionService.java
│   │   └── model/
│   │       └── FraudAnalysisResult.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── ai-advisor-service/
│   ├── src/main/java/com/neobank/advisor/
│   │   ├── AiAdvisorApplication.java
│   │   ├── controller/
│   │   │   └── AdvisorController.java
│   │   ├── service/
│   │   │   ├── SpendingInsightService.java
│   │   │   └── TransactionCategorizationService.java
│   │   ├── repository/
│   │   │   └── TransactionRepository.java
│   │   └── model/
│   │       └── SpendingInsights.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── notification-service/
│   ├── src/main/java/com/neobank/notification/
│   │   ├── NotificationApplication.java
│   │   ├── config/
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   └── KafkaErrorHandlerConfig.java
│   │   ├── consumer/
│   │   │   └── NotificationEventConsumer.java
│   │   └── service/
│   │       └── NotificationService.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── ledger-service/
│   ├── src/main/java/com/neobank/ledger/
│   │   ├── LedgerApplication.java
│   │   ├── config/
│   │   │   ├── KafkaConsumerConfig.java
│   │   │   └── KafkaErrorHandlerConfig.java
│   │   ├── consumer/
│   │   │   └── AuditEventConsumer.java
│   │   ├── repository/
│   │   │   └── AuditLogRepository.java
│   │   └── model/
│   │       └── AuditLog.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
│
└── graphql-gateway/
    ├── src/main/java/com/neobank/gateway/
    │   ├── GraphQLGatewayApplication.java
    │   ├── controller/
    │   │   └── NeoBankGraphQLController.java
    │   ├── client/
    │   │   ├── AccountServiceClient.java
    │   │   ├── TransactionServiceClient.java
    │   │   ├── FraudServiceClient.java
    │   │   └── AiAdvisorServiceClient.java
    │   └── model/
    │       ├── Account.java
    │       ├── Transaction.java
    │       └── SpendingInsights.java
    ├── src/main/resources/
    │   ├── application.yml
    │   └── graphql/schema.graphqls
    ├── Dockerfile
    └── pom.xml
```

---

## ✅ Prerequisites

```
Java 25          — eclipse-temurin:25-jdk
Maven 3.9+       — build tool
Docker Desktop   — all infrastructure runs in Docker
IntelliJ IDEA    — recommended IDE
DBeaver          — GUI for Cassandra (free at dbeaver.io)
16GB RAM         — minimum for all containers + Ollama models
```

---

## 🚀 Getting Started

### Step 1 — Clone or create project structure

```bash
mkdir -p ~/projects/neobank
cd ~/projects/neobank
```

### Step 2 — Start local Docker registry

```bash
docker run -d -p 5000:5000 --name local-registry registry:2
```

### Step 3 — Start infrastructure

```bash
cd infra
docker-compose up -d kafka-kraft cassandra ollama kafka-ui
```

### Step 4 — Wait for Cassandra (60-90 seconds)

```bash
# Watch until you see "Starting listening for CQL clients"
docker logs cassandra -f
```

### Step 5 — Create Cassandra keyspace and tables

```bash
docker exec -it cassandra cqlsh < infra/init-cassandra.cql
```

### Step 6 — Pull Ollama AI models (one time only — stored in volume)

```bash
docker exec -it ollama ollama pull mistral
docker exec -it ollama ollama pull llama3.2

# Verify models downloaded
docker exec -it ollama ollama list
```

### Step 7 — Build shared-events first (required by all services)

```bash
cd shared-events
mvn clean install
```

### Step 8 — Build and push all services

```bash
cd infra
chmod +x build-all.sh
./build-all.sh
```

### Step 9 — Start all services

```bash
cd infra
docker-compose up -d
```

### Step 10 — Verify everything is running

```bash
docker ps

# Health checks
curl http://localhost:8081/actuator/health  # account-service
curl http://localhost:8082/actuator/health  # transaction-service
curl http://localhost:8083/actuator/health  # fraud-detection-service
curl http://localhost:8084/actuator/health  # ai-advisor-service
curl http://localhost:8085/actuator/health  # notification-service
curl http://localhost:8086/actuator/health  # ledger-service
curl http://localhost:8080/actuator/health  # graphql-gateway

# Kafka UI
open http://localhost:8090
```

---

## 🗄️ Cassandra Schema

```sql
-- Create keyspace
CREATE KEYSPACE IF NOT EXISTS neobank
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE neobank;

-- Accounts
CREATE TABLE IF NOT EXISTS accounts_by_id (
    account_id   UUID,
    customer_id  UUID,
    account_type TEXT,
    balance      DECIMAL,
    currency     TEXT,
    status       TEXT,
    created_at   TIMESTAMP,
    PRIMARY KEY (account_id)
);

CREATE TABLE IF NOT EXISTS accounts_by_customer (
    customer_id  UUID,
    account_id   UUID,
    account_type TEXT,
    balance      DECIMAL,
    status       TEXT,
    PRIMARY KEY (customer_id, account_id)
);

-- Transactions
CREATE TABLE IF NOT EXISTS transactions_by_account (
    account_id       UUID,
    transaction_time TIMESTAMP,
    transaction_id   UUID,
    type             TEXT,
    amount           DECIMAL,
    balance_after    DECIMAL,
    merchant         TEXT,
    category         TEXT,
    status           TEXT,
    PRIMARY KEY (account_id, transaction_time, transaction_id)
) WITH CLUSTERING ORDER BY (transaction_time DESC);

CREATE TABLE IF NOT EXISTS transactions_by_merchant (
    merchant         TEXT,
    transaction_time TIMESTAMP,
    account_id       UUID,
    amount           DECIMAL,
    status           TEXT,
    PRIMARY KEY (merchant, transaction_time)
) WITH CLUSTERING ORDER BY (transaction_time DESC);

-- Audit log (immutable — never update or delete)
CREATE TABLE IF NOT EXISTS audit_log (
    account_id  UUID,
    event_time  TIMESTAMP,
    event_id    UUID,
    event_type  TEXT,
    payload     TEXT,
    service     TEXT,
    PRIMARY KEY (account_id, event_time, event_id)
) WITH CLUSTERING ORDER BY (event_time DESC);

-- AI generated summaries
CREATE TABLE IF NOT EXISTS ai_summaries (
    account_id   UUID,
    generated_at TIMESTAMP,
    summary_id   UUID,
    insight_type TEXT,
    content      TEXT,
    PRIMARY KEY (account_id, generated_at, summary_id)
) WITH CLUSTERING ORDER BY (generated_at DESC);
```

---

## 📦 Dependencies Per Service

```
shared-events (plain Maven jar — no Spring Boot)
  jackson-databind
  jackson-datatype-jsr310
  lombok

account-service
  spring-boot-starter-web
  spring-boot-starter-data-cassandra
  spring-kafka
  spring-boot-starter-validation
  spring-boot-starter-actuator
  lombok
  jackson-databind
  shared-events

transaction-service
  spring-boot-starter-web
  spring-boot-starter-data-cassandra
  spring-kafka
  spring-boot-starter-validation
  spring-boot-starter-actuator
  lombok
  jackson-databind
  jackson-datatype-jsr310
  shared-events

fraud-detection-service
  spring-boot-starter (no web)
  spring-boot-starter-data-cassandra
  spring-kafka
  spring-ai-ollama-spring-boot-starter
  spring-boot-starter-actuator
  lombok
  jackson-databind
  jackson-datatype-jsr310
  shared-events

ai-advisor-service
  spring-boot-starter-web
  spring-boot-starter-data-cassandra
  spring-ai-ollama-spring-boot-starter
  spring-boot-starter-validation
  spring-boot-starter-actuator
  lombok
  jackson-databind
  jackson-datatype-jsr310
  shared-events

notification-service
  spring-boot-starter (no web)
  spring-kafka
  spring-boot-starter-mail
  spring-boot-starter-actuator
  lombok
  jackson-databind
  jackson-datatype-jsr310
  shared-events

ledger-service
  spring-boot-starter (no web)
  spring-boot-starter-data-cassandra
  spring-kafka
  spring-boot-starter-actuator
  lombok
  jackson-databind
  jackson-datatype-jsr310
  shared-events

graphql-gateway
  spring-boot-starter-graphql
  spring-boot-starter-web
  spring-boot-starter-webflux (for WebClient only)
  spring-boot-starter-validation
  spring-boot-starter-actuator
  lombok
  jackson-databind
  jackson-datatype-jsr310
```

---

## 🌐 API Reference

### Account Service

```
POST   /api/accounts              Create new account
GET    /api/accounts/{id}         Get account by ID
GET    /api/accounts/customer/{customerId}  All accounts for customer
PUT    /api/accounts/{id}/freeze  Freeze account (admin only)
GET    /api/accounts/{id}/balance Get current balance
```

### Transaction Service

```
POST   /api/transfers             Initiate transfer (returns PENDING)
GET    /api/transactions/{id}     Get transaction status
GET    /api/transactions?accountId={id}&limit=20  Transaction history
```

### AI Advisor Service

```
GET    /api/advisor/insights/{accountId}      Spending insights (last 90 days)
POST   /api/advisor/categorize                Categorize a transaction
GET    /api/advisor/budget/{accountId}        Monthly budget recommendation
```

### GraphQL Gateway

```
Endpoint: POST http://localhost:8080/graphql
Playground: http://localhost:8080/graphiql

query CustomerDashboard($customerId: ID!) {
  customer(id: $customerId) {
    name
    accounts {
      accountNumber
      balance
      transactions(limit: 5) {
        amount
        type
        merchant
        category
        timestamp
      }
      spendingInsights {
        topCategories { name amount }
        savingsTip
        monthlyBudgetRecommendation
      }
      fraudAlerts {
        severity
        description
      }
    }
  }
}
```

---

## ⚙️ Environment Variables

```
# All services
SPRING_APPLICATION_NAME                <service-name>
SPRING_KAFKA_BOOTSTRAP_SERVERS         kafka-kraft:9092

# Services with Cassandra
SPRING_CASSANDRA_CONTACT_POINTS        cassandra
SPRING_CASSANDRA_PORT                  9042
SPRING_CASSANDRA_KEYSPACE_NAME         neobank
SPRING_CASSANDRA_LOCAL_DATACENTER      datacenter1

# Services with Spring AI
SPRING_AI_OLLAMA_BASE_URL              http://ollama:11434
SPRING_AI_OLLAMA_CHAT_MODEL            mistral (fraud) / llama3.2 (advisor)

# GraphQL Gateway
SERVICES_ACCOUNT_URL                   http://account-service:8081
SERVICES_TRANSACTION_URL               http://transaction-service:8082
SERVICES_FRAUD_URL                     http://fraud-detection-service:8083
SERVICES_AI_ADVISOR_URL                http://ai-advisor-service:8084
```

---

## 🔑 Key Design Decisions

```
1.  KRaft mode Kafka
    No Zookeeper. Kafka manages its own cluster metadata.
    Production ready since Kafka 3.3. Simpler, faster.

2.  Spring Web (not reactive)
    Easier to learn and debug. CompletableFuture for parallel calls.
    Reactive adds complexity with no benefit at learning-project scale.

3.  Ollama local AI
    Zero cost. No API key. Runs on 16GB RAM.
    mistral for structured JSON (fraud). llama3.2 for conversation (advisor).

4.  Manual Kafka acknowledgement
    enable-auto-commit: false. ack-mode: MANUAL_IMMEDIATE.
    Offset committed only after successful processing.
    No silent message loss.

5.  Dead Letter Topic on every service
    Failed messages go to originalTopic.DLT after 3 retries.
    Exponential backoff: 1s, 2s, 4s.
    Prevents one bad message freezing the partition.

6.  Shared events module
    Single source of truth for all Kafka event classes.
    Built first. Installed to local Maven repo.
    All services depend on com.neobank:shared-events.

7.  Cassandra — one table per query pattern
    No joins. Ever.
    Partition key = what you filter by (WHERE clause).
    Clustering key = how results are sorted (ORDER BY clause).
    Design queries first. Build tables to serve those queries.

8.  Transaction state machine
    PENDING → COMPLETED or FAILED.
    API returns PENDING immediately (~50ms).
    Fraud check and balance deduction happen asynchronously via Kafka.

9.  Immutable audit log
    Ledger Service only ever inserts. Never updates. Never deletes.
    Every financial event stored with full JSON payload.
    Legally required in banking. Cassandra is ideal for this.

10. Separate Dockerfiles per service
    Same content. One per service folder.
    Rule: one service = one Dockerfile = one image = one container.

11. infra/ folder
    One docker-compose.yml for everything.
    One build-all.sh to build and push all services.
    Infrastructure completely separate from application code.

12. Local Docker registry at localhost:5000
    No cloud needed. No AWS. Runs on your laptop.
    docker run -d -p 5000:5000 --name local-registry registry:2
```

---

## 🏗️ Build Order

```bash
# Always in this order — shared-events must be first
1. cd shared-events         && mvn clean install
2. cd account-service       && mvn clean package -DskipTests
3. cd transaction-service   && mvn clean package -DskipTests
4. cd fraud-detection-service && mvn clean package -DskipTests
5. cd ai-advisor-service    && mvn clean package -DskipTests
6. cd notification-service  && mvn clean package -DskipTests
7. cd ledger-service        && mvn clean package -DskipTests
8. cd graphql-gateway       && mvn clean package -DskipTests
```

---

## 💻 Dev Workflow

```bash
# Building account-service today
docker-compose up -d kafka-kraft cassandra kafka-ui
# Run account-service from IntelliJ on localhost:8081
# Fast iteration — no Docker rebuild needed

# Building fraud-detection-service today
docker-compose up -d kafka-kraft cassandra ollama
# Run fraud-detection-service from IntelliJ on localhost:8083

# Building ai-advisor-service today
docker-compose up -d cassandra ollama
# Run ai-advisor-service from IntelliJ on localhost:8084

# Full integration test
docker-compose up -d
# All 9 containers running

# Rebuild and restart one service only
cd account-service
mvn clean package -DskipTests
docker build -t localhost:5000/account-service:latest .
docker push localhost:5000/account-service:latest
docker-compose up -d --no-deps account-service
```

---

## 🐳 Dockerfile (identical for all services)

```dockerfile
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📊 Ports Reference

```
graphql-gateway           http://localhost:8080
graphql-playground        http://localhost:8080/graphiql
account-service           http://localhost:8081
transaction-service       http://localhost:8082
fraud-detection-service   http://localhost:8083
ai-advisor-service        http://localhost:8084
notification-service      http://localhost:8085
ledger-service            http://localhost:8086
kafka-ui                  http://localhost:8090
kafka-kraft               localhost:9092
cassandra                 localhost:9042
ollama                    http://localhost:11434
local-docker-registry     localhost:5000
```

---

## 🤖 AI Models

```
Service                   Model       Size    Best for
fraud-detection-service   mistral     3.8GB   Structured JSON output, analysis
ai-advisor-service        llama3.2    2.0GB   Conversational, spending advice

Total AI RAM usage        ~6GB
Remaining for services    ~10GB
System + IDE              ~6GB
Total                     ~16GB (fits exactly on 16GB machine)
```

---

## 📅 Learning Milestones

```
Week 1 — Cassandra + Account Service
  ✦ Partition key vs clustering key
  ✦ Spring Data Cassandra — @Table @PrimaryKey @PrimaryKeyColumn
  ✦ Query-first table design
  ✦ Account CRUD with REST

Week 2 — Transaction Service + Kafka patterns
  ✦ PENDING → COMPLETED state machine
  ✦ Kafka producer + consumer in same service
  ✦ Manual acknowledgement
  ✦ Dead Letter Topic setup

Week 3 — Spring AI + Fraud Detection
  ✦ ChatClient setup with Ollama
  ✦ Prompt engineering for fraud analysis
  ✦ Structured output — entity() maps JSON to Java class
  ✦ Kafka-driven AI pipeline

Week 4 — AI Advisor Service
  ✦ Spending categorization with AI
  ✦ Context window management
  ✦ Storing AI responses in Cassandra

Week 5 — Notification + Ledger Services
  ✦ Pure Kafka consumer pattern
  ✦ Immutable audit log design
  ✦ Email sending with Spring Mail

Week 6 — GraphQL Gateway
  ✦ Schema definition language (SDL)
  ✦ @QueryMapping and @SchemaMapping
  ✦ DataLoader — solving N+1 query problem
  ✦ WebClient for service-to-service calls

Week 7 — Hardening + Integration Testing
  ✦ Full docker-compose end-to-end test
  ✦ Distributed tracing with Micrometer
  ✦ Performance tuning Cassandra + Kafka
  ✦ Write about it on LinkedIn
```

---

## 🚨 Common Mistakes to Avoid

```
1. localhost vs container name
   WRONG : SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092  (inside Docker)
   RIGHT : SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-kraft:9092

2. Auto commit enabled
   WRONG : enable-auto-commit: true
   RIGHT : enable-auto-commit: false + ack.acknowledge() manually

3. No DLT configured
   One bad message freezes the entire partition forever
   Always add DefaultErrorHandler + DeadLetterPublishingRecoverer

4. Cassandra SQL thinking
   WRONG : design normalized tables → join at query time
   RIGHT : design one table per query pattern → no joins ever

5. Forgetting to install shared-events first
   All other services depend on it
   Always: cd shared-events && mvn clean install

6. Starting all 9 containers while developing
   Wastes RAM. Run only what the service you are building needs.

7. Changing CLUSTER_ID after data exists in volume
   Kafka will refuse to start
   Keep CLUSTER_ID consistent or wipe the volume

8. Wrong replication factor for single broker
   WRONG : KAFKA_DEFAULT_REPLICATION_FACTOR=3 (only 1 broker)
   RIGHT : KAFKA_DEFAULT_REPLICATION_FACTOR=1 for local dev

9. Calling ack.acknowledge() before publishing downstream event
   If publish fails after ack — message is lost permanently
   Always ack AFTER all downstream operations succeed

10. Trusting all packages in JsonDeserializer
    WRONG : spring.json.trusted.packages=*
    RIGHT : spring.json.trusted.packages=com.neobank.events
```

---


---

*Built for learning — Java + Spring Boot + Kafka + Cassandra + Spring AI + GraphQL*