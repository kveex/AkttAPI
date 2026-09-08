package me.kveex.akttapispringed.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistrationRequest {
    @Size(min = 1, max = 255, message = "Логин должен быть в диапазоне от 1 до 255 символов!")
    @NotBlank
    private String login;
    @Size(min = 8, message = "Пароль должен быть длинной минимум 8 символов!")
    @NotBlank
    private String password;
}
