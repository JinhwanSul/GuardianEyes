
package com.google.ar.core.examples.java.helloar;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.Plane;
import com.google.ar.core.PlaybackStatus;
import com.google.ar.core.Point;
import com.google.ar.core.RecordingConfig;
import com.google.ar.core.RecordingStatus;
import com.google.ar.core.Session;
import com.google.ar.core.Track;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.GLError;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.PlaybackFailedException;
import com.google.ar.core.exceptions.RecordingFailedException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.joda.time.DateTime;
import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

//import com.google.mlkit.vision.common.InputImage;


public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {

  private static final String TAG = HelloArActivity.class.getSimpleName();

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

  private static final String MP4_DATASET_FILENAME_TEMPLATE = "arcore-dataset-%s.mp4";
  private static final String MP4_DATASET_TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss";

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

  private enum AppState {
    IDLE,
    RECORDING,
    PLAYBACK
  }

  // Randomly generated UUID and custom MIME type to mark the anchor track for this sample.
  private static final UUID ANCHOR_TRACK_ID =
          UUID.fromString("a65e59fc-2e13-4607-b514-35302121c138");
  private static final String ANCHOR_TRACK_MIME_TYPE =
          "application/hello-recording-playback-anchor";

  private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
  public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
  private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
  private final String ARDUINO_ADDRESS = "E8:D8:84:00:DB:4E";
  private final String ARDUINO_NAME = "ESP32_SuperSonic";
  private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

  private final AtomicReference<AppState> currentState = new AtomicReference<>(AppState.IDLE);

  private String playbackDatasetPath;
  private String lastRecordingDatasetPath;

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;
  private TextView textView;
  private TextView arduinoTextView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;
  private SampleRender render;

  private Button startRecordingButton;
  private Button stopRecordingButton;
  private Button startPlaybackButton;
  private Button stopPlaybackButton;
  private Button exploreButton;
  private TextView recordingPlaybackPathTextView;

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;

  private final DepthSettings depthSettings = new DepthSettings();

  // Box Shader
  private VertexBuffer boxVertexBuffer;
  private Mesh boxMesh;
  private Shader boxShader;

  private VertexBuffer boxTexVertexBuffer;
  private Mesh boxTexMesh;
  private Shader boxTexShader;

  // Point Cloud
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
  // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
  private long lastPointCloudTimestamp = 0;

  // Virtual object (ARCore pawn)
  private Mesh virtualObjectMesh;
  private Shader virtualObjectShader;

  // Environmental HDR
  private Texture dfgTexture;
  private SpecularCubemapFilter cubemapFilter;

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model

  //private MyObjectdetector myObjectdetector;
  private TFObjectDetector myObjectdetector;
  private YuvToRgbConverter yuvToRgbConverter;

  public static RectF objRect = new RectF(0,0,0,0);
  public static Pair<Float, Float> coor;

  private Handler mHandler; // Our main handler that will receive callback notifications

  private BluetoothAdapter mBTAdapter;
  private Set<BluetoothDevice> mPairedDevices;
  private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
  private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    loadInternalStateFromIntentExtras();

    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    textView = findViewById(R.id.text_view);
    arduinoTextView = findViewById(R.id.arduioTextView);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // Set up touch listener.
//    tapHelper = new TapHelper(/*context=*/ this);
//    surfaceView.setOnTouchListener(tapHelper);

    // Set up renderer.
    render = new SampleRender(surfaceView, this, getAssets());
    //myObjectdetector = new MyObjectdetector();
    myObjectdetector = new TFObjectDetector(this);
    yuvToRgbConverter = new YuvToRgbConverter(this);
    coor = null;

    installRequested = false;

    depthSettings.onCreate(this);

    startRecordingButton = (Button)findViewById(R.id.start_recording_button);
    stopRecordingButton = (Button)findViewById(R.id.stop_recording_button);
    startRecordingButton.setOnClickListener(view -> startRecording());
    stopRecordingButton.setOnClickListener(view -> stopRecording());

    startPlaybackButton = (Button)findViewById(R.id.playback_button);
    stopPlaybackButton = (Button)findViewById(R.id.close_playback_button);
    exploreButton = (Button)findViewById(R.id.explore_button);
    startPlaybackButton.setOnClickListener(view -> startPlayback());
    stopPlaybackButton.setOnClickListener(view -> stopPlayback());
    exploreButton.setOnClickListener(view -> exploreMP4());
    recordingPlaybackPathTextView = findViewById(R.id.recording_playback_path);
    surfaceView.setOnLongClickListener(view -> changeViewMode());

    mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

    updateUI();
  }

  private boolean changeViewMode() {
    depthSettings.setDepthColorVisualizationEnabled(!depthSettings.depthColorVisualizationEnabled());
    return true;
  }

  private void exploreMP4() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("video/mp4");
    startActivityForResult(intent, 10);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);

    if(requestCode==10){
      if (resultCode==RESULT_OK) {
        Log.d(TAG, "jeff inputStream check:" + ConvertFilepathUtil.getPath(getApplicationContext(), Uri.parse(data.toUri(0))));
        playbackDatasetPath = ConvertFilepathUtil.getPath(getApplicationContext(), Uri.parse(data.toUri(0)));
        Toast.makeText(HelloArActivity.this, "result ok!" + playbackDatasetPath, Toast.LENGTH_SHORT).show();
        updateUI();
      }else{
        Toast.makeText(HelloArActivity.this, "result cancle!", Toast.LENGTH_SHORT).show();
      }
    }
  }

  /** Performs action when playback button is clicked. */
  private void startPlayback() {
    if (playbackDatasetPath == null) {
      return;
    }
    currentState.set(AppState.PLAYBACK);
    restartActivityWithIntentExtras();
  }

  /** Performs action when close_playback button is clicked. */
  private void stopPlayback() {
    currentState.set(AppState.IDLE);
    restartActivityWithIntentExtras();
  }

  private static final String DESIRED_DATASET_PATH_KEY = "desired_dataset_path_key";
  private static final String DESIRED_APP_STATE_KEY = "desired_app_state_key";
  private static final int PERMISSIONS_REQUEST_CODE = 0;

  private void restartActivityWithIntentExtras() {
    Intent intent = this.getIntent();
    Bundle bundle = new Bundle();
    bundle.putString(DESIRED_APP_STATE_KEY, currentState.get().name());
    bundle.putString(DESIRED_DATASET_PATH_KEY, playbackDatasetPath);
    intent.putExtras(bundle);
    this.finish();
    this.startActivity(intent);
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      session.close();
      session = null;
    }
    super.onDestroy();
  }

  private void setPlaybackDatasetPath() {
    if (session.getPlaybackStatus() == PlaybackStatus.OK) {
      Log.d(TAG, "Session is already playing back.");
      setStateAndUpdateUI(AppState.PLAYBACK);
      return;
    }
    if (playbackDatasetPath != null) {
      try {
        File fileCheck = new File(playbackDatasetPath);
        session.setPlaybackDatasetUri(Uri.fromFile(fileCheck));
      } catch (PlaybackFailedException e) {
        String errorMsg = "Failed to set playback MP4 dataset. " + e;
        Log.e(TAG, errorMsg, e);
        messageSnackbarHelper.showError(this, errorMsg);
        Log.d(TAG, "Setting app state to IDLE, as the playback is not in progress.");
        setStateAndUpdateUI(AppState.IDLE);
        return;
      }
      setStateAndUpdateUI(AppState.PLAYBACK);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        if (this.checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          this.requestPermissions(new String[] {WRITE_EXTERNAL_STORAGE}, 1);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
        if (currentState.get() == AppState.PLAYBACK) {
          // Dataset playback will start when session.resume() is called.
          setPlaybackDatasetPath();
        }

        mHandler = new Handler(Looper.getMainLooper()){
          @Override
          public void handleMessage(Message msg){
            if(msg.what == MESSAGE_READ){
              String readMessage = null;
              try {
                readMessage = new String((byte[]) msg.obj, "UTF-8");
              } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
              }
              arduinoTextView.setText(readMessage.split("cm")[0]);
            }

            if(msg.what == CONNECTING_STATUS){
              if(msg.arg1 == 1)
                arduinoTextView.setText("Connected to Device: " + msg.obj);
              else
                arduinoTextView.setText("Connection Failed");
            }
          }
        };

        new Thread()
        {
          @Override
          public void run() {
            boolean fail = false;
            mPairedDevices = mBTAdapter.getBondedDevices();

            for (BluetoothDevice device : mPairedDevices) {
              if (device.getName().equals(ARDUINO_NAME)) {
                try {
                  mBTSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                  fail = true;
                  Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
                // Establish the Bluetooth socket connection.
                try {
                  mBTSocket.connect();
                } catch (IOException e) {
                  try {
                    fail = true;
                    Log.d("jinhwan", e.toString());
                    mBTSocket.close();
                    mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                            .sendToTarget();
                  } catch (IOException e2) {
                    //insert code to deal with this
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                  }
                }
                if(!fail) {
                  mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                  mConnectedThread.start();
                  mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, ARDUINO_NAME)
                          .sendToTarget();
                }
                break;
              }
            }
          }
        }.start();

      } catch (UnavailableArcoreNotInstalledException
              | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      configureSession();
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }

    if (currentState.get() == AppState.PLAYBACK) {
      // Must be called after dataset playback is started by call to session.resume().
      checkPlaybackStatus();
    }
    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  /** Checks the playback is in progress without issues. */
  private void checkPlaybackStatus() {
    Log.d(TAG, "GuardianEyes "+session.getPlaybackStatus());
    if ((session.getPlaybackStatus() != PlaybackStatus.OK)
            && (session.getPlaybackStatus() != PlaybackStatus.FINISHED)) {
      setStateAndUpdateUI(AppState.IDLE);
    }
  }

  private void updateUI() {
    Log.d(TAG, "GuardianEyes update UI:" + currentState.get());
    switch (currentState.get()) {
      case IDLE:
        startRecordingButton.setVisibility(View.VISIBLE);
        startRecordingButton.setEnabled(true);
        stopRecordingButton.setVisibility(View.GONE);
        stopRecordingButton.setEnabled(false);
        stopPlaybackButton.setVisibility(View.INVISIBLE);
        stopPlaybackButton.setEnabled(false);
        startPlaybackButton.setVisibility(View.VISIBLE);
        startPlaybackButton.setEnabled(playbackDatasetPath != null);
        exploreButton.setVisibility(View.VISIBLE);
        exploreButton.setEnabled(true);
        recordingPlaybackPathTextView.setText(
                getResources()
                        .getString(
                                R.string.playback_path_text,
                                playbackDatasetPath == null ? "" : playbackDatasetPath));
        break;
      case RECORDING:
        startRecordingButton.setVisibility(View.GONE);
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setVisibility(View.VISIBLE);
        stopRecordingButton.setEnabled(true);
        stopPlaybackButton.setVisibility(View.INVISIBLE);
        stopPlaybackButton.setEnabled(false);
        startPlaybackButton.setEnabled(false);
        recordingPlaybackPathTextView.setText(
                getResources()
                        .getString(
                                R.string.recording_path_text,
                                lastRecordingDatasetPath == null ? "" : lastRecordingDatasetPath));
        break;
      case PLAYBACK:
        startRecordingButton.setVisibility(View.INVISIBLE);
        stopRecordingButton.setVisibility(View.INVISIBLE);
        startPlaybackButton.setVisibility(View.INVISIBLE);
        exploreButton.setVisibility(View.INVISIBLE);
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setEnabled(false);
        stopPlaybackButton.setVisibility(View.VISIBLE);
        stopPlaybackButton.setEnabled(true);
        exploreButton.setEnabled(false);
        recordingPlaybackPathTextView.setText("");
        break;
    }
  }

  /** Generates a new MP4 dataset filename based on the current system time. */
  private static String getNewMp4DatasetFilename() {
    return String.format(
            Locale.ENGLISH,
            MP4_DATASET_FILENAME_TEMPLATE,
            DateTime.now().toString(MP4_DATASET_TIMESTAMP_FORMAT));
  }

  /** Generates a new MP4 dataset path based on the current system time. */
  private String getNewDatasetPath() {
    File baseDir = this.getExternalFilesDir(null);
    if (baseDir == null) {
      return null;
    }
    return new File(this.getExternalFilesDir(null), getNewMp4DatasetFilename()).getAbsolutePath();
  }

  private boolean flag = false;
  /** Performs action when start_recording button is clicked. */
  private void startRecording() {
    flag = true;
    try {
      lastRecordingDatasetPath = getNewDatasetPath();
      if (lastRecordingDatasetPath == null) {
        Log.d(TAG, "Failed to generate a MP4 dataset path for recording.");
        return;
      }

      Track anchorTrack =
              new Track(session).setId(ANCHOR_TRACK_ID).setMimeType(ANCHOR_TRACK_MIME_TYPE);

      session.startRecording(
              new RecordingConfig(session)
                      .setMp4DatasetUri(Uri.fromFile(new File(lastRecordingDatasetPath)))
                      .setAutoStopOnPause(false)
                      .addTrack(anchorTrack));
    } catch (RecordingFailedException e) {
      String errorMessage = "Failed to start recording. " + e;
      Log.e(TAG, errorMessage, e);
      messageSnackbarHelper.showError(this, errorMessage);
      return;
    }
    if (session.getRecordingStatus() != RecordingStatus.OK) {
      Log.d(TAG,
              "Failed to start recording, recording status is " + session.getRecordingStatus());
      return;
    }
    setStateAndUpdateUI(AppState.RECORDING);
  }

  /** Performs action when stop_recording button is clicked. */
  private void stopRecording() {
    flag = false;
    saveData();

    try {
      session.stopRecording();
    } catch (RecordingFailedException e) {
      String errorMessage = "Failed to stop recording. " + e;
      Log.e(TAG, errorMessage, e);
      messageSnackbarHelper.showError(this, errorMessage);
      return;
    }
    if (session.getRecordingStatus() == RecordingStatus.OK) {
      Log.d(TAG,
              "Failed to stop recording, recording status is " + session.getRecordingStatus());
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
    updateUI();
  }

  private void saveData() {
    String path = getFilesDir().getAbsolutePath();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String filePath = path + "/data_" + timeStamp + ".csv";
    File firstYFile = new File(filePath);

    String[][] splitted = new String[dataString.length][];
    for(int i = 0; i < dataString.length; ++i)
      splitted[i] = dataString[i].split("\n");
    String output = "";
    for(int i = 0; i < splitted[0].length; ++i) {
      String tmp = "";
      for(int j = 0; j < splitted.length; ++j) {
        if(j == 0) tmp += splitted[j][i];
        else tmp += "," + splitted[j][i];
      }
      if(i == 0) output += tmp;
      else output += "\n" + tmp;
    }

    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(firstYFile));
      writer.write(output);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    dataString = new String[pointsX.length];
  }

  @Override
  public void onPause() {
    super.onPause();

    if (session != null) {
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(SampleRender render) {
    // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
    // an IOException.
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /*width=*/ 1, /*height=*/ 1);

      cubemapFilter =
              new SpecularCubemapFilter(
                      render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
      // Load DFG lookup table for environmental lighting
      dfgTexture =
              new Texture(
                      render,
                      Texture.Target.TEXTURE_2D,
                      Texture.WrapMode.CLAMP_TO_EDGE,
                      /*useMipmaps=*/ false);
      // The dfg.raw file is a raw half-float texture with two channels.
      final int dfgResolution = 64;
      final int dfgChannels = 2;
      final int halfFloatSize = 2;

      ByteBuffer buffer =
              ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
      try (InputStream is = getAssets().open("models/dfg.raw")) {
        is.read(buffer.array());
      }
      // SampleRender abstraction leaks here.
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
      GLES30.glTexImage2D(
              GLES30.GL_TEXTURE_2D,
              /*level=*/ 0,
              GLES30.GL_RG16F,
              /*width=*/ dfgResolution,
              /*height=*/ dfgResolution,
              /*border=*/ 0,
              GLES30.GL_RG,
              GLES30.GL_HALF_FLOAT,
              buffer);
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

      //box shader
      boxShader = Shader.createFromAssets(render,"shaders/box.vert","shaders/box.frag",null);
      boxVertexBuffer = new VertexBuffer(render, 2, null);
      final VertexBuffer[] boxVertexBuffers = {boxVertexBuffer};
      boxMesh = new Mesh(render, Mesh.PrimitiveMode.LINE_LOOP, null, boxVertexBuffers);

      boxTexShader = Shader.createFromAssets(render, "shaders/boxTex.vert", "shaders/boxTex.frag", null);
      boxTexVertexBuffer = new VertexBuffer(render, 4, null);
      final VertexBuffer[] boxTexVertexBuffers = {boxTexVertexBuffer};
      boxTexMesh = new Mesh(render, Mesh.PrimitiveMode.TRIANGLE_STRIP, null, boxTexVertexBuffers);

      // Point cloud
      pointCloudShader =
              Shader.createFromAssets(
                      render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", /*defines=*/ null)
                      .setVec4(
                              "u_Color", new float[]{31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
                      .setFloat("u_PointSize", 5.0f);
      // four entries per vertex: X, Y, Z, confidence
      pointCloudVertexBuffer =
              new VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
              new Mesh(
                      render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/ null, pointCloudVertexBuffers);

      // Virtual object to render (ARCore pawn)
      Texture virtualObjectAlbedoTexture =
              Texture.createFromAsset(
                      render,
                      "models/pawn_albedo.png",
                      Texture.WrapMode.CLAMP_TO_EDGE,
                      Texture.ColorFormat.SRGB);
      Texture virtualObjectPbrTexture =
              Texture.createFromAsset(
                      render,
                      "models/pawn_roughness_metallic_ao.png",
                      Texture.WrapMode.CLAMP_TO_EDGE,
                      Texture.ColorFormat.LINEAR);
      virtualObjectMesh = Mesh.createFromAsset(render, "models/pawn.obj");
      virtualObjectShader =
              Shader.createFromAssets(
                      render,
                      "shaders/environmental_hdr.vert",
                      "shaders/environmental_hdr.frag",
                      /*defines=*/ new HashMap<String, String>() {
                        {
                          put(
                                  "NUMBER_OF_MIPMAP_LEVELS",
                                  Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                        }
                      })
                      .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
                      .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
                      .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
                      .setTexture("u_DfgTexture", dfgTexture);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }
  }

  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

  @Override
  public void onDrawFrame(SampleRender render) {
    if (session == null) {
      return;
    }

    // Texture names should only be set once on a GL thread unless they change. This is done during
    // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
    // initialized during the execution of onSurfaceCreated.
    if (!hasSetTextureNames) {
      session.setCameraTextureNames(
              new int[]{backgroundRenderer.getCameraColorTexture().getTextureId()});
      hasSetTextureNames = true;
    }

    // -- Update per-frame state

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session);

    // Obtain the current frame from ARSession. When the configuration is set to
    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
    // camera framerate.
    Frame frame;
    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      Log.e(TAG, "Camera not available during onDrawFrame", e);
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      return;
    }

    Camera camera = frame.getCamera();


    // Update BackgroundRenderer state to match the depth settings.
    try {
      backgroundRenderer.setUseDepthVisualization(
              render, depthSettings.depthColorVisualizationEnabled());
      backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
      return;
    }
    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
    // used to draw the background camera image.
    backgroundRenderer.updateDisplayGeometry(frame);

    if (camera.getTrackingState() == TrackingState.TRACKING
            && (depthSettings.useDepthForOcclusion()
            || depthSettings.depthColorVisualizationEnabled())) {
      try (Image depthImage = frame.acquireDepthImage()) {
        backgroundRenderer.updateCameraDepthTexture(depthImage);
      } catch (NotYetAvailableException e) {
        // This normally means that depth data is not available yet. This is normal so we will not
        // spam the logcat with this.
      }
    }

    // Handle one tap per frame.
//    handleTap(frame, camera);


    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

    // Show a message based on whether tracking has failed, if planes are detected, and if the user
    // has placed any objects.
    String message = null;
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
        message = SEARCHING_PLANE_MESSAGE;
      } else {
        message = TrackingStateHelper.getTrackingFailureReasonString(camera);
      }
    }
    if (message == null) {
      messageSnackbarHelper.hide(this);
    } else {
      messageSnackbarHelper.showMessage(this, message);
    }

    // -- Draw background

    if (frame.getTimestamp() != 0) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render);
    }

    // If not tracking, don't draw 3D objects.
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      return;
    }

    // -- Draw non-occluded virtual objects (planes, point cloud)

    // Get projection matrix.
