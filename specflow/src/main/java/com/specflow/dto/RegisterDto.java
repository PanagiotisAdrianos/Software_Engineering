package com.specflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDto {

    @NotBlank(message = "Το username είναι υποχρεωτικό")
    @Size(min = 3, max = 50, message = "Το username πρέπει να έχει 3-50 χαρακτήρες")
    private String username;

    @NotBlank(message = "Το email είναι υποχρεωτικό")
    @Email(message = "Μη έγκυρο email")
    private String email;

    @NotBlank(message = "Ο κωδικός είναι υποχρεωτικός")
    @Size(min = 6, message = "Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες")
    private String password;
}
