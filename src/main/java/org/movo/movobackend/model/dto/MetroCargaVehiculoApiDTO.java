package org.movo.movobackend.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetroCargaVehiculoApiDTO {
    @JsonProperty("VEHICULO")
    private Integer vehiculo;

    @JsonProperty("FECHA")
    private String fecha;

    @JsonProperty("SERVICIO")
    private Integer servicio;

    @JsonProperty("CARGA")
    private Integer carga;
}

