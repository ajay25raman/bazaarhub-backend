package com.bazaarhub.backend.customer.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.customer.application.dto.request.AddressCreateRequest;
import com.bazaarhub.backend.customer.application.dto.request.AddressUpdateRequest;
import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
import com.bazaarhub.backend.customer.application.service.CustomerAddressService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/addresses")
@Tag(name = "Customer Address Management", description = "Endpoints for authenticated customers to manage delivery addresses, default selection, and address books")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('CUSTOMER_PROFILE_READ', 'CUSTOMER_PROFILE_UPDATE', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    public CustomerAddressController(CustomerAddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @Operation(summary = "Add delivery address", description = "Adds a new delivery address for the authenticated customer. If designated as default or is first address, unsets any existing default.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Address created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on address payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        AddressResponse response = addressService.addAddress(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Address created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all addresses", description = "Retrieves all saved delivery addresses for the authenticated customer, ordered with the default address first.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address list retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        List<AddressResponse> addresses = addressService.getCustomerAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    @GetMapping("/default")
    @Operation(summary = "Get default delivery address", description = "Retrieves the customer's primary default delivery address.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default address retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No default address found", content = @Content)
    })
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        AddressResponse address = addressService.getDefaultAddress(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(address));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID", description = "Retrieves a single delivery address by ID. Enforces customer data ownership.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Address belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        AddressResponse response = addressService.getAddressById(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update delivery address", description = "Updates details of an existing delivery address. Enforces customer data ownership.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on update payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Address belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        AddressResponse response = addressService.updateAddress(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Set address as default", description = "Sets the specified address as the customer's primary default address, transactionally unsetting any prior default.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default address updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Address belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        AddressResponse response = addressService.setDefaultAddress(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete delivery address", description = "Removes a delivery address. If the deleted address was the default, automatically promotes the most recently updated remaining address to default.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or expired JWT", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Address belongs to another customer", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Address referenced by existing orders", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        addressService.deleteAddress(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted successfully", null));
    }
}
