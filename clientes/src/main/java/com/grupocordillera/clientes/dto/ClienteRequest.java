package com.grupocordillera.clientes.dto;

import com.grupocordillera.clientes.model.Cliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String telefono;

    @NotBlank
    private String direccion;

    private Cliente.TipoCliente tipoCliente;

    private Cliente.Estado estado;
}
