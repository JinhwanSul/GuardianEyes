package com.google.ar.core.examples.java.helloar;

import java.util.ArrayList;
import java.util.List;

public class TrackingResult {
  public List<float[]> boxResults;
  public List<String> titles;

  public TrackingResult() {
    boxResults = new ArrayList<>();
    titles = new ArrayList<>();
  }

  public int size() {
    return boxResults.size();
  }
}
