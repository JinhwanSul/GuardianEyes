# GuardianEyes

The GuardianEyes is a walking aid mobile application for a visually impaired person.
It perceives potential dangers near the user by mobile phone camera and provides live feedback.

GuardianEyes consists of three features.
1. Object detection & tracking
2. Floor detection
3. Blind spot detection with sonar sensor

## GuardianEyes system configuration
### 1. Object detection & tracking
The GuardianEyes detects objects near the user, tracks them, and provides 3d sound feedback, i.e., when the object approaches closer to the user, it generates louder sound feedback.

The object detection deep learning model is a tensorflow public model[[link]](https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/2), and the object tracking is implemented with Simple Online and Real-time Tracking algorithm[[link]](https://arxiv.org/pdf/1602.00763.pdf)
The 3d sound feedback is implemented using the Google gvr engine [[link]](https://developers.google.com/vr/reference/android/com/google/vr/sdk/audio/GvrAudioEngine)

### 2. Floor detection
The GuardianEyes can detect a significant difference in floor condition. It classifies floor conditions into Plane, Obstacle, and Hole. 
The floor detection is implemented with 3d real world coodinates obtained from the Google AR Core engine[[link]](https://developers.google.com/ar). 
The Plane state is when the y-coordinate is stationary, and the Obstacle state is when the y-coordinate difference is a positive value over certain threshold. On the other hand, the Hole state is when the y-coordinate difference is a negative value.
When the Obstacle or Hole state is detected, GuardianEyes gives speech feedback implemented with Text-To-Speech(TTS) engine. 

### 3. Blind spot approaching detection with sonar sensor
Object detection and tracking and Floot detection rely on a smartphone camera. GuardianEyes can prevent potential danger from approaching outside the camera angle, but it requires additional hardware. GuardianEyes provide compatible source code with ESP32 Arduino board with sonar sensor. If the sonar sensor detects something approaching outside the camera angle, GuardianEyes provides vibration feedback to the user. 

## Experiment Result
### Upstair test
| Evaluation metric  | Detect | Falas Positive | Accuracy(%)|
| :------------ |:---------------:| -----:|------:|
| Stationary object      | - | 4 |0|
| Moving object| 1        |   0 |100|
| Obstacle | 2        |    0 |100|
| Hole | -        |    0 |-|
| Total | 3        |   4 |43|

### Downstair test
| Evaluation metric  | Detect | Falas Positive | Accuracy(%)|
| :------------ |:---------------:| -----:|------:|
| Stationary object      | - | 4 |0|
| Moving object| 5        |   0 |100|
| Obstacle | -       |    2 |0|
| Hole | 2        |    0 |100|
| Total | 7        |   6 |54|

### Classroom test
| Evaluation metric  | Detect | Falas Positive | Accuracy(%)|
| :------------ |:---------------:| -----:|------:|
| Stationary object      | 3 | 0 |100|
| Moving object| -        |   0 |0|
| Obstacle | -        |    0 |0|
| Hole | -        |    0 |0|
| Total | 3        |   0 |100|

### 301 building 1st floor, real situation
| Evaluation metric  | Detect | Falas Positive | Accuracy(%)|
| :------------ |:---------------:| -----:|------:|
| Stationary object      | 3 | 3 |50|
| Moving object| 5      |   0 |100|
| Obstacle | 1       |    4 |20|
| Hole | -        |    2 |0|
| Total | 9      |  9 |50|

## Requirements
To install this app, you'll need the follwing:
- Android device over 29
- Android Studio 4.1 or later
