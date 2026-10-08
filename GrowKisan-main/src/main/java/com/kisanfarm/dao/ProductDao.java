package com.kisanfarm.dao;

import com.kisanfarm.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for products.
 */
public interface ProductDao {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    Long save(Product product);

    int update(Product product);

    int deleteById(Long id);
}
