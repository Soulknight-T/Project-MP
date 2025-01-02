package edu.illinois.cs.cs124.ay2022.mp;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.MAPPER;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.PLACES_COUNT;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.checkCSV;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.compareGeopoints;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.configureLogging;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.countMarkers;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.pause;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.startActivity;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fasterxml.jackson.databind.JsonNode;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow;
import edu.illinois.cs.cs124.ay2022.mp.network.Client;
import edu.illinois.cs.cs124.ay2022.mp.network.Server;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/*
 * This is the MP0 test suite.
 * The code below is used to evaluate your app during testing, local grading, and official grading.
 * You will probably not understand all of the code below, but you'll need to have some
 * understanding of how it works so that you can determine what is wrong with your app and what you
 * need to fix.
 *
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 * You may modify the code below if it is useful during your own local testing, but any changes you
 * make will be lost once you submit.
 * Please keep that in mind, particularly if you see differences between your local scores and your
 * official scores.
 *
 * Our test suites are broken into two parts.
 * The unit tests (in the UnitTests class) are tests that we can perform without running your app.
 * They test things like whether a specific method works properly, or the behavior of your API
 * server.
 * Unit tests are usually fairly fast.
 *
 * The integration tests (in the IntegrationTests class) are tests that require simulating your app.
 * This allows us to test things like your API client, and higher-level aspects of your app's
 * behavior, such as whether it displays the right thing on the display.
 * Because integration tests require simulating your app, they run more slowly.
 *
 * Our test suites will also include a mixture of graded and ungraded tests.
 * The graded tests are marking with the `@Graded` annotation which contains a point total.
 * Ungraded tests do not have this annotation.
 * Some ungraded tests will work immediately, and are there to help you pinpoint regressions:
 * meaning changes that you made that might have broken things that were working previously.
 * The ungraded tests below were actually written by me (Geoff) during MP development.
 * Other ungraded tests are simply there to help your development process.
 */
@RunWith(Enclosed.class)
public final class MP0Test {
  // Where we expect the map to be centered
  private static final GeoPoint DEFAULT_CENTER =
      new GeoPoint(40.10986682167534, -88.22831928981661);

  static {
    // Make sure the CSV has not been modified
    checkCSV();
  }

  // Unit tests that don't require simulating the entire app
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class UnitTests {

    // Create an HTTP client to test the server with
    static OkHttpClient httpClient = new OkHttpClient();

    // Static blocks run when the static class is created, very much like a constructor
    static {
      // Start the API server
      Server.start();
    }

    // Reset the server before each test
    @Before
    public void resetServer() {
      Server.reset();
    }

    // THIS TEST SHOULD WORK
    // Test whether the GET /places server route works properly
    @Test(timeout = 3000L)
    public void test0_PlacesRoute() throws IOException {
      // Formulate a GET request to the API server for the /places/ route
      Request placesRequest =
          new Request.Builder().url(FavoritePlacesApplication.SERVER_URL + "/places/").build();
      // Execute the request
      Response placesResponse = httpClient.newCall(placesRequest).execute();

      // The request should have succeeded
      assertWithMessage("Request should have succeeded")
          .that(placesResponse.isSuccessful())
          .isTrue();

      // The response body should not be null
      ResponseBody body = placesResponse.body();
      assertWithMessage("Response body should not be null").that(body).isNotNull();

      // The response body should be a JSON array with the expected size
      JsonNode placeList = MAPPER.readTree(body.string());
      assertWithMessage("Places list is not the right size").that(placeList).hasSize(PLACES_COUNT);
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

    // Reset the server before each test
    @Before
    public void resetServer() {
      Server.reset();
    }

    // After each test make sure the client connected successfully
    @After
    public void checkClient() {
      assertWithMessage("Client should be connected").that(client.getConnected()).isTrue();
    }

    // Graded test that the activity displays the correct title
    @Graded(points = 40, friendlyName = "Test Activity Title")
    @Test(timeout = 30000L)
    public void test1_ActivityTitle() {
      // Start the main activity, and once it starts, check that it has the correct title
      startActivity()
          .onActivity(
              activity ->
                  assertWithMessage("MainActivity has wrong title")
                      .that(activity.getTitle())
                      .isEqualTo("Favorite Places"));
    }

    // Graded test that the app centers the map correctly after launch
    @Graded(points = 50, friendlyName = "Test Map Center")
    @Test(timeout = 30000L)
    public void test2_MapCenter() {
      // Start the main activity, and once it starts, check that the map is centered correctly
      startActivity()
          .onActivity(
              activity -> {
                // Let the UI catch up
                pause();
                // Grab the MapView and examine its center
                MapView mapView = activity.findViewById(R.id.map);
                assertThat(compareGeopoints(mapView.getMapCenter(), DEFAULT_CENTER)).isTrue();
              });
    }

    // THIS TEST SHOULD WORK
    // Test that the API client retrieves the list of places correctly
    @Test(timeout = 10000L)
    public void test3_ClientGetPlaces() throws InterruptedException, ExecutionException {
      // A CompletableFuture allows us to wait for the result of an asynchronous call
      CompletableFuture<ResultMightThrow<List<Place>>> completableFuture =
          new CompletableFuture<>();
      // When getPlaces returns, it causes the CompletableFuture to complete
      client.getPlaces(completableFuture::complete);
      // Wait for the CompletableFuture to complete
      ResultMightThrow<List<Place>> result = completableFuture.get();

      assertWithMessage("getPlaces threw an exception").that(result.getException()).isNull();
      List<Place> places = result.getResult();

      // The List<Places> should not be null, which is returned by getPlaces when something went
      // wrong
      assertWithMessage("Request failed").that(places).isNotNull();

      // Check that the List<Place> has the correct size
      assertWithMessage("Places list is not the right size").that(places).hasSize(PLACES_COUNT);
    }

    // THIS TEST SHOULD WORK
    // Test that the main activity displays the right number of places after launch
    @Test(timeout = 10000L)
    public void test4_ActivityPlaceCount() {
      startActivity()
          .onActivity(
              activity -> {
                pause();
                MapView mapView = activity.findViewById(R.id.map);
                assertThat(countMarkers(mapView)).isEqualTo(PLACES_COUNT);
              });
    }
  }
}
// DO NOT REMOVE THIS LINE
// md5: 1fd951f266dea686ecc31dc30b105f71
