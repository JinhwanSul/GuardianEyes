# GuardianEyes

A GuardianEyes app helps the walking amblyopia. It can detect objects using ssd-MobileNet-v.1.   and measure distance using ARCore. Also, it detects the topography of the floor and distinguishes between walls and stairs.

GuardianEyes 앱은 약시와 시각장애인의 보행을 도와주는 앱입니다. GuardianEyes는 휴대폰의 카메라를 이용하여 사용자의 주변 상황을 전달하고, 전방의 지면 상황을 감지하여 사용자에게 실시간 피드백을 제공합니다.

GuardianEyes는 크게 세가지 기능으로 구성되어있습니다.
1. Object detection & tracking
2. Floor detection
3. Blind spot approaching detection with sonar sensor

## GuardianEyes system configuration
### 1. Object detection & tracking
Object detection & tracking은 사용자 주변의 물체를 감지하고, 해당 물체를 tracking하여 이에 대한 3d sound feedback을 제공합니다. 즉, 물체가 가까워질수록 더 큰 소리로 사용자에게 feedback을 제공하므로서 생생한 상황을 전달합니다.

Object detection은 tensorflow의 open 모델을 이용하였으며[[link]](https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/2), tracking은 Simple Online and Real-time Tracking algorithm[[link]](https://arxiv.org/pdf/1602.00763.pdf) 을 이용하였습니다.
물체를 감지하고 해당 물체를 추적하여 3d sound feedback을 제공합니다. 3d sound feedback은 google gvr engine을 이용해서 [[link]](https://developers.google.com/vr/reference/android/com/google/vr/sdk/audio/GvrAudioEngine) 구현하였습니다.

### 2. Floor detection
Floor Detection은 바닥의 변화를 감지하는 기능입니다. 크게 obstacle, plane, 그리고 hole을 구분하는데, 이는 Google AR Core의 real-world 3D 좌표를 이용해 구분합니다. [[link]](https://developers.google.com/ar) 바닥의 경우는 좌표의 y값이 크게 변하지 않는데, obstacle의 경우 y좌표의 변화량이 일정 threshold 이상의 양의 값으로 나타나기 때문의 이를 구분할 수 있다. Hole의 경우 y값이 갑자기 떨어지는 현상을 보이기 때문에 마찬가지로 이를 구분할 수 있습니다. Obstacle과 Hole의 경우 TTS를 사용해 사용자에게 피드백을 줍니다.


### 3. Blind spot approaching detection with sonar sensor
GuardianEyes는 카메라를 이용하기 때문에 카메라 시야 밖의 위험에 대한 피드백의 경우 다른 장비를 추가적으로 필요합니다.
이를 위해서 ESP32 아두이노 칩과 초음파 센서를 이용하여 카메라 밖에서 가까이 접근한 물체에 대한 피드백을 진동으로 전달합니다.

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
