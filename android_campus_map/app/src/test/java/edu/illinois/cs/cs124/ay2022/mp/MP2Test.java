package edu.illinois.cs.cs124.ay2022.mp;

import static com.google.common.truth.Truth.assertWithMessage;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.MAPPER;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.PLACES_COUNT;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.compareGeopoints;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.configureLogging;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.getMarkers;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.pause;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.randomGeoPointInMap;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.randomGeoPointOnMap;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.startActivity;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.illinois.cs.cs124.ay2022.mp.activities.AddPlaceActivity;
import edu.illinois.cs.cs124.ay2022.mp.activities.MainActivity;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow;
import edu.illinois.cs.cs124.ay2022.mp.network.Client;
import edu.illinois.cs.cs124.ay2022.mp.network.Server;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowIntent;

@RunWith(Enclosed.class)
@SuppressWarnings("SpellCheckingInspection")
public final class MP2Test {
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class UnitTests {

    // Create an HTTP client to test the server with
    private static final OkHttpClient httpClient = new OkHttpClient();

    static {
      // Start the API server
      Server.start();
    }

    @Before
    public void resetServer() {
      Server.reset();
    }

    private List<Place> serverGetPlaces() throws IOException {
      Request placesRequest =
          new Request.Builder().url(FavoritePlacesApplication.SERVER_URL + "/places/").build();
      Response placesResponse = httpClient.newCall(placesRequest).execute();

      assertWithMessage("Request should have succeeded")
          .that(placesResponse.isSuccessful())
          .isTrue();

      ResponseBody body = placesResponse.body();
      assertWithMessage("Response body should not be null").that(body).isNotNull();

      return MAPPER.readValue(body.string(), new TypeReference<>() {});
    }

    private void testServerFavoritePlacePostHelper(
        final String id,
        final String name,
        final Double latitude,
        final Double longitude,
        final String description,
        final int expectedResponse,
        final int expectedCount)
        throws IOException {
      String newPlaceJSON = Helpers.makePlaceJSON(id, name, latitude, longitude, description);
      Request favoritePlaceRequest =
          new Request.Builder()
              .url(FavoritePlacesApplication.SERVER_URL + "/favoriteplace/")
              .post(
                  RequestBody.create(
                      newPlaceJSON, MediaType.parse("application/json; charset=utf-8")))
              .build();
      Response favoritePlaceResponse = httpClient.newCall(favoritePlaceRequest).execute();
      String message = "Request should not have succeeded";
      if (expectedResponse == 200) {
        message = "Request should have succeeded";
      }
      assertWithMessage(message).that(favoritePlaceResponse.code()).isEqualTo(expectedResponse);

      List<Place> newPlaces = serverGetPlaces();
      assertWithMessage("Wrong count of places after add").that(newPlaces).hasSize(expectedCount);
      Optional<Place> newPlace = newPlaces.stream().filter(p -> p.getId().equals(id)).findFirst();
      if (expectedResponse == 200) {
        assertWithMessage("Should have added the place").that(newPlace.isPresent()).isTrue();
      } else {
        assertWithMessage("Should not have added the place").that(newPlace.isPresent()).isFalse();
      }
      if (expectedResponse != 200) {
        return;
      }
      Place place = newPlace.get();
      assertWithMessage("Wrong name on newly-added place").that(place.getName()).isEqualTo(name);
      assertWithMessage("Wrong latitude on newly-added place")
          .that(place.getLatitude())
          .isEqualTo(latitude);
      assertWithMessage("Wrong longitude on newly-added place")
          .that(place.getLongitude())
          .isEqualTo(longitude);
      assertWithMessage("Wrong description on newly-added place")
          .that(place.getDescription())
          .isEqualTo(description);
    }

    @Graded(points = 20, friendlyName = "Test Server Add Place POST")
    @Test(timeout = 4000L)
    public void test0_ServerFavoritePlacePost() throws IOException {
      List<Place> allPlaces = serverGetPlaces();
      assertWithMessage("Wrong initial number of places")
          .that(allPlaces.size())
          .isEqualTo(PLACES_COUNT);

      // Test invalid requests
      Request invalidRequest1 =
          new Request.Builder()
              .url(FavoritePlacesApplication.SERVER_URL + "/favoriteplace/")
              .post(RequestBody.create("dogs", MediaType.parse("application/json; charset=utf-8")))
              .build();
      Response invalidResponse1 = httpClient.newCall(invalidRequest1).execute();
      assertWithMessage("Request should not have succeeded")
          .that(invalidResponse1.code())
          .isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);

      Request invalidRequest2 =
          new Request.Builder()
              .url(FavoritePlacesApplication.SERVER_URL + "/favoriteplace/")
              .post(
                  RequestBody.create(
                      "{\"cats\": \"true\"}", MediaType.parse("application/json; charset=utf-8")))
              .build();
      Response invalidResponse2 = httpClient.newCall(invalidRequest2).execute();
      assertWithMessage("Request should not have succeeded")
          .that(invalidResponse2.code())
          .isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);

