# Phase 10: Payment Management Module (`com.bazaarhub.backend.payment`)

## 1. Module Overview
The Payment module provides production-grade payment gateway orchestration, checkout initialization, cryptographic HMAC signature verification, asynchronous webhook reconciliation, Cash on Delivery (COD) workflows, and administrative refund execution for BazaarHub.

Built following Clean Architecture, SOLID principles, and database-driven RBAC, it strictly aligns with MariaDB's existing `payments`, `payment_transactions`, and `refunds` tables without executing any DDL or modifying database schemas.

---

## 2. Business Responsibilities & Features
- **Authoritative Server-Side Amount Calculation**: Payment amounts are derived exclusively from order totals in paise (1 INR = 100 paise). Client-provided amounts are never trusted.
- **Server-Side Razorpay Gateway Integration**: Test mode only (`razorpay.mode=test`), configured via environment variables with strict HTTP connection and read timeouts.
- **Razorpay Order Creation**: Initializes Razorpay orders, attaches metadata notes, records `PaymentEntity` in `PENDING` state, and returns checkout parameters (`keyId`, `amountInPaise`, customer prefill).
- **HMAC SHA256 Signature Verification**: Validates `razorpay_order_id|razorpay_payment_id` against `razorpay.key.secret` using constant-time byte comparisons (`MessageDigest.isEqual`) to prevent timing attacks.
- **Asynchronous Webhook Processing**: Consumes raw byte payloads at `/api/v1/payments/webhooks/razorpay`, verifies `X-Razorpay-Signature` against `razorpay.webhook.secret`, and idempotently handles:
  - `payment.captured`: Transitions payment to `CAPTURED`, order payment status to `PAID`, order status to `CONFIRMED`, and vendor sub-orders to `CONFIRMED`.
  - `payment.failed`: Records error description, transaction failure, and updates order payment status to `FAILED`.
  - `refund.processed`: Marks refund record as `COMPLETED`.
- **Cash on Delivery (COD)**: Configurable threshold (`bazaarhub.payment.cod.max-amount=50000.00`), instant order confirmation upon checkout, and administrative cash collection (`POST /api/v1/admin/payments/{id}/cod-collect`).
- **Comprehensive Refund Management**: Supports partial and full refunds against captured payments, strictly enforcing that total committed refunds do not exceed the original payment amount.
- **Comprehensive Audit Ledger**: Every attempt, authorization, capture, void, and refund is recorded in `payment_transactions`.

---

## 3. Architecture & Clean Layer Responsibilities

```
payment/
├── domain/
│   ├── model/                  # PaymentStatus, TransactionType, TransactionStatus, RefundStatus, PaymentGatewayType
│   └── exception/              # PaymentNotFoundException, RefundNotFoundException, PaymentVerificationException,
│                               # UnauthorizedPaymentAccessException, InvalidPaymentOperationException,
│                               # InvalidRefundOperationException, PaymentGatewayException
│
├── application/
│   ├── dto/
│   │   ├── request/            # CreateRazorpayOrderRequest, VerifyRazorpayPaymentRequest, SelectCodPaymentRequest,
│   │   │                       # CollectCodPaymentRequest, CreateRefundRequest
│   │   └── response/           # RazorpayOrderCheckoutResponse, PaymentResponse, PaymentSummaryResponse,
│   │                           # PaymentTransactionResponse, RefundResponse, WebhookProcessingResult
│   ├── mapper/                 # PaymentMapper
│   └── service/                # PaymentService, PaymentServiceImpl, PaymentWebhookService,
│                               # PaymentWebhookServiceImpl, RefundService, RefundServiceImpl
│
├── infrastructure/
│   ├── config/                 # RazorpayProperties, CodProperties
│   ├── gateway/                # PaymentGateway, RazorpayPaymentGatewayImpl, RazorpayOrderResponse,
│   │                           # RazorpayPaymentDetails, RazorpayRefundResponse
│   └── persistence/
│       ├── entity/             # PaymentEntity, PaymentTransactionEntity, RefundEntity
│       └── repository/         # SpringDataPaymentRepository, SpringDataPaymentTransactionRepository,
│                               # SpringDataRefundRepository
│
└── presentation/
    └── rest/                   # CustomerPaymentController (/api/v1/payments),
                                # PaymentWebhookController (/api/v1/payments/webhooks),
                                # AdminPaymentController (/api/v1/admin/payments, /api/v1/admin/refunds)
```

