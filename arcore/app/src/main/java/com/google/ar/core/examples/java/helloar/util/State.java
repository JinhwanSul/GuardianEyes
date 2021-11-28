package com.google.ar.core.examples.java.helloar.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.ar.core.examples.java.helloar.HelloArActivity;

import java.util.Locale;

enum Floor{
    PLANE, WALL, DOWN, UP, OBSTACLE;
}

public class State {
    private Floor wallstate;
    private boolean dangerous;

    private TextToSpeech tts;
    private HelloArActivity context;
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
        tts.setSpeechRate(1.0f);
    }

    public void setWallstate(Floor state){
        if(!wallstate.equals(state)){
            feedbackWall(state);
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
                    tts.speak("올라가는 계단이 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
                }
                break;
            case DOWN:
                if(!tts.isSpeaking()) {
                    tts.speak("내려가는 계단이 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
                }
                break;
            case WALL:
                if(!tts.isSpeaking()) {
                    tts.speak("벽이 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
                }
                break;
            case OBSTACLE:
                if(!tts.isSpeaking()) {
                    tts.speak("장애물이 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
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
                tts.speak("물체가 다가오고 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }
}
