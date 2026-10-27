package org.movo.movobackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetroLlegada {
    private int linea;          // route
    private Integer direction;  // direction (1 or 2)
    private String destino;     // destinationStopDescription
    private int minutos;        // remainingMinutes
    private String servicio;    // service
    private Integer vehiculo;   // VEHICULO (from cargaVehiculo)
    private Integer carga;      // CARGA % occupancy (from cargaVehiculo)

    public MetroLlegada(int linea, String destino, int minutos) {
        this.linea = linea;
        this.destino = destino;
        this.minutos = minutos;
    }
}
