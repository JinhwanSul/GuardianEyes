package com.google.ar.core.examples.java.helloar;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.PrecomputedText;
import android.util.Log;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.ArrayList;
import java.util.List;

public class MyObjectdetector {

    private LocalModel localModel;
    private CustomObjectDetectorOptions customObjectDetectorOptions;
    private ObjectDetector objectDetector;

    MyObjectdetector () {
        localModel = new LocalModel.Builder()
                .setAssetFilePath("detectSample.tflite")
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

     public List<Recognition> getResults (InputImage image ) {
        List<Recognition> recList = new ArrayList<Recognition>() {};
       Log.d("asdf", "before processing");
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
