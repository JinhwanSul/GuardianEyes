package com.snu.mobile.computing.guardianeyes.java.main.tracking;

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
