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
  private float avgHeight = 0, threshold = 0.2f;
  private final int averageCalculationFrameNum = 30;

  private HelloArActivity context;
  private String[] saveData;

  private List<Float> data;
  private final int FRAME_DECISION_NUM = 60;

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
//    saveData = new String[pointsY.length];
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
          float res = hit.getHitPose().ty() - camera.getPose().ty();
          int frameNum = context.frame_count - context.DISCARD_FRAME_NUM;

          if(frameNum > 0 && frameNum < averageCalculationFrameNum) { // frameCount 수만큼 평균 높이 구하기
            avgHeight = (avgHeight * frameNum + res) / (frameNum + 1);
            context.avgHeightTextView.setText("Average height : " + avgHeight + "m");
          }
          else {
            if(num == 0) context.textView.setText("Height difference : " + res + "m"); // 중점의 경우를 화면에 출력

            // TODO: Implement feedback of floor detection
            data.add(res - avgHeight);
            if(data.size() > FRAME_DECISION_NUM) {
              data.remove(0);
              Floor st = classifyState();
              Log.d("asdff", st.name());
              state.setWallstate(st);
            }

//            if(START_RECORDING) {
//              if(dataString[num] == null) {
//                dataString[num] = Float.toString(res);
//              } else {
//                dataString[num] += "\n" + res;
//              }
//            }
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

  private int classifyFragment(List<Float> dataFragment) {
    Log.d("state", "dataFragement"+dataFragment.toString());
    double slope = Math.abs(Util.LinearSlope(dataFragment));
    Log.d("state", "single slope"+String.valueOf(slope));
    if (slope > 0.1) {
      return 1; // single wall
    } else {
      return 0; // single plane
    }
  }

  private Floor classifyState() {
    int FRAGMENT_SIZE = 5;
    List<Float> fragment;
    List<Integer> dicisionByte = new ArrayList<>();
    Log.d("asdff", "Slope: " + String.valueOf(Util.findRepresentativeValue(data.subList(0,6))));
    if (Util.findRepresentativeValue(data.subList(0,6)) < -0.2f) {
      Log.d("state", "down ");
      return Floor.DOWN;
    }

    int prev = 0;
    for(int i = 0; i < FRAME_DECISION_NUM / FRAGMENT_SIZE; i++) {
      fragment = new ArrayList<>(data.subList(i * FRAGMENT_SIZE, (i + 1) * FRAGMENT_SIZE));

      if (i == 0 && classifyFragment(fragment) == 0) {
        Log.d("state", "plane");
        return Floor.PLANE;
      } else {
        prev = 1;
      }
      dicisionByte.add(classifyFragment(fragment));
    }

    int bitChange = 0;
    for(int j = 0; j < dicisionByte.size(); j++) {
      int now = dicisionByte.get(j);
      if( prev == now ) {
        dicisionByte.remove(j);
      } else {
        bitChange++;
      }
      prev = now;
    }
    Log.d("state", "bitchange: "+String.valueOf(bitChange));

    if (bitChange < 2) {
      return Floor.WALL;
    } else if (bitChange >= 2 && bitChange <= 5) {
      return Floor.OBSTACLE;
    } else if (bitChange > 6) {
      return Floor.UP;
    }
    return Floor.PLANE;
  }
}
