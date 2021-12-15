package com.snu.mobile.computing.guardianeyes.java.main.util;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import com.snu.mobile.computing.guardianeyes.java.main.HelloArActivity;

import java.util.Locale;

enum Floor{
    PLANE, WALL, DOWN, UP, OBSTACLE;
}

public class State {
    private Floor wallstate;
    private boolean dangerous;

    public TextToSpeech tts;
    private HelloArActivity context;
    private Bundle ttsBundle;

    public State(HelloArActivity context){
        wallstate = Floor.PLANE;
        dangerous = false;
        this.context = context;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });
        tts.setPitch(1.0f);
        tts.setSpeechRate(0.8f);

        ttsBundle = new Bundle();
        ttsBundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.8f);
    }

    public void setWallstate(Floor state){
        if(!wallstate.equals(state)){
            feedbackWall(state);
            context.textView.setText(state.name());
        }
        wallstate = state;
    }

    public void setDangerous(boolean danger){
        if(!dangerous && danger){
            feedbackDangerouse(danger);
        }
        dangerous = danger;
    }

    public void feedbackWall(Floor state){
        switch(state) {
            case UP:
                if(!tts.isSpeaking()) {
                    tts.speak("전방에 올라가는 길이 있습니다.", TextToSpeech.QUEUE_FLUSH, ttsBundle, null);
                }
                break;
            case DOWN:
                if(!tts.isSpeaking()) {
                    tts.speak("전방에 내려가는 길이 있습니다.", TextToSpeech.QUEUE_FLUSH, ttsBundle, null);
                }
                break;
            case WALL:
                if(!tts.isSpeaking()) {
                    tts.speak("전방에 벽이 있습니다.", TextToSpeech.QUEUE_FLUSH, ttsBundle, null);
                }
                break;
            case OBSTACLE:
                if(!tts.isSpeaking()) {
                    tts.speak("장애물이 있습니다. 조심하세요.", TextToSpeech.QUEUE_FLUSH, ttsBundle, null);
                }
                break;
            case PLANE:
                break;
            default:
                break;
        }
    }

    public void feedbackDangerouse(boolean danger){
        if (danger){
            //feedback danger state
            if(!tts.isSpeaking()) {
//                tts.speak("물체가 다가오고 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }
}
