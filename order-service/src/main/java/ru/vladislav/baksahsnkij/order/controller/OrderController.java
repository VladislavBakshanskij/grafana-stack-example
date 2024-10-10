package ru.vladislav.baksahsnkij.order.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import ru.vladislav.baksahsnkij.order.model.db.Order;
import ru.vladislav.baksahsnkij.order.model.dto.CreateOrderRequest;
import ru.vladislav.baksahsnkij.order.service.OrderService;

@RestController
@RequestMapping("orders")
public record OrderController(OrderService orderService) {
    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request, @RequestHeader(HttpHeaders.AUTHORIZATION) long customerId) {
        return orderService.create(request, customerId);
    }
}