package org.nsu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;


@Data
public class Hits {

    private List<Hit> hits;
    private String locale;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hit {

        private Point point;
        private List<Double> extent;
        private String name;
        private String country;
        private String countrycode;
        private String osm_id;
        private String osm_type;
        private String osm_key;
        private String osm_value;
        private String city;
        private String street;
        private String postcode;
        private String housenumber;
        private String house_number;


        @Data
        public static class Point {

            private double lat;
            private double lng;
        }

    }

}



