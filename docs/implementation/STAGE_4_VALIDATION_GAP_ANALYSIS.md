# BazaarHub — Stage 4 Request Body Validation Gap Analysis (REM-QLTY-01)

> **Document Type**: Security & Quality Audit — Request Body Validation & Structured DTOs  
> **Target System**: BazaarHub Enterprise Multi-Vendor E-Commerce Backend  
> **Stack**: Java 21 | Spring Boot 4.1.2 | Spring Security | Clean Architecture  
> **Total Request-Body Endpoints Audited**: 106 Endpoints across 59 REST Controllers  
> **Audit Status**: Complete  

---

## 1. Executive Summary & Audit Methodology

A comprehensive static analysis and code audit was executed across all 59 REST controllers in `src/main/java`. Every endpoint receiving a `@RequestBody` parameter was inspected to verify:
1. Presence of `@Valid` parameter annotations.
2. Robustness of Jakarta Bean Validation constraints on target DTO fields (`@NotNull`, `@NotBlank`, `@Size`, `@Positive`, `@Pattern`, `@Min`, `@Max`).
3. Elimination of unstructured `Map<String, String>` or untyped request payloads.
4. Protection against payload bombing on raw String receivers.
5. Global exception handling fidelity and HTTP 400 error schema standardization.

### Audit Summary Statistics
- **Total `@RequestBody` Endpoints Inspected**: **106**
- **Endpoints Already Fully Validated with `@Valid`**: **96**
- **Endpoints with Identified Validation Gaps**: **10**
  - Unstructured `Map<String, String>` payloads: **5** (4 Support Ticket Attachments, 1 Delivery Proof Upload)
  - Existing DTOs missing `@Valid` on controller parameter: **4** (`suspendSeller`, `markReadyForPickup`, `calculateRate`, `validateCheckout`)
  - Raw String Payload requiring size boundary: **1** (`handleRazorpayWebhook`)

---

## 2. Comprehensive Inventory of Confirmed Validation Gaps

| # | HTTP Method | Endpoint Path | Controller & Method | Current Parameter Declaration | Current DTO / Type | Gap & Vulnerability Description | Required Remediation | Compatibility Risk |
| :-: | :---: | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `POST` | `/api/v1/admin/sellers/{sellerId}/suspend` | `AdminSellerController.suspendSeller` | `@RequestBody(required = false) AdminActionRequest request` | `AdminActionRequest` | Missing `@Valid`. If body provided, `@Size(max = 500)` on `reason` is completely bypassed. | Add `@Valid` to parameter; keep `required = false`. | **Zero Risk** (Valid payloads unchanged; rejects reasons > 500 chars). |
| **2** | `POST` | `/api/v1/payments/webhooks/razorpay` | `PaymentWebhookController.handleRazorpayWebhook` | `@RequestBody String rawPayload` | `String` (Raw JSON) | Raw String payload lacks size upper bound (vulnerable to payload bombing / OOM). Cannot convert to DTO due to HMAC signature requirement. | Keep raw String body intact for HMAC. Add header check & length cap (`<= 64KB`). | **Zero Risk** (Razorpay webhooks are ~2KB; signature check preserved). |
| **3** | `POST` | `/api/v1/shipments/{shipmentId}/ready-for-pickup` | `InternalShipmentController.markReadyForPickup` | `@RequestBody(required = false) ReadyForPickupRequest request` | `ReadyForPickupRequest` | Missing `@Valid`. DTO lacks validation on package weight and dimensions (allows negative weight/dimensions). | Add `@Valid` to parameter. Add `@Positive` on weight/dimensions, `@Size(max=500)` on notes in DTO. | **Zero Risk** (Valid real-world packaging values accepted; rejects negative numbers). |
| **4** | `POST` | `/api/v1/shipments/rate-quote` | `InternalShipmentController.calculateRate` | `@RequestBody(required = false) ShippingRateQuoteRequest request` | `ShippingRateQuoteRequest` | Missing `@Valid`. DTO lacks validation on weight, subtotal, and PIN code formats. | Add `@Valid` to parameter. Add `@Positive` on weight & sellerOrderId, `@PositiveOrZero` on subtotal, `@Pattern` on 6-digit Indian PIN codes in DTO. | **Zero Risk** (Valid requests pass; rejects negative values and malformed PIN codes). |
| **5** | `POST` | `/api/v1/admin/support/tickets/attachments` | `AdminSupportController.uploadAttachment` | `@RequestBody Map<String, String> payload` | `Map<String, String>` (Unstructured) | Unvalidated Map allows null/blank image payload or missing keys, leading to potential NPE in Cloudinary uploader. | Create strongly-typed `AttachmentUploadRequest` with `@NotBlank String image`. Add `@Valid @RequestBody`. | **Zero Risk** (JSON field contract `"image"` preserved). |
| **6** | `POST` | `/api/v1/customer/tickets/attachments` | `CustomerSupportController.uploadAttachment` | `@RequestBody Map<String, String> payload` | `Map<String, String>` (Unstructured) | Unvalidated Map allows null/blank image payload or missing keys, leading to potential NPE in Cloudinary uploader. | Replace Map with `@Valid @RequestBody AttachmentUploadRequest`. | **Zero Risk** (JSON field contract `"image"` preserved). |
| **7** | `POST` | `/api/v1/delivery/tickets/attachments` | `DeliverySupportController.uploadAttachment` | `@RequestBody Map<String, String> payload` | `Map<String, String>` (Unstructured) | Unvalidated Map allows null/blank image payload or missing keys, leading to potential NPE in Cloudinary uploader. | Replace Map with `@Valid @RequestBody AttachmentUploadRequest`. | **Zero Risk** (JSON field contract `"image"` preserved). |
| **8** | `POST` | `/api/v1/seller/tickets/attachments` | `SellerSupportController.uploadAttachment` | `@RequestBody Map<String, String> payload` | `Map<String, String>` (Unstructured) | Unvalidated Map allows null/blank image payload or missing keys, leading to potential NPE in Cloudinary uploader. | Replace Map with `@Valid @RequestBody AttachmentUploadRequest`. | **Zero Risk** (JSON field contract `"image"` preserved). |
| **9** | `POST` | `/api/v1/checkout/validate` | `CustomerCheckoutController.validateCheckout` | `@RequestBody(required = false) CheckoutValidateRequest request` | `CheckoutValidateRequest` | Missing `@Valid`. `addressId` is unvalidated (allows negative or 0 values to reach database query). | Add `@Valid` to parameter. Add `@Positive` to `addressId` in `CheckoutValidateRequest`. | **Zero Risk** (Valid address IDs accepted; rejects negative IDs). |
| **10**| `POST` | `/api/v1/delivery/proof/upload` | `DeliveryBoyController.uploadProof` | `@RequestBody Map<String, String> payload` | `Map<String, String>` (Unstructured) | Unvalidated Map allows null/blank image payload or arbitrary unbounded folder strings, risking Cloudinary proxy abuse. | Create strongly-typed `DeliveryProofUploadRequest` with `@NotBlank String image`, optional `shipmentId`, and `@Size(max=200) String folder`. Add `@Valid @RequestBody`. | **Zero Risk** (JSON fields `"image"` and `"folder"` preserved). |

