package com.bazaarhub.backend.cart.application.service;

import com.bazaarhub.backend.cart.application.dto.request.CheckoutAddressRequest;
import com.bazaarhub.backend.cart.application.dto.request.CheckoutValidateRequest;
import com.bazaarhub.backend.cart.application.dto.response.CartItemResponse;
import com.bazaarhub.backend.cart.application.dto.response.CartResponse;
import com.bazaarhub.backend.cart.application.dto.response.CheckoutSummaryResponse;
import com.bazaarhub.backend.cart.application.dto.response.CheckoutValidationResponse;
import com.bazaarhub.backend.cart.domain.exception.EmptyCartException;
import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
import com.bazaarhub.backend.customer.application.mapper.CustomerMapper;
import com.bazaarhub.backend.customer.domain.exception.AddressNotFoundException;
import com.bazaarhub.backend.customer.domain.exception.UnauthorizedAddressAccessException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing checkout readiness, address verification, price revalidation,
 * inventory stock revalidation, and checkout summary calculation.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartService cartService;
    private final SpringDataAddressRepository addressRepository;

    public CheckoutService(CartService cartService,
                           SpringDataAddressRepository addressRepository) {
        this.cartService = cartService;
        this.addressRepository = addressRepository;
    }

    /**
     * Prepares the checkout summary including real-time item availability,
     * price check, address verification, and overall checkout readiness status.
     */
    @Transactional(readOnly = true)
    public CheckoutSummaryResponse getCheckoutSummary(Long userId, Long addressId) {
        CartResponse cart = cartService.getCart(userId);
        List<String> validationErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            validationErrors.add("Cart is empty. Add products to cart before proceeding to checkout.");
            return CheckoutSummaryResponse.builder()
                    .canProceedToCheckout(false)
                    .checkoutStatus("EMPTY_CART")
                    .validationErrors(validationErrors)
                    .warnings(warnings)
                    .cart(cart)
                    .itemsCount(0)
                    .sellersCount(0)
                    .subtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .taxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .deliveryFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .grandTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }

        // 1. Revalidate all cart items for active status and available stock
        for (CartItemResponse item : cart.getItems()) {
            if (!item.isPurchasable()) {
                validationErrors.add("Product '" + item.getProductName() + "' (Variant: " + item.getVariantName() +
                        ") is inactive, unapproved, or unavailable.");
            } else if (!item.isInStock()) {
                validationErrors.add("Item '" + item.getProductName() + " - " + item.getVariantName() +
                        "' has insufficient stock (Requested: " + item.getQuantity() +
                        ", Available: " + item.getAvailableStock() + ").");
            }
        }

        // 2. Resolve and validate Delivery Address
        AddressResponse selectedAddress = null;
        if (addressId != null) {
            AddressEntity address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new AddressNotFoundException(addressId));

            if (!address.getUser().getId().equals(userId)) {
                throw new UnauthorizedAddressAccessException("Cannot use delivery address belonging to another customer");
            }
            selectedAddress = CustomerMapper.toAddressResponse(address);
        } else {
            // Fallback to customer's default address if available
            selectedAddress = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .map(CustomerMapper::toAddressResponse)
                    .orElse(null);

            if (selectedAddress == null) {
                warnings.add("No delivery address selected. Please select or add an address to complete checkout.");
            }
        }

        boolean canProceed = validationErrors.isEmpty() && selectedAddress != null;
        String checkoutStatus = canProceed ? "READY" : "ACTION_REQUIRED";

        return CheckoutSummaryResponse.builder()
                .canProceedToCheckout(canProceed)
                .checkoutStatus(checkoutStatus)
                .validationErrors(validationErrors)
                .warnings(warnings)
                .cart(cart)
                .selectedAddress(selectedAddress)
                .itemsCount(cart.getTotalItems())
                .sellersCount(cart.getSellers() != null ? cart.getSellers().size() : 0)
                .subtotal(cart.getSubtotal())
                .taxAmount(cart.getTaxAmount())
                .deliveryFee(cart.getDeliveryFee())
                .grandTotal(cart.getGrandTotal())
                .build();
    }

    /**
     * Validates whether current cart and address are strictly ready for order placement.
     */
    @Transactional(readOnly = true)
    public CheckoutValidationResponse validateCheckout(Long userId, CheckoutValidateRequest request) {
        CartResponse cart = cartService.getCart(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cart is empty. Add products before proceeding to checkout.");
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Revalidate inventory and product eligibility
        for (CartItemResponse item : cart.getItems()) {
            if (!item.isPurchasable()) {
                errors.add("Item '" + item.getProductName() + "' is not eligible for checkout (inactive or unapproved).");
            } else if (!item.isInStock()) {
                errors.add("Item '" + item.getProductName() + "' does not have sufficient stock (Requested: " +
                        item.getQuantity() + ", Available: " + item.getAvailableStock() + ").");
            }
        }

        // 2. Validate address if provided
        Long addressId = request != null ? request.getAddressId() : null;
        if (addressId != null) {
            AddressEntity address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new AddressNotFoundException(addressId));

            if (!address.getUser().getId().equals(userId)) {
                throw new UnauthorizedAddressAccessException("Cannot use delivery address belonging to another customer");
            }
        } else {
            boolean hasDefault = addressRepository.findByUserIdAndIsDefaultTrue(userId).isPresent();
            if (!hasDefault) {
                errors.add("A valid delivery address must be provided for checkout.");
            }
        }

        boolean isValid = errors.isEmpty();

        log.info("[CHECKOUT VALIDATION] Customer ID: {}, Valid: {}, Errors: {}", userId, isValid, errors.size());

        return CheckoutValidationResponse.builder()
                .valid(isValid)
                .errors(errors)
                .warnings(warnings)
                .cartId(cart.getCartId())
                .addressId(addressId)
                .subtotal(cart.getSubtotal())
                .grandTotal(cart.getGrandTotal())
                .build();
    }

    /**
     * Sets or updates the active delivery address for checkout.
     */
    @Transactional(readOnly = true)
    public CheckoutSummaryResponse selectCheckoutAddress(Long userId, CheckoutAddressRequest request) {
        if (request == null || request.getAddressId() == null) {
            throw new IllegalArgumentException("Address ID is required");
        }

        AddressEntity address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new AddressNotFoundException(request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedAddressAccessException("Cannot select address belonging to another customer");
        }

        return getCheckoutSummary(userId, address.getId());
    }
}
