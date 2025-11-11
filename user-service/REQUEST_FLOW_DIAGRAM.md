# Request Flow Diagram

## Creating a User - Complete Flow

```
┌─────────────┐
│   Client    │
│  (Browser/  │
│   Postman)  │
└──────┬──────┘
       │
       │ POST /api/users
       │ Content-Type: application/json
       │ {
       │   "username": "johndoe",
       │   "email": "john@example.com",
       │   "first_name": "John",
       │   "last_name": "Doe"
       │ }
       │
       ▼
┌─────────────────────────────────────────────────────────┐
│              UserController                             │
│  @PostMapping("/api/users")                            │
│  - Receives HTTP request                               │
│  - Validates request body (@Valid)                     │
│  - Calls UserService.createUser()                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ CreateUserRequest
                     ▼
┌─────────────────────────────────────────────────────────┐
│              GlobalExceptionHandler                     │
│  - Catches validation errors                           │
│  - Returns 400 BAD REQUEST if validation fails         │
│  - Returns formatted error JSON                        │
└─────────────────────────────────────────────────────────┘
                     │
                     │ (if valid)
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserService                                │
│  @Transactional                                         │
│  1. Check if username exists                           │
│  2. Check if email exists                              │
│  3. Create User domain object                          │
│  4. Call UserRepository.save()                         │
│  5. Convert User to UserResponse                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ User (domain model)
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserRepository                             │
│  1. Build SQL INSERT query                             │
│  2. Set parameters (username, email, etc.)             │
│  3. Execute query using NamedParameterJdbcTemplate     │
│  4. Handle DataIntegrityViolationException             │
│  5. Return User with generated ID                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ SQL: INSERT INTO users ...
                     ▼
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL Database                        │
│  1. Check UNIQUE constraints (username, email)         │
│  2. Generate ID (BIGSERIAL)                            │
│  3. Set created_at, updated_at timestamps              │
│  4. Insert row into users table                        │
│  5. Return inserted row                                │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ User (with ID and timestamps)
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserRepository                             │
│  - Maps ResultSet to User record                       │
│  - Returns User object                                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ User (domain model)
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserService                                │
│  - Converts User to UserResponse DTO                   │
│  - Returns UserResponse                                │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ UserResponse
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserController                             │
│  - Returns ResponseEntity with:                        │
│    - Status: 201 CREATED                               │
│    - Body: UserResponse JSON                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ HTTP 201 Created
                     │ {
                     │   "id": 1,
                     │   "username": "johndoe",
                     │   "email": "john@example.com",
                     │   "first_name": "John",
                     │   "last_name": "Doe",
                     │   "created_at": "2024-01-15T10:30:00",
                     │   "updated_at": "2024-01-15T10:30:00"
                     │ }
                     ▼
┌─────────────┐
│   Client    │
│  Receives   │
│  Response   │
└─────────────┘
```

## Error Flow Example - Duplicate Username

```
┌─────────────┐
│   Client    │
│  POST /api/users
│  { "username": "johndoe", ... }
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────────────┐
│              UserController                             │
│  - Validation passes                                    │
│  - Calls UserService.createUser()                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserService                                │
│  - Calls userRepository.findByUsername("johndoe")      │
│  - User exists!                                         │
│  - Throws IllegalArgumentException                      │
│    "Username already exists: johndoe"                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ IllegalArgumentException
                     ▼
┌─────────────────────────────────────────────────────────┐
│              GlobalExceptionHandler                     │
│  @ExceptionHandler(IllegalArgumentException.class)     │
│  - Catches exception                                    │
│  - Returns 400 BAD REQUEST                             │
│  - Body: { "error": "Username already exists: johndoe" }│
└────────────────────┬────────────────────────────────────┘
                     │
                     │ HTTP 400 Bad Request
                     ▼
┌─────────────┐
│   Client    │
│  Receives   │
│  Error      │
└─────────────┘
```

## Getting a User by ID - Complete Flow

```
┌─────────────┐
│   Client    │
│  GET /api/users/1
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────────────┐
│              UserController                             │
│  @GetMapping("/api/users/{id}")                        │
│  - Extracts id from URL path                           │
│  - Calls UserService.getUserById(1)                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ id = 1
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserService                                │
│  - Calls userRepository.findById(1)                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ id = 1
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserRepository                             │
│  - Executes SQL: SELECT * FROM users WHERE id = 1      │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ SQL Query
                     ▼
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL Database                        │
│  - Finds user with id = 1                              │
│  - Returns row data                                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ ResultSet (user data)
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserRepository                             │
│  - Maps ResultSet to User record                       │
│  - Returns Optional<User>                              │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Optional<User> (present)
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserService                                │
│  - Converts User to UserResponse                       │
│  - Returns UserResponse                                │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ UserResponse
                     ▼
┌─────────────────────────────────────────────────────────┐
│              UserController                             │
│  - Returns ResponseEntity with:                        │
│    - Status: 200 OK                                    │
│    - Body: UserResponse JSON                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ HTTP 200 OK
                     │ { "id": 1, "username": "johndoe", ... }
                     ▼
┌─────────────┐
│   Client    │
│  Receives   │
│  Response   │
└─────────────┘
```

## Layer Responsibilities

### Controller Layer
- **Responsibility**: Handle HTTP requests/responses
- **Does**: 
  - Validate request format
  - Map HTTP to service calls
  - Return HTTP status codes
  - Convert service responses to JSON
- **Does NOT**: 
  - Know about database
  - Contain business logic
  - Handle SQL queries

### Service Layer
- **Responsibility**: Business logic
- **Does**:
  - Validate business rules
  - Coordinate multiple repository calls
  - Handle transactions
  - Convert domain models to DTOs
- **Does NOT**:
  - Know about HTTP
  - Know about SQL
  - Handle HTTP status codes

### Repository Layer
- **Responsibility**: Data access
- **Does**:
  - Execute SQL queries
  - Map database rows to domain models
  - Handle database exceptions
- **Does NOT**:
  - Know about HTTP
  - Contain business logic
  - Know about DTOs

### Database Layer
- **Responsibility**: Data persistence
- **Does**:
  - Store data
  - Enforce constraints
  - Generate IDs
  - Handle transactions
- **Does NOT**:
  - Know about Java
  - Know about business logic
  - Know about HTTP

## Data Flow Transformation

```
HTTP Request (JSON)
    ↓
CreateUserRequest (DTO)
    ↓
User (Domain Model)
    ↓
SQL INSERT
    ↓
Database Row
    ↓
User (Domain Model)
    ↓
UserResponse (DTO)
    ↓
HTTP Response (JSON)
```

## Why This Layered Approach?

1. **Separation of Concerns**: Each layer has one responsibility
2. **Testability**: Can test each layer independently
3. **Maintainability**: Changes in one layer don't affect others
4. **Scalability**: Can optimize each layer independently
5. **Reusability**: Service layer can be used by different controllers (REST, GraphQL, etc.)