      // Test bad requests
      testServerFavoritePlacePostHelper(
          null,
          "Xyz Challen",
          88.8,
          -88.8,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          "meow",
          "Xyz Challen",
          88.8,
          -88.8,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          null,
          88.8,
          -88.8,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          "Xyz Challen",
          null,
          -88.8,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          "Xyz Challen",
          88.8,
          null,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          "Xyz Challen",
          88.8,
          -88.8,
          null,
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);

      testServerFavoritePlacePostHelper(
          "",
          "Xyz Challen",
          88.8,
          -88.8,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          "",
          88.8,
          -88.8,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          "Xyz Challen",
          88.8,
          -88.8,
          "",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          "Xyz Challen",
          88.8,
          -180.1,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);
      testServerFavoritePlacePostHelper(
          UUID.randomUUID().toString(),
          "Xyz Challen",
          90.1,
          -179.9,
          "Cat tent",
          HttpURLConnection.HTTP_BAD_REQUEST,
          PLACES_COUNT);

      // Test OK requests
      String gracieUUID = UUID.randomUUID().toString();
      String xyzUUID = UUID.randomUUID().toString();

      testServerFavoritePlacePostHelper(
          gracieUUID,
          "Gracie Challen",
          88.8,
          -88.8,
          "Dog Park",
          HttpURLConnection.HTTP_OK,
          PLACES_COUNT + 1);
      testServerFavoritePlacePostHelper(
          gracieUUID,
          "Gracie Challen",
          88.8,
          -88.8,
          "Banjo's House",
          HttpURLConnection.HTTP_OK,
          PLACES_COUNT + 1);
      testServerFavoritePlacePostHelper(
          xyzUUID,
          "Xyz Challen",
          9.9,
          -9.9,
          "Under the bed",
          HttpURLConnection.HTTP_OK,
          PLACES_COUNT + 2);
      testServerFavoritePlacePostHelper(
          xyzUUID,
          "Xyz Challen",
          9.9,
          -9.9,
          "On your desk",
          HttpURLConnection.HTTP_OK,
          PLACES_COUNT + 2);

      // Make sure 0.0 latitude and longitude work
      testServerFavoritePlacePostHelper(
          xyzUUID,
          "Xyz Challen",
          0.0,
          -9.9,
          "On your desk",
          HttpURLConnection.HTTP_OK,
          PLACES_COUNT + 2);
      testServerFavoritePlacePostHelper(
          xyzUUID,
          "Xyz Challen",
          -9.9,
          0.0,
          "On your desk",
          HttpURLConnection.HTTP_OK,
          PLACES_COUNT + 2);

      // Random testing stage
      Server.reset();
      assertWithMessage("Wrong number of places")
          .that(serverGetPlaces().size())
          .isEqualTo(PLACES_COUNT);

