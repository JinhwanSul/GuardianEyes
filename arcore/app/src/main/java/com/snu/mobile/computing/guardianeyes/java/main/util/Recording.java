package com.snu.mobile.computing.guardianeyes.java.main.util;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.core.PlaybackStatus;
import com.google.ar.core.RecordingConfig;
import com.google.ar.core.RecordingStatus;
import com.google.ar.core.Track;
import com.snu.mobile.computing.guardianeyes.java.common.helpers.SnackbarHelper;
import com.snu.mobile.computing.guardianeyes.java.main.HelloArActivity;
import com.google.ar.core.exceptions.PlaybackFailedException;
import com.google.ar.core.exceptions.RecordingFailedException;

import org.joda.time.DateTime;

import java.io.File;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;


public class Recording {

  private static final String TAG = Recording.class.getSimpleName();

  private static final String MP4_DATASET_FILENAME_TEMPLATE = "arcore-dataset-%s.mp4";
  private static final String MP4_DATASET_TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss";
  private static final UUID ANCHOR_TRACK_ID = UUID.fromString("a65e59fc-2e13-4607-b514-35302121c138");
  private static final String ANCHOR_TRACK_MIME_TYPE = "application/hello-recording-playback-anchor";

  private static final String DESIRED_DATASET_PATH_KEY = "desired_dataset_path_key";
  private static final String DESIRED_APP_STATE_KEY = "desired_app_state_key";
  private static final int PERMISSIONS_REQUEST_CODE = 0;

  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();

  public String playbackDatasetPath;
  public String lastRecordingDatasetPath;

  private File path;
  private HelloArActivity context;

  public final AtomicReference<AppState> currentState = new AtomicReference<>(AppState.IDLE);

  public enum AppState {
    IDLE,
    RECORDING,
    PLAYBACK
  }

  public Recording(HelloArActivity context) {
    this.context = context;
    this.path = context.getExternalFilesDir(null);
  }

  /** Generates a new MP4 dataset filename based on the current system time. */
  private String getNewMp4DatasetFilename() {
    return String.format(
            Locale.ENGLISH,
            MP4_DATASET_FILENAME_TEMPLATE,
            DateTime.now().toString(MP4_DATASET_TIMESTAMP_FORMAT));
  }

  /** Generates a new MP4 dataset path based on the current system time. */
  private String getNewDatasetPath(File baseDir) {
    if (baseDir == null) {
      return null;
    }
    return new File(baseDir, getNewMp4DatasetFilename()).getAbsolutePath();
  }

  public void startRecording() {
    try {
      lastRecordingDatasetPath = getNewDatasetPath(path);
      if (lastRecordingDatasetPath == null) {
        Log.d(TAG, "Failed to generate a MP4 dataset path for recording.");
        return;
      }

      Track anchorTrack =
              new Track(context.session).setId(ANCHOR_TRACK_ID).setMimeType(ANCHOR_TRACK_MIME_TYPE);

      context.session.startRecording(
              new RecordingConfig(context.session)
                      .setMp4DatasetUri(Uri.fromFile(new File(lastRecordingDatasetPath)))
                      .setAutoStopOnPause(false)
                      .addTrack(anchorTrack));
    } catch (RecordingFailedException e) {
      String errorMessage = "Failed to start recording. " + e;
      Log.e(TAG, errorMessage, e);
      messageSnackbarHelper.showError(context, errorMessage);
      return;
    }
    if (context.session.getRecordingStatus() != RecordingStatus.OK) {
      Log.d(TAG,
              "Failed to start recording, recording status is " + context.session.getRecordingStatus());
      return;
    }
    setStateAndUpdateUI(AppState.RECORDING);
  }

