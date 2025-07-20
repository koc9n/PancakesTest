package org.pancakelab.model;

import java.util.UUID;

public class Ingredient {
    private final UUID id = UUID.randomUUID();
    private final String name;

    public Ingredient(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
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
        return id.hashCode();
    }
}
