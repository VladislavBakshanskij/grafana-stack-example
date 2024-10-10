package ru.vladislav.baksahsnkij.customer.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.vladislav.baksahsnkij.customer.model.db.Customer;
import ru.vladislav.baksahsnkij.customer.model.dto.CreateCustomer;
import ru.vladislav.baksahsnkij.customer.repository.CustomerRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public record CustomerService(CustomerRepository customerRepository,
                              KafkaTemplate<String, Object> kafkaTemplate) {
    public Customer create(CreateCustomer createCustomer) {
        long start = System.currentTimeMillis();
        long end;
        do {
            end = System.currentTimeMillis();
        } while (end - start < TimeUnit.SECONDS.toMillis(2));

        Customer saved = customerRepository.save(new Customer(null, createCustomer.name()));
        kafkaTemplate.send(
                "change",
                UUID.randomUUID().toString(),
                saved
        ).join();
        return saved;
    }

    public Customer getById(long id){
        return customerRepository.findById(id).orElseThrow();
    }
}
