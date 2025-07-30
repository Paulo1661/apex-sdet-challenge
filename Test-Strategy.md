# 🧪 QA Test Strategy – Email Micro-Service

## 1. 🎯 Objectives

* Verify the correct behavior of the `/send-email` endpoint.
* Ensure robustness against invalid inputs.
* Identify functional and non-functional boundaries.

## 2. 📌 Scope

### In-Scope

* POST request to `/send-email`
* HTTP responses: 202, 422, 503
* JSON schema validation

### Out-of-Scope

* Actual email sending/receiving
* Authentication

## 3. 🧪 Test Types

* Functional Testing
* Negative Testing
* Boundary/Edge Case Testing
* Schema Validation
* Load Simulation
* Security Testing

## 4. 🛠 Tools

* Rest-Assured via Serenity BDD
* Google Sheets (test cases)
* WireMock for mocking the email service
* Gatling for load testing
* GitHub

## 5. ⚠️ Risks & Assumptions

* **Risk**: Job workers fail without actually sending the email
* **Assumption**: No authentication required
* **Assumption**: A `202` response means the job was accepted without error
