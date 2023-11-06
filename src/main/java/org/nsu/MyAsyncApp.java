package org.nsu;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.nsu.dto.PlaceDescription;
import org.nsu.dto.FeatureCollection;
import org.nsu.dto.FeatureCollection.Feature;
import org.nsu.dto.Hits;
import org.nsu.dto.Hits.Hit;
import org.nsu.dto.WeatherData;

@Log4j2
public class MyAsyncApp implements AutoCloseable {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AsyncHttpClient client;

    private final String weatherAPIKey;

    private final String hitsAPIKey;

    private final String placesAPIKey;

    public MyAsyncApp(int connectionTimeout, int requestTimeout, String weatherAPIKey, String hitsAPIKey,
            String placesAPIKey) {
        this.hitsAPIKey = "key=" + hitsAPIKey;
        this.weatherAPIKey = "appid=" + weatherAPIKey;
        this.placesAPIKey = "apikey=" + placesAPIKey;
        client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(connectionTimeout)
                .setRequestTimeout(requestTimeout)
                .build());
    }

    public static void main(String[] args) {
        System.out.println("Enter a request");
        try (MyAsyncApp myAsyncApp = new MyAsyncApp(5000, 20000, "d8c43d67785771c32ee49a48ecf8c2ea"
                , "2d259c4a-082b-45c6-a46a-71e519287333",
                "5ae2e3f221c38a28845f05b6d94ab5fc99f1f1ef326006c5a505de88")) {
            Scanner scanner = new Scanner(System.in);
            myAsyncApp.findHits(scanner.nextLine())
                    .thenAcceptAsync(hit -> System.out.println(myAsyncApp.findWeatherAndPlaces(hit).join())).join();
        }
    }

    public CompletableFuture<Hit> findHits(String suggestion) {
        return client.prepareGet(
                        "https://graphhopper.com/api/1/geocode?q={suggestion}&locale=en&{key}"
                                .replace("{suggestion}", suggestion)
                                .replace("{key}", hitsAPIKey))
                .execute()
                .toCompletableFuture()
                .thenApply(response -> {
                    try {
                        Hits myResponse = objectMapper.readValue(response.getResponseBody(),
                                Hits.class);
                        List<Hit> hits = myResponse.getHits();

                        for (int i = 0; i < hits.size(); i++) {
                            System.out.println(
                                    i + 1 + " Name: " + hits.get(i).getName() + " lon = " + hits.get(i).getPoint()
                                            .getLng() + " lat = " + hits.get(i).getPoint().getLat());
                        }
                        System.out.println("Select place");

                        Scanner scanner = new Scanner(System.in);
                        int idx;
                        while (true) {
                            idx = scanner.nextInt();
                            if (idx < 1 || idx >= hits.size()) {
                                System.out.println("Wrong place number");
                            } else {
                                break;
                            }
                        }
                        return hits.get(idx - 1);
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
    }

    public CompletableFuture<String> findWeather(Hit hit) {
        return client.prepareGet(
                        "https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&{key}"
                                .replace("{lat}", String.valueOf(hit.getPoint().getLat()))
                                .replace("{lon}", String.valueOf(hit.getPoint().getLng()))
                                .replace("{key}", weatherAPIKey))
                .execute()
                .toCompletableFuture()
                .thenApplyAsync(response -> {
                    try {
                        WeatherData weatherData = objectMapper.readValue(response.getResponseBody(), WeatherData.class);
                        return "-".repeat(100) + "\n" +
                                "Temperature: " + (Math.round(weatherData.getMain().getTemp() - 273.15)) + " C\n"
                                + "Feels like: " + (Math.round(weatherData.getMain().getFeelsLike() - 273.15)) + " C\n"
                                + "Cloudiness: " + weatherData.getClouds().getAll() + "%\n"
                                + "Weather Description: " + weatherData.getWeather()[0].getDescription() + "\n"
                                + "-".repeat(100) + "\n";
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public List<Feature> findPlaces(Hit hit) {
        return client.prepareGet(
                        "http://api.opentripmap.com/0.1/ru/places/radius?radius=150&lon={lon}&lat={lat}&{key}"
                                .replace("{lat}", String.valueOf(hit.getPoint().getLat()))
                                .replace("{lon}", String.valueOf(hit.getPoint().getLng()))
                                .replace("{key}",placesAPIKey ))
                .execute()
                .toCompletableFuture()
                .thenApplyAsync(response -> {
                    try {
                        return objectMapper.readValue(response.getResponseBody(), FeatureCollection.class);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }).join().getFeatures();
    }

    public CompletableFuture<String> findDescription(Feature feature) {
        return client.prepareGet("http://api.opentripmap.com/0.1/ru/places/xid/" + feature.getProperties().getXid()
                        + "?" + placesAPIKey)
                .execute()
                .toCompletableFuture().thenApplyAsync(response -> {
                    try {
                        PlaceDescription placeDescription = objectMapper.readValue(response.getResponseBody(),
                                PlaceDescription.class);
                        if (placeDescription.getName() != null && !placeDescription.getName().isEmpty()) {
                            return "Name: " + placeDescription.getName() + "\n"
                                    + (placeDescription.getInfo() != null ? "Description: " + placeDescription.getInfo()
                                    .getDescr()
                                    : "");
                        } else {
                            return "";
                        }
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public CompletableFuture<String> findWeatherAndPlaces(Hit hit) {
        return findWeather(hit).thenCombineAsync(findPlaces(hit).stream().map(this::findDescription)
                        .reduce(CompletableFuture.completedFuture(""),
                                (accum, it) -> accum.thenCombine(it, (res1, res2) -> res1 + res2)),
                (res1, res2) -> res1 + res2);
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }
}
