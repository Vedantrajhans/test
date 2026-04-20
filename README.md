Name : Vedant Rajhans
2026 Induction Batch 
Title : Event Management Platform (Concert)

# Festiva - Concert Management Platform

Festiva is a full-stack web application designed to manage concerts, bookings, and user interactions efficiently. It provides a seamless experience for organizers to create events and for users to explore and book concerts.

---

## Features

### User Features

* Register and login securely (JWT-based authentication)
* Browse available concerts
* View concert details (venue, date, ticket types)
* Book tickets for concerts
* Provide feedback/reviews (only after attending)

### Organizer Features

* Create and manage concerts
* Add ticket types and pricing
* View bookings and user engagement

### Security

* Stateless authentication using JWT
* Role-based access control (User / Organizer)
* Protected APIs using Spring Security

---

## Tech Stack

### Backend

* Java 17
* Spring Boot
* Spring Security (JWT)
* Spring Data JPA (Hibernate)
* PostgreSQL

### Frontend

* React.js
* Axios (API communication)

### Tools & Others

* Maven
* Git & Bitbucket
* REST APIs

---

##  Setup Instructions

### Clone the Repository

```
git clone <your-repo-url>
cd vedant_induction_fork
```

---

### Configure Database (PostgreSQL)

Create a database:

```
CREATE DATABASE festiva;
```

Update `application.properties`:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/festiva
spring.datasource.username=your_username
spring.datasource.password=your_password
```

---

### Run Backend

```
mvn clean install
mvn spring-boot:run
```

Server will start on:

```
http://localhost:8080
```

---

### Run Frontend

```
cd client
npm install
npm start
```
---

## Important Notes

* `application.properties` is ignored for security reasons.
* Only users with confirmed bookings can submit feedback.
* Ensure PostgreSQL is running before starting backend.

---

## Future Enhancements

* Payment gateway integration
* Email notifications
* Admin dashboard
* Advanced search & filters

---

## License

This project is for learning and demonstration purposes.
