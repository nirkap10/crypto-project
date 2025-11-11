# User Service - Complete Architecture Explanation

## Table of Contents
1. [What Was Built](#what-was-built)
2. [Why Each Component Was Necessary](#why-each-component-was-necessary)
3. [Design Patterns & Principles](#design-patterns--principles)
4. [RESTful API Principles Applied](#restful-api-principles-applied)
5. [Future Considerations & Next Steps](#future-considerations--next-steps)

---

## What Was Built

### 1. Project Structure Setup
- **Added user-service module to parent pom.xml**
- **Created user-service/pom.xml** with all necessary dependencies
- **Created Spring Boot application class** (`UserServiceApplication.java`)

### 2. Database Layer
- **Flyway migration script** (`V1__create_users.sql`)
- **Users table** with proper schema, constraints, and indexes

### 3. Application Layers
- **Model**: `User` record (domain entity)
- **Repository**: `UserRepository` (data access)
- **Service**: `UserService` (business logic)
- **Controller**: `UserController` (REST API endpoints)
- **DTOs**: `CreateUserRequest`, `UserResponse` (data transfer objects)
- **Exception Handling**: `GlobalExceptionHandler`, `UserNotFoundException`

### 4. Configuration
- **application.properties** with database connection, Flyway, and server port
- **Test class** for basic context loading

---

## Why Each Component Was Necessary

### 1. Module Structure (pom.xml)

**What**: Added user-service as a separate Maven module

**Why Necessary**:
- **Microservices Architecture**: Your project follows a microservices pattern (api, price-ingestor, user-service). Each service should be independently deployable.
- **Separation of Concerns**: User management is a distinct domain that should be isolated.
- **Scalability**: You can scale user-service independently from other services.
- **Technology Flexibility**: Each service can evolve its tech stack independently.

**Dependencies Added**:
- `spring-boot-starter-web`: REST API capabilities
- `spring-boot-starter-validation`: Input validation
- `spring-boot-starter-jdbc`: Database access (matching price-ingestor pattern)
- `flyway-core` + `flyway-database-postgresql`: Database migrations
- `postgresql`: PostgreSQL driver

---

### 2. Database Migration (Flyway)

**What**: `V1__create_users.sql` creates the users table

**Why Necessary**:
- **Version Control**: Database schema changes are tracked in code
- **Reproducibility**: Any environment can be set up with the same schema
- **Team Collaboration**: Everyone gets the same database structure
- **Consistency**: Matches the pattern used in price-ingestor (`V1__create_prices.sql`)

**Design Decisions**:
```sql
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,           -- Auto-incrementing ID
  username VARCHAR(100) NOT NULL UNIQUE,  -- Must be unique
  email VARCHAR(255) NOT NULL UNIQUE,     -- Must be unique
  first_name VARCHAR(100),                -- Optional
  last_name VARCHAR(100),                 -- Optional
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),  -- Audit field
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()   -- Audit field
);
```

**Why These Fields**:
- `id`: Primary key for relationships and lookups
- `username` + `email` UNIQUE: Prevents duplicates at database level (defense in depth)
- `created_at` / `updated_at`: Audit trail (who created when, last modification)
- Indexes on username/email: Fast lookups (O(log n) instead of O(n))

---

### 3. Layered Architecture

#### Layer 1: Model (`User` record)

**What**: Java record representing a user entity

**Why Necessary**:
- **Domain Model**: Represents the core business concept
- **Immutability**: Records are immutable by default (thread-safe, predictable)
- **Validation**: Constructor validation ensures data integrity
- **Type Safety**: Compile-time guarantees about data structure

**Design Decision - Why Records?**:
- Modern Java feature (Java 14+)
- Less boilerplate than classes
- Immutable by default
- Perfect for DTOs and domain models

---

#### Layer 2: Repository (`UserRepository`)

**What**: Data access layer using `NamedParameterJdbcTemplate`

**Why Necessary**:
- **Separation of Concerns**: Database logic separated from business logic
- **SQL Injection Prevention**: Named parameters prevent SQL injection
- **Reusability**: Repository methods can be reused across services
- **Testability**: Can be mocked for unit testing
- **Consistency**: Matches pattern used in `PriceRepository`

**Key Methods**:
- `save(User)`: Insert new user, returns user with generated ID
- `findById(Long)`: Get user by primary key
- `findByUsername(String)`: Get user by username (for login/auth)
- `findByEmail(String)`: Get user by email (for registration checks)
- `findAll()`: Get all users (for admin/list views)

**Why NamedParameterJdbcTemplate?**:
- More readable than positional parameters (`?`)
- Prevents SQL injection
- Easy parameter binding
- Matches your existing codebase pattern

**Error Handling**:
- `DataIntegrityViolationException`: Catches unique constraint violations
- Converts database errors to meaningful business exceptions
- `EmptyResultDataAccessException`: Returns `Optional.empty()` for not found

---

#### Layer 3: Service (`UserService`)

**What**: Business logic layer

**Why Necessary**:
- **Business Rules**: Encapsulates business logic (e.g., "username must be unique")
- **Transaction Management**: `@Transactional` ensures data consistency
- **Validation**: Pre-validates before database operations
- **Abstraction**: Controller doesn't need to know about database details

**Key Operations**:

1. **createUser()**:
   - Checks if username exists (before database)
   - Checks if email exists (before database)
   - Creates user entity
   - Saves to database
   - Returns DTO (not domain model)

2. **getUserById()**:
   - Fetches user
   - Throws `UserNotFoundException` if not found (not null)
   - Converts to DTO

3. **getUserByUsername()**:
   - Useful for authentication/login scenarios
   - Returns user by username

4. **getAllUsers()**:
   - Returns list of all users
   - Converts domain models to DTOs

**Why @Transactional?**:
- Ensures all database operations succeed or fail together
- Prevents partial updates
- Important for multi-step operations

---

#### Layer 4: Controller (`UserController`)

**What**: REST API endpoints

**Why Necessary**:
- **API Gateway**: Exposes business functionality via HTTP
- **HTTP Semantics**: Maps HTTP methods to operations
- **Request/Response Handling**: Converts HTTP requests to service calls
- **Status Codes**: Returns appropriate HTTP status codes

**Endpoints**:

1. `POST /api/users` - Create user
2. `GET /api/users/{id}` - Get user by ID
3. `GET /api/users/username/{username}` - Get user by username
4. `GET /api/users` - Get all users

---

#### Layer 5: DTOs (Data Transfer Objects)

**What**: `CreateUserRequest` and `UserResponse`

**Why Necessary**:
- **API Contract**: Defines what clients send/receive
- **Separation**: API structure can differ from database structure
- **Validation**: Request DTOs have validation annotations
- **Security**: Don't expose internal domain model details
- **Versioning**: Can evolve API without changing database

**CreateUserRequest**:
- Validation annotations (`@NotBlank`, `@Email`, `@Size`)
- JSON property mapping (`@JsonProperty("first_name")`)
- Validated before reaching service layer

**UserResponse**:
- Only exposes what clients need
- Includes timestamps (created_at, updated_at)
- Factory method `from(User)` converts domain to DTO

---

#### Layer 6: Exception Handling

**What**: `GlobalExceptionHandler` and `UserNotFoundException`

**Why Necessary**:
- **Consistent Error Responses**: All errors follow same format
- **HTTP Status Codes**: Maps exceptions to proper status codes
- **User-Friendly Messages**: Converts technical errors to readable messages
- **Centralized**: One place handles all exceptions

**Exception Mapping**:
- `UserNotFoundException` → 404 NOT FOUND
- `IllegalArgumentException` → 400 BAD REQUEST
- `MethodArgumentNotValidException` → 400 BAD REQUEST (validation errors)
- `Exception` → 500 INTERNAL SERVER ERROR

**Why Separate Exception Types?**:
- Different exceptions need different status codes
- `UserNotFoundException` → 404 (resource not found)
- `IllegalArgumentException` → 400 (bad request, e.g., duplicate username)

---

## Design Patterns & Principles

### 1. Layered Architecture
```
Controller → Service → Repository → Database
```

**Benefits**:
- Separation of concerns
- Easy to test each layer
- Changes in one layer don't affect others

### 2. Dependency Injection
- Constructor injection (not field injection)
- Spring manages dependencies
- Easy to mock for testing

### 3. Repository Pattern
- Abstracts database access
- Can swap database implementations
- Single Responsibility Principle

### 4. DTO Pattern
- Separates API from domain model
- Can version API independently
- Hides internal structure

### 5. Exception Handling Pattern
- Global exception handler
- Custom exceptions for different scenarios
- Consistent error responses

---

## RESTful API Principles Applied

### 1. Resource-Based URLs
✅ **Applied**: `/api/users` represents the "users" resource
- Resources are nouns, not verbs
- URLs are hierarchical and intuitive

### 2. HTTP Methods Map to Operations
✅ **Applied**:
- `POST /api/users` → Create user
- `GET /api/users/{id}` → Read user
- `GET /api/users` → List users

**Why Not PUT/PATCH/DELETE?**:
- Not implemented yet (future step)
- Current scope: Create and Read operations

### 3. HTTP Status Codes
✅ **Applied**:
- `201 CREATED` → User successfully created
- `200 OK` → Successful GET request
- `400 BAD REQUEST` → Validation errors, duplicate username/email
- `404 NOT FOUND` → User not found
- `500 INTERNAL SERVER ERROR` → Unexpected errors

### 4. Stateless
✅ **Applied**: Each request contains all information needed
- No server-side session state
- Can scale horizontally

### 5. JSON Request/Response
✅ **Applied**: 
- Request body is JSON
- Response body is JSON
- Content-Type: `application/json`

### 6. Resource Identification
✅ **Applied**:
- Users identified by ID in URL: `/api/users/{id}`
- Primary key used for resource identification

### 7. Error Handling
✅ **Applied**:
- Errors returned as JSON
- Appropriate status codes
- Error messages are descriptive

### 8. RESTful Naming Conventions
✅ **Applied**:
- Plural nouns: `/api/users` (not `/api/user`)
- Lowercase URLs
- Hyphens for multi-word (snake_case in JSON: `first_name`)

### What Could Be More RESTful?

**Current**: `GET /api/users/username/{username}`
**More RESTful**: `GET /api/users?username={username}` (query parameter)

**Why I Chose Path Parameter**:
- More explicit
- Easier to understand
- Still RESTful (resources can have multiple identifiers)

---

## Future Considerations & Next Steps

### What I Thought About While Building

### 1. **Authentication & Authorization**
- **Current**: No authentication (anyone can create users)
- **Next Step**: Add JWT tokens, OAuth2, or API keys
- **Why Important**: Protect user data, prevent abuse

### 2. **Password Management**
- **Current**: No password field
- **Next Step**: Add password field (hashed with BCrypt)
- **Why Important**: Users need to authenticate

### 3. **Update & Delete Operations**
- **Current**: Only Create and Read
- **Next Step**: 
  - `PUT /api/users/{id}` - Full update
  - `PATCH /api/users/{id}` - Partial update
  - `DELETE /api/users/{id}` - Delete user
- **Why Important**: Complete CRUD operations

### 4. **Pagination**
- **Current**: `getAllUsers()` returns all users
- **Next Step**: Add pagination (`?page=1&size=20`)
- **Why Important**: Performance with large datasets

### 5. **Filtering & Sorting**
- **Current**: No filtering
- **Next Step**: `GET /api/users?email=john@example.com&sort=created_at:desc`
- **Why Important**: Find users efficiently

### 6. **Email Verification**
- **Current**: Email stored but not verified
- **Next Step**: Send verification email, mark as verified
- **Why Important**: Ensure valid email addresses

### 7. **Rate Limiting**
- **Current**: No rate limiting
- **Next Step**: Limit requests per IP/user
- **Why Important**: Prevent abuse, DDoS protection

### 8. **Logging & Monitoring**
- **Current**: Basic Spring Boot logging
- **Next Step**: Structured logging, metrics, tracing
- **Why Important**: Debug issues, monitor performance

### 9. **API Versioning**
- **Current**: No versioning
- **Next Step**: `/api/v1/users`, `/api/v2/users`
- **Why Important**: Evolve API without breaking clients

### 10. **Database Connection Pooling**
- **Current**: Spring Boot default (HikariCP)
- **Next Step**: Configure pool size, timeouts
- **Why Important**: Handle concurrent requests

### 11. **Caching**
- **Current**: No caching
- **Next Step**: Cache user lookups (Redis, Caffeine)
- **Why Important**: Reduce database load

### 12. **Testing**
- **Current**: Basic test class
- **Next Step**: 
  - Unit tests for service layer
  - Integration tests for repository
  - API tests for controller
- **Why Important**: Ensure code works, prevent regressions

### 13. **API Documentation**
- **Current**: No documentation
- **Next Step**: OpenAPI/Swagger documentation
- **Why Important**: Developers need to know how to use API

### 14. **Database Transactions**
- **Current**: `@Transactional` on createUser
- **Next Step**: Consider transaction isolation levels
- **Why Important**: Handle concurrent updates

### 15. **Soft Deletes**
- **Current**: Hard delete (when implemented)
- **Next Step**: Add `deleted_at` field, filter deleted users
- **Why Important**: Data recovery, audit trail

---

## Summary

### What Was Built
- Complete user service with REST API
- Database migration with Flyway
- Layered architecture (Controller → Service → Repository)
- Exception handling
- Input validation
- DTOs for API contract

### Why It Was Necessary
- Follows microservices architecture
- Separates concerns (each layer has one responsibility)
- Matches existing codebase patterns (price-ingestor)
- Provides RESTful API for user management
- Handles errors consistently

### RESTful Principles Applied
- Resource-based URLs
- HTTP methods map to operations
- Proper status codes
- JSON request/response
- Stateless design
- Error handling

### Next Steps Considered
- Authentication & authorization
- Password management
- Update & delete operations
- Pagination & filtering
- Email verification
- Rate limiting
- Logging & monitoring
- API versioning
- Caching
- Testing
- API documentation

This architecture is **scalable**, **maintainable**, and **follows industry best practices**. It's ready for production with additional features added incrementally.

