package edu.illinois.cs.cs124.ay2022.mp.models;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
/*
 * Model storing information about a place retrieved from the backend server.
 *
 * You will need to understand some of the code in this file and make changes starting with MP1.
 */
@SuppressWarnings("unused")
public final class Place {
  /*
   * The Jackson JSON serialization library that we are using requires an empty constructor.
   * So don't remove this!
   */
  public Place() {
  }

  public Place(
      final String setId,
      final String setName,
      final double setLatitude,
      final double setLongitude,
      final String setDescription) {
    id = setId;
    name = setName;
    latitude = setLatitude;
    longitude = setLongitude;
    description = setDescription;
  }
  public Place(
      final String setId,
      final String setName,
      final double setLatitude,
      final double setLongitude,
      final String setDescription,
      final boolean setHighlight) {
    id = setId;
    name = setName;
    latitude = setLatitude;
    longitude = setLongitude;
    description = setDescription;
    highlight = setHighlight;
  }

  // ID of the place
  private String id;

  public String getId() {
    return id;
  }

  // Name of the person who submitted this favorite place
  private String name;

  public String getName() {
    return name;
  }

  // Latitude and longitude of the place
  private Double latitude = null;

  public Double getLatitude() {
    return latitude;
  }

  private Double longitude = null;

  public Double getLongitude() {
    return longitude;
  }
  private boolean highlight;
  public boolean getHighlight() {
    return highlight;
  }
  // Description of the place
  private String description;
  private final ObjectMapper mapper = new ObjectMapper();

  public String getDescription() {
    return description;
  }

  public static ArrayList<Place> search(final List<Place> p, final String q) {
    ArrayList<Place> toReturn = new ArrayList<>();
    String tempDescription;
    if (p == null || q == null) {
      throw new IllegalArgumentException();
    }
    ArrayList<Place> copyOfPlace = new ArrayList(p);
    if (q.length() == 0) {
      return copyOfPlace;
    }
    for (Place temp : p) {
      String copy = "";
      tempDescription = temp.getDescription().replace('.', ' ');
      tempDescription = tempDescription.replace('!', ' ');
      tempDescription = tempDescription.replace('?', ' ');
      tempDescription = tempDescription.replace(',', ' ');
      tempDescription = tempDescription.replace(':', ' ');
      tempDescription = tempDescription.replace(';', ' ');
      tempDescription = tempDescription.replace('/', ' ');
      for (char i : tempDescription.toCharArray()) {
        if (Character.isAlphabetic(i)) {
          copy = copy + i;
        } else if (Character.isDigit(i)) {
          copy = copy + i;
        } else if (Character.isWhitespace(i)) {
          copy = copy + i;
        }
      }
      copy = copy.toLowerCase();
      copy = copy.trim();
      String copyQ = q.toLowerCase();
      copyQ = copyQ.trim();
      String[] splitCopy = copy.split(" ");
      for (String tempCopy : splitCopy) {
        if (tempCopy.equals(copyQ)) {
          toReturn.add(temp);
          break;
        }
      }
    }
    return toReturn;
  }
}
