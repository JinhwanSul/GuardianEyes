package com.google.ar.core.examples.java.helloar.util;

import android.content.Context;
import android.os.Vibrator;

import com.google.ar.core.Camera;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Trackable;
import com.google.ar.core.examples.java.helloar.HelloArActivity;

import java.util.Arrays;
import java.util.List;

public class Checker {

  private float width, height;

  private float[] pointsX = {0.50f, 0.50f};
  private float[] pointsY = {0.50f, 0.75f};
  private String[] dataString = new String[pointsX.length];
  private float avgHeight = 0, threshold = 0.2f;
  private int frameCount = 30, curFrame = 0, discardFrame = 30;

  private HelloArActivity context;
  private String[] saveData;

  // set this flag in order to start recording.
  public boolean START_RECORDING = false;

  public Checker(HelloArActivity context) {
    this.context = context;
    this.saveData = new String[pointsX.length];
  }

  public String[] getSaveData() {
    String[] record = saveData.clone();
    saveData = new String[pointsY.length];
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

          if(curFrame < discardFrame) { // discardFrame 수만큼 첫 프레임 버리기
            curFrame++;
          }
          else if(curFrame - discardFrame < frameCount) { // frameCount 수만큼 평균 높이 구하기
            int frameNum = curFrame - discardFrame;
            avgHeight = (avgHeight * frameNum + res) / (frameNum + 1);
            curFrame++;

            context.avgHeightTextView.setText("Average height : " + avgHeight + "m");
          }
          else {
            if(num == 0) context.textView.setText("Height difference : " + res + "m"); // 중점의 경우를 화면에 출력

            // TODO: Implement feedback of floor detection
            Vibrator vi = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if(res < avgHeight - threshold) { // Downstair
              vi.vibrate(500);
            }
            else if(res > avgHeight + threshold) { // Wall
              // Wall
              vi.vibrate(100);
            }
            else if(true) { // Upstair
              // feedback
            }
            else if(true) { // Obstacle
              // feedback
            }

            if(START_RECORDING) {
              if(dataString[num] == null) {
                dataString[num] = Float.toString(res);
              } else {
                dataString[num] += "\n" + res;
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

        if(START_RECORDING) {
          if(dataString[num] == null) dataString[num] = "0.00";
          else dataString[num] += "\n" + "0.00";
        }
      }

    }
  }
}
