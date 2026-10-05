package com.amalitech.module6.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskForm(@NotBlank @Size(max = 200) String title, Boolean completed) {
}
