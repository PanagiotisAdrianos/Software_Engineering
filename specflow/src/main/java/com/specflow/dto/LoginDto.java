package com.specflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDto {

    @NotBlank(message = "Το username είναι υποχρεωτικό")
    private String username;

    @NotBlank(message = "Ο κωδικός είναι υποχρεωτικός")
    private String password;
}
