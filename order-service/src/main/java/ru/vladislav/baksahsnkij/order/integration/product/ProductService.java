package ru.vladislav.baksahsnkij.order.integration.product;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ProductService {
    private final ProductClient productClient;

    public ProductService(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Cacheable(cacheNames = "products", key = "#root.args")
    public Product getById(Long id) {
        return productClient.getById(id);
    }
}
