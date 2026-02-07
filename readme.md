# Coffee Shop Barista Dilemma - HCLTech Hackathon 2026

Solution for Problem 2 - Coffee Shop Barista Dilemma from the February 7, 2026 hackathon.

## Features
- Real-time order placement via web form
- Dynamic priority queue (wait time 40%, complexity 25%, loyalty 10%, urgency 25%)
- Hard constraint: no customer waits > 10 minutes
- Fairness handling (tolerates 1–3 skips)
- Workload balancing across 3 baristas
- Queue transparency (estimated wait, reason, status)
- Batch simulation mode with Monte Carlo averaging

## Tech Stack
- Spring Boot 3.5.10 (Java 17/21)
- Thymeleaf for frontend
- Apache Commons Math3 for Poisson distribution

## How to Run
1. Clone repo
2. `mvn clean package`
3. `mvn spring-boot:run` or run from IDE
4. Open http://localhost:8080/

## Demo
- Place orders at home page
- Watch queue processing at /queue
- Run batch simulation at /simulate