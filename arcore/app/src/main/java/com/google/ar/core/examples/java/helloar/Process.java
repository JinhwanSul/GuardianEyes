package com.google.ar.core.examples.java.helloar;

public class Process {
    public static native int Sort(); // do SORT algorithm. Return 1 for first frame, return 0 for others.

    public static native void initializeData(); // initialize data. This should be called before setData.

    public static void setData(String title, float x, float y, float width, float height) { // set detection data. should call (number of detection box) times.
        setTitle(title);
        setBox(x, y, width, height);
    }
    private static native void setTitle(String title); // set Title
    private static native void setBox(float x, float y, float width, float height); // set box data.

    public static native float[] getData(int index); // get result data. return Float array [x, y, width, height, id, frame_count]
    public static native String getTrackingTitle(int index); // get title.
}





