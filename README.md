# GuardianEyes

A GuardianEyes app helps the walking amblyopia. It can detect object using tensorflow lite and measure distance using ARCore. Also, it detects the topography of the floor and distinguishes between walls and stairs.

가디언 아이즈 앱은 약시의 보행을 도와줍니다. 이 앱은 텐서 플로우 라이트 모델을 통해 물체를 감지하고 그것과의 거리를 ARCore를 통해 측정합니다. 또한 그것은 바닥의 지형을 감지하여 벽과 계단을 구분합니다.

(앱 데모 사진)

## Getting Started
To install this app, you'll need the folling:
- Android device over 29
- Android Studio 4.1 or later

## Tensorflow lite model
We use this model
(link)
It can classfy a lot of object and detect it.

## ARCore
ARCore helps us that make 3D coordinate of the world. We can get the mobile phone and detected object's 3D coordinate. We can calculted between camera and object relative speed and height difference.

## Ultra sound sensor
The ultrasonic sensor detects an object approaching from the angle of the camera.
