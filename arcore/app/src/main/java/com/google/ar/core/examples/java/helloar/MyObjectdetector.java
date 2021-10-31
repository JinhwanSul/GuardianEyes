package com.google.ar.core.examples.java.helloar;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.PrecomputedText;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.ArrayList;
import java.util.List;

public class MyObjectdetector {

    private static LocalModel localModel;
    private static CustomObjectDetectorOptions customObjectDetectorOptions;
    private static ObjectDetector objectDetector;

    MyObjectdetector () {
        localModel = new LocalModel.Builder()
                .setAssetFilePath("detect.tflite")
                .build();
        // Live detection and tracking
        customObjectDetectorOptions = new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(3)
                        .build();

        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
    }

     public static List<Recognition> getResults (InputImage image ) {
        List<Recognition> recList = new ArrayList<Recognition>() {};
        objectDetector
                 .process(image)
                 .addOnFailureListener(e -> {
                     e.printStackTrace();
                 })
                 .addOnSuccessListener(results -> {
                     for (DetectedObject detectedObject : results) {
                         Rect boundingBox = detectedObject.getBoundingBox();
                         Integer trackingId = detectedObject.getTrackingId();
                         for (DetectedObject.Label label : detectedObject.getLabels()) {
                             String text = label.getText();
                             Integer index = label.getIndex();
                             Float confidence = label.getConfidence();
                             RectF rectFBoundingBox = new RectF(boundingBox);
                             Recognition recognition = new Recognition(index.toString(), text, confidence, rectFBoundingBox);
                             recList.add(recognition);
                         }
                     }
                 });

        return recList;
     }
}
