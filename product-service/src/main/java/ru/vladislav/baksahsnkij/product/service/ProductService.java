package ru.vladislav.baksahsnkij.product.service;

import io.micrometer.tracing.SpanName;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.ContinueSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskDecorator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.vladislav.baksahsnkij.product.repository.TaskRepository;
import ru.vladislav.baksahsnkij.product.model.Product;
import ru.vladislav.baksahsnkij.product.model.Task;
import ru.vladislav.baksahsnkij.product.repository.ProductRepository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductService {
    AtomicInteger atomicInteger = new AtomicInteger(0);


    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;


    private final TaskRepository taskRepository;

    private final TaskDecorator taskDecorator;

    private final Tracer tracer;

    private final SpecialService specialService;

    public ProductService(ProductRepository productRepository, TaskRepository taskRepository, TaskDecorator taskDecorator, Tracer tracer, SpecialService specialService) {
        this.productRepository = productRepository;
        this.taskRepository = taskRepository;
        this.taskDecorator = taskDecorator;
        this.tracer = tracer;
        this.specialService = specialService;
    }

    @SpanName("my_span_name")
    public Product create(Product product) {
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            if (product.id() == 3) {
                throw new RuntimeException();
            }
            specialService.newSpanWithAllFields();
            specialService.newSpanWithName();
            specialService.newSpanWithValue();
            specialService.newSpanWithDefault();
            specialService.continueSpan();
            specialService.continueSpanWithLog();
            specialService.spanNameAndTag(atomicInteger.getAndIncrement());

            CompletableFuture.runAsync(() -> taskRepository.save(new Task()),
                    command -> executorService.execute(taskDecorator.decorate(command)));
            return productRepository.save(product);
        } catch (Exception e) {
            log.error("invalid id {} ", product, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid id", e);
        }
    }

    @ContinueSpan(log = "my_continue_span")
    public Product get(Long id) {
        long start = System.currentTimeMillis();
        long end;
        do {
            end = System.currentTimeMillis();

        } while (end - start < TimeUnit.SECONDS.toMillis(2));

        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
