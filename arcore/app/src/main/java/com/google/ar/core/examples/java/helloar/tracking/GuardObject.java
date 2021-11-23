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

//  public GuardObject(float x, float y, float z) {
//    this.x = x;
//    this.y = y;
//    this.z = z;
//  }
  public GuardObject(int unit) {
    x = new ArrayList<>();
    y = new ArrayList<>();
    z = new ArrayList<>();
//    dx = new ArrayList<>();
//    dy = new ArrayList<>();
//    dz = new ArrayList<>();
    count = 0;
    this.unit = unit;
  }

  public void update(float x, float y, float z) {
//    if(count > 0) {
//      dx.add(x * 100 - this.x.get(this.x.size() - 1));
//      dy.add(y * 100 - this.y.get(this.y.size() - 1));
//      dz.add(z * 100 - this.z.get(this.z.size() - 1));
//    }
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
    if(count < 2) return 0f;

    int idx = 0; // TODO: need change
//    float cosTheta = (x.get(idx) * dx.get(idx) + y.get(idx) * dy.get(idx) + z.get(idx) * dz.get(idx)) / (distance() * speed());

    float dx = x.get(x.size() - 1) - x.get(idx);
//    float dy = y.get(idx + 1) - y.get(idx);
    float dz = z.get(z.size() - 1) - z.get(idx);

    float cosTheta = (x.get(idx) * dx + z.get(idx) * dz) / (speed() * distance());
//    return (float) Math.acos(cosTheta);
    return cosTheta;
  }

  public float distance() { // (x, y, z)의 크기
    if(count < 1) return 0f;

    int idx = 0; // TODO: need change

//    return (float) Math.sqrt(x.get(idx) * x.get(idx) + y.get(idx) * y.get(idx) + z.get(idx) * z.get(idx));
    return (float) Math.sqrt(x.get(idx) * x.get(idx) + z.get(idx) * z.get(idx));
  }

  public float speed() { // (dx, dy, dz)의 크기
    if(count < 2) return 0f;

    int idx = 0; // TODO: need change

    float dx = x.get(x.size() - 1) - x.get(idx);
//    float dy = y.get(idx + 1) - y.get(idx);
    float dz = z.get(z.size() - 1) - z.get(idx);

//    return (float) Math.sqrt(dx.get(idx) * dx.get(idx) + dy.get(idx) * dy.get(idx) + dz.get(idx) * dz.get(idx));
    return (float) Math.sqrt(dx * dx + dz * dz);
  }

  public void clear() {
    x.clear();
    y.clear();
    z.clear();
//    dx.clear();
//    dy.clear();
//    dz.clear();
    count = 0;
  }
}
