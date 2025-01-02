package edu.illinois.cs.cs124.ay2022.mp;

import static android.os.Looper.getMainLooper;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.robolectric.Shadows.shadowOf;

import android.util.Log;
import android.view.View;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import edu.illinois.cs.cs124.ay2022.mp.activities.MainActivity;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.network.Server;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.robolectric.shadows.ShadowLog;

/*
 * This file contains helper code used by the test suites.
 * You should not need to modify it.
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 */
@SuppressWarnings("unused")
public class Helpers {
  // Number of places that we expect
  public static final int PLACES_COUNT = 58;

  // Map boundaries
  public static final double MAP_LIMIT_NORTH = 40.1741;
  public static final double MAP_LIMIT_SOUTH = 40.0247;
  public static final double MAP_LIMIT_WEST = -88.3331;
  public static final double MAP_LIMIT_EAST = -88.1433;

  // ObjectMapper properly configured for testing
  public static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // List of places used for testing
  public static final List<Place> PLACES = loadPlacesFromCSV();

  // Helper method to start the activity
  public static ActivityScenario<MainActivity> startActivity() {
    ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
    scenario.moveToState(Lifecycle.State.CREATED);
    scenario.moveToState(Lifecycle.State.RESUMED);

    assertThat(Server.isRunning(true)).isTrue();
    return scenario;
  }

  // Check the CSV for changes which might break our tests
  public static String checkCSV() {
    String input =
        new Scanner(Server.class.getResourceAsStream("/places.csv"), "UTF-8")
            .useDelimiter("\\A")
            .next();
    String fingerprint = fingerprintCSV(input);
    if (!fingerprint.equals(PLACES_HASH)) {
      throw new IllegalStateException("places.csv was modified");
    }
    return fingerprint;
  }

