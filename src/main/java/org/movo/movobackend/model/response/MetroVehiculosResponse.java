package org.movo.movobackend.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.movo.movobackend.model.MetroVehiculo;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroVehiculosResponse {
    private boolean success;
    private List<MetroVehiculo> vehiculos;
}

