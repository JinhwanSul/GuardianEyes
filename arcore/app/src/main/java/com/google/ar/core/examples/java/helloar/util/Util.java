package com.google.ar.core.examples.java.helloar.util;

import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;

import java.util.List;

public class Util {

  /*
  * 좌표들을 받아서 같은 길이의 재배열된 좌표를 반환
  * 선형 회귀 등을 이용
  * */
  public static float[] arrangePoints(float[] input) {
    // TODO: implement this method

    float[] output = input.clone();

    return output;
  }

  /*
  * 좌표들을 받아서 대표값 하나를 반환
  * 평균 등 이용
  * */
  public static float findRepresentativeValue(float[] input) {
    // TODO: implement this method

    float output = 0f;

    return output;
  }

  /*
  * 주어진 좌표를 양자화하기
  * 정수로 양자화 or 소수 첫째짜리까지만 or ...
  * */
  public static float[] quantizePoints(float[] input) {
    // TODO: implement this method

    float[] output = input.clone();

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
