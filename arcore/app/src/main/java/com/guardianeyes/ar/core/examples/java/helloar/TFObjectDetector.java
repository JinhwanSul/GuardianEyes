package com.guardianeyes.ar.core.examples.java.helloar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TFObjectDetector {

    private static final int TF_OD_API_INPUT_SIZE = 500;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detectSample.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
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
            Log.e("asdf", "Exception initializing Detector!");
        }
    }

    public List<Detector.Recognition> getResults(Bitmap bitmap) {

        Bitmap croppedBitmap = Bitmap.createScaledBitmap(bitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, true);

        final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;

        final List<Detector.Recognition> mappedRecognitions =
                new ArrayList<Detector.Recognition>();

        // Temporary for 1 box!
        RectF rectFBoundingBox = null;
        Detector.Recognition recognition = null;

        for (final Detector.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                mappedRecognitions.add(result);

                recognition = result;
                rectFBoundingBox = result.getLocation();
                if(rectFBoundingBox.right < 0.0f) {
                    rectFBoundingBox.right = 0.0f;
                }
                if(rectFBoundingBox.left < 0.0f) {
                    rectFBoundingBox.left = 0.0f;
                }
                if(rectFBoundingBox.top < 0.0f) {
                    rectFBoundingBox.top = 0.0f;
                }
                if(rectFBoundingBox.bottom < 0.0f) {
                    rectFBoundingBox.bottom = 0.0f;
                }
            }
        }

        if (recognition != null && rectFBoundingBox != null) {
            HelloArActivity.objRect = rectFBoundingBox;
            HelloArActivity.coor = recognition.getCenterCoordinate();
        }

        return mappedRecognitions;
    }
}