---

## 3. Critical Webhook Exception Analysis (Razorpay Webhook)

- **Endpoint**: `POST /api/v1/payments/webhooks/razorpay`
- **Controller**: `PaymentWebhookController.handleRazorpayWebhook`
- **Current Signature**:
  ```java
  public ResponseEntity<ApiResponse<WebhookProcessingResult>> handleRazorpayWebhook(
          @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader,
          @RequestBody String rawPayload)
  ```
- **Cryptographic Requirement**: Razorpay HMAC SHA-256 signature verification requires the exact byte-for-byte unparsed request body string (`rawPayload`). Parsing this into a Jackson DTO prior to signature calculation would alter whitespace, field ordering, and number formatting, permanently breaking HMAC verification.
- **Architectural Decision**:
  - Keep `@RequestBody String rawPayload` as raw String.
  - Enforce perimeter validation:
    1. Signature header `X-Razorpay-Signature` presence and non-blank check -> HTTP 400.
    2. Payload presence and non-blank check -> HTTP 400.
    3. Payload size cap at 64 KB (`rawPayload.length() <= 65536`) -> HTTP 400.
  - Zero disruption to existing signature verification and payment status transitions.

---

## 4. Structured DTO Implementation Specifications

### 4.1. `AttachmentUploadRequest.java`
- **Location**: `src/main/java/com/bazaarhub/backend/support/application/dto/request/AttachmentUploadRequest.java`
- **Target Endpoints**:
  - `POST /api/v1/admin/support/tickets/attachments`
  - `POST /api/v1/customer/tickets/attachments`
  - `POST /api/v1/delivery/tickets/attachments`
  - `POST /api/v1/seller/tickets/attachments`
- **Schema & Annotations**:
  ```java
  package com.bazaarhub.backend.support.application.dto.request;

  import io.swagger.v3.oas.annotations.media.Schema;
  import jakarta.validation.constraints.NotBlank;
  import jakarta.validation.constraints.Size;
  import lombok.*;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Support ticket attachment upload payload")
  public class AttachmentUploadRequest {

      @NotBlank(message = "Attachment image content is required")
      @Size(max = 1_000_000, message = "Attachment image content exceeds maximum permitted size")
      @Schema(description = "Base64 data URI or image URL to upload", example = "https://example.com/photo.png")
      private String image;
  }
  ```

