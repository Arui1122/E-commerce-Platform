package com.ecommerce.inventory.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.StockReservationRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        inventoryRepository.deleteAll();
        
        // Create test data
        Inventory testInventory = new Inventory();
        testInventory.setProductId(1L);
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(10);
        inventoryRepository.save(testInventory);
    }

    @Test
    void createInventory_ShouldReturnCreatedInventory() throws Exception {
        // Given
        InventoryRequest request = new InventoryRequest();
        request.setProductId(2L);
        request.setQuantity(50);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(2)))
                .andExpect(jsonPath("$.quantity", is(50)))
                .andExpect(jsonPath("$.reservedQuantity", is(0)))
                .andExpect(jsonPath("$.availableQuantity", is(50)));
    }

    @Test
    void getInventory_ExistingProduct_ShouldReturnInventory() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(100)))
                .andExpect(jsonPath("$.reservedQuantity", is(10)))
                .andExpect(jsonPath("$.availableQuantity", is(90)));
    }

    @Test
    void getInventory_NonExistingProduct_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/inventory/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Inventory Not Found")));
    }

    @Test
    void checkStock_SufficientStock_ShouldReturnTrue() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/inventory/check/1")
                .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasStock", is(true)))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.requestedQuantity", is(50)));
    }

    @Test
    void checkStock_InsufficientStock_ShouldReturnFalse() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/inventory/check/1")
                .param("quantity", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasStock", is(false)));
    }

    @Test
    void reserveStock_SufficientStock_ShouldReturnSuccess() throws Exception {
        // Given
        StockReservationRequest request = new StockReservationRequest();
        request.setProductId(1L);
        request.setQuantity(20);
        request.setReferenceId("ORDER-123");

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved", is(true)))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(20)));
    }

    @Test
    void reserveStock_InsufficientStock_ShouldReturnConflict() throws Exception {
        // Given
        StockReservationRequest request = new StockReservationRequest();
        request.setProductId(1L);
        request.setQuantity(100); // More than available (90)
        request.setReferenceId("ORDER-124");

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reserved", is(false)));
    }

    @Test
    void releaseReservedStock_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/inventory/1/release")
                .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("successfully")))
                .andExpect(jsonPath("$.productId", is("1")))
                .andExpect(jsonPath("$.quantity", is("5")));
    }

    @Test
    void confirmReservedStock_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/inventory/1/confirm")
                .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("successfully")))
                .andExpect(jsonPath("$.productId", is("1")))
                .andExpect(jsonPath("$.quantity", is("5")));
    }

    @Test
    void replenishStock_ShouldReturnUpdatedInventory() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/inventory/1/replenish")
                .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(150))); // 100 + 50
    }

    @Test
    void getLowStockProducts_ShouldReturnLowStockList() throws Exception {
        // Given - Create a low stock product
        Inventory lowStockProduct = new Inventory();
        lowStockProduct.setProductId(3L);
        lowStockProduct.setQuantity(5);
        lowStockProduct.setReservedQuantity(0);
        inventoryRepository.save(lowStockProduct);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/low-stock")
                .param("minQuantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId", is(3)))
                .andExpect(jsonPath("$[0].availableQuantity", is(5)));
    }

    @Test
    void batchGetInventories_ShouldReturnInventoryList() throws Exception {
        // Given
        String productIds = "[1, 2]";

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productIds))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Only product 1 exists
                .andExpect(jsonPath("$[0].productId", is(1)));
    }

    @Test
    void healthCheck_ShouldReturnHealthy() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/inventory/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.message", containsString("running")));
    }

    @Test
    void invalidRequest_ShouldReturnValidationError() throws Exception {
        // Given - Invalid request with null productId
        InventoryRequest request = new InventoryRequest();
        request.setQuantity(50); // Missing productId

        // When & Then
        mockMvc.perform(post("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }
}
