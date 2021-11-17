#include "com_google_ar_core_examples_java_helloar_Process.h"

#include <opencv2/opencv.hpp>
#include <jni.h>

using namespace cv;

extern "C" {
    JNIEXPORT void JNICALL Java_com_google_ar_core_examples_java_helloar_Process_ConvertRGBtoGray(JNIEnv *ENV, jclass instacne, jlong matAddrInput, jlong matAddrResult) {
        Mat &matInput = *(Mat *)matAddrInput;
        Mat &matResult = *(Mat *)matAddrResult;

        cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
    }
}