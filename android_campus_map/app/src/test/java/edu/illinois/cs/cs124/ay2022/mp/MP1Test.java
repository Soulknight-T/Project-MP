package edu.illinois.cs.cs124.ay2022.mp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertWithMessage;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.PLACES;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.PLACES_COUNT;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.configureLogging;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.countMarkersOverlay;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.pause;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.searchFor;
import static edu.illinois.cs.cs124.ay2022.mp.Helpers.startActivity;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.network.Client;
import edu.illinois.cs.cs124.ay2022.mp.network.Server;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(Enclosed.class)
@SuppressWarnings("SpellCheckingInspection")
public final class MP1Test {

  // Unit tests that don't require simulating the entire app
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class UnitTests {

    // Create an HTTP client to test the server with
    private static final OkHttpClient httpClient = new OkHttpClient();

    // Static blocks run when the static class is created, very much like a constructor
    static {
      // Start the API server
      Server.start();
    }

    @Before
    public void resetServer() {
      Server.reset();
    }

    @Graded(points = 20, friendlyName = "Test Load Place Fields")
    @Test(timeout = 3000L)
    public void test0_LoadPlaceFields() throws IOException {
      // Build a GET request for /places/
      Request placesRequest =
          new Request.Builder().url(FavoritePlacesApplication.SERVER_URL + "/places/").build();
      // Execute the request
      Response placesResponse = httpClient.newCall(placesRequest).execute();
      // The request should have succeeded
      assertWithMessage("Request should succeed").that(placesResponse.isSuccessful()).isTrue();
      // The response body should not be null
      ResponseBody body = placesResponse.body();
      assertWithMessage("Body should not be null").that(body).isNotNull();
      // The response body should be a JSON array
      JsonNode placesList = new ObjectMapper().readTree(body.string());
      assertWithMessage("Request should return a JSON array")
          .that(placesList instanceof ArrayNode)
          .isTrue();
      // The JSON array should contain the correct number of places
      assertWithMessage("Wrong place count").that(placesList).hasSize(PLACES_COUNT);
      // Check the JSON nodes for the correct fields
      for (JsonNode placeNode : placesList) {
        assertWithMessage("JSON is missing field id").that(placeNode.has("id")).isTrue();
        assertWithMessage("JSON id field is empty")
            .that(placeNode.get("id").textValue())
            .isNotEmpty();

        assertWithMessage("JSON is missing field name").that(placeNode.has("name")).isTrue();
        assertWithMessage("JSON name field is empty")
            .that(placeNode.get("name").textValue())
            .isNotEmpty();

        assertWithMessage("JSON is missing field latitude")
            .that(placeNode.has("latitude"))
            .isTrue();
        assertWithMessage("JSON latitude field is wrong type")
            .that(placeNode.get("latitude").isDouble())
            .isTrue();
        double latitude = placeNode.get("latitude").asDouble();
        assertWithMessage("JSON latitude field is invalid")
            .that(-90.0 <= latitude && latitude <= 90.0)
            .isTrue();

        assertWithMessage("JSON is missing field longitude")
            .that(placeNode.has("longitude"))
            .isTrue();
        assertWithMessage("JSON longitude field is wrong type")
            .that(placeNode.get("longitude").isDouble())
            .isTrue();
        double longitude = placeNode.get("longitude").asDouble();
        assertWithMessage("JSON longitude field is invalid")
            .that(-180.0 <= longitude && longitude <= 180.0)
            .isTrue();

        assertWithMessage("JSON is missing field description")
            .that(placeNode.has("description"))
            .isTrue();
        assertWithMessage("JSON description field is empty")
            .that(placeNode.get("description").textValue())
            .isNotEmpty();
      }
    }

    private void testSearchHelper(String searchInput, int expectedCount) {
      List<Place> results = Place.search(PLACES, searchInput);
      assertWithMessage("search modified passed list").that(PLACES).hasSize(PLACES_COUNT);
      assertWithMessage("incorrect search result for \"" + searchInput + "\"")
          .that(results.size())
          .isEqualTo(expectedCount);
    }

    @Graded(points = 30, friendlyName = "Test Places Search Method")
    @Test(timeout = 3000L)
    public void test1_PlacesSearch() {
      try {
        Place.search(null, null);
        fail("search didn't reject null");
      } catch (Exception e) {
        assertWithMessage("Threw wrong kind of exception")
            .that(e)
            .isInstanceOf(IllegalArgumentException.class);
      }
      try {
        Place.search(PLACES, null);
        fail("search didn't reject null");
      } catch (Exception e) {
        assertWithMessage("Threw wrong kind of exception")
            .that(e)
            .isInstanceOf(IllegalArgumentException.class);
      }
      try {
        Place.search(null, "test");
        fail("search didn't reject null");
      } catch (Exception e) {
        assertWithMessage("Threw wrong kind of exception")
            .that(e)
            .isInstanceOf(IllegalArgumentException.class);
      }

      assertWithMessage("Didn't handle empty list")
          .that(Place.search(new ArrayList<>(), "thai"))
          .hasSize(0);

      testSearchHelper("thai", 3);
      testSearchHelper(" THAI", 3);
      testSearchHelper("tHaI  ", 3);
      testSearchHelper("trex", 1);
      testSearchHelper("MCDONALDS", 1);
      testSearchHelper("some  ", 5);
      testSearchHelper("JOURNALS", 1);
      testSearchHelper("study", 4);
      testSearchHelper("vibeS", 1);
      testSearchHelper(" bOba ", 1);
      testSearchHelper("farmers", 1);
      testSearchHelper(" favorite\t", 4);
      testSearchHelper("aesthetic", 1);
      testSearchHelper("\tCURL\t", 1);
      testSearchHelper("try", 1);
      testSearchHelper("blues", 1);
      testSearchHelper("MTD", 1);
      testSearchHelper("activity", 3);
      testSearchHelper("fun", 1);
      testSearchHelper(" am", 0);
      testSearchHelper("es ", 0);
      testSearchHelper("OUTSIDE", 1);
      testSearchHelper("2ND", 1);
      testSearchHelper("", PLACES_COUNT);
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

    @Graded(points = 30, friendlyName = "Test Places Search Bar")
    @Test(timeout = 30000L)
    public void test2_PlacesSearchBar() {
      startActivity()
          .onActivity(
              unused -> {
                pause();
                // Check that the right number of places is shown initially
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT));

                // Perform a search that returns no results
                onView(withId(R.id.search)).perform(searchFor("abcdefgh"));
                // Pauses are required here to let the UI catch up
                pause();
                // All results should still be shown
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT));
                // Make sure that clearing the search causes the full list to be displayed again
                onView(withId(R.id.search)).perform(searchFor(""));
                pause();
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT));

                // Perform a search that returns some results
                onView(withId(R.id.search)).perform(searchFor("  thai "));
                pause();
                onView(withId(R.id.map)).check(countMarkersOverlay(3));
                onView(withId(R.id.search)).perform(searchFor(""));
                pause();
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT));

                // Perform a search that returns one result
                onView(withId(R.id.search)).perform(searchFor("oUtside"));
                pause();
                onView(withId(R.id.map)).check(countMarkersOverlay(1));
                onView(withId(R.id.search)).perform(searchFor(""));
                pause();
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT));

                // Perform a search that returns some more results
                onView(withId(R.id.search)).perform(searchFor(" some "));
                pause();
                onView(withId(R.id.map)).check(countMarkersOverlay(5));
                onView(withId(R.id.search)).perform(searchFor(""));
                pause();
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT));
              });
    }
  }
}
// DO NOT REMOVE THIS LINE
// md5: 8c898e62a36a5577b72cade2a0211ad1
