# API Documentation - Hệ Thống Trạm Cảm Xúc MCS

## Thông tin chung

**Base URL (API Gateway):** `http://localhost:8080`

Tất cả các API phải được gọi qua API Gateway trên port 8080. Gateway sẽ tự động route requests đến các service tương ứng:
- `service-identity` - Port 8082
- `service-music` - Port 8081  
- `service-payment` - Port 8085

**URL Pattern:** `http://localhost:8080/{service-name}/{endpoint}`

Ví dụ:
- `http://localhost:8080/service-identity/auth/login`
- `http://localhost:8080/service-music/songs`
- `http://localhost:8080/service-payment/plans`

---

## 1. SERVICE-IDENTITY APIs

Base Path: `/service-identity`

### 1.1. Authentication APIs (`/auth`)

#### 1.1.1. Đăng nhập
```
POST /service-identity/auth/login
```

**Request Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "accessToken": "string",
    "refreshToken": "string",
    "authenticated": true
  }
}
```

---

#### 1.1.2. Kiểm tra token (Introspect)
```
POST /service-identity/auth/introspect
```

**Request Body:**
```json
{
  "token": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "valid": true
  }
}
```

---

#### 1.1.3. Làm mới token
```
POST /service-identity/auth/refresh
```

**Request Body:**
```json
{
  "token": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "accessToken": "string",
    "refreshToken": "string",
    "authenticated": true
  }
}
```

---

#### 1.1.4. Đăng xuất
```
POST /service-identity/auth/logout
```

**Request Body:**
```json
{
  "token": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "Logout successful",
  "result": null
}
```

---

#### 1.1.5. Đăng nhập bằng OAuth2 (Google/Facebook)
```
POST /service-identity/auth/outbound/authentication?type={google|facebook}
```

**Query Parameters:**
- `type`: "google" hoặc "facebook"

**Request Body:**
```json
{
  "token": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "accessToken": "string",
    "refreshToken": "string",
    "authenticated": true
  }
}
```

---

#### 1.1.6. Quên mật khẩu (Gửi OTP)
```
POST /service-identity/auth/forgot-password?email={email}
```

**Query Parameters:**
- `email`: Email của người dùng

**Response:**
```json
{
  "code": 1000,
  "message": "OTP sent to email",
  "result": null
}
```

---

#### 1.1.7. Đặt lại mật khẩu
```
POST /service-identity/auth/reset-password
```

**Request Body:**
```json
{
  "email": "string",
  "otp": "string",
  "newPassword": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "Password reset successfully",
  "result": null
}
```

---

### 1.2. User APIs (`/users`)

#### 1.2.1. Đăng ký tài khoản
```
POST /service-identity/users/registration
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "string",
  "lastName": "string",
  "dob": "2000-01-01"
}
```

**Validations:**
- Email: bắt buộc và đúng định dạng
- Password: tối thiểu 8 ký tự
- FirstName & LastName: bắt buộc

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "2000-01-01",
    "avatarUrl": "string",
    "role": "USER",
    "status": "PENDING_VERIFICATION"
  }
}
```

---

#### 1.2.2. Xác thực email
```
POST /service-identity/users/verify-email
```

**Request Body:**
```json
{
  "email": "string",
  "otpCode": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "Email verified successfully",
  "result": null
}
```

---

#### 1.2.3. Lấy thông tin người dùng hiện tại
```
GET /service-identity/users/my-info
```

**Headers:**
- `Authorization`: Bearer {accessToken}

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "2000-01-01",
    "avatarUrl": "string",
    "role": "USER",
    "status": "ACTIVE"
  }
}
```

---

#### 1.2.4. Gửi lại OTP đăng ký
```
POST /service-identity/users/resend-registration-otp?email={email}
```

**Query Parameters:**
- `email`: Email cần gửi lại OTP

**Response:**
```json
{
  "code": 1000,
  "message": "OTP has been resent",
  "result": null
}
```

---

#### 1.2.5. Lấy danh sách người dùng (Phân trang & Tìm kiếm)
```
GET /service-identity/users?page={page}&size={size}&keyword={keyword}
```

**Query Parameters:**
- `page`: Số trang (mặc định: 1)
- `size`: Số lượng mỗi trang (mặc định: 10)
- `keyword`: Từ khóa tìm kiếm (optional) - tìm theo email, firstName, lastName

**Headers:**
- `Authorization`: Bearer {accessToken}

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "currentPage": 1,
    "totalPages": 10,
    "pageSize": 10,
    "totalElements": 100,
    "data": [
      {
        "id": "uuid",
        "email": "string",
        "firstName": "string",
        "lastName": "string",
        "dob": "2000-01-01",
        "avatarUrl": "string",
        "role": "USER",
        "status": "ACTIVE"
      }
    ]
  }
}
```

