package org.movo.movobackend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.movo.movobackend.model.*;
import org.movo.movobackend.model.dto.MetroCargaVehiculoApiDTO;
import org.movo.movobackend.model.dto.MetroVehicleLocationApiDTO;
import org.movo.movobackend.model.entity.LineaEntity;
import org.movo.movobackend.model.entity.ParadaEntity;
import org.movo.movobackend.model.response.MetroItinerarioResponse;
import org.movo.movobackend.model.response.MetroLlegadaResponse;
import org.movo.movobackend.model.response.MetroParadaResponse;
import org.movo.movobackend.model.response.MetroVehiculosResponse;
import org.movo.movobackend.repository.LineaRepository;
import org.movo.movobackend.repository.ParadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetroService {
    private static final Logger log = LoggerFactory.getLogger(MetroService.class);
    private static final String OPERADOR_METRO = "METRO";

    @Value("${metro.api.base-url:https://tranviaonline.metrotenerife.com}")
    private String metroBaseUrl;

    private final RestTemplate restTemplate;
    private final LineaRepository lineaRepository;
    private final ParadaRepository paradaRepository;

    // --- CACHES ---
    private final Cache<String, MetroInfoPanel[]> panelCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(10)
            .build();

    private final Cache<String, MetroVehicleLocationApiDTO[]> vehicleLocationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .maximumSize(10)
            .build();

    private final Cache<String, List<MetroCargaVehiculoApiDTO>> cargaVehiculoCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(10)
            .build();

    // Cached METRO rows from the database (same pattern as TitsaService.paradasUnicasCache)
    private final Cache<String, List<ParadaEntity>> paradasBdCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(1)
            .build();

    public MetroService(RestTemplate restTemplate, LineaRepository lineaRepository, ParadaRepository paradaRepository) {
        this.restTemplate = restTemplate;
        this.lineaRepository = lineaRepository;
        this.paradaRepository = paradaRepository;
    }

    // --- UPSTREAM FETCHERS (CACHED) ---

    private MetroInfoPanel[] obtenerPanelCacheado() {
        return panelCache.get("panel", k -> descargarPanelCompleto());
    }

    private MetroInfoPanel[] descargarPanelCompleto() {
        try {
            MetroInfoPanel[] panel = restTemplate.getForObject(metroBaseUrl + "/api/infoStops/infoPanel", MetroInfoPanel[].class);
            return panel != null ? panel : new MetroInfoPanel[0];
        } catch (Exception e) {
            log.warn("Failed to fetch Metrotenerife infoPanel: {}", e.toString());
            return new MetroInfoPanel[0];
        }
    }

    private MetroVehicleLocationApiDTO[] obtenerVehicleLocationsCacheados() {
        return vehicleLocationCache.get("vehicles", k -> descargarVehicleLocations());
    }

    private MetroVehicleLocationApiDTO[] descargarVehicleLocations() {
        try {
            MetroVehicleLocationApiDTO[] locations = restTemplate.getForObject(metroBaseUrl + "/api/infoStops/vehicleLocation", MetroVehicleLocationApiDTO[].class);
            return locations != null ? locations : new MetroVehicleLocationApiDTO[0];
        } catch (Exception e) {
            log.warn("Failed to fetch Metrotenerife vehicleLocation: {}", e.toString());
            return new MetroVehicleLocationApiDTO[0];
        }
    }

    private List<MetroCargaVehiculoApiDTO> obtenerCargaVehiculosCacheada() {
        return cargaVehiculoCache.get("carga", k -> descargarCargaVehiculos());
    }

    private List<MetroCargaVehiculoApiDTO> descargarCargaVehiculos() {
        try {
            MetroCargaVehiculoApiDTO[] cargas = restTemplate.getForObject(metroBaseUrl + "/api/cargaVehiculo", MetroCargaVehiculoApiDTO[].class);
            return cargas != null ? Arrays.asList(cargas) : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch Metrotenerife cargaVehiculo: {}", e.toString());
            return List.of();
        }
    }

    private Map<String, MetroCargaVehiculoApiDTO> mapaCargaPorServicio() {
        return obtenerCargaVehiculosCacheada().stream()
                .filter(c -> c.getServicio() != null)
                .collect(Collectors.toMap(
                        c -> String.valueOf(c.getServicio()),
                        c -> c,
                        (a, b) -> b
                ));
    }

    // --- HELPERS DE BASE DE DATOS ---

    private List<ParadaEntity> obtenerParadasMetroBD() {
        return paradasBdCache.get("all", k -> List.copyOf(paradaRepository.findByOperador(OPERADOR_METRO)));
    }

    private Map<String, ParadaEntity> mapaParadasUnicasPorCodigo() {
        return obtenerParadasMetroBD().stream()
                .collect(Collectors.toMap(
                        p -> p.getParadaId().trim().toUpperCase(Locale.ROOT),
                        p -> p,
                        (p1, p2) -> p1,
                        LinkedHashMap::new
                ));
    }

    private Map<String, List<Integer>> mapaLineasPorCodigoParada() {
        return obtenerParadasMetroBD().stream()
                .collect(Collectors.groupingBy(
                        p -> p.getParadaId().trim().toUpperCase(Locale.ROOT),
                        Collectors.mapping(
                                p -> parseLineaInt(p.getLinea()),
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(TreeSet::new),
                                        ArrayList::new
                                )
                        )
                ));
    }

    private int parseLineaInt(String linea) {
        try {
            return Integer.parseInt(linea.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // --- LÍNEAS Y PARADAS (Base de Datos) ---

    public List<MetroLinea> getLineas() {
        List<LineaEntity> lineasBD = lineaRepository.findByOperador(OPERADOR_METRO);
        return lineasBD.stream()
                .map(entidad -> new MetroLinea(entidad.getId(), entidad.getNombre()))
                .toList();
    }

    public List<MetroParada> getTodasLasParadas() {
        MetroInfoPanel[] panel = obtenerPanelCacheado();
        Map<String, Integer> saePorCodigo = Arrays.stream(panel)
                .filter(p -> p.getStop() != null)
                .collect(Collectors.toMap(
                        p -> p.getStop().trim().toUpperCase(Locale.ROOT),
                        MetroInfoPanel::getStopSAE,
                        (a, b) -> a
                ));

        Map<String, List<Integer>> lineasPorCodigo = mapaLineasPorCodigoParada();

        return mapaParadasUnicasPorCodigo().values().stream()
                .map(entidad -> {
                    String code = entidad.getParadaId().trim().toUpperCase(Locale.ROOT);
                    return new MetroParada(
                            saePorCodigo.getOrDefault(code, 0),
                            code,
                            entidad.getNombre(),
                            null,
                            null,
                            entidad.getLat(),
                            entidad.getLng(),
                            lineasPorCodigo.getOrDefault(code, List.of())
                    );
                })
                .toList();
    }

    public List<MetroParada> getParadasPorLinea(int route, int direction) {
        List<ParadaEntity> paradasBD = paradaRepository.findByOperadorAndLineaAndSentidoOrderByOrdenParadaAsc(
                OPERADOR_METRO, String.valueOf(route), direction);

        MetroInfoPanel[] panel = obtenerPanelCacheado();
        Map<String, Integer> saePorCodigo = Arrays.stream(panel)
                .filter(p -> p.getRoute() == route && p.getDirection() == direction && p.getStop() != null)
                .collect(Collectors.toMap(
                        p -> p.getStop().trim().toUpperCase(Locale.ROOT),
                        MetroInfoPanel::getStopSAE,
                        (a, b) -> a
                ));

        Map<String, List<Integer>> lineasPorCodigo = mapaLineasPorCodigoParada();

        return paradasBD.stream()
                .map(entidad -> {
                    String code = entidad.getParadaId().trim().toUpperCase(Locale.ROOT);
                    return new MetroParada(
                            saePorCodigo.getOrDefault(code, 0),
                            code,
                            entidad.getNombre(),
                            entidad.getOrdenParada(),
                            entidad.getSentido(),
                            entidad.getLat(),
                            entidad.getLng(),
                            lineasPorCodigo.getOrDefault(code, List.of(route))
                    );
                })
                .toList();
    }

    public MetroItinerarioResponse obtenerItinerario(int route, int direction) {
        List<MetroParada> paradas = getParadasPorLinea(route, direction);

        // Fallback to building from infoPanel + DB coordinates if DB has no rows for that route/direction
        if (paradas.isEmpty()) {
            MetroInfoPanel[] panel = obtenerPanelCacheado();
            Map<String, ParadaEntity> mapaCoordenadas = mapaParadasUnicasPorCodigo();
            Map<String, List<Integer>> lineasPorCodigo = mapaLineasPorCodigoParada();

            paradas = Arrays.stream(panel)
                    .filter(p -> p.getRoute() == route && p.getDirection() == direction)
                    .collect(Collectors.toMap(
                            p -> p.getStopSAE() + "-" + p.getOrderStop(),
                            p -> p,
                            (a, b) -> a
                    ))
                    .values().stream()
                    .sorted(Comparator.comparingInt(MetroInfoPanel::getOrderStop))
                    .map(p -> {
                        String code = p.getStop() != null ? p.getStop().trim().toUpperCase(Locale.ROOT) : "";
                        ParadaEntity coord = mapaCoordenadas.get(code);
                        return new MetroParada(
                                p.getStopSAE(),
                                code.isEmpty() ? null : code,
                                coord != null ? coord.getNombre() : p.getStopDescription(),
                                p.getOrderStop(),
                                p.getDirection(),
                                coord != null ? coord.getLat() : null,
                                coord != null ? coord.getLng() : null,
                                lineasPorCodigo.getOrDefault(code, List.of(route))
                        );
                    })
                    .toList();
        }

        return new MetroItinerarioResponse(!paradas.isEmpty(), paradas);
    }

    // --- LLEGADAS Y DETALLE DE PARADA ---

    public MetroLlegadaResponse obtenerLlegadas(int stopSAE) {
        MetroInfoPanel[] panel = obtenerPanelCacheado();
        Map<String, MetroCargaVehiculoApiDTO> cargaMap = mapaCargaPorServicio();

        List<MetroInfoPanel> matches = Arrays.stream(panel)
                .filter(p -> p.getStopSAE() == stopSAE)
                .toList();

        List<MetroLlegada> llegadas = mapearLlegadas(matches, cargaMap);
        return new MetroLlegadaResponse(true, llegadas);
    }

    public MetroLlegadaResponse obtenerLlegadasPorIdentificador(String stopIdOrSae) {
        if (stopIdOrSae == null || stopIdOrSae.isBlank()) {
            return new MetroLlegadaResponse(false, List.of());
        }
        String trimmed = stopIdOrSae.trim();
        try {
            int sae = Integer.parseInt(trimmed);
            return obtenerLlegadas(sae);
        } catch (NumberFormatException ignored) {
            String code = trimmed.toUpperCase(Locale.ROOT);
            Map<String, ParadaEntity> paradasMap = mapaParadasUnicasPorCodigo();
            if (!paradasMap.containsKey(code)) {
                return new MetroLlegadaResponse(false, List.of());
            }
            MetroInfoPanel[] panel = obtenerPanelCacheado();
            Map<String, MetroCargaVehiculoApiDTO> cargaMap = mapaCargaPorServicio();
            List<MetroInfoPanel> matches = Arrays.stream(panel)
                    .filter(p -> code.equalsIgnoreCase(p.getStop()))
                    .toList();
            return new MetroLlegadaResponse(true, mapearLlegadas(matches, cargaMap));
        }
    }

    public MetroParadaResponse getParada(String stopIdOrSae) {
        if (stopIdOrSae == null || stopIdOrSae.isBlank()) {
            return new MetroParadaResponse(false, null, List.of());
        }
        String trimmed = stopIdOrSae.trim();
        MetroInfoPanel[] panel = obtenerPanelCacheado();
        Map<String, ParadaEntity> paradasMap = mapaParadasUnicasPorCodigo();
        Map<String, List<Integer>> lineasPorCodigo = mapaLineasPorCodigoParada();

        ParadaEntity entidad;
        Integer requestedSae = null;

        try {
            requestedSae = Integer.parseInt(trimmed);
            int saeLookup = requestedSae;
            String codeFromPanel = Arrays.stream(panel)
                    .filter(p -> p.getStopSAE() == saeLookup && p.getStop() != null)
                    .map(p -> p.getStop().trim().toUpperCase(Locale.ROOT))
                    .findFirst()
                    .orElse(null);
            entidad = codeFromPanel != null ? paradasMap.get(codeFromPanel) : null;
        } catch (NumberFormatException ignored) {
            entidad = paradasMap.get(trimmed.toUpperCase(Locale.ROOT));
        }

        if (entidad == null) {
            return new MetroParadaResponse(false, null, List.of());
        }

        String code = entidad.getParadaId().trim().toUpperCase(Locale.ROOT);
        int sae = requestedSae != null ? requestedSae : Arrays.stream(panel)
                .filter(p -> code.equalsIgnoreCase(p.getStop()))
                .mapToInt(MetroInfoPanel::getStopSAE)
                .findFirst()
                .orElse(0);

        MetroParada parada = new MetroParada(
                sae,
                code,
                entidad.getNombre(),
                null,
                null,
                entidad.getLat(),
                entidad.getLng(),
                lineasPorCodigo.getOrDefault(code, List.of())
        );

        MetroLlegadaResponse llegadasResp = requestedSae != null
                ? obtenerLlegadas(requestedSae)
                : obtenerLlegadasPorIdentificador(code);

        return new MetroParadaResponse(true, parada, llegadasResp.getLlegadas());
    }

    private List<MetroLlegada> mapearLlegadas(List<MetroInfoPanel> matches, Map<String, MetroCargaVehiculoApiDTO> cargaMap) {
        return matches.stream()
                .collect(Collectors.toMap(
                        p -> p.getRoute() + "-" + p.getDirection() + "-" + p.getService(),
                        p -> p,
                        (a, b) -> a.getRemainingMinutes() <= b.getRemainingMinutes() ? a : b
                ))
                .values().stream()
                .sorted(Comparator.comparingInt(MetroInfoPanel::getRemainingMinutes))
                .map(p -> {
                    MetroCargaVehiculoApiDTO cargaDto = p.getService() != null
                            ? cargaMap.get(p.getService().trim())
                            : null;
                    return new MetroLlegada(
                            p.getRoute(),
                            p.getDirection(),
                            p.getDestinationStopDescription(),
                            p.getRemainingMinutes(),
                            p.getService(),
                            cargaDto != null ? cargaDto.getVehiculo() : null,
                            cargaDto != null ? cargaDto.getCarga() : null
                    );
                })
                .toList();
    }

    // --- PARADAS CERCANAS (Base de Datos + Tiempo Real) ---

    public List<MetroParadaResponse> getParadasCercanas(double latUsuario, double lngUsuario, int radioMetros) {
        record Cerca(ParadaEntity parada, double metros) {}

        MetroInfoPanel[] panel = obtenerPanelCacheado();
        Map<String, MetroCargaVehiculoApiDTO> cargaMap = mapaCargaPorServicio();
        Map<String, List<Integer>> lineasPorCodigo = mapaLineasPorCodigoParada();

        Map<String, List<MetroInfoPanel>> panelPorCodigo = Arrays.stream(panel)
                .filter(p -> p.getStop() != null)
                .collect(Collectors.groupingBy(p -> p.getStop().trim().toUpperCase(Locale.ROOT)));

        return mapaParadasUnicasPorCodigo().values().stream()
                .filter(p -> p.getLat() != null && p.getLng() != null)
                .map(p -> new Cerca(p, calcularDistancia(latUsuario, lngUsuario, p.getLat(), p.getLng())))
                .filter(c -> c.metros() <= radioMetros)
                .sorted(Comparator.comparingDouble(Cerca::metros))
                .map(c -> {
                    ParadaEntity entidad = c.parada();
                    String code = entidad.getParadaId().trim().toUpperCase(Locale.ROOT);
                    List<MetroInfoPanel> llegadasRaw = panelPorCodigo.getOrDefault(code, List.of());
                    int sae = llegadasRaw.stream().mapToInt(MetroInfoPanel::getStopSAE).findFirst().orElse(0);

                    MetroParada parada = new MetroParada(
                            sae,
                            code,
                            entidad.getNombre(),
                            null,
                            null,
                            entidad.getLat(),
                            entidad.getLng(),
                            lineasPorCodigo.getOrDefault(code, List.of())
                    );
                    List<MetroLlegada> llegadas = mapearLlegadas(llegadasRaw, cargaMap);
                    return new MetroParadaResponse(true, parada, llegadas);
                })
                .toList();
    }

    // --- VEHÍCULOS EN TIEMPO REAL Y OCUPACIÓN ---

    public MetroVehiculosResponse getVehiculos(Integer route, Integer direction) {
        MetroVehicleLocationApiDTO[] locations = obtenerVehicleLocationsCacheados();
        Map<String, MetroCargaVehiculoApiDTO> cargaMap = mapaCargaPorServicio();

        List<MetroVehiculo> vehiculos = Arrays.stream(locations)
                .filter(v -> route == null || v.getRoute() == route)
                .filter(v -> direction == null || v.getDirection() == direction)
                .map(v -> {
                    String serviceKey = v.getService() != null ? v.getService().trim() : null;
                    MetroCargaVehiculoApiDTO cargaDto = serviceKey != null ? cargaMap.get(serviceKey) : null;
                    Double lat = v.getCoordinate() != null ? v.getCoordinate().getLat() : null;
                    Double lon = v.getCoordinate() != null ? v.getCoordinate().getLon() : null;

                    return new MetroVehiculo(
                            v.getService(),
                            cargaDto != null ? cargaDto.getVehiculo() : null,
                            v.getRoute(),
                            v.getDirection(),
                            lat,
                            lon,
                            v.getNextStop(),
                            v.getNextStopDescription(),
                            v.getDestinationStop(),
                            v.getDestinationStopDescription(),
                            v.getRemainingMinutes(),
                            v.getOrderStop(),
                            cargaDto != null ? cargaDto.getCarga() : null,
                            cargaDto != null ? cargaDto.getFecha() : null
                    );
                })
                .toList();

        return new MetroVehiculosResponse(true, vehiculos);
    }

    public List<MetroCargaVehiculoApiDTO> getCargaVehiculos(Integer servicio, Integer vehiculo) {
        return obtenerCargaVehiculosCacheada().stream()
                .filter(c -> servicio == null || Objects.equals(c.getServicio(), servicio))
                .filter(c -> vehiculo == null || Objects.equals(c.getVehiculo(), vehiculo))
                .toList();
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}