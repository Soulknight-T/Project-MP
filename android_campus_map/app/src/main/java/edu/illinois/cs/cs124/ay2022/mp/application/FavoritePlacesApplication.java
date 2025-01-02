package edu.illinois.cs.cs124.ay2022.mp.application;

import android.app.Application;
import android.os.Build;
import edu.illinois.cs.cs124.ay2022.mp.network.Client;
import edu.illinois.cs.cs124.ay2022.mp.network.Server;

/*
 * One instance of the Application class is created when the app is launched and persists
 * throughout its lifetime.
 * This is unlike activities, which are created and destroyed as the user navigates to different
 * screens in the app.
 * As a result, the Application class can be a good place to store constants and initialize things
 * that are potentially needed by multiple activities, such as our places API client.
 *
 * You may need to change the code in this file, but probably not that much.
 */
@SuppressWarnings("unused")
public final class FavoritePlacesApplication extends Application {
  // Default API server port and URL
  // You can modify the port setting if it is conflicting with something else on your machine
  public static final int DEFAULT_SERVER_PORT = 8989;

  public static final String SERVER_URL = "http://localhost:" + DEFAULT_SERVER_PORT + "/";

  // Put your ID (from ID.txt) here
  public static final String CLIENT_ID = "6ef63dc4-f05e-438b-a999-66d65b78069b";

  // Reference to our API client, which would potentially be used by multiple app activities
  private Client client;

  public Client getClient() {
    return client;
  }

  // Called when the app is created
  @Override
  public void onCreate() {
    super.onCreate();

    // We start the API server differently depending on whether we are in a testing environment or
    // not
    if (Build.FINGERPRINT.equals("robolectric")) {
      Server.start();
    } else {
      new Thread(Server::start).start();
    }

    // Start the API client
    client = Client.start();
  }
}
