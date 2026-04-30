package com.autenticacion.authentication.DTO;

import lombok.Data;

@Data
public class RegistroRequest {
    private String username;
    private String email;
    private String password;
}
