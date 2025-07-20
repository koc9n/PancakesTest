package org.pancakelab.model;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Ingredient {
    private final AtomicReference<UUID> id = new AtomicReference<>(UUID.randomUUID());
    private final String name;

    public Ingredient(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id.get();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id.get().hashCode();
    }
}
