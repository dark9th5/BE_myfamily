# Implementation Notes: Login/Refresh/Social Auth Flow

## Summary of Changes

This implementation fixes the authentication flow to work end-to-end with database-backed users and proper JWT configuration.

## Changes Made

### 1. Validation (Issue A)
- **File**: `AuthController.kt`
- **Change**: Added `@Valid` annotation to `@RequestBody` parameters in `login()` and `refresh()` methods
- **Impact**: Request validation is now enforced using Jakarta Bean Validation. Empty username/password/refresh_token will return 400 with error details via `GlobalExceptionHandler`

### 2. Database-Backed UserDetailsService (Issue B)
- **Deleted**: `security/UserDetailsConfig.kt` (in-memory configuration)
- **Created**: `security/DatabaseUserDetailsService.kt`
- **Change**: Implements `UserDetailsService` that loads users from the database via `UserRepository`
- **Impact**: Registered users can now log in. Spring Security automatically uses this service for authentication.

### 3. JWT Configuration (Issue C)
- **File**: `security/KeyConfig.kt` (already exists)
- **Status**: ✅ Already implemented correctly
- **Details**: 
  - Generates ephemeral RSA key pair on startup
  - Exposes `JwtEncoder` and `JwtDecoder` beans
  - Validates issuer claim using `security.jwt.issuer` property
  - **Note**: Key pair is ephemeral per restart. For production, replace with persistent keys or configure issuer-uri + JWKS

### 4. Duplicate Endpoint Removal (Issue D)
- **File**: `AuthController.kt`
- **Change**: Removed duplicate `/auth/me` endpoint
- **Impact**: `/auth/me` is now only handled by `MeController` using `JwtAuthenticationToken`

### 5. Social Login User Upsert (Issue E)
- **File**: `auth/social/SocialAuthController.kt`
- **Change**: Implemented user upsert logic for Google and Facebook login
- **Details**:
  - **Google**: Prefers `profile.email` as username, falls back to `google:{sub}`
  - **Facebook**: Prefers `profile.email` as username, falls back to `facebook:{id}`
  - Creates new users with random encoded password (not used for social login)
  - Stores email and profile information in database
- **Impact**: Social login now persists users to DB, enabling refresh token flow to work

### 6. Shared PasswordEncoder (Issue F)
- **File**: `service/UserService.kt`
- **Change**: Inject `PasswordEncoder` bean instead of creating `new BCryptPasswordEncoder()`
- **Impact**: Uses the same password encoder configured in `SecurityConfig` for consistency

## Testing the Flow

### Prerequisites
1. Database must be running and accessible (MySQL at `jdbc:mysql://localhost:3306/family_db`)
2. Configure social login credentials in `application.properties`:
   - `social.google.clientId`
   - `social.facebook.appId` and `social.facebook.appSecret`

### Test Scenarios

#### 1. User Registration & Password Login
```bash
# Register a new user
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'

# Login with registered user
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'

# Response: { "access_token": "...", "refresh_token": "...", "token_type": "Bearer", "expires_in": 900 }
```

#### 2. Refresh Token Flow
```bash
# Use refresh token from login response
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "YOUR_REFRESH_TOKEN_HERE"
  }'

# Response: New access_token and refresh_token
```

#### 3. Validation Errors
```bash
# Login with empty credentials (should return 400)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "password": ""
  }'

# Response: { "status": 400, "error": "BAD_REQUEST", "message": "username: must not be blank; password: must not be blank" }
```

#### 4. Social Login (Google)
```bash
# Login with Google ID token
curl -X POST http://localhost:8080/auth/social/google \
  -H "Content-Type: application/json" \
  -d '{
    "id_token": "GOOGLE_ID_TOKEN_HERE"
  }'

# User is created/updated in database
# Response: { "access_token": "...", "refresh_token": "...", ... }

# Refresh works for social login users too
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "REFRESH_TOKEN_FROM_SOCIAL_LOGIN"
  }'
```

#### 5. Get User Info
```bash
# Use access token from any login method
curl -X GET http://localhost:8080/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Response: { "sub": "username", "email": "...", "name": "...", "picture": null }
```

## Security Considerations

### JWT Keys
- **Current**: Ephemeral RSA key pair generated on startup
- **Production**: Consider:
  - Persistent key pairs stored securely
  - Or configure `spring.security.oauth2.resourceserver.jwt.issuer-uri` to use external authorization server
  - Or use JWKS endpoint for key rotation

### Social Login
- Users created via social login get random passwords
- These passwords are never exposed and cannot be used for password-based login
- Social users must continue using social login or reset password via a separate flow

### Token Rotation
- Refresh tokens are rotated on each use (new refresh_token issued)
- Old refresh tokens become invalid after use
- Access tokens have 15-minute TTL (configurable via `security.jwt.access-token-ttl-seconds`)
- Refresh tokens have 30-day TTL (configurable via `security.jwt.refresh-token-ttl-seconds`)

## Acceptance Criteria Status

- ✅ Registering a user via POST /api/users/register makes that user able to log in via POST /auth/login
- ✅ POST /auth/refresh with a valid refresh_token issues new tokens
- ✅ Social login (Google/Facebook) upserts user records and issues tokens
- ✅ Subsequent refresh works for social login sessions
- ✅ /auth/me resolves only from MeController using JwtAuthenticationToken
- ✅ Validation errors return 400 with GlobalExceptionHandler
- ✅ App starts with working JwtEncoder/Decoder beans
- ✅ Tokens are verifiable by the resource server
- ✅ Issuer is validated

## Known Limitations

1. **Database Required**: Tests will fail without a running database. Consider using H2 for tests.
2. **Ephemeral Keys**: JWT verification fails after app restart (all tokens become invalid).
3. **No Role Management**: All users get ROLE_USER by default. User entity doesn't model roles/authorities.
4. **Social Login Email**: If email is not provided by social provider, a placeholder email is used.
5. **No Email Verification**: Registered users can log in immediately without email verification.

## Next Steps (Optional)

1. Add integration tests that use an embedded database (H2)
2. Implement persistent key storage or external JWT issuer
3. Add role/authority management to User entity
4. Implement password reset flow for social login users
5. Add email verification for registration
6. Add rate limiting for login attempts
7. Implement token revocation/blacklist mechanism
