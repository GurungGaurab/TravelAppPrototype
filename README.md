# TravelApp

TravelApp is a Spring Boot travel planning prototype that supports user accounts, itinerary planning, booking simulation, budgeting, recommendations, notifications, and shared itineraries.

## CI/CD

This project uses GitHub Actions for continuous integration.

On every push and pull request to `main`, the workflow will:

- compile the application
- run the automated test suite

The workflow file is located at `.github/workflows/ci.yml`.

## Run Locally

```bash
./mvnw spring-boot:run
```
