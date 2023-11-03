package org.nsu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceDescription {

    private String xid;
    private String name;
    private String osm;
    private String wikidata;
    private String rate;
    private String image;
    private String wikipedia;
    private String kinds;
    private Sources sources;
    private Bbox bbox;
    private Point point;
    private String otm;
    private Info info;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Sources{
        private String geometry;
        private String[] attributes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Bbox {
        private double lat_max;
        private double lat_min;
        private double lon_max;
        private double lon_min;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Point {
        private double lon;
        private double lat;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Info {
        private String descr;
        private String image;
        private int img_width;
    }
}
