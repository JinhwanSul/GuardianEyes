package com.snu.mobile.computing.guardianeyes.java.main.tracking;

import android.graphics.RectF;

import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.util.List;

public class Tracker {

  public TrackingResult objectTracking(List<Detector.Recognition> result) {
    Process.initializeData();
    for(final Detector.Recognition rec : result) {
      RectF rect = rec.getLocation();
      Process.setData(rec.getTitle(), rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    Process.Sort();

    TrackingResult trackingResult = new TrackingResult();
    for(int i = 0; ; ++i) {
      float[] res = Process.getData(i);
      String title = Process.getTrackingTitle(i);

      if(res == null) break; // end of data

      trackingResult.boxResults.add(res);
      trackingResult.titles.add(title);
    }

    return trackingResult;
  }
}
