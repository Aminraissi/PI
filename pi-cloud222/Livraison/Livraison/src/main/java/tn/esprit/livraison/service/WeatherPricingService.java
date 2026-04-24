package tn.esprit.livraison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherPricingService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.weather.api-base:https://api.open-meteo.com/v1/forecast}")
    private String weatherApiBase;

    @Value("${app.weather.timeout-ms:4000}")
    private long weatherTimeoutMs;

    @Value("${app.weather.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;

    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public WeatherPricingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    }

    public WeatherSnapshot fetchCurrentWeather(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            return fallback();
        }

        String key = cacheKey(lat, lng);
        CachedWeather cached = cache.get(key);
        if (cached != null && Duration.between(cached.at(), LocalDateTime.now()).getSeconds() < cacheTtlSeconds) {
            return cached.snapshot();
        }

        try {
            String encodedLat = URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8);
            String encodedLng = URLEncoder.encode(String.valueOf(lng), StandardCharsets.UTF_8);
            String url = weatherApiBase
                    + "?latitude=" + encodedLat
                    + "&longitude=" + encodedLng
                    + "&current=temperature_2m,precipitation,rain,showers,snowfall,wind_speed_10m,weather_code"
                    + "&timezone=auto";

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(weatherTimeoutMs, 1000)))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallback();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.path("current");
            if (current.isMissingNode() || current.isNull()) {
                return fallback();
            }

            double wind = current.path("wind_speed_10m").asDouble(0.0);
            double rain = current.path("precipitation").asDouble(0.0);
            double rainOnly = current.path("rain").asDouble(0.0);
            double showers = current.path("showers").asDouble(0.0);
            double snowfall = current.path("snowfall").asDouble(0.0);
            double effectivePrecipitation = Math.max(rain, Math.max(rainOnly + showers, snowfall));
            int code = current.path("weather_code").asInt(0);
            double surchargePercent = computeSurchargePercent(wind, effectivePrecipitation, code);
            String condition = weatherCodeToCondition(code, effectivePrecipitation);
            if ("clear".equals(condition) && wind >= 35 && surchargePercent > 0) {
                condition = "windy";
            }

            WeatherSnapshot snapshot = new WeatherSnapshot(
                    wind,
                    effectivePrecipitation,
                    condition,
                    surchargePercent,
                    LocalDateTime.now(),
                    false
            );
            cache.put(key, new CachedWeather(LocalDateTime.now(), snapshot));
            return snapshot;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return fallback();
        }
    }

    public List<DailyForecast> fetchDailyForecast(double lat, double lng, LocalDate startDate, LocalDate endDate) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || startDate == null || endDate == null) {
            return Collections.emptyList();
        }
        if (endDate.isBefore(startDate)) {
            LocalDate swap = startDate;
            startDate = endDate;
            endDate = swap;
        }
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            startDate = today;
        }
        long span = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (span > 15) {
            endDate = startDate.plusDays(15);
        }

        try {
            String encodedLat = URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8);
            String encodedLng = URLEncoder.encode(String.valueOf(lng), StandardCharsets.UTF_8);
            String url = weatherApiBase
                    + "?latitude=" + encodedLat
                    + "&longitude=" + encodedLng
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,rain_sum,snowfall_sum,wind_speed_10m_max,weather_code"
                    + "&start_date=" + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    + "&end_date=" + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    + "&timezone=auto";

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(weatherTimeoutMs, 1000)))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode daily = root.path("daily");
            if (daily.isMissingNode() || daily.isNull()) {
                return Collections.emptyList();
            }

            JsonNode times = daily.path("time");
            JsonNode tMax = daily.path("temperature_2m_max");
            JsonNode tMin = daily.path("temperature_2m_min");
            JsonNode precip = daily.path("precipitation_sum");
            JsonNode rain = daily.path("rain_sum");
            JsonNode snow = daily.path("snowfall_sum");
            JsonNode wind = daily.path("wind_speed_10m_max");
            JsonNode codes = daily.path("weather_code");

            List<DailyForecast> result = new ArrayList<>();
            int size = times.isArray() ? times.size() : 0;
            for (int i = 0; i < size; i++) {
                String dateStr = times.get(i).asText("");
                if (dateStr.isBlank()) continue;
                double windKmh = wind.isArray() && i < wind.size() ? wind.get(i).asDouble(0.0) : 0.0;
                double precipitationMm = precip.isArray() && i < precip.size() ? precip.get(i).asDouble(0.0) : 0.0;
                double rainMm = rain.isArray() && i < rain.size() ? rain.get(i).asDouble(0.0) : 0.0;
                double snowMm = snow.isArray() && i < snow.size() ? snow.get(i).asDouble(0.0) : 0.0;
                double effectivePrecipitation = Math.max(precipitationMm, Math.max(rainMm, snowMm));
                int code = codes.isArray() && i < codes.size() ? codes.get(i).asInt(0) : 0;
                double surcharge = computeSurchargePercent(windKmh, effectivePrecipitation, code);
                String condition = weatherCodeToCondition(code, effectivePrecipitation);
                if ("clear".equals(condition) && windKmh >= 35 && surcharge > 0) {
                    condition = "windy";
                }
                double tempMax = tMax.isArray() && i < tMax.size() ? tMax.get(i).asDouble(Double.NaN) : Double.NaN;
                double tempMin = tMin.isArray() && i < tMin.size() ? tMin.get(i).asDouble(Double.NaN) : Double.NaN;

                result.add(new DailyForecast(
                        LocalDate.parse(dateStr),
                        tempMin,
                        tempMax,
                        windKmh,
                        effectivePrecipitation,
                        condition,
                        surcharge
                ));
            }
            return result;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Collections.emptyList();
        }
    }

    public WeatherSnapshot fallback() {
        return new WeatherSnapshot(0.0, 0.0, "unknown", 0.0, LocalDateTime.now(), true);
    }

    private double computeSurchargePercent(double windSpeedKmh, double precipitationMm, int weatherCode) {
        boolean adverseWind = windSpeedKmh >= 35;
        boolean adverseRain = precipitationMm >= 0.2 || isRainLikeCode(weatherCode) || isSnowLikeCode(weatherCode);
        boolean storm = isStormCode(weatherCode);
        if (!adverseWind && !adverseRain && !storm) {
            return 0.0;
        }

        double surcharge = 0.0;

        if (windSpeedKmh >= 55) surcharge += 0.12;
        else if (windSpeedKmh >= 40) surcharge += 0.08;
        else if (windSpeedKmh >= 35) surcharge += 0.04;

        if (isStormCode(weatherCode) || precipitationMm >= 6) surcharge += 0.18;
        else if (isRainLikeCode(weatherCode) || isSnowLikeCode(weatherCode) || precipitationMm >= 2) surcharge += 0.12;
        else if (precipitationMm >= 0.2) surcharge += 0.06;

        return Math.min(0.30, surcharge);
    }

    private boolean isRainLikeCode(int weatherCode) {
        return (weatherCode >= 51 && weatherCode <= 67) || (weatherCode >= 80 && weatherCode <= 82);
    }

    private boolean isSnowLikeCode(int weatherCode) {
        return (weatherCode >= 71 && weatherCode <= 77) || weatherCode == 85 || weatherCode == 86;
    }

    private boolean isStormCode(int weatherCode) {
        return weatherCode == 95 || weatherCode == 96 || weatherCode == 99;
    }

    private String weatherCodeToCondition(int weatherCode, double precipitationMm) {
        if (isStormCode(weatherCode)) return "storm";
        if (isSnowLikeCode(weatherCode)) return "snow";
        if (weatherCode >= 61 && weatherCode <= 67) return "rain";
        if (weatherCode >= 51 && weatherCode <= 57) return "drizzle";
        if (weatherCode >= 80 && weatherCode <= 82) return "rain";
        if (weatherCode == 85 || weatherCode == 86) return "snow";
        if (precipitationMm >= 2) return "rain";
        if (precipitationMm >= 0.2) return "drizzle";
        if (weatherCode == 1 || weatherCode == 2 || weatherCode == 3) return "cloudy";
        if (weatherCode >= 45 && weatherCode <= 48) return "fog";
        return "clear";
    }

    private String cacheKey(double lat, double lng) {
        return String.format("%.3f,%.3f", lat, lng);
    }

    private record CachedWeather(LocalDateTime at, WeatherSnapshot snapshot) {}

    public record DailyForecast(
            LocalDate date,
            double temperatureMin,
            double temperatureMax,
            double windSpeedKmh,
            double precipitationMm,
            String condition,
            double surchargePercent
    ) {}

    public record WeatherSnapshot(
            double windSpeedKmh,
            double precipitationMm,
            String condition,
            double surchargePercent,
            LocalDateTime fetchedAt,
            boolean fallback
    ) {}
}




