# Spring Electro Store - E-commerce Backend API

## 🚀 Introduction
This is a comprehensive E-commerce Backend built with **Spring Boot 3**, utilizing microservice patterns (Layered Architecture, CQRS) and high-performance technologies (**Redis**, **Elasticsearch**).

## 🛠️ Tech Stack
- **Core:** Java 17, Spring Boot 3.3
- **Database:** PostgreSQL (Main), Redis (Cache/Cart), Elasticsearch (Search)
- **Security:** Spring Security, JWT, OAuth2 (Google)
- **Integration:** VNPay (Payment), Cloudinary (Storage), GHN (Shipping)
- **DevOps:** Docker, Docker Compose, GitHub Actions (CI/CD)

## 🌟 Key Features
1.  **Product Management:** Advanced filtering (JPA Specs), Full-text Search (Elasticsearch).
2.  **Shopping Cart:** High-performance cart management using Redis.
3.  **Order Processing:** ACID Transactions, Atomic Stock Updates (Concurrency safe).
4.  **Payment:** VNPay Gateway integration with secure checksum validation.
5.  **Vouchers:** Flexible discount logic using **Strategy Pattern**.
6.  **Real-time:** WebSocket notifications for order status updates.
7.  **Self-healing:** Automated Cron Jobs to cleanup unpaid orders and restore stock.

## 🐳 How to Run (Docker)
The entire system (App + DB + Redis + ES) can be started with a single command:

```bash
docker-compose up --build
