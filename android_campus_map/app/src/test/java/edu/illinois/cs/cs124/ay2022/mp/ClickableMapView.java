package edu.illinois.cs.cs124.ay2022.mp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import java.lang.reflect.Field;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

/*
 * Helper class for osmdroid MapView testing on later checkpoints.
 * Prying into the internals of this class is necessary to be able to deliver click events during
 * testing.
 * You should not need to modify it.
 *
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 */
@SuppressWarnings("unused")
public class ClickableMapView {
  private final MapView mapView;
  private final GestureDetector.OnDoubleTapListener doubleTapListener;
  private final GestureDetector.OnGestureListener gestureListener;

  @SuppressWarnings("JavaReflectionMemberAccess")
  public ClickableMapView(final MapView setMapView) {
    mapView = setMapView;

    /*
     * The code below is generally considered to be NOT A GOOD IDEA.
     * We are using a feature of Java called reflection to allow us to access private fields on the
     * MapView class.
     * This is the only way (at least that I could figure out) how to deliver click events to the
     * MapView during testing.
     *
     * Because these fields are not part of the public API of the MapView class, the
     * code below could stop working at any moment: for example, if these fields were ever
     * renamed.
     * However, to allow the MapView component to be able to be tested, this was currently
     * necessary.
     *
     * The next right thing to do here would be to contact the maintainers of osmdroid and ask
     * that these fields be opened, or that new interfaces be provided to facilitate testing.
     * And hey, someone did that: https://github.com/osmdroid/osmdroid/issues/1859.
     */
    try {
      Field mGestureDetector = MapView.class.getDeclaredField("mGestureDetector");
      mGestureDetector.setAccessible(true);
      GestureDetector detector = (GestureDetector) mGestureDetector.get(mapView);

      Field mDoubleTapListener = GestureDetector.class.getDeclaredField("mDoubleTapListener");
      mDoubleTapListener.setAccessible(true);
      doubleTapListener = (GestureDetector.OnDoubleTapListener) mDoubleTapListener.get(detector);

      Field mListener = GestureDetector.class.getDeclaredField("mListener");
      mListener.setAccessible(true);
      gestureListener = (GestureDetector.OnGestureListener) mListener.get(detector);

      if (doubleTapListener == null || gestureListener == null) {
        throw new IllegalStateException();
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    update();
  }

  // Update the MapView
  public void update() {
    // Robolectric does not replicate certain calls to layout components.
    // Failing to call the draw method will result in markers that are not positioned, which
    // makes testing impossible
    mapView.draw(
        new Canvas(
            Bitmap.createBitmap(mapView.getWidth(), mapView.getHeight(), Bitmap.Config.ARGB_8888)));
  }

  // Deliver a click event to the map at a specified x, y pixel coordinates value
  public boolean click(final Point point) {
    // Create and deliver the event
    MotionEvent e =
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            point.x,
            point.y,
            0);
    return doubleTapListener.onSingleTapConfirmed(e);
  }

  // Deliver a long press event to the map at a specified latitude and longitude
  public void longPress(final GeoPoint geoPoint) {
    // Convert the latitude and longitude to a x, y pixel coordinates value
    Point point = new Point();
    mapView.getProjection().toPixels(geoPoint, point);

    // Create and deliver the event
    MotionEvent e =
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            point.x,
            point.y,
            0);
    gestureListener.onLongPress(e);
  }
}
