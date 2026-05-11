package com.autenticacion.authentication.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UsuarioAdminResponse {
    private Long id;
    private String username;
    private String email;
    private String rol;
    private String estado;
}
