# Spring Electro Store - E-commerce Platform 🛒

A comprehensive B2C e-commerce web application for electronic products. This project focuses on handling complex business logic, third-party integrations (Payment & Shipping), and advanced search capabilities.

## ✨ Key Highlights

* **💳 Payment Integration:** Secure online payment processing via **VNPay** (including IPN handling and checksum validation).
* **🚚 Shipping Integration:** Automatic shipping fee calculation based on user address using **GHN (Giao Hang Nhanh) API**.
* **🔍 Advanced Search:** High-performance product search and filtering implementation using **Elasticsearch**.
* **⚡ Optimization:**
    * **Redis** for inventory management (Rate Limiting to prevent race conditions).
    * **Redis** for shopping cart caching.
* **🎫 Promotion System:** Flexible Voucher system implemented using **Strategy Pattern**.

## 🛠️ Technology Stack

**Backend:**
* Spring Boot 3
* Spring Data JPA (PostgreSQL)
* Spring Data Elasticsearch
* Redis (Redisson Client)
* VNPay SDK & GHN API

**Frontend:**
* Vue.js 3 + Vite
* Vuetify UI Library
* Pinia (State Management)
* *Frontend Repo:* [Link to your Vue Frontend Repo if separate]

## 🏗️ Business Modules

* **Inventory Management:** Handles stock synchronization and locking during checkout.
* **Order Processing:** Finite State Machine for order status (Pending -> Paid -> Shipping -> Delivered).
* **Search Engine:** Syncs data from PostgreSQL to Elasticsearch for fast read operations.

## 🚀 How to Run

### Prerequisites
* Java 17+
* Docker (for Redis, Postgres, Elasticsearch)

### Setup Steps

1.  **Start Infrastructure Services**
    ```bash
    docker-compose up -d
    # This will start PostgreSQL, Redis, and Elasticsearch containers
    ```

2.  **Configure API Keys**
    Update `application.yml` with your Sandbox keys for VNPay and GHN.

3.  **Run the Application**
    ```bash
    ./gradlew bootRun
    ```

4.  **Access Swagger UI**
    Open `http://localhost:8080/swagger-ui.html` to explore the API.

## 📂 Project Structure