### 4.2. `DeliveryProofUploadRequest.java`
- **Location**: `src/main/java/com/bazaarhub/backend/delivery/application/dto/request/DeliveryProofUploadRequest.java`
- **Target Endpoint**: `POST /api/v1/delivery/proof/upload`
- **Schema & Annotations**:
  ```java
  package com.bazaarhub.backend.delivery.application.dto.request;

  import io.swagger.v3.oas.annotations.media.Schema;
  import jakarta.validation.constraints.NotBlank;
  import jakarta.validation.constraints.Positive;
  import jakarta.validation.constraints.Size;
  import lombok.*;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Delivery proof image upload payload")
  public class DeliveryProofUploadRequest {

      @Positive(message = "Shipment ID must be positive")
      @Schema(description = "Associated Shipment ID", example = "101")
      private Long shipmentId;

      @NotBlank(message = "Proof image content is required")
      @Size(max = 1_000_000, message = "Proof image content exceeds maximum permitted size")
      @Schema(description = "Base64 data URI or image URL of proof", example = "https://res.cloudinary.com/.../proof.jpg")
      private String image;

      @Size(max = 200, message = "Folder name must not exceed 200 characters")
      @Schema(description = "Optional destination Cloudinary folder", example = "bazaarhub/delivery/proofs")
      private String folder;
  }
  ```

---

## 5. Global Exception Handling Enhancements

Inspect [GlobalRestExceptionHandler.java](file:///Users/ajaykumar/bazaarhub-backend/src/main/java/com/bazaarhub/backend/shared/presentation/rest/exception/GlobalRestExceptionHandler.java):
- **Current Handling**:
  - `MethodArgumentNotValidException` -> HTTP 400 Bad Request with `ApiResponse.error("Validation failed", errors)`.
  - `ConstraintViolationException` -> HTTP 400 Bad Request with `ApiResponse.error("Validation failed", errors)`.
  - `HandlerMethodValidationException` -> HTTP 400 Bad Request with `ApiResponse.error("Validation failed", errors)`.
  - `HttpMessageNotReadableException` -> HTTP 400 Bad Request with `ApiResponse.error("Malformed request payload")`.
- **Identified Deficits & Enhancements**:
  1. Add `MethodArgumentTypeMismatchException` handler -> HTTP 400 Bad Request with `"Invalid parameter '{name}': value '{value}' cannot be converted to {type}"`. (Previously unhandled, falling through to HTTP 500).
  2. Add `MissingServletRequestParameterException` handler -> HTTP 400 Bad Request with `"Required request parameter '{name}' is missing"`.
  3. Include `getGlobalErrors()` in `MethodArgumentNotValidException` mapping to support class-level constraints alongside field-level constraints.

---

## 6. Audit & Implementation Status

All 10 confirmed validation gaps have been completely remediated, verified, and closed:
1. `AdminSellerController.suspendSeller` -> Added `@Valid`. Verified: rejection of reasons > 500 characters.
2. `PaymentWebhookController.handleRazorpayWebhook` -> Enforced signature header presence, payload presence, and 64KB maximum payload length without altering raw String body.
3. `InternalShipmentController.markReadyForPickup` -> Added `@Valid` and added `@Positive` on weight/dimensions, `@Size(max=500)` on notes to `ReadyForPickupRequest`.
4. `InternalShipmentController.calculateRate` -> Added `@Valid` and added `@Positive` on weight/orderId, `@PositiveOrZero` on subtotal, `@Pattern` on Indian PIN codes to `ShippingRateQuoteRequest`.
5. `AdminSupportController.uploadAttachment` -> Replaced Map with `@Valid @RequestBody AttachmentUploadRequest`.
6. `CustomerSupportController.uploadAttachment` -> Replaced Map with `@Valid @RequestBody AttachmentUploadRequest`.
7. `DeliverySupportController.uploadAttachment` -> Replaced Map with `@Valid @RequestBody AttachmentUploadRequest`.
8. `SellerSupportController.uploadAttachment` -> Replaced Map with `@Valid @RequestBody AttachmentUploadRequest`.
9. `CustomerCheckoutController.validateCheckout` -> Added `@Valid` and added `@Positive` on addressId to `CheckoutValidateRequest`.
10. `DeliveryBoyController.uploadProof` -> Replaced Map with `@Valid @RequestBody DeliveryProofUploadRequest`.

### Global Exception Handling
- Enhanced `GlobalRestExceptionHandler` with handlers for `MethodArgumentTypeMismatchException` and `MissingServletRequestParameterException`.
- Extended `MethodArgumentNotValidException` to aggregate both `getFieldErrors()` and `getGlobalErrors()`.

### Verification Suite
- Dedicated Integration Test: `RequestBodyValidationIntegrationTest` — **25 tests passed, 0 failures**.
- Comprehensive Regression Suite: **113 tests passed, 0 failures, 0 skipped**.
- Status: **STAGE 4 REMEDIATION COMPLETED & VERIFIED**.
