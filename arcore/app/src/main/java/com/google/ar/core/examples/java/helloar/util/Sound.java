package com.google.ar.core.examples.java.helloar.util;

import static com.google.vr.sdk.audio.GvrAudioEngine.MaterialName.CURTAIN_HEAVY;
import static com.google.vr.sdk.audio.GvrAudioEngine.MaterialName.PLASTER_SMOOTH;

import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.helloar.HelloArActivity;
import com.google.vr.sdk.audio.GvrAudioEngine;

public class Sound {

  private String SOUND_FILE = "beep_01.wav";
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

  public void make3Dsound(Pose pos, int num){
    //this is sound module, what function take this module?
    int soundId;

    switch(num) {
      case 1:
        soundId = mGvrAudioEngine.createSoundObject(SOUND_FILE);
        break;
      default:
        soundId = mGvrAudioEngine.createSoundObject(SOUND_FILE2);
    }

    float[] translation = new float[3];
    pos.getTranslation(translation, 0);
    mGvrAudioEngine.setSoundObjectPosition(soundId, translation[0], translation[1], translation[2]);
    mGvrAudioEngine.playSound(soundId,false); //loop playback
    mGvrAudioEngine.setSoundObjectDistanceRolloffModel(soundId, GvrAudioEngine.DistanceRolloffModel.LOGARITHMIC,0,4);
  }
}
