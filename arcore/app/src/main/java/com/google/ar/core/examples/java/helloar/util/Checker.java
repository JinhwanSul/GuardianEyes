package com.google.ar.core.examples.java.helloar.util;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Trackable;
import com.google.ar.core.examples.java.helloar.HelloArActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
  private final int FRAME_DECISION_NUM = 30;
  private final int FRAGMENT_SIZE = 3;
  private final int AVERAGE_SIZE = 5;

  private int frame_count = 0;
  private final int DISCARD_FRAME_NUM = 100;

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
            avgHeight = (avgHeight * frameNum + res) / (frameNum + 1);
            context.avgHeightTextView.setText("Average height : " + avgHeight + "m");
          }
          else if(frameNum >= averageCalculationFrameNum) {
//            if(num == 0) context.textView.setText("Height difference : " + res + "m"); // 중점의 경우를 화면에 출력

            // TODO: Implement feedback of floor detection
            if(state.tts.isSpeaking()) {
              data.clear();
            } else {
              data.add(res - avgHeight);

              if(data.size() > FRAME_DECISION_NUM + AVERAGE_SIZE - 1) {
                data.remove(0);
                Floor st = classifyState();
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
        if(num == 0) context.textView.setText("can't find proper surface");

//        if(START_RECORDING) {
//          if(dataString[num] == null) dataString[num] = "0.00";
//          else dataString[num] += "\n" + "0.00";
//        }
      }

    }
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

    context.textView.setText(decisionByte.toString());

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