  // Helpers to load data from the places.csv file
  public static List<Place> loadPlacesFromCSV() {
    checkCSV();

    try {
      List<Place> places = MAPPER.readValue(loadPlacesStringFromCSV(), new TypeReference<>() {});
      places.sort(Comparator.comparing(Place::getId));
      if (places.size() != PLACES_COUNT) {
        throw new IllegalStateException("Wrong place count");
      }
      return places;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String loadPlacesStringFromCSV() {
    String input =
        new Scanner(Server.class.getResourceAsStream("/places.csv"), "UTF-8")
            .useDelimiter("\\A")
            .next();

    CSVReader csvReader = new CSVReaderBuilder(new StringReader(input)).withSkipLines(2).build();
    ArrayNode places = JsonNodeFactory.instance.arrayNode();
    for (String[] parts : csvReader) {
      ObjectNode place = JsonNodeFactory.instance.objectNode();
      place.put("id", parts[0]);
      place.put("name", parts[1]);
      place.put("latitude", Double.parseDouble(parts[2]));
      place.put("longitude", Double.parseDouble(parts[3]));
      place.put("description", parts[4]);
      places.add(place);
    }
    return places.toPrettyString();
  }

  // Get a list of all the Markers on the map, since not every Overlay is a Marker
  @SuppressWarnings("unchecked")
  public static List<Marker> getMarkers(MapView mapView) {
    return (List<Marker>)
        (List<?>)
            mapView.getOverlays().stream()
                .filter(o -> o instanceof Marker)
                .collect(Collectors.toList());
  }

  public static int countMarkers(MapView mapView) {
    return getMarkers(mapView).size();
  }

  // View assertions used by the MP test suites
  public static ViewAssertion countMarkersOverlay(int expected) {
    return (view, noViewFoundException) -> {
      if (noViewFoundException != null) {
        throw noViewFoundException;
      }
      if (!(view instanceof MapView)) {
        throw new IllegalStateException("View passed to countMarkersOverlay should be a MapView");
      }
      MapView mapView = (MapView) view;
      assertThat(countMarkers(mapView)).isEqualTo(expected);
    };
  }

  public static ViewAction searchFor(String query) {
    return searchFor(query, false);
  }

  public static ViewAction searchFor(String query, boolean submit) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return allOf(isDisplayed());
      }

      @Override
      public String getDescription() {
        if (submit) {
          return "Set query to " + query + " and submit";
        } else {
          return "Set query to " + query + " but don't submit";
        }
      }

      @Override
      public void perform(UiController uiController, View view) {
        SearchView v = (SearchView) view;
        v.setQuery(query, submit);
      }
    };
  }

  // Pause helpers to improve the stability of our Robolectric tests
  public static void pause(int length) {
    try {
      shadowOf(getMainLooper()).runToEndOfTasks();
      Thread.sleep(length);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void pause() {
    pause(100);
  }

  // Set up logging properly for testing
  public static void configureLogging() {
    if (System.getenv("OFFICIAL_GRADING") == null) {
      ShadowLog.setLoggable("ActivityScenario", Log.WARN);
      ShadowLog.stream = System.out;
    }
  }

  private static final double MAX_GEOPOINT_DIFF = 0.00001;

  // Fuzzy comparison of GeoPoints using the maximum diff defined above
  public static boolean compareGeopoints(IGeoPoint first, IGeoPoint second) {
    double latDiff = Math.abs(first.getLatitude() - second.getLatitude());
    double lonDiff = Math.abs(first.getLongitude() - second.getLongitude());
    return latDiff < MAX_GEOPOINT_DIFF && lonDiff < MAX_GEOPOINT_DIFF;
  }

  public static final String PLACES_HASH = "40537703c53990288b03612e29ac5914";

  // Fingerprint the CSV file to check for changes
  public static String fingerprintCSV(final String input) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      throw new IllegalStateException("MD5 algorithm should be available", e);
    }

    String toFingerprint =
        Arrays.stream(input.split(System.lineSeparator()))
            .filter(s -> !s.startsWith("# md5: "))
            .map(String::trim)
            .collect(Collectors.joining("\n"));

    return String.format(
            "%1$32s",
            new BigInteger(1, digest.digest(toFingerprint.getBytes(StandardCharsets.UTF_8)))
                .toString(16))
        .replace(' ', '0');
  }

  // Various helper methods for creating Place objects and JSON strings
  public static String makePlaceJSON(
      final String id,
      final String name,
      final Double latitude,
      final Double longitude,
      final String description) {
    ObjectNode place = JsonNodeFactory.instance.objectNode();
    place.put("id", id);
    place.put("name", name);
    if (latitude != null) {
      place.put("latitude", latitude);
    }
    if (longitude != null) {
      place.put("longitude", longitude);
    }
    place.put("description", description);
    return place.toPrettyString();
  }

  public static Place makePlaceFromJson(
      final String id,
      final String name,
      final Double latitude,
      final Double longitude,
      final String description)
      throws JsonProcessingException {
    return MAPPER.readValue(makePlaceJSON(id, name, latitude, longitude, description), Place.class);
  }

  // Grab a random GeoPoint within our map bounds
  public static GeoPoint randomGeoPointOnMap(Random random) {
    double randomLatitude =
        ((MAP_LIMIT_NORTH - MAP_LIMIT_SOUTH) * random.nextDouble()) + MAP_LIMIT_SOUTH;
    double randomLongitude =
        ((MAP_LIMIT_EAST - MAP_LIMIT_WEST) * random.nextDouble()) + MAP_LIMIT_WEST;
    return new GeoPoint(randomLatitude, randomLongitude);
  }

  // Grab a random GeoPoint that is actually visible on the map as currently zoomed and panned
  public static GeoPoint randomGeoPointInMap(Random random, MapView mapView) {
    IGeoPoint center = mapView.getMapCenter();
    double northBorder = center.getLatitude() - (mapView.getLatitudeSpanDouble() / 2.0);
    double southBorder = center.getLatitude() + (mapView.getLatitudeSpanDouble() / 2.0);
    double westBorder = center.getLongitude() - (mapView.getLongitudeSpanDouble() / 2.0);
    double eastBorder = center.getLongitude() + (mapView.getLongitudeSpanDouble() / 2.0);
    double randomLatitude = ((northBorder - southBorder) * random.nextDouble()) + southBorder;
    double randomLongitude = ((eastBorder - westBorder) * random.nextDouble()) + westBorder;
    return new GeoPoint(randomLatitude, randomLongitude);
  }
}
