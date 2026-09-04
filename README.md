# CSV Processor - Spring Boot

Spring Boot REST API that accepts a CSV file upload and returns a modified CSV.

## Prerequisites
- Java 17+
- Maven

## Run
```bash
mvn spring-boot:run
```

## API
- **GET** `/csv/health` — Health check
- **POST** `/csv/upload` — Upload a CSV file, returns modified CSV

## Test with curl
```bash
curl -F "file=@sample.csv" http://localhost:8080/csv/upload -o modified_output.csv
```
