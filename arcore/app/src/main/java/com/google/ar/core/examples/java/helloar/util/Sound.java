package com.google.ar.core.examples.java.helloar.util;

import static com.google.vr.sdk.audio.GvrAudioEngine.MaterialName.CURTAIN_HEAVY;
import static com.google.vr.sdk.audio.GvrAudioEngine.MaterialName.PLASTER_SMOOTH;

import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.helloar.HelloArActivity;
import com.google.vr.sdk.audio.GvrAudioEngine;

public class Sound {

  private String SOUND_FILE = "interval_beep.wav";
  private String SOUND_FILE2 = "beep_03.wav";

  public GvrAudioEngine mGvrAudioEngine;

  public Sound(HelloArActivity context) {
    mGvrAudioEngine = new GvrAudioEngine(context, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
    new Thread(
      () -> {
        //Cf. https://developers.google.com/vr/android/reference/com/google/vr/sdk/audio/GvrAudioEngine
        mGvrAudioEngine.preloadSoundFile(SOUND_FILE);
        mGvrAudioEngine.preloadSoundFile(SOUND_FILE2);
        mGvrAudioEngine.setRoomProperties(15, 15, 15, PLASTER_SMOOTH, PLASTER_SMOOTH, CURTAIN_HEAVY);
    }).start();
  }


  final private float amplitude = 1.0f;

  public int make3Dsound(float x, float y, float z, int num){
    //this is sound module, what function take this module?
    int soundId;

    switch(num) {
      case 1:
        soundId = mGvrAudioEngine.createSoundObject(SOUND_FILE);
        break;
      default:
        soundId = mGvrAudioEngine.createSoundObject(SOUND_FILE2);
    }

    mGvrAudioEngine.setSoundObjectPosition(soundId, x*amplitude, y*amplitude, z*amplitude);
    mGvrAudioEngine.setSoundObjectDistanceRolloffModel(soundId, GvrAudioEngine.DistanceRolloffModel.LOGARITHMIC,0,4);
    mGvrAudioEngine.playSound(soundId,true); //loop playback
    mGvrAudioEngine.setSoundVolume(soundId, 2.0f);
    return soundId;
  }

  public void updatePos(int num, float x, float y, float z) {
    mGvrAudioEngine.setSoundObjectPosition(num, x*amplitude, y*amplitude, z*amplitude);
  }

  public void stop3DSound(int num) {
    mGvrAudioEngine.stopSound(num);
  }

  public void update() {
    mGvrAudioEngine.update();
  }
}
