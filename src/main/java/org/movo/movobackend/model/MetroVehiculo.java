package org.movo.movobackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetroVehiculo {
    private String servicio;
    private Integer vehiculo;
    private int linea;
    private int direction;
    private Double lat;
    private Double lon;
    private String siguienteParadaCodigo;
    private String siguienteParada;
    private String destinoCodigo;
    private String destino;
    private int minutosSiguienteParada;
    private int ordenParada;
    private Integer carga;
    private String fechaActualizacion;
}