---

## 4. API Endpoints Reference

### Customer Endpoints (`/api/v1/payments`)
| Method | Endpoint | Description | Auth / Role |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/payments/razorpay/create-order` | Initiates Razorpay checkout order | `ROLE_CUSTOMER` (`PAYMENT_CREATE`) |
| `POST` | `/api/v1/payments/razorpay/verify` | Verifies Razorpay checkout signature | `ROLE_CUSTOMER` (`PAYMENT_CREATE`) |
| `POST` | `/api/v1/payments/cod/select` | Selects Cash on Delivery payment | `ROLE_CUSTOMER` (`PAYMENT_CREATE`) |
| `GET` | `/api/v1/payments/{paymentId}` | Get payment details by payment ID | `ROLE_CUSTOMER` (Owner) / `ROLE_ADMIN` |
| `GET` | `/api/v1/payments/order/{orderId}` | Get payment details by order ID | `ROLE_CUSTOMER` (Owner) / `ROLE_ADMIN` |
| `GET` | `/api/v1/payments` | Paginated listing of customer's payments | `ROLE_CUSTOMER` (`PAYMENT_READ`) |
| `GET` | `/api/v1/payments/{paymentId}/refunds` | Get refunds for a specific payment | `ROLE_CUSTOMER` (Owner) / `ROLE_ADMIN` |
| `GET` | `/api/v1/payments/refunds` | Paginated listing of customer's refunds | `ROLE_CUSTOMER` (`PAYMENT_READ`) |

### Public Webhook Receiver (`/api/v1/payments/webhooks`)
| Method | Endpoint | Description | Auth / Verification |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/payments/webhooks/razorpay` | Asynchronous Razorpay webhook events | Public (HMAC `X-Razorpay-Signature`) |

### Administrative Endpoints (`/api/v1/admin`)
| Method | Endpoint | Description | Auth / Role |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/admin/payments/{paymentId}/cod-collect` | Collects COD cash settlement | `ROLE_ADMIN` (`PAYMENT_MANAGE`) |
| `POST` | `/api/v1/admin/payments/{paymentId}/refunds` | Initiates partial or full refund | `ROLE_ADMIN` (`REFUND_MANAGE`) |
| `GET` | `/api/v1/admin/payments` | Paginated list of all platform payments | `ROLE_ADMIN` (`PAYMENT_MANAGE`) |
| `GET` | `/api/v1/admin/refunds` | Paginated list of all platform refunds | `ROLE_ADMIN` (`REFUND_MANAGE`) |
| `GET` | `/api/v1/admin/refunds/{refundId}` | Get refund details by ID | `ROLE_ADMIN` (`REFUND_MANAGE`) |

---

## 5. Security & Verification Architecture
1. **Timing Attack Immunity**: Constant-time signature comparison using `MessageDigest.isEqual(...)`.
2. **Replay Protection & Idempotency**:
   - `create-order` reuses existing pending Razorpay order parameters.
   - `verify` checks whether the payment is already `CAPTURED` and returns immediate success.
   - Webhook checks existing status before updating state or transitioning orders.
3. **RBAC & Data Ownership**:
   - Customers can only create and inspect payments for orders they own. Any attempt to inspect or verify another customer's payment results in `403 Forbidden` (`UnauthorizedPaymentAccessException`).
   - Webhook endpoint is publicly accessible from network standpoint but rejected with `400 Bad Request` if the cryptographic signature fails against `razorpay.webhook.secret`.
