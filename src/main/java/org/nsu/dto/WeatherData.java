package org.nsu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class WeatherData {
    private Coord coord;
    private Weather[] weather;
    private String base;
    private Main main;
    private int visibility;
    private Wind wind;
    private Clouds clouds;
    private long dt;
    private Sys sys;
    private int timezone;
    private int id;
    private String name;
    private int cod;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Coord {
        private double lon;
        private double lat;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Main {
        private double temp;
        @JsonProperty("feels_like")
        private double feelsLike;
        @JsonProperty("temp_min")
        private double tempMin;
        @JsonProperty("temp_max")
        private double tempMax;
        private int pressure;
        private int humidity;
        @JsonProperty("sea_level")
        private int seaLevel;
        @JsonProperty("grnd_level")
        private int grndLevel;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Wind {
        private double speed;
        private int deg;
        private double gust;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Clouds {
        private int all;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Sys {
        private String country;
        private long sunrise;
        private long sunset;
    }
}
