# TravelApp

TravelApp is a Spring Boot travel planning prototype that supports user accounts, itinerary planning, booking simulation, budgeting, recommendations, notifications, and shared itineraries.

## CI/CD

This project uses GitHub Actions for continuous integration.

On every push and pull request to `main`, the workflow will:

- compile the application
- run the automated test suite

The workflow file is located at `.github/workflows/ci.yml`.

## Run Locally

## Getting Started

1. *Clone the repository*
   
   git clone https://github.com/GurungGaurab/TravelAppPrototype.git
   cd TravelAppPrototype
   

2. *Build the project*
   
   mvn clean install
   

3. *Run the application*
   
   mvn spring-boot:run
   

4. *Open in browser*
   
   http://localhost:8080
   

```bash
./mvnw spring-boot:run
```
