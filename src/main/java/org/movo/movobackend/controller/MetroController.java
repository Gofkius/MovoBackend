package org.movo.movobackend.controller;

import org.movo.movobackend.model.MetroLinea;
import org.movo.movobackend.model.MetroParada;
import org.movo.movobackend.model.dto.MetroCargaVehiculoApiDTO;
import org.movo.movobackend.model.response.MetroItinerarioResponse;
import org.movo.movobackend.model.response.MetroLlegadaResponse;
import org.movo.movobackend.model.response.MetroParadaResponse;
import org.movo.movobackend.model.response.MetroVehiculosResponse;
import org.movo.movobackend.service.MetroService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/metro")
public class MetroController {
    private final MetroService metroService;

    public MetroController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping("/lineas")
    public List<MetroLinea> getLineas() {
        return metroService.getLineas();
    }

    @GetMapping("/paradas")
    public List<MetroParada> getParadas(
            @RequestParam(name = "linea", required = false) Integer linea,
            @RequestParam(name = "route", required = false) Integer route,
            @RequestParam(name = "sentido", required = false) Integer sentido,
            @RequestParam(name = "direction", required = false) Integer direction) {
        Integer r = linea != null ? linea : route;
        Integer d = sentido != null ? sentido : direction;
        if (r != null && d != null) {
            return metroService.getParadasPorLinea(r, d);
        }
        return metroService.getTodasLasParadas();
    }

    @GetMapping("/parada/{id}")
    public MetroParadaResponse getParada(@PathVariable String id) {
        return metroService.getParada(id);
    }

    @GetMapping("/itinerario")
    public MetroItinerarioResponse getItinerario(
            @RequestParam(name = "route", required = false) Integer route,
            @RequestParam(name = "linea", required = false) Integer linea,
            @RequestParam(name = "direction", required = false) Integer direction,
            @RequestParam(name = "sentido", required = false) Integer sentido) {
        int r = route != null ? route : (linea != null ? linea : 1);
        int d = direction != null ? direction : (sentido != null ? sentido : 1);
        return metroService.obtenerItinerario(r, d);
    }

    @GetMapping("/llegadas")
    public MetroLlegadaResponse getLlegadas(
            @RequestParam(name = "stopSAE", required = false) Integer stopSAE,
            @RequestParam(name = "stop", required = false) String stop) {
        if (stopSAE != null) {
            return metroService.obtenerLlegadas(stopSAE);
        }
        if (stop != null && !stop.isBlank()) {
            return metroService.obtenerLlegadasPorIdentificador(stop);
        }
        return new MetroLlegadaResponse(false, List.of());
    }

    @GetMapping("/llegadas/{id}")
    public MetroParadaResponse getLlegadasPorId(@PathVariable String id) {
        return metroService.getParada(id);
    }

    @GetMapping("/cercanas")
    public List<MetroParadaResponse> getCercanas(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "500") int radio) {
        return metroService.getParadasCercanas(lat, lng, radio);
    }

    @GetMapping("/vehiculos")
    public MetroVehiculosResponse getVehiculos(
            @RequestParam(name = "route", required = false) Integer route,
            @RequestParam(name = "linea", required = false) Integer linea,
            @RequestParam(name = "direction", required = false) Integer direction,
            @RequestParam(name = "sentido", required = false) Integer sentido) {
        Integer r = route != null ? route : linea;
        Integer d = direction != null ? direction : sentido;
        return metroService.getVehiculos(r, d);
    }

    @GetMapping("/carga")
    public List<MetroCargaVehiculoApiDTO> getCarga(
            @RequestParam(name = "servicio", required = false) Integer servicio,
            @RequestParam(name = "vehiculo", required = false) Integer vehiculo) {
        return metroService.getCargaVehiculos(servicio, vehiculo);
    }
}
