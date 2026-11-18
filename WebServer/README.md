COMP3011 Assignment 1 - Speech-to-Text Web Application

Overview

A Java Spring Boot web application that records audio in the browser, sends it to the backend, and uses the OpenAI Speech-to-Text API to generate a transcription.

Technologies

Java 25
Spring Boot 4.1.1
Maven
HTML and JavaScript
OpenAI Speech-to-Text API
JUnit 5
Mockito

Running

The application reads the OpenAI API key from the operating system environment variable OPENAI_API_KEY.

Build the project with:

mvnw.cmd clean package

Run the application with:

java -jar target/Assignment1-0.0.1-SNAPSHOT.jar

Then open:

[http://localhost:8080/](http://localhost:8080/)

REST API

GET /api/v1/admin/uptime
POST /api/v1/admin/shutdown
GET /api/v1/global/stats
POST /api/v1/transcriptions

The application supports graceful shutdown and tracks cumulative input and output token usage.

Concurrency

Java virtual threads are enabled to support large numbers of simultaneous blocking HTTP requests.

Global token statistics use AtomicLong for thread-safe updates.

Testing

Regression tests cover server uptime, shutdown handling, global token statistics, speech transcription, thread-safe statistics updates, and 250 concurrent blocking HTTP requests.

Run all tests with:

mvnw.cmd test

Security

The OpenAI API key is read from the operating system environment.

It is not hard-coded, not sent to the browser, and not written to logs.

Status

The executable JAR passes all 11 TITAN functional tests.
