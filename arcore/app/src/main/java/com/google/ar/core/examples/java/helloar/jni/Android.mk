LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#opencv library
OPENCVROOT:= /Users/jeff/GuardianEyes/arcore/opencv
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/native/jni/OpenCV.mk


LOCAL_MODULE    := native-lib
LOCAL_SRC_FILES := main.cpp Hungarian.cpp KalmanTracker.cpp
LOCAL_LDLIBS += -llog

include $(BUILD_SHARED_LIBRARY)