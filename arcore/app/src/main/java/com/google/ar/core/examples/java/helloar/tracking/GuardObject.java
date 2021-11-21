package com.google.ar.core.examples.java.helloar.tracking;

public class GuardObject {
  private float x, y, z, dx, dy ,dz;

  public GuardObject(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public void update(float x, float y, float z) {
    dx = x - this.x;
    dy = y - this.y;
    dz = z - this.z;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public float angle() {
    float cosTheta = (x * dx + y * dy + z * dz) / ((float) distance() * speed());
    return (float) Math.acos(cosTheta);
  }

  public float distance() {
    return (float) Math.sqrt(x*x+y*y+z*z);
  }

  public float speed() {
    return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
  }
}
