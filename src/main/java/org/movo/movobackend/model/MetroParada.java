package org.movo.movobackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetroParada {
    private int idParada;          // stopSAE
    private String codigo;         // 3-letter stop ID (e.g. "INT", "FUN")
    private String nombre;         // stopDescription
    private Integer orden;         // orderStop
    private Integer direction;
    private Double lat;
    private Double lon;
    private List<Integer> lineas;

    public MetroParada(int idParada, String nombre, int orden, int direction, Double lat, Double lon) {
        this.idParada = idParada;
        this.nombre = nombre;
        this.orden = orden;
        this.direction = direction;
        this.lat = lat;
        this.lon = lon;
    }
}
