package org.movo.movobackend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroVehicleLocationApiDTO {
    private String service;
    private Coordinate coordinate;
    private int route;
    private int direction;
    private int remainingMinutes;
    private String nextStop;
    private String nextStopDescription;
    private String destinationStop;
    private String destinationStopDescription;
    private int orderStop;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Coordinate {
        private Double lat;
        private Double lon;
    }
}

