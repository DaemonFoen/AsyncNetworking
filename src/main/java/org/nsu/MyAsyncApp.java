package org.nsu;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.nsu.dto.PlaceDescription;
import org.nsu.dto.FeatureCollection;
import org.nsu.dto.FeatureCollection.Feature;
import org.nsu.dto.Hits;
import org.nsu.dto.Hits.Hit;
import org.nsu.dto.WeatherData;


public class MyAsyncApp {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AsyncHttpClient client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
            .setConnectTimeout(5000)
            .setRequestTimeout(10000)
            .build());

    public static void main(String[] args) {
        findHits(args[0]).thenAcceptAsync(hit -> System.out.println(findWeatherAndPlaces(hit).join())).join();
    }

    public static CompletableFuture<Hit> findHits(String suggestion) {
        return client.prepareGet(
                        "https://graphhopper.com/api/1/geocode?q={suggestion}&locale=en&key=2d259c4a-082b-45c6-a46a-71e519287333".replace(
                                "{suggestion}", suggestion))
                .execute()
                .toCompletableFuture()
                .thenApply(response -> {
                    try {
                        Hits myResponse = objectMapper.readValue(response.getResponseBody(),
                                Hits.class);
                        List<Hit> hits = myResponse.getHits();

                        for (int i = 0; i < hits.size(); i++) {
                            System.out.println(i + 1 + " Name: " + hits.get(i).getName());
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

    public static CompletableFuture<String> findWeather(Hit hit) {
        return client.prepareGet(
                        "https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid=d8c43d67785771c32ee49a48ecf8c2ea"
                                .replace("{lat}", String.valueOf(hit.getPoint().getLat()))
                                .replace("{lon}", String.valueOf(hit.getPoint().getLng())))
                .execute()
                .toCompletableFuture()
                .thenApplyAsync(response -> {
                    try {
                        System.out.println("weather found");
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

    public static List<Feature> findPlaces(Hit hit) {
        return client.prepareGet(
                        "http://api.opentripmap.com/0.1/en/places/radius?radius=100&lon={lon}&lat={lat}&apikey=5ae2e3f221c38a28845f05b6d94ab5fc99f1f1ef326006c5a505de88"
                                .replace("{lat}", String.valueOf(hit.getPoint().getLat()))
                                .replace("{lon}", String.valueOf(hit.getPoint().getLng())))
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

    public static CompletableFuture<String> findDescription(Feature feature) {
        return client.prepareGet("http://api.opentripmap.com/0.1/ru/places/xid/" + feature.getProperties().getXid()
                        + "?apikey=5ae2e3f221c38a28845f05b6d94ab5fc99f1f1ef326006c5a505de88")
                .execute()
                .toCompletableFuture().thenApplyAsync(response -> {
                    try {
                        PlaceDescription placeDescription = objectMapper.readValue(response.getResponseBody(), PlaceDescription.class);
                        if (placeDescription.getName() != null){
                            return "Name: " + placeDescription.getName() + "\n"
                                    + (placeDescription.getInfo() != null ? "Description: " + placeDescription.getInfo().getDescr()
                                    : "");
                        }else {
                            return "";
                        }
                    } catch (Throwable e){
                        throw new RuntimeException(e);
                    }
                });
    }

    public static CompletableFuture<String> findWeatherAndPlaces(Hit hit) {
        //TODO Reduce for CompletableFuture
        return findWeather(hit).thenCombineAsync(findPlaces(hit).stream().map(MyAsyncApp::findDescription)
                        .reduce(CompletableFuture.completedFuture(""),
                                (accum, it) -> accum.thenCombine(it, (res1, res2) -> res1 + res2)),
                (res1, res2) -> res1 + res2);
    }


}

//
//    private static void showFinalResultToUser() {
//    }
//
//    private static void showWeatherToUser(Weather weather) {
//    }
//
//    private static void showLocationsToUser(List<Location> locations) {
//    }
//

//
//    public CompletableFuture<Weather> getWeather(Location location) {
//        // ??????????? HTTP-?????? ??? ????????? ??????
//        // ?????????? ?????? ? ??????
//    }
//
//    public CompletableFuture<List<InterestingPlace>> getInterestingPlaces(Location location) {
//        // ??????????? HTTP-?????? ??? ????????? ?????????? ????
//        // ?????????? ?????? ?????????? ????
//    }
//
//    public CompletableFuture<String> getDescription(InterestingPlace place) {
//        // ??????????? HTTP-?????? ??? ????????? ???????? ?????
//        // ?????????? ????????
//    }
//
//}
//    public static void main(String[] args) {
//        String userInput = "??????? ??????"; // ????? ????? ???????????? ???? ????????????
//        Executor executor = Executors.newFixedThreadPool(4); // ??? ??????? ??? ??????????? ?????
//
//        CompletableFuture<List<Location>> locationsFuture = findLocationsAsync(userInput, executor);
//
//        locationsFuture.thenAccept(locations -> {
//            Location selectedLocation = selectLocation(locations);
//            CompletableFuture<Weather> weatherFuture = findWeatherAsync(selectedLocation, executor);
//            CompletableFuture<List<Place>> placesFuture = findInterestingPlacesAsync(selectedLocation, executor);
//
//            CompletableFuture<List<Description>> descriptionsFuture = placesFuture.thenComposeAsync(places -> {
//                List<CompletableFuture<Description>> descriptionFutures = places.stream()
//                        .map(place -> findDescriptionAsync(place, executor))
//                        .collect(Collectors.toList());
//
//                return CompletableFuture.allOf(descriptionFutures.toArray(new CompletableFuture[0]))
//                        .thenApply(ignoredVoid -> descriptionFutures.stream()
//                                .map(CompletableFuture::join)
//                                .collect(Collectors.toList()));
//            });
//
//            List<Description> descriptions = descriptionsFuture.join();
//            displayInformation(selectedLocation, descriptions, weatherFuture.join());
//        });
//    }
//
//    private static CompletableFuture<List<Location>> findLocationsAsync(String userInput, Executor executor) {
//        CompletableFuture<List<Location>> future = CompletableFuture.supplyAsync(() -> {
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url("https://api.example.com/locations?q=" + userInput)
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (response.isSuccessful()) {
//                    String responseBody = response.body().string();
//                    JSONArray jsonLocations = new JSONArray(responseBody);
//
//                    List<Location> locations = new LinkedList<>();
//                    for (int i = 0; i < jsonLocations.length(); i++) {
//                        JSONObject jsonLocation = jsonLocations.getJSONObject(i);
//                        Location location = new Location(jsonLocation.getString("name"), jsonLocation.getDouble("lat"), jsonLocation.getDouble("lon"));
//                        locations.add(location);
//                    }
//
//                    return locations;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            return Collections.emptyList();
//        }, executor);
//
//        return future;
//    }
//
//    private static CompletableFuture<Weather> findWeatherAsync(Location location, Executor executor) {
//        CompletableFuture<Weather> future = CompletableFuture.supplyAsync(() -> {
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url("https://api.example.com/weather?lat=" + location.getLatitude() + "&lon=" + location.getLongitude())
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (response.isSuccessful()) {
//                    String responseBody = response.body().string();
//                    JSONObject jsonWeather = new JSONObject(responseBody);
//                    double temperature = jsonWeather.getDouble("temperature");
//                    String conditions = jsonWeather.getString("conditions");
//                    return new Weather(temperature, conditions);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            return new Weather(0, "Unknown");
//        }, executor);
//
//        return future;
//    }
//
//    private static CompletableFuture<List<Place>> findInterestingPlacesAsync(Location location, Executor executor) {
//        CompletableFuture<List<Place>> future = CompletableFuture.supplyAsync(() -> {
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url("https://api.example.com/places?lat=" + location.getLatitude() + "&lon=" + location.getLongitude())
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (response.isSuccessful()) {
//                    String responseBody = response.body().string();
//                    JSONArray jsonPlaces = new JSONArray(responseBody);
//
//                    List<Place> places = new LinkedList<>();
//                    for (int i = 0; i < jsonPlaces.length(); i++) {
//                        JSONObject jsonPlace = jsonPlaces.getJSONObject(i);
//                        Place place = new Place(jsonPlace.getString("name"), jsonPlace.getString("id"));
//                        places.add(place);
//                    }
//
//                    return places;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            return Collections.emptyList();
//        }, executor);
//
//        return future;
//    }
//
//    private static CompletableFuture<Description> findDescriptionAsync(Place place, Executor executor) {
//        CompletableFuture<Description> future = CompletableFuture.supplyAsync(() -> {
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url("https://api.example.com/description?id=" + place.getId())
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (response.isSuccessful()) {
//                    String responseBody = response.body().string();
//                    JSONObject jsonDescription = new JSONObject(responseBody);
//                    String text = jsonDescription.getString("text");
//                    return new Description(text);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            return new Description("No description available");
//        }, executor);
//
//        return future;
//    }
//
//    private static void displayInformation(Location location, List<Description> descriptions, Weather weather) {
//        System.out.println("Location: " + location.getName());
//        System.out.println("Weather: " + weather.getTemperature() + "?C, " + weather.getConditions());
//        System.out.println("Interesting Places:");
//        for (Description description : descriptions) {
//            System.out.println(description.getText());
//        }
//    }
//
//    private static Location selectLocation(List<Location> locations) {
//        // ? ???? ?????? ????? ??????????? ????? ??????? ?????????????
//        return locations.get(0);
//    }

