package com.github.javarar.poke;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private String name;
    private Double height;
    private Double weight;
    private List<String> abilities;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response response = (Response) o;
        return Objects.equals(name, response.name) && Objects.equals(height, response.height) && Objects.equals(weight, response.weight) && Objects.equals(abilities, response.abilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, height, weight, abilities);
    }
}
