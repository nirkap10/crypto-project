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
  - **Database Logic** (Repository): Handles SQL queries, parameter binding, and row mapping. Focuses on "HOW to get/store data"
  - **Business Logic** (Service): Handles rules, validations, and workflows. Focuses on "WHAT the application should do"
  - Example: Repository executes `SELECT * FROM users WHERE username = :username`, while Service decides "if username exists, throw error"
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

**What Are Named Parameters?**:
Named parameters use descriptive names (like `:username`, `:email`) instead of positional placeholders (`?`).

**Example from UserRepository**:
```java
String sql = "SELECT * FROM users WHERE username = :username";
MapSqlParameterSource params = new MapSqlParameterSource()
    .addValue("username", username);  // Binds "username" to :username
```

**Comparison: Named Parameters vs Positional Parameters**:

| Aspect | Named Parameters (`:username`) | Positional Parameters (`?`) |
|--------|-------------------------------|----------------------------|
| **Readability** | ✅ Clear: `WHERE username = :username AND email = :email` | ❌ Unclear: `WHERE username = ? AND email = ?` |
| **Order Dependency** | ✅ Order doesn't matter | ❌ Must match exact order |
| **SQL Injection** | ✅ Safe (auto-escaped) | ✅ Safe (if used correctly) |
| **Maintenance** | ✅ Easy to add/remove parameters | ❌ Hard to maintain with many parameters |

