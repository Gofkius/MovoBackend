package org.movo.movobackend.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.movo.movobackend.model.MetroLlegada;
import org.movo.movobackend.model.MetroParada;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroParadaResponse {
    private boolean success;
    private MetroParada parada;
    private List<MetroLlegada> llegadas;
}

