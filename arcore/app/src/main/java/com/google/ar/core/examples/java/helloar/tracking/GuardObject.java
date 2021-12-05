package com.google.ar.core.examples.java.helloar.tracking;

import android.util.Log;

import com.google.ar.core.examples.java.helloar.util.Util;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

public class GuardObject {
//  private float x, y, z, dx, dy ,dz;
  public List<Float> x, y, z;
//  dx, dy, dz;
  private int count;
  private int unit;
  private int soundId;
  final private int FRAME_INTERVAL=15;

  private String info = "Collecting info";
  private int red = 0x00, green = 0x00, blue = 0x00;

  public void setInfo(String str, int r, int g, int b) {
    info = str;
    red = r;
    green = g;
    blue = b;
  }

  public void setSndId(int num) { soundId = num; }

  public int getSndId() { return soundId; }

  public String getInfo() {
    return info;
  }

  public int getRed() {
    return red;
  }

  public int getGreen() {
    return green;
  }

  public int getBlue() {
    return blue;
  }

  public GuardObject(int unit) {
    x = new ArrayList<>();
    y = new ArrayList<>();
    z = new ArrayList<>();
    count = 0;
    this.unit = unit;
  }

  public void update(float x, float y, float z) {
    this.x.add(x * 100);
    this.y.add(y * 100);
    this.z.add(z * 100);
    count++;
  }

  public void filter() {
    this.x = Util.arrangePoints(this.x);
    this.y = Util.arrangePoints(this.y);
    this.z = Util.arrangePoints(this.z);
  }

  public float angle() {

    if(count < FRAME_INTERVAL*2) return 0f;

    float x1 = 0f;
    float y1 = 0f;
    float z1 = 0f;
    float x2 = 0f;
    float y2 = 0f;
    float z2 = 0f;

    for(int i = count - 1; i > count - 16; i--) {
      x2 += x.get(i);
      y2 += y.get(i);
      z2 += z.get(i);
    }

    for(int i = count - 16; i > count - 31; i--) {
      x1 += x.get(i);
      y1 += y.get(i);
      z1 += z.get(i);
    }

    x2 /= FRAME_INTERVAL;
    y2 /= FRAME_INTERVAL;
    z2 /= FRAME_INTERVAL;
    x1 /= FRAME_INTERVAL;
    y1 /= FRAME_INTERVAL;
    z1 /= FRAME_INTERVAL;

    float dx = x2 - x1;
    float dz = z2 - z1;

    float cosTheta = (x2 * dx + z2 * dz) / (speed() * distance());
    return cosTheta;
  }

  public float distance() { // (x, y, z)의 크기
    if(count < FRAME_INTERVAL) return 0f;

    float x1 = 0f;
    float y1 = 0f;
    float z1 = 0f;
    for(int i = count - 1; i > count - 16; i--) {
      x1 += x.get(i);
      y1 += y.get(i);
      z1 += z.get(i);
    }
    x1 /= FRAME_INTERVAL;
    y1 /= FRAME_INTERVAL;
    z1 /= FRAME_INTERVAL;
    return (float) Math.sqrt(x1*x1+z1*z1);
  }

  public float speed() { // (dx, dy, dz)의 크기
    if(count < FRAME_INTERVAL*2) return 0f;

    float x1 = 0f;
    float y1 = 0f;
    float z1 = 0f;
    float x2 = 0f;
    float y2 = 0f;
    float z2 = 0f;

    for(int i = count - 1; i > count - 16; i--) {
      x2 += x.get(i);
      y2 += y.get(i);
      z2 += z.get(i);
    }

    for(int i = count - 16; i > count - 31; i--) {
      x1 += x.get(i);
      y1 += y.get(i);
      z1 += z.get(i);
    }

    x2 /= FRAME_INTERVAL;
    y2 /= FRAME_INTERVAL;
    z2 /= FRAME_INTERVAL;
    x1 /= FRAME_INTERVAL;
    y1 /= FRAME_INTERVAL;
    z1 /= FRAME_INTERVAL;

    float dx = x2 - x1;
    float dz = z2 - z1;

    return (float) Math.sqrt(dx * dx + dz * dz);
  }

  public void clear() {
    if(count < FRAME_INTERVAL*2) return;
    for(int i = 0; i < count - FRAME_INTERVAL; i++) {
      x.remove(0);
      y.remove(0);
      z.remove(0);
    }
    count = FRAME_INTERVAL;
  }
}
