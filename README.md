# Education Platform Backend

This repository contains the backend system for an **Education Platform**, built using **Java Spring Boot** with a **microservices architecture**.

The project is designed to support core features such as **user authentication**, **account management**, and **inter-service communication**.  
It focuses on clean architecture, scalability, and real-world backend practices.

## 🚧 Project Status
This project is currently **under development**.  
Some features and services are still being implemented and may change over time.

## 🏗 Architecture Overview
- Microservices-based architecture
- Each service has its own responsibility and database
- Asynchronous communication between services using **Kafka**
- Docker used for local development and service orchestration

## 🧩 Services (Current)
- **Auth Service** – Handles user registration, login, and account verification
- **User Service** – Manages user profile data
- **Common Lib** – Shared DTOs, events, and constants across services

## 🛠 Tech Stack
- Java 21
- Spring Boot
- PostgreSQL
- Apache Kafka
- Docker & Docker Compose

## 📌 Notes
- Frontend will be developed in a **separate repository**
- This project is mainly used for **learning and practicing backend microservices design**

---