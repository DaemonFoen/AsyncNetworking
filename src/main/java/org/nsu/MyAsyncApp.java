package org.nsu;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.jsoup.Jsoup;
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
    private final String locale;

    public MyAsyncApp(int connectionTimeout, int requestTimeout, String weatherAPIKey, String hitsAPIKey,
            String placesAPIKey, String locale) {
        this.hitsAPIKey = "key=" + hitsAPIKey;
        this.weatherAPIKey = "appid=" + weatherAPIKey;
        this.placesAPIKey = "apikey=" + placesAPIKey;
        this.locale = locale;
        client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(connectionTimeout)
                .setRequestTimeout(requestTimeout)
                .build());
    }

    public static void main(String[] args) {
        System.out.println("Введите запрос");
        try (MyAsyncApp myAsyncApp = new MyAsyncApp(5000, 20000, "d8c43d67785771c32ee49a48ecf8c2ea"
                , "2d259c4a-082b-45c6-a46a-71e519287333",
                "5ae2e3f221c38a28845f05b6d94ab5fc99f1f1ef326006c5a505de88", "ru")) {
            Scanner scanner = new Scanner(System.in);
            myAsyncApp.findHits(scanner.nextLine())
                    .thenAcceptAsync(hit -> System.out.println(myAsyncApp.findWeatherAndPlaces(hit).join())).join();
        }
    }

    public CompletableFuture<Hit> findHits(String suggestion) {
        return client.prepareGet(
                        "https://graphhopper.com/api/1/geocode?q={suggestion}&limit=10&locale=en&{key}"
                                .replace("{suggestion}", suggestion)
                                .replace("{key}", hitsAPIKey)
                                .replace("{locale}", locale))
                .execute()
                .toCompletableFuture()
                .thenApply(response -> {
                    try {
                        Hits myResponse = objectMapper.readValue(
                                new String(response.getResponseBodyAsBytes(), Charset.defaultCharset()), Hits.class);
                        List<Hit> hits = myResponse.getHits();
                        if (hits.isEmpty()) {
                            System.out.println("No hits?");
                            System.exit(0);
                        }
                        for (int i = 0; i < hits.size(); i++) {
                            System.out.println(
                                    i + 1 + " Название: " + hits.get(i).getName() + " Страна: " + hits.get(i)
                                            .getCountry() + " Город: " + hits.get(i).getCity()
                                            + " lon: " + hits.get(i).getPoint()
                                            .getLng() + " lat: " + hits.get(i).getPoint().getLat());
                        }
                        System.out.println("Выберите место");

                        Scanner scanner = new Scanner(System.in);
                        int idx;
                        while (true) {
                            var str = scanner.next();
                            try {
                                idx = Integer.parseInt(str);
                                if (idx < 1 || idx > hits.size()) {
                                    throw new NumberFormatException();
                                } else {
                                    break;
                                }
                            }catch (NumberFormatException e){
                                System.out.println("Неверный номер места");
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
                        WeatherData weatherData = objectMapper.readValue(
                                new String(response.getResponseBodyAsBytes(), Charset.defaultCharset()),
                                WeatherData.class);
                        return "-".repeat(100) + "\n" +
                                "Температура: " + (Math.round(weatherData.getMain().getTemp() - 273.15)) + " C\n"
                                + "Ощущается как: " + (Math.round(weatherData.getMain().getFeelsLike() - 273.15))
                                + " C\n"
                                + "Облачность: " + weatherData.getClouds().getAll() + "%\n"
                                + "Давление: " + weatherData.getMain().getGrndLevel() * 0.75 + " мм рт. ст.\n"
                                + "Описание: " + weatherData.getWeather()[0].getDescription() + "\n"
                                + "-".repeat(100) + "\n";
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public List<Feature> findPlaces(Hit hit) {
        return client.prepareGet(
                        "http://api.opentripmap.com/0.1/{locale}/places/radius?radius=500&lon={lon}&lat={lat}&{key}"
                                .replace("{lat}", String.valueOf(hit.getPoint().getLat()))
                                .replace("{lon}", String.valueOf(hit.getPoint().getLng()))
                                .replace("{key}", placesAPIKey)
                                .replace("{locale}", locale))
                .execute()
                .toCompletableFuture()
                .thenApplyAsync(response -> {
                    try {
                        return objectMapper.readValue(
                                new String(response.getResponseBodyAsBytes(), Charset.defaultCharset()),
                                FeatureCollection.class);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }).join().getFeatures();
    }

    public CompletableFuture<String> findDescription(Feature feature) {
        return client.prepareGet(
                        ("http://api.opentripmap.com/0.1/{locale}/places/xid/" + feature.getProperties().getXid()
                                + "?" + placesAPIKey)
                                .replace("{locale}", locale))
                .execute()
                .toCompletableFuture().thenApplyAsync(response -> {
                    try {
                        PlaceDescription placeDescription = objectMapper.readValue(
                                new String(response.getResponseBodyAsBytes(), Charset.defaultCharset()),
                                PlaceDescription.class);

                        if (placeDescription.getName() != null && !placeDescription.getName().isEmpty()) {
                            return "Название: " + placeDescription.getName() +" Тип :" + placeDescription.getKinds() + "\n"
                                    + (placeDescription.getInfo() != null ? "Описание: " + Jsoup.parse(placeDescription.getInfo()
                                    .getDescr()).text() + "\n"
                                    : "\n");
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
