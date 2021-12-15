package com.snu.mobile.computing.guardianeyes.java.main.util;

import com.google.ar.core.Camera;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Trackable;
import com.snu.mobile.computing.guardianeyes.java.main.HelloArActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Checker {

  private float width, height;

  private float[] pointsX = {0.50f};
  private float[] pointsY = {0.50f};
  private String[] dataString = new String[pointsX.length];
  private float avgHeight = 0, threshold = 0.01f;
  private float wallThr = 0.04f;
  private final int averageCalculationFrameNum = 24;

  private HelloArActivity context;
  private String[] saveData;

  private List<Float> data;
  private final int FRAME_DECISION_NUM = 15;
  private final int FRAGMENT_SIZE = 3;
  private final int AVERAGE_SIZE = 5;

  private int frame_count = 0;
  private final int DISCARD_FRAME_NUM = 30;

  private float slopeThreshold = 0.02f;

  // tts feedback
  //private TextToSpeech tts;

  // state class
  private State state;

  // set this flag in order to start recording.
  public boolean START_RECORDING = true;

  public Checker(HelloArActivity context) {
    this.context = context;
    this.saveData = new String[pointsX.length];
    this.state = new State(context);
    this.data = new ArrayList<>();
  }

  public String[] getSaveData() {
    String[] record = dataString.clone();
    return record;
  }

  public void checkWallOrHole(Frame frame, Camera camera, float width, float height) {
    for(int num = 0; num < pointsX.length; ++num) {
      float coorX = pointsX[num], coorY = pointsY[num];
      List<HitResult> hitResultList = frame.hitTest(coorX * width, coorY * height);
      boolean isHit = false;

      for(HitResult hit : hitResultList) {
        Trackable trackable = hit.getTrackable();

        if(trackable instanceof DepthPoint)
        {
          frame_count++;

          float res = hit.getHitPose().ty() - camera.getPose().ty();
          int frameNum = frame_count - DISCARD_FRAME_NUM;

          if(frameNum > 0 && frameNum < averageCalculationFrameNum) { // frameCount 수만큼 평균 높이 구하기
//            avgHeight = (avgHeight * frameNum + res) / (frameNum + 1);
//            context.avgHeightTextView.setText("Average height : " + avgHeight + "m");
            context.avgHeightTextView.setText("Loading...");
          }
          else if(frameNum >= averageCalculationFrameNum) {
//            if(num == 0) context.textView.setText("Height difference : " + res + "m"); // 중점의 경우를 화면에 출력
            context.avgHeightTextView.setText("Executing...");

            // TODO: Implement feedback of floor detection
            if(state.tts.isSpeaking()) {
              data.clear();
            } else {
              data.add(res);
              if(data.size() > FRAME_DECISION_NUM) {
                data.remove(0);
                Floor st = classify(data);
                state.setWallstate(st);
              }
            }
          }

          isHit = true;
          break;
        }
      }

      // Surface가 탐지되지 않아 hit한 점이 없을 때
      if(!isHit) {
        data.clear();
        if(num == 0) context.avgHeightTextView.setText("No Plane Detected");
//        if(START_RECORDING) {
//          if(dataString[num] == null) dataString[num] = "0.00";
//          else dataString[num] += "\n" + "0.00";
//        }
      }

    }
  }

  private List<Float> medianFilter(List<Float> input, int size) {
    List<Float> res = new ArrayList<>();
    for(int i = 0; i < input.size(); ++i) {
      int half = size / 2;
      int start = Math.max(0, i - half);
      int end = Math.min(input.size() - 1, i + half);

      List<Float> subList = input.subList(start, end + 1);
      subList.sort(Comparator.naturalOrder());

      res.add(subList.get(subList.size() / 2));
    }
    return res;
  }

  private List<Integer> roundList(List<Float> input) {
    List<Integer> res = new ArrayList<>();
    for(float i : input) {
      res.add(Math.round(i));
    }
    return res;
  }

  private int increasing(List<Integer> input) {
    int cnt = 1;
    for(int i = 0; i < input.size() - 1; ++i) {
      if(input.get(i + 1) < input.get(i))
        return -1;
      if(input.get(i + 1) > input.get(i))
        cnt++;
    }
    return cnt;
  }

  private Floor classify(List<Float> input) {
    input = averageList(input);
    double slope = Util.LinearSlope(input);

    if(slope > slopeThreshold)
      return Floor.OBSTACLE;
    else if(input.get(0) - input.get(input.size() - 1) > 0.2f)
      return Floor.DOWN;
    return Floor.PLANE;
  }

  private List<Float> averageList(List<Float> input) {
    List<Float> res = new ArrayList<>();
    float sum = 0;

    for(int i = 0; i < AVERAGE_SIZE; ++i) {
      sum += input.get(i);
    }
    res.add(sum / AVERAGE_SIZE);

    for(int i = AVERAGE_SIZE; i < input.size(); ++i) {
      sum -= input.get(i - AVERAGE_SIZE);
      sum += input.get(i);
      res.add(sum / AVERAGE_SIZE);
    }

    return res;
  }

  private int classifyFragment(List<Float> dataFragment) {
    double slope = Math.abs(Util.LinearSlope(dataFragment));

    if (slope > wallThr) {
      return 2; // wall
    } else if (slope > threshold) {
      return 1; // obstacle or stair
    } else {
      return 0;
    }
  }

  public void alarmObject(boolean danger) {
    state.setDangerous(danger);
  }

  private Floor classifyState() {
    List<Float> avgData = averageList(data);
    List<Float> fragment;
    List<Integer> decisionByte = new ArrayList<>();

    if (Util.findRepresentativeValue(avgData) < -0.2f) {
      return Floor.DOWN;
    }

    for(int i = 0; i < FRAME_DECISION_NUM / FRAGMENT_SIZE; i++) {
      fragment = new ArrayList<>(avgData.subList(i * FRAGMENT_SIZE, (i + 1) * FRAGMENT_SIZE));
      decisionByte.add(classifyFragment(fragment));
    }

    if(decisionByte.get(0) == 0) return Floor.PLANE;

    int prev = 1;
    int bitChange = 0, wallCount = 0;
    for(int j = 0; j < decisionByte.size(); j++) {
      int now = decisionByte.get(j);
      if (now == 2) {
        wallCount++;
      }

      if((prev != 0 && now == 0) || (prev == 0 && now != 0)) {
        bitChange++;
      }
      prev = now;
    }

    if (wallCount >= 2) {
      return Floor.WALL;
    } else if (wallCount == 0 && bitChange >= 3) {
      return Floor.UP;
    }

    return Floor.OBSTACLE;
  }
}