//    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);
//
//    // Get camera matrix and draw.
//    camera.getViewMatrix(viewMatrix, 0);
//
//    // Visualize tracked points.
//    // Use try-with-resources to automatically release the point cloud.
//    try (PointCloud pointCloud = frame.acquirePointCloud()) {
//      if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
//        pointCloudVertexBuffer.set(pointCloud.getPoints());
//        lastPointCloudTimestamp = pointCloud.getTimestamp();
//      }
//      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
//      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
//      render.draw(pointCloudMesh, pointCloudShader);
//    }
    //render.draw(boxMesh, boxShader);

    Image cameraImage = null;
    try {
      cameraImage = frame.acquireCameraImage();
    } catch (NotYetAvailableException e) {
      // NotYetAvailableException is an exception that can be expected when the camera is not ready
      // yet. The image may become available on a next frame.
    } catch (RuntimeException e) {
      // A different exception occurred, e.g. DeadlineExceededException, ResourceExhaustedException.
      // Handle this error appropriately.
      Log.e(TAG, "Runtime error from acquiring camera image", e);
    } finally {
      if (cameraImage != null) {
//         convert image to inputImage
//        InputImage inputImage = InputImage.fromMediaImage(cameraImage, 0);

        Bitmap bitmapImage = Bitmap.createBitmap(cameraImage.getWidth(), cameraImage.getHeight(), Bitmap.Config.ARGB_8888);
        yuvToRgbConverter.yuvToRgb(cameraImage, bitmapImage);

        android.graphics.Matrix rotateMatrix = new android.graphics.Matrix();
        rotateMatrix.postRotate(90);
        bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), rotateMatrix, false);

        // InputImage inputImage = InputImage.fromBitmap(bitmapImage, 0);
        // myObjectdetector.getResults(inputImage);
        List<Detector.Recognition> result = myObjectdetector.getResults(bitmapImage);
        calDistance(frame, virtualSceneFramebuffer.getWidth(), virtualSceneFramebuffer.getHeight());
        drawResultRects(frame, virtualSceneFramebuffer.getWidth(), virtualSceneFramebuffer.getHeight(), render, result);
        for(int i = 0; i < pointsX.length; ++i) checkWallOrHole(frame, camera, i);
        cameraImage.close();
      }
    }

    // Visualize planes.
    planeRenderer.drawPlanes(
            render,
            session.getAllTrackables(Plane.class),
            camera.getDisplayOrientedPose(),
            projectionMatrix
    );
  }

  // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  final float inputSize = 500.0f;
