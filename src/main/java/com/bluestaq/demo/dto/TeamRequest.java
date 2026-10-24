package com.bluestaq.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /teams}.
 *
 * @param name the team's name; required, at most 100 characters (the column length in schema.sql)
 */
public record TeamRequest(@NotBlank @Size(max = 100) String name) {
}