---

#### 1.2.6. Cập nhật thông tin cá nhân
```
PUT /service-identity/users/me
```

**Headers:**
- `Authorization`: Bearer {accessToken}

**Request Body:**
```json
{
  "firstName": "string",
  "lastName": "string",
  "dob": "2000-01-01",
  "avatarUrl": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "2000-01-01",
    "avatarUrl": "string",
    "role": "USER",
    "status": "ACTIVE"
  }
}
```

---

#### 1.2.7. Đổi mật khẩu
```
POST /service-identity/users/change-password
```

**Headers:**
- `Authorization`: Bearer {accessToken}

**Request Body:**
```json
{
  "oldPassword": "string",
  "newPassword": "string"
}
```

**Validations:**
- newPassword: tối thiểu 8 ký tự

**Response:**
```json
{
  "code": 1000,
  "message": "Password changed successfully",
  "result": null
}
```

---

#### 1.2.8. Lấy chi tiết người dùng
```
GET /service-identity/users/{userId}
```

**Path Parameters:**
- `userId`: UUID của người dùng

**Headers:**
- `Authorization`: Bearer {accessToken}

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "dob": "2000-01-01",
    "avatarUrl": "string",
    "role": "USER",
    "status": "ACTIVE"
  }
}
```

---

#### 1.2.9. Bật/tắt trạng thái người dùng
```
PUT /service-identity/users/{userId}/status
```

**Path Parameters:**
- `userId`: UUID của người dùng

**Headers:**
- `Authorization`: Bearer {accessToken}

**Response:**
```json
{
  "code": 1000,
  "message": "User status updated successfully",
  "result": null
}
```

---

## 2. SERVICE-MUSIC APIs

Base Path: `/service-music`

### 2.1. Genre APIs (`/genres`)

#### 2.1.1. Tạo thể loại nhạc
```
POST /service-music/genres
```

**Request Body:**
```json
{
  "name": "string",
  "key": "string"
}
```

**Validations:**
- Name: bắt buộc
- Key: bắt buộc

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "name": "string",
    "key": "string"
  }
}
```

---

#### 2.1.2. Lấy danh sách thể loại
```
GET /service-music/genres
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": [
    {
      "id": "uuid",
      "name": "string",
      "key": "string"
    }
  ]
}
```

---

#### 2.1.3. Cập nhật thể loại
```
PUT /service-music/genres/{id}
```

**Path Parameters:**
- `id`: UUID của thể loại

**Request Body:**
```json
{
  "name": "string",
  "key": "string"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "name": "string",
    "key": "string"
  }
}
```

---

#### 2.1.4. Xóa thể loại
```
DELETE /service-music/genres/{id}
```

**Path Parameters:**
- `id`: UUID của thể loại

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": "Genre has been deleted"
}
```

---

### 2.2. Artist APIs (`/artists`)

#### 2.2.1. Đăng ký làm nghệ sĩ
```
POST /service-music/artists/register
```

**Headers:**
- `Authorization`: Bearer {accessToken}
- Yêu cầu: User phải có claim `artist: true` trong JWT

**Request Body:**
```json
{
  "stageName": "string",
  "bio": "string",
  "avatarUrl": "string",
  "coverImageUrl": "string",
  "termsAccepted": true
}
```

**Validations:**
- stageName: bắt buộc
- termsAccepted: phải là true

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "userId": "uuid",
    "stageName": "string",
    "bio": "string",
    "avatarUrl": "string",
    "coverImageUrl": "string",
    "status": "PENDING",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

#### 2.2.2. Lấy thông tin nghệ sĩ của tôi
```
GET /service-music/artists/my-profile
```

**Headers:**
- `Authorization`: Bearer {accessToken}

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "userId": "uuid",
    "stageName": "string",
    "bio": "string",
    "avatarUrl": "string",
    "coverImageUrl": "string",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

#### 2.2.3. Lấy danh sách nghệ sĩ
```
GET /service-music/artists
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "userId": "uuid",
    "stageName": "string",
    "bio": "string",
    "avatarUrl": "string",
    "coverImageUrl": "string",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

### 2.3. Song APIs (`/songs`)

#### 2.3.1. Lấy URL upload (Presigned URL)
```
GET /service-music/songs/presigned-url?fileName={fileName}
```

