package ru.vladislav.baksahsnkij.product.infra;

import io.micrometer.common.annotation.ValueResolver;
import ru.vladislav.baksahsnkij.product.model.Product;

public class ProductIdMeterTagResolver implements ValueResolver {
    @Override
    public String resolve(Object object) {
        if (object instanceof Product product) {
            return product.id().toString();
        }
        return null;
    }
}
