package com.google.ar.core.examples.java.helloar;
import org.opencv.core.Rect;

public class Process {
    public static native int Sort(); // do SORT algorithm. Return 1 for first frame, return 0 for others.

    public static native void initializeData(); // initialize data. This should be called before setData.

    public static native void setData(float x, float y, float width, float height); // set data.

    public static native float[] getData(int index); // get result data. return Float array [x, y, width, height, id, frame_count]
}