//  private float prevfirstHitTy = 0.0f;
//  private float prevLastHitTy = 0.0f;
  float[] pointsX = {0.50f, 0.50f};
  float[] pointsY = {0.50f, 0.75f};
  private String[] dataString = new String[pointsX.length];

  private void checkWallOrHole(Frame frame, Camera camera, int num) {
    // Need adjustment
    float w = virtualSceneFramebuffer.getWidth(), h = virtualSceneFramebuffer.getHeight();
    float coorX = pointsX[num], coorY = pointsY[num];
    List<HitResult> hitResultList = frame.hitTest(coorX * w, coorY * h);
    boolean isHit = false;

    for(HitResult hit : hitResultList) {
      Trackable trackable = hit.getTrackable();
      // If a plane was hit, check that it was hit inside the plane polygon.
      // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
//      if ((trackable instanceof Plane
//              && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
//              && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
//              || (trackable instanceof Point
//              && ((Point) trackable).getOrientationMode()
//              == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
//              || (trackable instanceof InstantPlacementPoint)
//              || (trackable instanceof DepthPoint))
      if(trackable instanceof DepthPoint)
      {
        if(num == 0) textView.setText(hit.getHitPose().ty() + "m");

        if(flag) {
          if(dataString[num] == null) {
            dataString[num] = Float.toString(hit.getHitPose().ty());
          } else {
            dataString[num] += "\n" + hit.getHitPose().ty();
          }
        }

        isHit = true;
        break;
      }
    }

    if(!isHit) {
      if(num == 0) textView.setText("can't find proper surface");

      if(flag) {
        if(dataString[num] == null) {
          dataString[num] = "0.00";
        } else {
          dataString[num] += "\n" + "0.00";
        }
      }
    }

//    float threshold = 0.4f;
//    if(prevfirstHitTy != 0 && prevLastHitTy != 0) {
//      if(Math.abs(prevfirstHitTy - firstY) > threshold) { // Detect Wall
//        Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//        vi.vibrate(300);
//      }
//      if(Math.abs(prevLastHitTy - lastY) > threshold) { // Detect Hole
//        Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//        vi.vibrate(300);
//      }
//    }

//    prevfirstHitTy = firstY;
//    prevLastHitTy = lastY;
//    return 0;
  }

  private void calDistance(Frame frame, float w, float h) {
    float minDistance = 10000.0f;
    List<HitResult> hitResultList = frame.hitTest(0.5f*w, 0.5f*h);

    for (HitResult hit : hitResultList) {
      if(hit.getDistance() < minDistance) {
        minDistance = hit.getDistance();
      }
    }

    hitResultList = frame.hitTest(0.45f*w, 0.5f*h);

    for (HitResult hit : hitResultList) {
      if(hit.getDistance() < minDistance) {
        minDistance = hit.getDistance();
      }
    }

    hitResultList = frame.hitTest(0.55f*w, 0.5f*h);

    for (HitResult hit : hitResultList) {
      if(hit.getDistance() < minDistance) {
        minDistance = hit.getDistance();
      }
    }

    hitResultList = frame.hitTest(0.5f*w, 0.55f*h);

    for (HitResult hit : hitResultList) {
      if(hit.getDistance() < minDistance) {
        minDistance = hit.getDistance();
      }
    }

    hitResultList = frame.hitTest(0.5f*w, 0.45f*h);

    for (HitResult hit : hitResultList) {
      if(hit.getDistance() < minDistance) {
        minDistance = hit.getDistance();
      }
    }

//    textView.setText("distance is " + minDistance + " m");
  }

  private void drawText(SampleRender render, String str, float x, float y) {

    Paint textPaint = new Paint();
    textPaint.setTextSize(32);
    textPaint.setAntiAlias(true);
    textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
    textPaint.setTextAlign(Paint.Align.LEFT);
//    textPaint.setTextScaleX(0.5f);
    Rect rect = new Rect();
    textPaint.getTextBounds(str, 0, str.length(), rect);

    Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    bitmap.eraseColor(0);
    canvas.drawText(str,rect.left,-rect.top,textPaint);

    float width = (float)(rect.width())/480.0f;
    float height = (float)(rect.height())/640.0f;

    float left = x;
    float top = y;
    float right = x + width;
    float bottom = y - height;

    FloatBuffer test = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    test.rewind();
    test.put(new float[]{
            left, top, 0.0f, 0.0f,
            right, top, 1.0f, 0.0f,
            left, bottom, 0.0f, 1.0f,
            right, bottom, 1.0f, 1.0f
    });
    boxTexVertexBuffer.set(test);
    Texture texture = new Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE, false);
    GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.getTextureId());
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
    GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
    boxTexShader.setTexture("u_Texture", texture);
    render.draw(boxTexMesh, boxTexShader);
    texture.close();
  }

  private void drawResultRects(Frame frame, float w, float h, SampleRender render, List<Detector.Recognition> result) {

    GLES30.glLineWidth(8.0f);
    for(final Detector.Recognition recog:result) {
      RectF rect = recog.getLocation();
      Pair<Float, Float> coord = recog.getCenterCoordinate();
      List<HitResult> hitResultList = frame.hitTest(coord.first/inputSize*w, coord.second/inputSize*h);
      Log.d(TAG, "GuardianEyes " + recog.getTitle() +" coord:" + coord + " input:" + w + "," + h + " left " + rect.left + " right " + rect.right + " top " + rect.top + " bottom " + rect.bottom);
      float minDist = 10000.0f;
      for (HitResult hit : hitResultList) {
        if(hit.getDistance() < minDist) minDist = hit.getDistance();
      }
      float top = 2.0f * ((inputSize - rect.top) / inputSize) - 1.0f;
      float bottom = 2.0f * ((inputSize - rect.bottom) / inputSize) - 1.0f;
      float left = 2.0f * (rect.left / inputSize) - 1.0f;
      float right = 2.0f * (rect.right / inputSize) - 1.0f;

      FloatBuffer test = ByteBuffer.allocateDirect(2 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
      test.put(new float[]{
              left, top,
              right, top,
              right, bottom,
              left, bottom});

      boxVertexBuffer.set(test);
      render.draw(boxMesh, boxShader);
      drawText(render,recog.getTitle() + " " + minDist + "m", left, top);
    }
    GLES30.glLineWidth(1.0f);
  }

  private void configureSession() {
    Config config = session.getConfig();
    config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    } else {
      config.setDepthMode(Config.DepthMode.DISABLED);
    }

    session.configure(config);
  }

  private void loadInternalStateFromIntentExtras() {
    if (getIntent() == null || getIntent().getExtras() == null) {
      return;
    }
    Bundle bundle = getIntent().getExtras();
    if (bundle.containsKey(DESIRED_DATASET_PATH_KEY)) {
      playbackDatasetPath = getIntent().getStringExtra(DESIRED_DATASET_PATH_KEY);
    }
    if (bundle.containsKey(DESIRED_APP_STATE_KEY)) {
      String state = getIntent().getStringExtra(DESIRED_APP_STATE_KEY);
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

  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
    try {
      final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
      return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
    } catch (Exception e) {
      Log.e(TAG, "Could not create Insecure RFComm Connection",e);
    }
    return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
  }
}