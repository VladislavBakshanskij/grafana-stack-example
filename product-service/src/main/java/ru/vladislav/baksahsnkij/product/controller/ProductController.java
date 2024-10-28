package ru.vladislav.baksahsnkij.product.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vladislav.baksahsnkij.product.model.Product;
import ru.vladislav.baksahsnkij.product.service.ProductService;

@RestController
@RequestMapping("products")
record ProductController(ProductService productService) {
    @PostMapping
    public Product create(@RequestBody Product product) {
        return productService.create(product);
    }

    @GetMapping("{id}")
    public Product getById(@PathVariable Long id) {
        return productService.get(id);
    }
}