**Why NamedParameterJdbcTemplate?**:
- **More Readable**: `:username` is clearer than `?` (especially with multiple parameters)
- **Prevents SQL Injection**: Parameters are automatically escaped and sanitized
- **Easy Parameter Binding**: Map parameter names to values explicitly (order doesn't matter)
- **Type Safety**: Compile-time checking of parameter names
- **Maintainability**: Easier to add/remove parameters without reordering
- **Matches Pattern**: Consistent with your existing `PriceRepository` codebase

**SQL Injection Prevention Example**:
```java
// ❌ UNSAFE - String concatenation (DO NOT DO THIS!)
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
// If username = "admin' OR '1'='1", this becomes:
// SELECT * FROM users WHERE username = 'admin' OR '1'='1' (SQL INJECTION!)

// ✅ SAFE - Named parameters (automatically escaped)
String sql = "SELECT * FROM users WHERE username = :username";
MapSqlParameterSource params = new MapSqlParameterSource()
    .addValue("username", username);  // Automatically escapes special characters
```

**Error Handling**:
- `DataIntegrityViolationException`: Catches unique constraint violations
- Converts database errors to meaningful business exceptions
- `EmptyResultDataAccessException`: Returns `Optional.empty()` for not found

**Comparison: Database Logic vs Business Logic**:

| Aspect | Database Logic (Repository) | Business Logic (Service) |
|--------|----------------------------|--------------------------|
| **Purpose** | "HOW to get/store data" | "WHAT should the application do" |
| **Focus** | SQL queries, parameter binding, row mapping | Rules, validations, workflows |
| **Example** | `SELECT * FROM users WHERE id = :id` | "If user not found, throw UserNotFoundException" |
| **Responsibilities** | Execute SQL, map rows to objects | Enforce business rules, coordinate operations |
| **Error Handling** | Database exceptions (SQLException, DataIntegrityViolationException) | Business exceptions (UserNotFoundException, IllegalArgumentException) |

**How Does Repository Achieve Separation?**

The Repository pattern creates separation through **abstraction** and **clear boundaries**:

1. **Abstraction Layer**: Repository hides all database implementation details from Service
   - Service doesn't see SQL queries
   - Service doesn't see JDBC code
   - Service doesn't see database-specific exceptions
   - Service only sees simple method calls like `findById(id)` or `save(user)`

2. **Simple Interface**: Repository provides clean, domain-focused methods
   ```java
   // Service sees this simple interface:
   Optional<User> findById(Long id);
   Optional<User> findByUsername(String username);
   User save(User user);
   
   // Service DOESN'T see:
   // - SQL queries
   // - NamedParameterJdbcTemplate
   // - MapSqlParameterSource
   // - ResultSet mapping
   // - Database connection details
   ```

3. **Dependency Direction**: Service depends on Repository (one-way dependency)
   - Service knows about Repository
   - Repository doesn't know about Service
   - Repository doesn't know about business rules
   - Repository doesn't know about DTOs

4. **Single Responsibility**: Each layer has one clear job
   - Repository: "Get/store data from database"
   - Service: "Enforce business rules and coordinate operations"

**Concrete Example: How Separation Works**

**In UserService.createUser()** (Business Logic):
```java
// Service layer - Business logic
public UserResponse createUser(CreateUserRequest request) {
    // Business rule: Check if username exists
    userRepository.findByUsername(request.username())  // ← Simple method call
        .ifPresent(user -> {
            throw new IllegalArgumentException("Username already exists");  // Business rule
        });
    
    // Business rule: Check if email exists
    userRepository.findByEmail(request.email())  // ← Simple method call
        .ifPresent(user -> {
            throw new IllegalArgumentException("Email already exists");  // Business rule
        });
    
    // Create user entity
    User user = new User(...);
    
    // Save to database (delegates to Repository)
    User savedUser = userRepository.save(user);  // ← Simple method call
    
    // Business rule: Return DTO, not domain model
    return UserResponse.from(savedUser);  // Business rule
}
```

**In UserRepository.findByUsername()** (Database Logic):
```java
// Repository layer - Database logic
public Optional<User> findByUsername(String username) {
    // Database logic: SQL query
    String sql = "SELECT * FROM users WHERE username = :username";
    
    // Database logic: Parameter binding
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("username", username);
    
    // Database logic: Execute query and map results
    try {
        User user = jdbc.queryForObject(sql, params, (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            // ... map database rows to Java object
        ));
        return Optional.ofNullable(user);
    } catch (EmptyResultDataAccessException e) {
        return Optional.empty();  // Database-level error handling
    }
}
```

**What Service Doesn't Know**:
- ❌ Service doesn't know about SQL
- ❌ Service doesn't know about `NamedParameterJdbcTemplate`
- ❌ Service doesn't know about `MapSqlParameterSource`
- ❌ Service doesn't know about `ResultSet` mapping
- ❌ Service doesn't know about database connection pooling
- ❌ Service doesn't know about database-specific exceptions

**What Repository Doesn't Know**:
- ❌ Repository doesn't know about business rules (e.g., "username must be unique")
- ❌ Repository doesn't know about DTOs (`UserResponse`, `CreateUserRequest`)
- ❌ Repository doesn't know about HTTP status codes (404, 400, etc.)
- ❌ Repository doesn't know about `@Transactional` annotations
- ❌ Repository doesn't know about REST API endpoints

**Benefits of This Separation**:

1. **Testability**: You can test Service without a real database
   ```java
   // Mock the repository in tests
   UserRepository mockRepo = mock(UserRepository.class);
   when(mockRepo.findByUsername("test")).thenReturn(Optional.empty());
   UserService service = new UserService(mockRepo);
   ```

2. **Flexibility**: You can change database without changing Service
   - Switch from PostgreSQL to MySQL? Only change Repository
   - Switch from JDBC to JPA? Only change Repository
   - Service code remains unchanged

3. **Maintainability**: Changes are isolated
   - Need to change SQL query? Only modify Repository
   - Need to change business rule? Only modify Service

4. **Reusability**: Repository can be used by multiple Services
   - `UserService` uses `UserRepository`
   - `AuthService` can also use `UserRepository`
   - `AdminService` can also use `UserRepository`

**What Would Happen Without Separation? (Bad Example)**

If business logic and database logic were mixed:

```java
// ❌ BAD: Service with database logic mixed in
@Service
public class UserService {
    private final NamedParameterJdbcTemplate jdbc;  // ← Database detail exposed!
    
    public UserResponse createUser(CreateUserRequest request) {
        // Business logic mixed with database logic
        String sql = "SELECT * FROM users WHERE username = :username";  // ← SQL in Service!
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("username", request.username());
        
        try {
            User existing = jdbc.queryForObject(sql, params, ...);  // ← JDBC in Service!
            throw new IllegalArgumentException("Username exists");
        } catch (EmptyResultDataAccessException e) {
            // Continue...
        }
        
        // More SQL in Service...
        String insertSql = "INSERT INTO users ...";  // ← More SQL!
        // ...
    }
}
```

**Problems with Mixed Approach**:
- ❌ Service is tightly coupled to database implementation
- ❌ Can't test Service without database
- ❌ Can't change database without changing Service
- ❌ Business rules mixed with SQL queries
- ❌ Hard to reuse database operations
- ❌ Violates Single Responsibility Principle

---

#### Layer 3: Service (`UserService`)

**What**: Business logic layer

**Why Necessary**:
- **Business Rules**: Encapsulates business logic (e.g., "username must be unique")
- **Transaction Management**: `@Transactional` ensures data consistency
- **Validation**: Pre-validates before database operations
- **Abstraction**: Controller doesn't need to know about database details

**What is Business Logic?**:
Business logic implements the **rules and workflows** of your application. It answers "WHAT should the application do?" rather than "HOW to do it".

**Examples of Business Logic in UserService**:
- **Rule**: "Username must be unique" → Service checks if username exists before creating
- **Rule**: "Email must be unique" → Service checks if email exists before creating  
- **Rule**: "If user not found, return 404 error" → Service throws `UserNotFoundException`
- **Rule**: "API should return DTOs, not domain models" → Service converts `User` to `UserResponse`
- **Workflow**: "Create user transactionally" → Service uses `@Transactional` to ensure all-or-nothing

**Key Operations**:

1. **createUser()**:
   - **Business Logic**: Checks if username exists (business rule enforcement)
   - **Business Logic**: Checks if email exists (business rule enforcement)
   - Creates user entity
   - Saves to database (delegates to Repository)
   - **Business Logic**: Returns DTO (not domain model) - API contract enforcement

2. **getUserById()**:
   - Fetches user (delegates to Repository)
   - **Business Logic**: Throws `UserNotFoundException` if not found (business rule: "resource not found = 404")
   - **Business Logic**: Converts to DTO (API contract)

3. **getUserByUsername()**:
   - Useful for authentication/login scenarios
   - Returns user by username
   - **Business Logic**: Converts to DTO and handles not-found scenario

4. **getAllUsers()**:
   - Returns list of all users
   - **Business Logic**: Converts domain models to DTOs (API contract)

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

