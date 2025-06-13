package com.ecommerce.product.service;

import com.ecommerce.product.dto.CategoryDTO;
import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.Product.ProductStatus;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Optional<ProductDTO> getProductById(Long id) {
        log.debug("Fetching product by id: {}", id);
        Optional<Product> product = productRepository.findById(id);
        
        // Increment view count if product exists
        if (product.isPresent()) {
            incrementViewCount(id);
        }
        
        return product.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Optional<ProductDTO> getProductBySku(String sku) {
        log.debug("Fetching product by sku: {}", sku);
        return productRepository.findBySku(sku)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getActiveProducts(Pageable pageable) {
        log.debug("Fetching active products with pagination");
        Page<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        return products.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String name, Long categoryId, 
                                         BigDecimal minPrice, BigDecimal maxPrice, 
                                         Pageable pageable) {
        log.debug("Searching products with filters: name={}, categoryId={}, minPrice={}, maxPrice={}", 
                  name, categoryId, minPrice, maxPrice);
        
        Page<Product> products = productRepository.findProductsWithFilters(
                name, categoryId, minPrice, maxPrice, pageable);
        
        return products.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchByKeyword(String keyword, Pageable pageable) {
        log.debug("Searching products by keyword: {}", keyword);
        Page<Product> products = productRepository.searchByKeyword(keyword, pageable);
        return products.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.debug("Fetching products by category id: {}", categoryId);
        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
        return products.map(this::convertToDTO);
    }

    @Cacheable(value = "products", key = "'popular'")
    @Transactional(readOnly = true)
    public List<ProductDTO> getPopularProducts() {
        log.debug("Fetching popular products");
        List<Product> products = productRepository.findTop10ByStatusOrderByViewCountDesc(ProductStatus.ACTIVE);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("Creating new product: {}", productDTO.getName());
        
        if (productDTO.getSku() != null && productRepository.existsBySku(productDTO.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + productDTO.getSku() + "' already exists");
        }

        Category category = null;
        if (productDTO.getCategory() != null && productDTO.getCategory().getId() != null) {
            category = categoryRepository.findById(productDTO.getCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + productDTO.getCategory().getId()));
        }

        Product product = Product.builder()
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .category(category)
                .brand(productDTO.getBrand())
                .sku(productDTO.getSku())
                .imageUrl(productDTO.getImageUrl())
                .status(productDTO.getStatus() != null ? productDTO.getStatus() : ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        
        return convertToDTO(savedProduct);
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("Updating product with id: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        if (productDTO.getSku() != null && !product.getSku().equals(productDTO.getSku()) && 
            productRepository.existsBySku(productDTO.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + productDTO.getSku() + "' already exists");
        }

        Category category = null;
        if (productDTO.getCategory() != null && productDTO.getCategory().getId() != null) {
            category = categoryRepository.findById(productDTO.getCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + productDTO.getCategory().getId()));
        }

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setCategory(category);
        product.setBrand(productDTO.getBrand());
        product.setSku(productDTO.getSku());
        product.setImageUrl(productDTO.getImageUrl());
        product.setStatus(productDTO.getStatus());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with id: {}", updatedProduct.getId());
        
        return convertToDTO(updatedProduct);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with id: " + id);
        }

        productRepository.deleteById(id);
        log.info("Product deleted successfully with id: {}", id);
    }

    private void incrementViewCount(Long productId) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setViewCount(product.getViewCount() + 1);
            productRepository.save(product);
        });
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory() != null ? convertCategoryToDTO(product.getCategory()) : null)
                .brand(product.getBrand())
                .sku(product.getSku())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .viewCount(product.getViewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private CategoryDTO convertCategoryToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
