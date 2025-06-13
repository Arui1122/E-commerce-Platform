package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findByActiveTrue();

    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findAllActiveOrderBySort();

    boolean existsByName(String name);
}
