package com.bazaarhub.backend.shared;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test suite for Pre-Phase 16 Remediation Stage 2: Global Pagination Protection.
 * Verifies global clamping of pageable requests across REST controllers, ensuring protection
 * against memory exhaustion (DoS) while preserving 0-indexed pagination, sorting, and backward compatibility.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaginationLimitSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataBrandRepository brandRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear dependent tables to ensure test isolation
        jdbcTemplate.execute("DELETE FROM product_variants");
        jdbcTemplate.execute("DELETE FROM product_images");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM brands");

        // Seed 120 brands to thoroughly test default, custom, max, and clamped pagination
        List<BrandEntity> brands = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            String paddedIndex = String.format("%03d", i);
            brands.add(BrandEntity.builder()
                    .name("Brand " + paddedIndex)
                    .slug("brand-" + paddedIndex)
                    .status(CatalogStatus.ACTIVE)
                    .build());
        }
        brandRepository.saveAll(brands);

        // Seed 50 categories for cross-controller verification
        List<CategoryEntity> categories = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            String paddedIndex = String.format("%03d", i);
            categories.add(CategoryEntity.builder()
                    .name("Category " + paddedIndex)
                    .slug("category-" + paddedIndex)
                    .status(CatalogStatus.ACTIVE)
                    .displayOrder(i)
                    .build());
        }
        categoryRepository.saveAll(categories);
    }

    @Test
    @DisplayName("Scenario 1: Default request with no page or size parameters returns page size 20")
    void testDefaultRequestWithoutParametersReturnsDefaultPageSize20() throws Exception {
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.number", is(0)))
                .andExpect(jsonPath("$.data.content", hasSize(20)))
                .andExpect(jsonPath("$.data.totalElements", is(120)))
                .andExpect(jsonPath("$.data.totalPages", is(6)));
    }

    @Test
    @DisplayName("Scenario 2: Request with valid size=10 returns page size 10")
    void testRequestWithValidSize10ReturnsPageSize10() throws Exception {
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.number", is(0)))
                .andExpect(jsonPath("$.data.content", hasSize(10)))
                .andExpect(jsonPath("$.data.totalElements", is(120)))
                .andExpect(jsonPath("$.data.totalPages", is(12)));
    }

    @Test
    @DisplayName("Scenario 3: Request with maximum allowed size=100 returns page size 100")
    void testRequestWithMaximumAllowedSize100ReturnsPageSize100() throws Exception {
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.number", is(0)))
                .andExpect(jsonPath("$.data.content", hasSize(100)))
                .andExpect(jsonPath("$.data.totalElements", is(120)))
                .andExpect(jsonPath("$.data.totalPages", is(2)));
    }

    @Test
    @DisplayName("Scenario 4: Request with size=101 is clamped to 100")
    void testRequestWithSize101IsClampedTo100() throws Exception {
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.number", is(0)))
                .andExpect(jsonPath("$.data.content", hasSize(100)))
                .andExpect(jsonPath("$.data.totalElements", is(120)))
                .andExpect(jsonPath("$.data.totalPages", is(2)));
    }

    @Test
    @DisplayName("Scenario 5: Malicious request with size=1000 or size=10000 is clamped to 100 (never returns >100 records)")
    void testMaliciousLargePageSizeRequestIsClampedTo100() throws Exception {
        // Test size=1000
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.content", hasSize(100)))
                .andExpect(jsonPath("$.data.content", hasSize(lessThanOrEqualTo(100))));

        // Test size=10000
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.content", hasSize(100)))
                .andExpect(jsonPath("$.data.content", hasSize(lessThanOrEqualTo(100))));
    }

    @Test
    @DisplayName("Scenario 6: Invalid or negative size parameters behave safely")
    void testInvalidOrNegativeSizeParametersBehaveSafely() throws Exception {
        // Negative size should fall back safely to default page size 20
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.content", hasSize(20)));

        // Zero size should fall back safely to default page size 20
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.content", hasSize(20)));

        // Non-numeric size parameter safely falls back to default page size according to Spring Data framework behavior
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "invalid-size"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.content", hasSize(20)));
    }

    @Test
    @DisplayName("Scenario 7: Sorting parameters continue to work properly with clamped pageable")
    void testSortingParametersWorkProperlyWithClampedPageable() throws Exception {
        // Request size=200 (clamped to 100) sorted descending by name
        mockMvc.perform(get("/api/v1/brands")
                        .param("size", "200")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.content", hasSize(100)))
                // Verify descending order: highest brand number first
                .andExpect(jsonPath("$.data.content[0].name", is("Brand 120")))
                .andExpect(jsonPath("$.data.content[1].name", is("Brand 119")))
                .andExpect(jsonPath("$.data.content[99].name", is("Brand 021")));
    }

    @Test
    @DisplayName("Scenario 8: Backward compatibility for existing legitimate client requests and across endpoints")
    void testBackwardCompatibilityAndCrossControllerClamping() throws Exception {
        // 1. Legitimate request with page 0, size 20, ascending sort
        mockMvc.perform(get("/api/v1/brands")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.number", is(0)))
                .andExpect(jsonPath("$.data.content[0].name", is("Brand 001")))
                .andExpect(jsonPath("$.data.content[19].name", is("Brand 020")));

        // 2. Legitimate request navigating to page 1
        mockMvc.perform(get("/api/v1/brands")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.number", is(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Brand 021")))
                .andExpect(jsonPath("$.data.content[19].name", is("Brand 040")));

        // 3. Cross-controller verification: CategoryController also clamps to 100
        mockMvc.perform(get("/api/v1/categories")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.content", hasSize(50))); // All 50 categories returned since 50 <= 100
    }
}
