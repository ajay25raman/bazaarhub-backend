package com.bazaarhub.backend.shared.presentation.rest.exception;

import com.bazaarhub.backend.identity.domain.exception.AccountNotActiveException;
import com.bazaarhub.backend.identity.domain.exception.InvalidCredentialsException;
import com.bazaarhub.backend.identity.domain.exception.UserAlreadyExistsException;
import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;
import com.bazaarhub.backend.shared.domain.exception.DomainException;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized REST exception handler translating domain, security, and framework exceptions
 * into uniform ApiResponse payloads.
 */
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("[REST EXCEPTION] Invalid credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[REST EXCEPTION] Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email/phone or password"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("[REST EXCEPTION] Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Unauthorized: " + ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[REST EXCEPTION] Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Forbidden: You do not have permission to access this resource"));
    }

    @ExceptionHandler({
            AccountNotActiveException.class,
            com.bazaarhub.backend.catalog.domain.exception.UnauthorizedProductAccessException.class,
            com.bazaarhub.backend.inventory.domain.exception.UnauthorizedInventoryAccessException.class,
            com.bazaarhub.backend.customer.domain.exception.UnauthorizedAddressAccessException.class,
            com.bazaarhub.backend.customer.domain.exception.UnauthorizedCustomerAccessException.class,
            com.bazaarhub.backend.wishlist.domain.exception.UnauthorizedWishlistAccessException.class,
            com.bazaarhub.backend.cart.domain.exception.UnauthorizedCartAccessException.class,
            com.bazaarhub.backend.order.domain.exception.UnauthorizedOrderAccessException.class,
            com.bazaarhub.backend.payment.domain.exception.UnauthorizedPaymentAccessException.class,
            com.bazaarhub.backend.delivery.domain.exception.UnauthorizedDeliveryAccessException.class,
            com.bazaarhub.backend.shipping.domain.exception.UnauthorizedShipmentAccessException.class,
            com.bazaarhub.backend.review.domain.exception.UnauthorizedReviewAccessException.class,
            com.bazaarhub.backend.notification.domain.exception.UnauthorizedNotificationAccessException.class,
            com.bazaarhub.backend.support.domain.exception.UnauthorizedTicketAccessException.class,
            com.bazaarhub.backend.ai.domain.exception.UnauthorizedAiAccessException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleForbiddenExceptions(RuntimeException ex) {
        log.warn("[REST EXCEPTION] Forbidden: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            com.bazaarhub.backend.payment.domain.exception.PaymentVerificationException.class,
            com.bazaarhub.backend.payment.domain.exception.InvalidPaymentOperationException.class,
            com.bazaarhub.backend.payment.domain.exception.InvalidRefundOperationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handlePaymentBadRequestExceptions(RuntimeException ex) {
        log.warn("[REST EXCEPTION] Payment operation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.bazaarhub.backend.payment.domain.exception.PaymentGatewayException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentGatewayException(com.bazaarhub.backend.payment.domain.exception.PaymentGatewayException ex) {
        log.error("[REST EXCEPTION] Payment Gateway Error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("Payment Gateway error: " + ex.getMessage()));
    }

    @ExceptionHandler({
            com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOperationException.class,
            com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOtpException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleDeliveryBadRequestExceptions(RuntimeException ex) {
        log.warn("[REST EXCEPTION] Delivery operation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            com.bazaarhub.backend.order.domain.exception.InvalidOrderStatusTransitionException.class,
            com.bazaarhub.backend.order.domain.exception.InvalidOrderOperationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrderExceptions(RuntimeException ex) {
        log.warn("[REST EXCEPTION] Order operation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            com.bazaarhub.backend.shipping.domain.exception.InvalidShipmentOperationException.class,
            com.bazaarhub.backend.shipping.domain.exception.IneligibleOrderForShipmentException.class,
            com.bazaarhub.backend.shipping.domain.exception.ShippingCalculationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleShippingBadRequestExceptions(RuntimeException ex) {
        log.warn("[REST EXCEPTION] Shipping operation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            com.bazaarhub.backend.review.domain.exception.InvalidReviewOperationException.class,
            com.bazaarhub.backend.review.domain.exception.IneligibleReviewException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleReviewBadRequestExceptions(RuntimeException ex) {
        log.warn("[REST EXCEPTION] Review operation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            org.springframework.dao.OptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(Exception ex) {
        log.warn("[REST EXCEPTION] Optimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Concurrent modification detected. Another request updated this record. Please retry."));
    }

    @ExceptionHandler({
            UserAlreadyExistsException.class,
            com.bazaarhub.backend.seller.domain.exception.SellerAlreadyExistsException.class,
            com.bazaarhub.backend.seller.domain.exception.StoreAlreadyExistsException.class,
            com.bazaarhub.backend.catalog.domain.exception.CategoryAlreadyExistsException.class,
            com.bazaarhub.backend.catalog.domain.exception.BrandAlreadyExistsException.class,
            com.bazaarhub.backend.catalog.domain.exception.ProductAlreadyExistsException.class,
            com.bazaarhub.backend.catalog.domain.exception.SkuAlreadyExistsException.class,
            com.bazaarhub.backend.customer.domain.exception.CustomerProfileAlreadyExistsException.class,
            com.bazaarhub.backend.customer.domain.exception.AddressInUseException.class,
            com.bazaarhub.backend.wishlist.domain.exception.WishlistItemAlreadyExistsException.class,
            com.bazaarhub.backend.delivery.domain.exception.DeliveryBoyAlreadyExistsException.class,
            com.bazaarhub.backend.shipping.domain.exception.ShipmentAlreadyExistsException.class,
            com.bazaarhub.backend.review.domain.exception.DuplicateReviewException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleAlreadyExists(RuntimeException ex) {
        log.warn("[REST EXCEPTION] Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("[REST EXCEPTION] Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.bazaarhub.backend.cart.domain.exception.CheckoutValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleCheckoutValidationException(com.bazaarhub.backend.cart.domain.exception.CheckoutValidationException ex) {
        log.warn("[REST EXCEPTION] Checkout validation failure: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ApiResponse.error(ex.getMessage(), ex.getValidationErrors()));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warn("[REST EXCEPTION] Business rule violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        log.warn("[REST EXCEPTION] Domain exception: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = new java.util.ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> {
            String msg = fe.getDefaultMessage();
            if (msg != null && !msg.isBlank()) {
                errors.add(msg);
            }
        });
        ex.getBindingResult().getGlobalErrors().forEach(ge -> {
            String msg = ge.getDefaultMessage();
            if (msg != null && !msg.isBlank()) {
                errors.add(msg);
            }
        });
        log.warn("[REST EXCEPTION] Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Invalid parameter '%s': value '%s' cannot be converted to %s",
                paramName, ex.getValue(), expectedType);
        log.warn("[REST EXCEPTION] Argument type mismatch: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        String message = String.format("Required request parameter '%s' is missing", ex.getParameterName());
        log.warn("[REST EXCEPTION] Missing request parameter: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        log.warn("[REST EXCEPTION] Constraint violation: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(org.springframework.web.method.annotation.HandlerMethodValidationException ex) {
        List<String> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
        log.warn("[REST EXCEPTION] Method validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[REST EXCEPTION] Malformed JSON request body: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed request payload"));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("[REST EXCEPTION] Method not supported: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedOperation(UnsupportedOperationException ex) {
        log.info("[REST EXCEPTION] Unsupported operation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("[REST EXCEPTION] Unexpected internal error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected internal error occurred"));
    }
}
