package com.google.ar.core.examples.java.helloar.util;

import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.List;

enum Method{MEAN, MIDDLE};

public class Util {

  /*
  * 좌표들을 받아서 같은 길이의 재배열된 좌표를 반환
  * 선형 회귀 등을 이용
  * */
  private static double[][] arrangePointsHelper(List<Float> input) {
    double[][] modifiedInput = new double[input.size()][2];
    for (int i = 0; i < input.size(); i++) {
      modifiedInput[i][0] = i;
      modifiedInput[i][1] = (double) input.get(i);
    }
    return modifiedInput;
  }

  public static List<Float> arrangePoints(List<Float> input) {
    // TODO: implement this method
    SimpleRegression simpleRegression = new SimpleRegression();
    simpleRegression.addData(arrangePointsHelper(input));
    List<Float> output = new ArrayList<>(input);
    for (int i = 0; i < input.size(); i++) {
      double linearValue = simpleRegression.predict( (double) input.get(i));
      output.add( (float) linearValue );
    }
    return output;
  }

  /*
  * 좌표들을 받아서 대표값 하나를 반환
  * 평균 등 이용
  * */
  public static float findRepresentativeValue(List<Float> input) {
    // TODO: implement this method
    if (input.size() == 0) return -1.0f;

    float output = 0f;
    Method method = Method.MEAN;
    switch (method) {
      case MEAN:
        for (float f : input) output += f;
        output = output / (float) input.size();
        break;
      case MIDDLE:
        int mid = input.size() / 2;
        if ( input.size() % 2 == 0) {
          output = (input.get(mid) + input.get(mid-1)) / 2;
        } else {
          output = input.get(mid);
        }
        break;
      default:
        output = input.get(0);
        break;
    }

    return output;
  }

  /*
  * 주어진 좌표를 양자화하기
  * 정수로 양자화 or 소수 첫째짜리까지만 or ...
  * */
  public static List<Float> quantizePoints(List<Float> input) {
    // TODO: implement this method
    int decimalPoints = 0;
    double powTen = Math.pow(10.0, decimalPoints);
    List<Float> output = new ArrayList<>(input);
    for (int i = 0; i < input.size(); i++) {
      output.add((float) (Math.round(input.get(i) * powTen) / powTen));
    }
    return output;
  }

  public static float calDistance(Frame frame, float w, float h) {
    float minDistance = 10000.0f;

    List<HitResult> hitResultList = frame.hitTest(0.5f * w, 0.5f * h);
    if(!hitResultList.isEmpty()) minDistance = Math.min(minDistance, hitResultList.get(0).getDistance());

    hitResultList = frame.hitTest(0.45f*w, 0.5f*h);
    if(!hitResultList.isEmpty()) minDistance = Math.min(minDistance, hitResultList.get(0).getDistance());

    hitResultList = frame.hitTest(0.55f*w, 0.5f*h);
    if(!hitResultList.isEmpty()) minDistance = Math.min(minDistance, hitResultList.get(0).getDistance());

    hitResultList = frame.hitTest(0.5f*w, 0.55f*h);
    if(!hitResultList.isEmpty()) minDistance = Math.min(minDistance, hitResultList.get(0).getDistance());

    hitResultList = frame.hitTest(0.5f*w, 0.45f*h);
    if(!hitResultList.isEmpty()) minDistance = Math.min(minDistance, hitResultList.get(0).getDistance());

    return minDistance;
  }

}