  public void stopRecording() {
    try {
      context.session.stopRecording();
    } catch (RecordingFailedException e) {
      String errorMessage = "Failed to stop recording. " + e;
      Log.e(TAG, errorMessage, e);
      messageSnackbarHelper.showError(context, errorMessage);
      return;
    }
    if (context.session.getRecordingStatus() == RecordingStatus.OK) {
//      Log.d(TAG, "Failed to stop recording, recording status is " + session.getRecordingStatus());
      return;
    }
    if (new File(lastRecordingDatasetPath).exists()) {
      playbackDatasetPath = lastRecordingDatasetPath;
      Log.d(TAG, "MP4 dataset has been saved at: " + playbackDatasetPath);
    } else {
      Log.d(TAG,
              "Recording failed. File " + lastRecordingDatasetPath + " wasn't created.");
    }
    setStateAndUpdateUI(AppState.IDLE);
  }

  private void setStateAndUpdateUI(AppState state) {
    currentState.set(state);
    context.updateUI();
  }

  /** Performs action when playback button is clicked. */
  public void startPlayback() {
    if (playbackDatasetPath == null) {
      return;
    }
    currentState.set(AppState.PLAYBACK);
    restartActivityWithIntentExtras();
  }

  /** Performs action when close_playback button is clicked. */
  public void stopPlayback() {
    currentState.set(AppState.IDLE);
    restartActivityWithIntentExtras();
  }

  private void restartActivityWithIntentExtras() {
    Intent intent = context.getIntent();
    Bundle bundle = new Bundle();
    bundle.putString(DESIRED_APP_STATE_KEY, currentState.get().name());
    bundle.putString(DESIRED_DATASET_PATH_KEY, playbackDatasetPath);
    intent.putExtras(bundle);
    context.finish();
    context.startActivity(intent);
  }

  public void setPlaybackDatasetPath() {
    if (context.session.getPlaybackStatus() == PlaybackStatus.OK) {
      Log.d(TAG, "Session is already playing back.");
      setStateAndUpdateUI(AppState.PLAYBACK);
      return;
    }
    if (playbackDatasetPath != null) {
      try {
        File fileCheck = new File(playbackDatasetPath);
        context.session.setPlaybackDatasetUri(Uri.fromFile(fileCheck));
      } catch (PlaybackFailedException e) {
        String errorMsg = "Failed to set playback MP4 dataset. " + e;
        Log.e(TAG, errorMsg, e);
        messageSnackbarHelper.showError(context, errorMsg);
        Log.d(TAG, "Setting app state to IDLE, as the playback is not in progress.");
        setStateAndUpdateUI(AppState.IDLE);
        return;
      }
      setStateAndUpdateUI(AppState.PLAYBACK);
    }
  }

  /** Checks the playback is in progress without issues. */
  public void checkPlaybackStatus() {
//    Log.d(TAG, "GuardianEyes "+ context.session.getPlaybackStatus());
    if ((context.session.getPlaybackStatus() != PlaybackStatus.OK)
            && (context.session.getPlaybackStatus() != PlaybackStatus.FINISHED)) {
      setStateAndUpdateUI(AppState.IDLE);
    }
  }

  public void loadInternalStateFromIntentExtras() {
    Intent intent = context.getIntent();
    if (intent == null) return;
    Bundle bundle = intent.getExtras();
    if(bundle == null) return;

    if (bundle.containsKey(DESIRED_DATASET_PATH_KEY)) {
      playbackDatasetPath = intent.getStringExtra(DESIRED_DATASET_PATH_KEY);
    }
    if (bundle.containsKey(DESIRED_APP_STATE_KEY)) {
      String state = intent.getStringExtra(DESIRED_APP_STATE_KEY);
      if (state != null) {
        switch (state) {
          case "PLAYBACK":
            currentState.set(AppState.PLAYBACK);
            break;
          case "IDLE":
            currentState.set(AppState.IDLE);
            break;
          case "RECORDING":
            currentState.set(AppState.RECORDING);
            break;
          default:
            break;
        }
      }
    }
  }

  public void exploreMP4() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("video/mp4");
    context.startActivityForResult(intent, 10);
  }

  public boolean changeViewMode() {
    context.depthSettings.setDepthColorVisualizationEnabled(!context.depthSettings.depthColorVisualizationEnabled());
    return true;
  }
}
