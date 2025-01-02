package edu.illinois.cs.cs124.ay2022.mp.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/*
 * Favorite Place API server.
 *
 * Normally this code would run on a separate machine from your app, which would make requests to
 * it over the internet.
 * However, for our MP we have this run inside the app alongside the rest of your code, to allow
 * you to gain experience with full-stack app development.
 * You are both developing the client (the Android app) and the server that it requests data from.
 * This is a very common programming paradigm and one used by most or all of the smartphone apps
 * that you use regularly.
 *
 * You will need to some of the code here and make changes starting with MP1.
 */
public final class Server extends Dispatcher {
  // You may find this useful for debugging
  @SuppressWarnings("unused")
  private static final String TAG = Server.class.getSimpleName();

  // We are using the Jackson JSON serialization library to serialize and deserialize data on the
  // server
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // Stores the List<Place> containing information about all of the favorite places created during
  // server startup
  private List<Place> places;

  // Helper method for the GET /places route, called by the dispatch method below
  private MockResponse getPlaces() throws JsonProcessingException {
    return new MockResponse()
        // Indicate that the request succeeded (HTTP 200 OK)
        .setResponseCode(HttpURLConnection.HTTP_OK)
        // Load the JSON string with place information into the body of the response
        // We use Jackson to serialize the List<Place> to a String
        .setBody(OBJECT_MAPPER.writeValueAsString(places))
        /*
         * Set the HTTP header that indicates that this is JSON with the utf-8 charset.
         * There may be special characters in our data set, so it's important to mark it as utf-8
         * so it is parsed properly by clients.
         */
        .setHeader("Content-Type", "application/json; charset=utf-8");
  }

  private MockResponse postFavoritePlace(final RecordedRequest request) {
    Place temp;
    try {
      temp = OBJECT_MAPPER.readValue(request.getBody().readUtf8(), Place.class);
    } catch (JsonProcessingException e) {
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
    if (temp.getId() == null || temp.getName() == null || temp.getDescription() == null) {
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
    if (temp.getLatitude() == null || temp.getLongitude() == null) {
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
    if (temp.getLongitude() > 180.0 || temp.getLongitude() < -180.0) {
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
    if (temp.getLatitude() > 90.0 || temp.getLatitude() < -90.0) {
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
    if (temp.getId().length() != 36 || temp.getName().isBlank() || temp.getDescription().isBlank()) {
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
    for (Place tempPlace : places) {
      if (tempPlace.getId().equals(temp.getId())) {
        places.remove(tempPlace);
        places.add(temp);
        return new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json; charset=utf-8");
      }
    }
    places.add(temp);
    return new MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_OK)
        .setHeader("Content-Type", "application/json; charset=utf-8");
  }
  /*
   * Server request dispatcher.
   * Responsible for parsing the HTTP request and determining how to respond.
   * You will need to understand this code and augment it starting with MP2.
   */
  @Override
  public MockResponse dispatch(final RecordedRequest request) {
    try {
      // Reject malformed requests
      if (request.getPath() == null || request.getMethod() == null) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }

      /*
       * We perform a few normalization steps before we begin route dispatch, since this makes the
       * if-else statement below simpler.
       */

      // Normalize the path by removing trailing slashes and replacing multiple repeated slashes
      // with single slashes
      String path = request.getPath().replaceFirst("/*$", "").replaceAll("/+", "/");
      // Normalize the request method by converting to uppercase
      String method = request.getMethod().toUpperCase();

      // Main route dispatch tree, dispatching routes based on request path and type
      if (path.equals("") && method.equals("GET")) {
        // This route is used by the client during startup, so don't remove
        return new MockResponse().setBody("CS 124").setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.equals("/reset") && method.equals("GET")) {
        // This route is used during testing, so don't remove or alter
        doReset();
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.equals("/places") && method.equals("GET")) {
        // Return the JSON list of restaurants for a GET request to the path /restaurants
        return getPlaces();
      } else if (path.equals("/favoriteplace") && method.equals("POST")) {
        return postFavoritePlace(request);
      }
      // If the route didn't match above, then we return a 404 NOT FOUND
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
          // If we don't set a body here Volley will choke with a strange error
          // Normally a 404 for a web API would not need a body
          .setBody("Not Found");
    } catch (Exception e) {
      // Return a HTTP 500 if an exception is thrown
      // You may need to add logging here during later checkpoints
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
  }

  /*
   * Load place information from a CSV file and create a List<Place>.
   * You will need to examine and modify this code for MP1.
   */
  public static List<Place> loadPlaces() throws JsonProcessingException {
    // An unfortunate bit of code required to read an entire stream into a `String`
    String input =
        new Scanner(Server.class.getResourceAsStream("/places.csv"), "UTF-8")
            .useDelimiter("\\A")
            .next();

    // We skip the first two lines in the CSV
    // The first is a hash for verifying integrity during testing, and the second is the header
    CSVReader csvReader = new CSVReaderBuilder(new StringReader(input)).withSkipLines(2).build();

    // Load all CSV rows into a list and return
    List<Place> toReturn = new ArrayList<>();
    for (String[] parts : csvReader) {
      toReturn.add(
          new Place(
              parts[0], parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), parts[4]));
    }
    return toReturn;
  }

  /*
   * You do not need to modify the code below.
   * However, you may want to understand how it works.
   * It implements the singleton pattern and initializes the server when Server.start() is called.
   * We also check to make sure that no other servers are running on the same machine,
   * which can cause problems.
   */
  public static void start() {
    if (!isRunning(false)) {
      new Server();
    }
    if (!isRunning(true)) {
      throw new IllegalStateException("Server should be running");
    }
  }

  private static final int RETRY_COUNT = 8;
  private static final long RETRY_DELAY = 512;

  public static boolean isRunning(final boolean wait) {
    return isRunning(wait, RETRY_COUNT, RETRY_DELAY);
  }

  public static boolean isRunning(final boolean wait, final int retryCount, final long retryDelay) {
    for (int i = 0; i < retryCount; i++) {
      OkHttpClient client = new OkHttpClient();
      Request request =
          new Request.Builder().url(FavoritePlacesApplication.SERVER_URL).get().build();
      try {
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
          if (Objects.requireNonNull(response.body()).string().equals("CS 124")) {
            return true;
          } else {
            throw new IllegalStateException(
                "Another server is running on port "
                    + FavoritePlacesApplication.DEFAULT_SERVER_PORT);
          }
        }
      } catch (IOException ignored) {
        if (!wait) {
          break;
        }
        try {
          Thread.sleep(retryDelay);
        } catch (InterruptedException ignored1) {
        }
      }
    }
    return false;
  }

  public static void reset() {
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder().url(FavoritePlacesApplication.SERVER_URL + "/reset/").get().build();
    try {
      Response response = client.newCall(request).execute();
      if (!response.isSuccessful()) {
        throw new IllegalStateException();
      }
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private Server() {
    doReset();

    Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.OFF);
    try {
      MockWebServer server = new MockWebServer();
      server.setDispatcher(this);
      server.start(FavoritePlacesApplication.DEFAULT_SERVER_PORT);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException(e.getMessage());
    }
  }

  private void doReset() {
    try {
      places = loadPlaces();
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
