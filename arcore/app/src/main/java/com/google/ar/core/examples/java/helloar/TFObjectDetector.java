package com.google.ar.core.examples.java.helloar;

import static java.lang.Float.max;
import static java.lang.Float.min;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TFObjectDetector {

    private static final int TF_OD_API_INPUT_SIZE = 500;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detectSample.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
    private static final ArrayList<String> USEFUL_LABELS = new ArrayList<String>(
            Arrays.asList("person", "bicycle", "car", "motorcycle", "bus", "truck", "cat", "dog", "chair", "dining table"));
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    private Detector detector;
    int cropSize;
    TFObjectDetector (Context context) {
        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                    context,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED
            );
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            Log.e("jinhwan", "Exception initializing Detector!");
        }
    }

    public List<Detector.Recognition> getResults(Bitmap bitmap) {

        Bitmap croppedBitmap = Bitmap.createScaledBitmap(bitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, true);

        final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;

        List<Detector.Recognition> mappedRecognitions =
                new ArrayList<Detector.Recognition>();

        mappedRecognitions = filterResults(results);

        return mappedRecognitions;
    }


    private boolean isUsefulObject(String label) {
        return USEFUL_LABELS.contains(label);
    }

    public float rectArea(RectF rect) {
        Log.d("jinhwan", "top:"+rect.top+" bottom:"+rect.bottom);
        return ((rect.right - rect.left) * (rect.bottom - rect.top));
    }

    private boolean closelyIntersect(RectF rect1, RectF rect2) {
        if (!rect1.intersect(rect2)) {
            return false;
        }

        Float area1 = rectArea(rect1);
        Float area2 = rectArea(rect2);
        Float intersectThreshold = 0.7f;
        Float intersectLeft = max(rect1.left, rect2.left);
        Float intersectRight = min(rect1.right, rect2.right);
        Float intersectBottom = min(rect1.bottom, rect2.bottom);
        Float intersectTop = max(rect1.top, rect2.top);

        Float intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop);
        if (intersectArea > intersectThreshold * area1 || intersectArea > intersectThreshold * area2) {
            return true;
        }
        return false;
    }

    private List<Detector.Recognition> filterResults(List<Detector.Recognition> results) {
        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        List<Detector.Recognition> filterdResults = new ArrayList<Detector.Recognition>();
        int rawdetected = results.size();
        int selected = 0;
        boolean flagSameObj;
        for (final Detector.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence && isUsefulObject(result.getTitle())) {
                flagSameObj = false;
                for(int i = 0; i < selected; i++) {
                    if (filterdResults.get(i).getTitle().equals(result.getTitle())) {
                        if (closelyIntersect(filterdResults.get(i).getLocation(), result.getLocation())) {
                            if(filterdResults.get(i).getConfidence() > result.getConfidence()) {
                                flagSameObj = true;
                                break;
                            } else {
                                filterdResults.remove(i);
                                selected = selected - 1;
                                break;
                            }
                        }
                    }
                }
                if (!flagSameObj) {
                    filterdResults.add(result);
                    selected = selected + 1;
                }
            }
        }

        return filterdResults;
    }
}
