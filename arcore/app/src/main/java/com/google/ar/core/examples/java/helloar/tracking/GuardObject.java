package com.google.ar.core.examples.java.helloar.tracking;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

public class GuardObject {
//  private float x, y, z, dx, dy ,dz;
  private List<Float> x, y, z, dx, dy, dz;
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
    dx = new ArrayList<>();
    dy = new ArrayList<>();
    dz = new ArrayList<>();
    count = 0;
    this.unit = unit;
  }

  public void update(float x, float y, float z) {
    if(count > 0) {
      dx.add(x - this.x.get(this.x.size() - 1));
      dy.add(y - this.y.get(this.y.size() - 1));
      dz.add(z - this.z.get(this.z.size() - 1));
    }
    this.x.add(x);
    this.y.add(y);
    this.z.add(z);

    count++;
  }

  public float angle() {
    if(count < unit) return 0f;

    int idx = 0; // TODO: need change
//    float cosTheta = (x.get(idx) * dx.get(idx) + y.get(idx) * dy.get(idx) + z.get(idx) * dz.get(idx)) / (distance() * speed());
    float cosTheta = (x.get(idx) * dx.get(idx) + z.get(idx) * dz.get(idx)) / (distance() * speed());
    return (float) Math.acos(cosTheta);
  }

  public float distance() { // (x, y, z)의 크기
    if(count < unit) return 0f;

    int idx = 0; // TODO: need change
//    return (float) Math.sqrt(x.get(idx) * x.get(idx) + y.get(idx) * y.get(idx) + z.get(idx) * z.get(idx));
    return (float) Math.sqrt(x.get(idx) * x.get(idx) + z.get(idx) * z.get(idx));
  }

  public float speed() { // (dx, dy, dz)의 크기
    if(count < unit) return 0f;

    int idx = 0; // TODO: need change
//    return (float) Math.sqrt(dx.get(idx) * dx.get(idx) + dy.get(idx) * dy.get(idx) + dz.get(idx) * dz.get(idx));
    return (float) Math.sqrt(dx.get(idx) * dx.get(idx) + dz.get(idx) * dz.get(idx));
  }

  public void clear() {
    x.clear();
    y.clear();
    z.clear();
    dx.clear();
    dy.clear();
    dz.clear();
    count = 0;
  }
}
