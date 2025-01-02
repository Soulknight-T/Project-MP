package edu.illinois.cs.cs124.ay2022.mp.network;

import android.os.Build;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
 * Client object used by the app to interact with the place API server.
 *
 * This class uses what is called a singleton pattern, as described more below.
 * We create a static method that will only create a client the first time it is called,
 * and mark the constructor as private to prevent others from creating additional instances of
 * the client.
 *
 * You will need to understand some of the code here and make changes starting with MP2.
 */
public final class Client {
  // You may find this useful when debugging
  private static final String TAG = Client.class.getSimpleName();

  // We are using the Jackson JSON serialization library to deserialize data from the server
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /*
   * Retrieve and deserialize a list of places from the backend server.
   * Takes as an argument a callback method to call when the request completes which will be passed
   * the deserialized list of places received from the server, wrapped in a ResultMightThrow
   * to allow us to also report errors.
   * We will discuss callbacks in more detail once you need to augment this code in MP2.
   */
  public void getPlaces(final Consumer<ResultMightThrow<List<Place>>> callback) {
    /*
     * Construct the request itself.
     * We use a StringRequest allowing us to receive a String from the server.
     * The String will be valid JSON containing a list of place objects which we can deserialize
     * into instances of our Place model.
     */
    StringRequest getPlacesRequest =
        new StringRequest(
            Request.Method.GET,
            FavoritePlacesApplication.SERVER_URL + "/places/",
            response -> {
              // This code runs on success
              try {
                /*
                 * Deserialize the String into a List<Restaurant> using Jackson.
                 * The TypeReference<>() {} is a bit of magic required to have Jackson
                 * return a List with the correct type.
                 * We wrap this in a try-catch to handle deserialization errors that may occur.
                 */
                List<Place> places = OBJECT_MAPPER.readValue(response, new TypeReference<>() {});
                // Pass the List<Place> to the callback
                callback.accept(new ResultMightThrow<>(places));
              } catch (JsonProcessingException error) {
                // Pass the Exception to the callback on error
                callback.accept(new ResultMightThrow<>(error));
              }
            },
            error -> {
              // This code runs on failure
              // Pass the Exception to the callback on error
              callback.accept(new ResultMightThrow<>(error));
            });

    // Actually queue the request
    // The callbacks above will be run once it completes
    requestQueue.add(getPlacesRequest);
  }

  public void postFavoritePlace(final Place place, final Consumer<ResultMightThrow<Boolean>> callback) {
    StringRequest postFavoritePlace =
        new StringRequest(
          Request.Method.POST,
          FavoritePlacesApplication.SERVER_URL + "/favoriteplace/",
            response -> {
              callback.accept(new ResultMightThrow<>(true));
            },
            error -> {
              callback.accept(new ResultMightThrow<>(error));
            }) {
          @Override
          public byte[] getBody() throws AuthFailureError {
            try {
              return OBJECT_MAPPER.writeValueAsString(place).getBytes(StandardCharsets.UTF_8);
            } catch (JsonProcessingException error) {
              throw new AuthFailureError();
            }
          }
          @Override
          public String getBodyContentType() {
            return "application/json; charset=utf-8";
          }
        };
    // Actually queue the request
    // The callbacks above will be run once it completes
    requestQueue.add(postFavoritePlace);
  }
  /*
   * You do not need to modify the code below.
   * However, you may want to understand how it works.
   * It implements the singleton pattern and initializes the client when Client.start() is called.
   * The client tests to make sure it can connect to the backend server on startup.
   * We also initialize the client somewhat differently depending on whether we are testing your code or actually
   * running the app.
   */

  private static final int INITIAL_CONNECTION_RETRY_DELAY = 1000;
  private static Client instance;
  private boolean connected = false;

  public boolean getConnected() {
    return connected;
  }

  public static Client start() {
    if (instance == null) {
      instance = new Client(Build.FINGERPRINT.equals("robolectric"));
    }
    return instance;
  }

  private static final int MAX_STARTUP_RETRIES = 8;
  private static final int THREAD_POOL_SIZE = 4;

  private final RequestQueue requestQueue;

  private Client(final boolean testing) {
    VolleyLog.DEBUG = false;

    Cache cache = new NoCache();
    Network network = new BasicNetwork(new HurlStack());
    HttpURLConnection.setFollowRedirects(true);

    if (testing) {
      requestQueue =
          new RequestQueue(
              cache,
              network,
              THREAD_POOL_SIZE,
              new ExecutorDelivery(Executors.newSingleThreadExecutor()));
    } else {
      requestQueue = new RequestQueue(cache, network);
    }

    URL serverURL;
    try {
      serverURL = new URL(FavoritePlacesApplication.SERVER_URL);
    } catch (MalformedURLException e) {
      Log.e(TAG, "Bad server URL: " + FavoritePlacesApplication.SERVER_URL);
      return;
    }

    new Thread(
            () -> {
              for (int i = 0; i < MAX_STARTUP_RETRIES; i++) {
                try {
                  HttpURLConnection connection = (HttpURLConnection) serverURL.openConnection();
                  String body =
                      new BufferedReader(new InputStreamReader(connection.getInputStream()))
                          .lines()
                          .collect(Collectors.joining("\n"));
                  if (!body.equals("CS 124")) {
                    throw new IllegalStateException("Invalid response from server");
                  }
                  connection.disconnect();
                  connected = true;
                  requestQueue.start();
                  break;
                } catch (Exception e) {
                  Log.e(TAG, e.toString());
                }
                try {
                  Thread.sleep(INITIAL_CONNECTION_RETRY_DELAY);
                } catch (InterruptedException ignored) {
                }
              }
            })
        .start();
  }
}
