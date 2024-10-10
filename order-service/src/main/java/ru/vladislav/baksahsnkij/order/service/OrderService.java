package ru.vladislav.baksahsnkij.order.service;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.vladislav.baksahsnkij.order.integration.customer.Customer;
import ru.vladislav.baksahsnkij.order.integration.customer.CustomerService;
import ru.vladislav.baksahsnkij.order.integration.product.Product;
import ru.vladislav.baksahsnkij.order.integration.product.ProductService;
import ru.vladislav.baksahsnkij.order.model.db.Order;
import ru.vladislav.baksahsnkij.order.model.dto.CreateOrderRequest;

import java.util.Random;
import java.util.UUID;

@Service
public record OrderService(ProductService productClient,
                    CustomerService customerClient,
                    KafkaTemplate<String, Object> kafkaTemplate) {

    public Order create(CreateOrderRequest request, long customerId) {
        if (request.productId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product id must not be null"
            );
        }
        Product product = productClient.getById(request.productId());
        Customer customer = customerClient.getById(customerId);
        Order order = new Order(
                new Random().nextLong(0, Long.MAX_VALUE),
                product,
                customer
        );
        kafkaTemplate.send(
                "change",
                UUID.randomUUID().toString(),
                order
        ).join();
        return order;
    }
}