      Random random = new Random(124);
      Set<String> availableUUIDs = new HashSet<>();
      Set<String> usedUUIDs = new HashSet<>();
      for (int i = 0; i < 32; i++) {
        if (availableUUIDs.isEmpty() || random.nextBoolean()) {
          availableUUIDs.add(UUID.randomUUID().toString());
        }
        String currentUUID =
            availableUUIDs.toArray(new String[] {})[random.nextInt(availableUUIDs.size())];
        usedUUIDs.add(currentUUID);
        String randomName = "" + random.nextInt();
        String randomDescription = "" + random.nextInt();
        double randomLatitude = 90.0 - (180.0 * random.nextDouble());
        double randomLongitude = 180.0 - (360.0 * random.nextDouble());
        testServerFavoritePlacePostHelper(
            currentUUID,
            randomName,
            randomLatitude,
            randomLongitude,
            randomDescription,
            HttpURLConnection.HTTP_OK,
            PLACES_COUNT + usedUUIDs.size());
      }
    }
  }

  // Integration tests that require simulating the entire app
  @RunWith(AndroidJUnit4.class)
  @LooperMode(LooperMode.Mode.PAUSED)
  @Config(qualifiers = "w1080dp-h2088dp")
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class IntegrationTests {
    // Establish a separate API client for testing
    private static final Client client = Client.start();

    // Static blocks run when the static class is created, very much like a constructor
    static {
      // Set up logging so that you can see log output during testing
      configureLogging();
    }

    @Before
    public void resetServer() {
      Server.reset();
    }

    // After each test make sure the client connected successfully
    @After
    public void checkClient() {
      assertWithMessage("Client should be connected").that(client.getConnected()).isTrue();
    }

    private List<Place> clientGetPlaces() throws InterruptedException, ExecutionException {
      // A CompletableFuture allows us to wait for the result of an asynchronous call
      CompletableFuture<ResultMightThrow<List<Place>>> completableFuture =
          new CompletableFuture<>();

      // When getPlaces returns, it causes the CompletableFuture to complete
      client.getPlaces(completableFuture::complete);
      // Wait for the CompletableFuture to complete
      ResultMightThrow<List<Place>> result = null;
      try {
        result = completableFuture.get(1, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        fail("GET request did not complete in 1 second");
      }

      assertWithMessage("getPlaces threw an exception").that(result.getException()).isNull();
      List<Place> places = result.getResult();

      // The List<Places> should not be null, which is returned by getPlaces when something
      // went wrong
      assertWithMessage("Request failed").that(places).isNotNull();

      return places;
    }

    private void clientPostPlace(final Place place)
        throws ExecutionException, InterruptedException {
      // A CompletableFuture allows us to wait for the result of an asynchronous call
      CompletableFuture<ResultMightThrow<Boolean>> completableFuture = new CompletableFuture<>();

      // When getPlaces returns, it causes the CompletableFuture to complete
      client.postFavoritePlace(place, completableFuture::complete);
      // Wait for the CompletableFuture to complete
      ResultMightThrow<Boolean> result = null;
      try {
        result = completableFuture.get(1, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        fail("POST request did not complete in 1 second");
      }

      assertWithMessage("postFavoritePlace threw an exception")
          .that(result.getException())
          .isNull();
      assertWithMessage("Request failed").that(result.getResult()).isTrue();
    }

    @Graded(points = 20, friendlyName = "Test Client Add Place POST")
    @Test(timeout = 10000L)
    public void test1_ClientFavoritePlacePost()
        throws ExecutionException, InterruptedException, JsonProcessingException {
      List<Place> allPlaces = clientGetPlaces();
      assertWithMessage("Wrong initial number of places").that(allPlaces).hasSize(PLACES_COUNT);

      Place newPlace =
          Helpers.makePlaceFromJson(
              UUID.randomUUID().toString(), "namename", 88.8, -88.8, "descriptiondescription");
      clientPostPlace(newPlace);

      allPlaces = clientGetPlaces();
      assertWithMessage("Wrong number of places after POST")
          .that(allPlaces)
          .hasSize(PLACES_COUNT + 1);
    }

    @Graded(points = 20, friendlyName = "Test Long Press Launches Activity")
    @Test(timeout = 30000L)
    public void test2_LongPressLaunchesActivity() {
      Random random = new Random(124);
      startActivity()
          .onActivity(
              activity -> {
                MapView mapView = activity.findViewById(R.id.map);
                ClickableMapView clickableMapView = new ClickableMapView(mapView);

                for (int i = 0; i < 8; i++) {
                  GeoPoint randomCenter = randomGeoPointOnMap(random);
                  mapView.getController().setCenter(randomCenter);
                  mapView.getController().setZoom(19.0);
                  clickableMapView.update();

                  GeoPoint randomClick = randomGeoPointInMap(random, mapView);
                  clickableMapView.longPress(randomClick);
                  pause();

                  // Check to make sure that the right Intent is launched
                  Intent started = shadowOf(activity).getNextStartedActivity();
                  assertWithMessage("Didn't start activity").that(started).isNotNull();
                  ShadowIntent shadowIntent = shadowOf(started);
                  assertWithMessage("Started wrong activity")
                      .that(shadowIntent.getIntentClass().getSimpleName())
                      .isEqualTo("AddPlaceActivity");

                  String latitude = started.getStringExtra("latitude");
                  assertWithMessage("Didn't launch activity properly: latitude null")
                      .that(latitude)
                      .isNotNull();
                  String longitude = started.getStringExtra("longitude");
                  assertWithMessage("Didn't launch activity properly: longitude null")
                      .that(longitude)
                      .isNotNull();

                  GeoPoint activityPoint =
                      new GeoPoint(Double.parseDouble(latitude), Double.parseDouble(longitude));
                  assertWithMessage(
                          "Didn't launch activity properly: latitude or longitude incorrect")
                      .that(compareGeopoints(activityPoint, randomClick))
                      .isTrue();
                }
              });
    }

    @Graded(points = 20, friendlyName = "Test Add Place Activity")
    @Test(timeout = 60000L)
    public void test3_AddPlaceActivity() {
      Random random = new Random(124);
      ActivityScenario<MainActivity> activityScenario = startActivity();
      activityScenario.onActivity(
          topMainActivity -> {
            pause();

            MapView topMapView = topMainActivity.findViewById(R.id.map);
            List<Marker> topMarkers = getMarkers(topMapView);
            assertWithMessage("Wrong number of places shown initially")
                .that(topMarkers)
                .hasSize(PLACES_COUNT);

            for (int i = 0; i < 8; i++) {
              GeoPoint randomPoint = randomGeoPointOnMap(random);
              Intent intent =
                  new Intent(ApplicationProvider.getApplicationContext(), AddPlaceActivity.class);
              intent.putExtra("latitude", Double.toString(randomPoint.getLatitude()));
              intent.putExtra("longitude", Double.toString(randomPoint.getLongitude()));
              String randomDescription = "" + random.nextInt();

              ActivityScenario.launch(intent)
                  .onActivity(
                      addPlaceActivity -> {
                        EditText editText = addPlaceActivity.findViewById(R.id.description);
                        editText.setText(randomDescription);
                        Button saveButton = addPlaceActivity.findViewById(R.id.save_button);
                        saveButton.performClick();
                        pause();
                        Intent started = shadowOf(topMainActivity).getNextStartedActivity();
                        assertWithMessage("Didn't start activity").that(started).isNotNull();
                        ShadowIntent shadowIntent = shadowOf(started);
                        assertWithMessage("Started wrong activity")
                            .that(shadowIntent.getIntentClass().getSimpleName())
                            .isEqualTo("MainActivity");
                        ActivityScenario.launch(started)
                            .onActivity(
                                finishedMainActivity -> {
                                  pause();

                                  MapView finishedMapView =
                                      finishedMainActivity.findViewById(R.id.map);
                                  List<Marker> finishedMarkers = getMarkers(finishedMapView);
                                  assertWithMessage("Wrong number of places shown after add")
                                      .that(finishedMarkers)
                                      .hasSize(PLACES_COUNT + 1);

                                  try {
                                    List<Place> places = clientGetPlaces();
                                    assertWithMessage("Wrong number of places from API after add")
                                        .that(places)
                                        .hasSize(PLACES_COUNT + 1);

                                    Place found = null;
                                    for (Place place : places) {
                                      if (place.getLatitude() == randomPoint.getLatitude()
                                          && place.getLongitude() == randomPoint.getLongitude()) {
                                        found = place;
                                        break;
                                      }
                                    }
                                    assertWithMessage("Place not added properly")
                                        .that(found)
                                        .isNotNull();
                                    assertWithMessage("Added place has wrong ID")
                                        .that(found.getId())
                                        .isEqualTo(FavoritePlacesApplication.CLIENT_ID);
                                    assertWithMessage("Added place has wrong description")
                                        .that(found.getDescription())
                                        .isEqualTo(randomDescription);

                                  } catch (InterruptedException | ExecutionException e) {
                                    throw new IllegalStateException(e);
                                  }
                                });
                      });
            }
          });
    }
  }
}
// DO NOT REMOVE THIS LINE
// md5: 040725a85615cfb2cf708eaa8e3c04d1
