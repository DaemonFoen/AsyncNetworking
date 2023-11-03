package org.nsu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureCollection {
    private String type;
    private List<Feature> features;

    @Data
    public static class Feature {
        private String type;
        private String id;
        private Geometry geometry;
        private Properties properties;

        @Data
        public static class Geometry {
            private String type;
            private double[] coordinates;
        }

        @Data
        public static class Properties {
            private String xid;
            private String name;
            private double dist;
            private int rate;
            private String osm;
            private String kinds;

            private String wikidata;
        }
    }
}






