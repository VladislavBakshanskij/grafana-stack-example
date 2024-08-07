package ru.vladislav.baksahsnkij.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.pyroscope.labels.LabelsSet;
import io.pyroscope.labels.Pyroscope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.lettuce.observability.MicrometerTracingAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@SpringBootApplication
@EnableCaching
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ClientResourcesBuilderCustomizer clientResourcesBuilderCustomizer(ObservationRegistry observationRegistry,
                                                                             @Value("${spring.application.name}") String application) {
        return clientResourcesBuilder -> clientResourcesBuilder.tracing(new MicrometerTracingAdapter(observationRegistry, application));
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer(ClientResources clientResources) {
        return clientConfigurationBuilder -> clientConfigurationBuilder.clientResources(clientResources);
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        return builder -> builder.withCacheConfiguration(
                        "customers",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                                )
                                .entryTtl(Duration.ofSeconds(5))
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(
                                                objectMapper,
                                                Customer.class
                                        ))
                                )
                )
                .withCacheConfiguration(
                        "products",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                                )
                                .entryTtl(Duration.ofSeconds(5))
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(
                                                objectMapper,
                                                Product.class
                                        ))
                                )
                );
    }

    @Bean
    public SdkTracerProviderBuilderCustomizer sdkTracerProviderBuilderCustomizer(@Value("${hostname}") String hostname) {
        return builder -> builder.addSpanProcessor(new BaggageTaggingSpanProcessor(List.of("rqid")))
                .addSpanProcessor(new SpanProcessor() {
                    @Override
                    public void onStart(Context context, ReadWriteSpan readWriteSpan) {
                        readWriteSpan.setAttribute("hostname", hostname);
                    }

                    @Override
                    public boolean isStartRequired() {
                        return true;
                    }

                    @Override
                    public void onEnd(ReadableSpan readableSpan) {

                    }

                    @Override
                    public boolean isEndRequired() {
                        return false;
                    }
                });
    }


    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper objectMapper,
                                                       KafkaProperties kafkaProperties,
                                                       SslBundles sslBundles) {
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildConsumerProperties(sslBundles),
                new StringSerializer(),
                new JsonSerializer<>(objectMapper)
        ));
        kafkaTemplate.setObservationEnabled(true);
        return kafkaTemplate;
    }
}

record Product(Long id) {

}

record Customer(Long id) {

}

@HttpExchange("products")
interface ProductClient {
    @GetExchange("{id}")
    Product getById(@PathVariable Long id);
}

@HttpExchange("customers")
interface CustomerClient {
    @GetExchange("{id}")
    Customer getById(@PathVariable Long id);
}


@Component
class ProductClientWrapper {
    private final ProductClient productClient;

    public ProductClientWrapper(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Cacheable(cacheNames = "products", key = "#root.args")
    public Product getById(Long id) {
        return productClient.getById(id);
    }
}

@Component
class CustomerClientWrapper {
    private final CustomerClient customerClient;

    public CustomerClientWrapper(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @Cacheable(cacheNames = "customers", key = "#root.args")
    public Customer getById(Long id) {
        return customerClient.getById(id);
    }
}

@Configuration
class ClientConfiguration {
    @Bean
    public ProductClient productClient(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder.baseUrl("http://localhost:8083")
                .build();
        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter).build();
        return factory.createClient(ProductClient.class);
    }

    @Bean
    public CustomerClient customerClient(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder.baseUrl("http://localhost:8081")
                .build();
        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter).build();
        return factory.createClient(CustomerClient.class);
    }
}

record CreateOrderRequest(Long productId) {

}

record Order(Long id, Product product, Customer customer) {
}

@Service
record OrderService(ProductClientWrapper productClient,
                    CustomerClientWrapper customerClient,
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

@RestController
@RequestMapping("orders")
record OrderController(OrderService orderService) {
    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request, @RequestHeader(HttpHeaders.AUTHORIZATION) long customerId) {
        return orderService.create(request, customerId);
    }
}


@Component
class AgentLabelFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public AgentLabelFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ThrowingRunnable<Exception> action = ThrowingRunnable.throwingRunnable(() -> filterChain.doFilter(request, response));
        Optional.of(tracer)
                .map(Tracer::currentSpan)
                .map(Span::context)
                .ifPresentOrElse(
                        context -> {
                            Map<String, String> labels = Map.of(
                                    "traceId", context.traceId(),
                                    "spanId", context.spanId()
                            );

                            Pyroscope.LabelsWrapper.run(new LabelsSet(labels), action);
                        },
                        action
                );

    }
}


interface ThrowingRunnable<E extends Throwable> extends Runnable {
    static <E extends Throwable> ThrowingRunnable<E> throwingRunnable(ThrowingRunnable<E> throwingRunnable) {
        return throwingRunnable;
    }

    void tryRun() throws E;

    @Override
    default void run() {
        try {
            tryRun();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