**Query Parameters:**
- `fileName`: Tên file cần upload

**Response:**
```json
{
  "uploadUrl": "string",
  "objectName": "string",
  "songId": "uuid"
}
```

---

#### 2.3.2. Tạo bài hát
```
POST /service-music/songs?objectName={objectName}
```

**Query Parameters:**
- `objectName`: Tên object sau khi upload

**Request Body:**
```json
{
  "title": "string",
  "artistId": "string",
  "fileName": "string",
  "genreIds": ["uuid1", "uuid2"]
}
```

**Response:**
```json
{
  "id": "uuid",
  "title": "string",
  "artistId": "string",
  "albumId": "string",
  "lyricUrl": "string",
  "rawUrl": "string",
  "streamUrl": "string",
  "duration": 0,
  "genres": [],
  "status": "PROCESSING",
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

---

### 2.4. Stream APIs (`/songs/stream`)

#### 2.4.1. Phát nhạc (Lấy manifest)
```
GET /service-music/songs/stream/{songId}/play
```

**Path Parameters:**
- `songId`: UUID của bài hát

**Headers:**
- `Authorization`: Bearer {accessToken} (Optional)
- JWT claim `quality` sẽ được sử dụng để chọn chất lượng

**Response:**
```
Content-Type: application/vnd.apple.mpegurl (hoặc tương tự)
Cache-Control: no-cache

[HLS/DASH Manifest Content]
```

---

#### 2.4.2. Lấy segment của bài hát
```
GET /service-music/songs/stream/{songId}/{quality}/{fileName}
```

**Path Parameters:**
- `songId`: UUID của bài hát
- `quality`: Chất lượng (ví dụ: 128k, 320k)
- `fileName`: Tên file segment

**Response:**
```
Content-Type: audio/mpeg (hoặc tương tự)
Cache-Control: public, max-age=3600

[Binary Audio Data]
```

---

### 2.5. User Preference APIs (`/preferences`)

#### 2.5.1. Hoàn thành onboarding
```
POST /service-music/preferences/onboarding
```

**Headers:**
- `Authorization`: Bearer {accessToken}

**Request Body:**
```json
{
  "favoriteGenreIds": ["uuid1", "uuid2", "uuid3"],
  "favoriteArtistIds": ["uuid1", "uuid2"]
}
```

**Validations:**
- favoriteGenreIds: 1-5 thể loại
- favoriteArtistIds: 1-3 nghệ sĩ

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "userId": "uuid",
    "favoriteGenres": [
      {
        "id": "uuid",
        "name": "string",
        "key": "string"
      }
    ],
    "favoriteArtists": [
      {
        "id": "uuid",
        "userId": "uuid",
        "stageName": "string",
        "bio": "string",
        "avatarUrl": "string",
        "coverImageUrl": "string",
        "status": "ACTIVE",
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "onboardingCompleted": true
  }
}
```

---

#### 2.5.2. Lấy sở thích của tôi
```
GET /service-music/preferences/my-preferences
```

**Headers:**
- `Authorization`: Bearer {accessToken}

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "userId": "uuid",
    "favoriteGenres": [...],
    "favoriteArtists": [...],
    "onboardingCompleted": true
  }
}
```

---

#### 2.5.3. Kiểm tra trạng thái onboarding
```
GET /service-music/preferences/onboarding-status
```

**Headers:**
- `Authorization`: Bearer {accessToken}

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": true
}
```

---

## 3. SERVICE-PAYMENT APIs

Base Path: `/service-payment`

### 3.1. Subscription Plan APIs (`/plans`)

#### 3.1.1. Tạo gói đăng ký
```
POST /service-payment/plans
```

**Request Body:**
```json
{
  "name": "Premium Plan",
  "description": "Full access to all features",
  "price": 99000,
  "duration": 1,
  "durationUnit": "MONTH",
  "features": {
    "maxDownloads": 100,
    "adFree": true,
    "highQuality": true
  }
}
```

**Validations:**
- name: bắt buộc
- price: >= 0

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "name": "Premium Plan",
    "description": "Full access to all features",
    "price": 99000,
    "duration": 1,
    "durationUnit": "MONTH",
    "features": {
      "maxDownloads": 100,
      "adFree": true,
      "highQuality": true
    },
    "active": true
  }
}
```

---

#### 3.1.2. Lấy danh sách gói đăng ký đang hoạt động
```
GET /service-payment/plans
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": [
    {
      "id": "uuid",
      "name": "Premium Plan",
      "description": "Full access to all features",
      "price": 99000,
      "duration": 1,
      "durationUnit": "MONTH",
      "features": {...},
      "active": true
    }
  ]
}
```

---

#### 3.1.3. Lấy chi tiết gói đăng ký
```
GET /service-payment/plans/{id}
```

**Path Parameters:**
- `id`: UUID của gói đăng ký

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "name": "Premium Plan",
    "description": "Full access to all features",
    "price": 99000,
    "duration": 1,
    "durationUnit": "MONTH",
    "features": {...},
    "active": true
  }
}
```

