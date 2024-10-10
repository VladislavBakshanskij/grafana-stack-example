package ru.vladislav.baksahsnkij.customer.controller;

import org.springframework.web.bind.annotation.*;
import ru.vladislav.baksahsnkij.customer.model.db.Customer;
import ru.vladislav.baksahsnkij.customer.model.dto.CreateCustomer;
import ru.vladislav.baksahsnkij.customer.service.CustomerService;

@RestController
@RequestMapping("customers")
public record CustomerController(CustomerService customerService) {
    @PostMapping
    public Customer createCustomer(@RequestBody CreateCustomer customer) {
        return customerService.create(customer);
    }

    @GetMapping("{id}")
    public Customer getById(@PathVariable Long id) {
        return customerService.getById(id);
    }
}
