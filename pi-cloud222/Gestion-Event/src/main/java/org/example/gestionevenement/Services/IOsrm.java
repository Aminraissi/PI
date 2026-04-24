package org.example.gestionevenement.Services;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface IOsrm {
     JsonNode getRoute(double[] from, double[] to);
     Object snapToRoad(List<double[]> points);
     Map<String, Object> getMatrix(List<double[]> coords, double[] user);
     public JsonNode optimizeTrip(double[] user, List<double[]> coords);
}