---

#### 3.1.4. Cập nhật gói đăng ký
```
PUT /service-payment/plans/{id}
```

**Path Parameters:**
- `id`: UUID của gói đăng ký

**Request Body:**
```json
{
  "name": "Premium Plan Updated",
  "description": "Updated description",
  "price": 119000,
  "duration": 1,
  "durationUnit": "MONTH",
  "features": {...}
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "id": "uuid",
    "name": "Premium Plan Updated",
    "description": "Updated description",
    "price": 119000,
    "duration": 1,
    "durationUnit": "MONTH",
    "features": {...},
    "active": true
  }
}
```

---

#### 3.1.5. Bật/tắt trạng thái gói đăng ký
```
PATCH /service-payment/plans/{id}/toggle
```

**Path Parameters:**
- `id`: UUID của gói đăng ký

**Response:**
```json
{
  "code": 1000,
  "message": "Plan status updated successfully",
  "result": null
}
```

---

### 3.2. Payment APIs (`/payments`)

#### 3.2.1. Tạo link thanh toán
```
POST /service-payment/payments/checkout
```

**Headers:**
- `X-User-Id`: UUID của người dùng

**Request Body:**
```json
{
  "planId": "uuid",
  "returnUrl": "https://example.com/payment/success",
  "cancelUrl": "https://example.com/payment/cancel"
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "string",
  "result": {
    "checkoutUrl": "string",
    "orderCode": 123456789,
    "qrCode": "string"
  }
}
```

---

#### 3.2.2. Webhook từ PayOS
```
POST /service-payment/payments/payos_transfer_handler
```

**Request Body:**
```json
{
  // PayOS Webhook payload
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "Webhook received",
  "result": null
}
```

---

## Response Format

Tất cả các API đều sử dụng định dạng response chung:

```json
{
  "code": 1000,           // Mã code (1000 = success)
  "message": "string",    // Thông báo (optional)
  "result": {}            // Dữ liệu kết quả (optional)
}
```

### Common HTTP Status Codes

- `200 OK`: Request thành công
- `400 Bad Request`: Dữ liệu không hợp lệ
- `401 Unauthorized`: Chưa xác thực hoặc token không hợp lệ
- `403 Forbidden`: Không có quyền truy cập
- `404 Not Found`: Không tìm thấy resource
- `500 Internal Server Error`: Lỗi server

---

## Authentication

### Bearer Token

Hầu hết các API yêu cầu JWT token trong header:

```
Authorization: Bearer {accessToken}
```

### Token Flow

1. Đăng ký: `POST /service-identity/users/registration`
2. Xác thực email: `POST /service-identity/users/verify-email`
3. Đăng nhập: `POST /service-identity/auth/login` → nhận `accessToken` và `refreshToken`
4. Sử dụng `accessToken` cho các API khác
5. Khi token hết hạn: `POST /service-identity/auth/refresh` với `refreshToken`

---

## Ghi chú

1. **API Gateway**: Tất cả requests phải đi qua port 8080
2. **Service Discovery**: Sử dụng Eureka Server trên port 8761
3. **Database**: PostgreSQL cho mỗi service
4. **Message Queue**: RabbitMQ cho async processing
5. **Storage**: MinIO cho lưu trữ file media
6. **Cache**: Redis cho caching
7. **Payment**: Tích hợp PayOS

---

## Environment Variables Required

### Service Identity
- `DB_URL_IDENTITY`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID_WEB`
- `GOOGLE_CLIENT_SECRET`
- `FB_APP_ID`
- `FB_APP_SECRET`

### Service Music
- `DB_URL_MUSIC`
- `DB_USERNAME`
- `DB_PASSWORD`
- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET`
- `MINIO_BUCKET`
- `JWT_SECRET`

### Service Payment
- `DB_URL_PAYMENT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PAYOS_CLIENT_ID`
- `PAYOS_API_KEY`
- `PAYOS_CHECK_SUM`

---

**Cập nhật lần cuối:** 2026-02-11

