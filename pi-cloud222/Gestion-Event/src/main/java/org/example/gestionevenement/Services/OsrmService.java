package org.example.gestionevenement.Services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OsrmService implements IOsrm {

    private final RestTemplate rest = new RestTemplate();
    private static final String OSRM = "http://router.project-osrm.org";

    public Map<String, Object> getMatrix(List<double[]> coords, double[] user) {

        StringBuilder sb = new StringBuilder();

        sb.append(user[1]).append(",").append(user[0]);

        for (double[] c : coords) {
            sb.append(";")
                    .append(c[1]).append(",").append(c[0]);
        }

        String url = OSRM + "/table/v1/driving/" + sb +
                "?annotations=duration,distance";

        return rest.getForObject(url, Map.class);
    }

    public JsonNode getRoute(double[] from, double[] to) {

        String url = OSRM + "/route/v1/driving/"
                + from[1] + "," + from[0] + ";"
                + to[1] + "," + to[0]
                + "?overview=full&geometries=geojson";

        return rest.getForObject(url, JsonNode.class);
    }

    public Object snapToRoad(List<double[]> points) {

        String coords = points.stream()
                .map(p -> p[0] + "," + p[1])
                .collect(Collectors.joining(";"));

        String url = "http://router.project-osrm.org/match/v1/driving/"
                + coords
                + "?geometries=geojson&overview=full";

        return rest.getForObject(url, Object.class);
    }

    public JsonNode optimizeTrip(double[] user, List<double[]> coords) {

        String coordStr = user[1] + "," + user[0] + ";"
                + coords.stream()
                .map(c -> c[1] + "," + c[0])
                .collect(Collectors.joining(";"));

        String url = OSRM + "/trip/v1/driving/" + coordStr +
                "?source=first&destination=last" +
                "&roundtrip=false" +
                "&overview=full" +
                "&geometries=geojson" +
                "&steps=true";

        return rest.getForObject(url, JsonNode.class);
    }
}