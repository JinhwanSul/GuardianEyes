
package com.google.ar.core.examples.java.helloar;

import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
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
import com.google.ar.core.examples.java.helloar.arduino.ConnectedThread;
import com.google.ar.core.examples.java.helloar.detection.TFObjectDetector;
import com.google.ar.core.examples.java.helloar.detection.YuvToRgbConverter;
import com.google.ar.core.examples.java.helloar.tracking.Tracker;
import com.google.ar.core.examples.java.helloar.tracking.TrackingResult;
import com.google.ar.core.examples.java.helloar.util.Checker;
import com.google.ar.core.examples.java.helloar.util.ConvertFilepathUtil;
import com.google.ar.core.examples.java.helloar.util.DataSaver;
import com.google.ar.core.examples.java.helloar.tracking.GuardObject;
import com.google.ar.core.examples.java.helloar.util.Recording;
import com.google.ar.core.examples.java.helloar.util.Sound;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {

  private static final String TAG = HelloArActivity.class.getSimpleName();

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

  static {
    System.loadLibrary("opencv_java4");
    System.loadLibrary("native-lib");
  }

  private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
  public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
  private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
  private final String ARDUINO_ADDRESS = "E8:D8:84:00:DB:4E";
  private final String ARDUINO_NAME = "ESP32_SuperSonic";
  private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

  private final float INPUT_SIZE = 500.0f;

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;
  private TextView arduinoTextView;
  public TextView textView;
  public TextView avgHeightTextView;
  private TextView fpsTextView;

  private boolean installRequested;

  public Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
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

  public final DepthSettings depthSettings = new DepthSettings();

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

  private TFObjectDetector myObjectdetector;
  private YuvToRgbConverter yuvToRgbConverter;

  private Handler mHandler; // Our main handler that will receive callback notifications

  private BluetoothAdapter mBTAdapter;
  private Set<BluetoothDevice> mPairedDevices;
  private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
  private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data

  private Map objectMapper = new HashMap<Integer, GuardObject>();

  private List<Detector.Recognition> detectResult;
  private TrackingResult trackingResults;

  // Recoding
  private Recording recording;
  // DataSaver
  private DataSaver dataSaver;
  // Sound
  private Sound sound;
  // Checker
  private Checker checker;
  // Tracker
  private Tracker tracker;

  private Vibrator vibrator;

  public int frame_count = 0;
  public final int FRAME_UNIT_NUM = 15;
  public final int DISCARD_FRAME_NUM = 100;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Setup recording
    recording = new Recording(this);
    recording.loadInternalStateFromIntentExtras();

    // Setup dataSaver
    dataSaver = new DataSaver(this.getFilesDir());

    // Setup sound
    sound = new Sound(this);
    // Setup checker
    checker = new Checker(this);
    // Setup tracker
    tracker = new Tracker();

    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    textView = findViewById(R.id.text_view);
    avgHeightTextView = findViewById(R.id.avg_height_text_view);
    arduinoTextView = findViewById(R.id.arduioTextView);
    fpsTextView = findViewById(R.id.FPSTextView);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // Set up renderer.
    render = new SampleRender(surfaceView, this, getAssets());
    myObjectdetector = new TFObjectDetector(this);
    yuvToRgbConverter = new YuvToRgbConverter(this);

    installRequested = false;
    depthSettings.onCreate(this);

    startRecordingButton = (Button)findViewById(R.id.start_recording_button);
    stopRecordingButton = (Button)findViewById(R.id.stop_recording_button);
    startRecordingButton.setOnClickListener(view -> recording.startRecording());
    stopRecordingButton.setOnClickListener(view -> recording.stopRecording());

    startPlaybackButton = (Button)findViewById(R.id.playback_button);
    stopPlaybackButton = (Button)findViewById(R.id.close_playback_button);
    exploreButton = (Button)findViewById(R.id.explore_button);
    startPlaybackButton.setOnClickListener(view -> recording.startPlayback());
    stopPlaybackButton.setOnClickListener(view -> recording.stopPlayback());
    exploreButton.setOnClickListener(view -> recording.exploreMP4());
    recordingPlaybackPathTextView = findViewById(R.id.recording_playback_path);
    surfaceView.setOnLongClickListener(view -> recording.changeViewMode());

    mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
    vibrator = (Vibrator) getSystemService(this.VIBRATOR_SERVICE);
    updateUI();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);

    if(requestCode == 10){
      if(resultCode == RESULT_OK) {
        recording.playbackDatasetPath = ConvertFilepathUtil.getPath(getApplicationContext(), Uri.parse(data.toUri(0)));
        Toast.makeText(this, "result ok!" + recording.playbackDatasetPath, Toast.LENGTH_SHORT).show();
        updateUI();
      }else{
        Toast.makeText(this, "result cancel!", Toast.LENGTH_SHORT).show();
      }
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

        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this, new String[] {WRITE_EXTERNAL_STORAGE}, 1);
          return;
        }

        if (ContextCompat.checkSelfPermission(this, BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this, new String[] {BLUETOOTH}, 2);
          return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{BLUETOOTH_CONNECT}, 3);
            return;
          }
        }

        if (ContextCompat.checkSelfPermission(this, BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this, new String[] {BLUETOOTH_ADMIN}, 4);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
        if (recording.currentState.get() == Recording.AppState.PLAYBACK) {
          // Dataset playback will start when session.resume() is called.
          recording.setPlaybackDatasetPath();
        }

        mHandler = new Handler(Looper.getMainLooper()){
          @Override
          public void handleMessage(Message msg){
            if(msg.what == MESSAGE_READ){
              String readMessage = null;
              readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
              arduinoTextView.setText(readMessage.split("cm")[0]);
              if ( Integer.parseInt(readMessage.split("cm")[0]) < 20) {
                vibrator.vibrate(1000);
              }

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
//                    Log.d(TAG, e.toString());
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

    if (recording.currentState.get() == Recording.AppState.PLAYBACK) {
      // Must be called after dataset playback is started by call to session.resume().
      recording.checkPlaybackStatus();
    }
    surfaceView.onResume();
    sound.mGvrAudioEngine.resume();
    displayRotationHelper.onResume();
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      session.close();
      session = null;
    }
    super.onDestroy();
  }

  @Override
  public void onPause() {
    super.onPause();

//    dataSaver.saveData(checker.getSaveData());

    if (session != null) {
      displayRotationHelper.onPause();
      surfaceView.onPause();
      sound.mGvrAudioEngine.pause();
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
      boxTexShader.setBlend(Shader.BlendFactor.ONE, Shader.BlendFactor.ONE_MINUS_SRC_ALPHA);
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

    long startTime = System.nanoTime();

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

    if (frame.getTimestamp() != 0) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render);
    }

    // If not tracking, don't draw 3D objects.
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      return;
    }

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

        Bitmap bitmapImage = Bitmap.createBitmap(cameraImage.getWidth(), cameraImage.getHeight(), Bitmap.Config.ARGB_8888);
        yuvToRgbConverter.yuvToRgb(cameraImage, bitmapImage);

        android.graphics.Matrix rotateMatrix = new android.graphics.Matrix();
        rotateMatrix.postRotate(90);
        bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), rotateMatrix, false);

        detectResult = myObjectdetector.getResults(bitmapImage);
        trackingResults = tracker.objectTracking(detectResult);

        drawResultRect(render);
        findVector(frame, render, virtualSceneFramebuffer.getWidth(), virtualSceneFramebuffer.getHeight());

        checker.checkWallOrHole(frame, camera, virtualSceneFramebuffer.getWidth(), virtualSceneFramebuffer.getHeight());

        cameraImage.close();

        if(camera.getTrackingState() == TrackingState.TRACKING) {
          frame_count++;
        }
      }
    }

    // Visualize planes.
    planeRenderer.drawPlanes(
            render,
            session.getAllTrackables(Plane.class),
            camera.getDisplayOrientedPose(),
            projectionMatrix
    );

    runOnUiThread(() -> {
      long elapsedTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      int fps = (int) (1000 / elapsedTime);
      fpsTextView.setText(Integer.toString(fps));
    });
  }

  private void findVector(Frame frame, SampleRender render, float w, float h) {
    Map newMap = new HashMap<Integer, GuardObject>();
    Pose cameraPos = frame.getCamera().getPose();

    final float areaRateW = 0.1f;
    final float areaRateH = 0.1f;
    final int checkPointIndexFrom = 4;
    final int checkPointIndexTo = 5;
    boolean checkDangerous = false;

    for(int i = 0; i < trackingResults.size(); ++i) {
      float[] box = trackingResults.boxResults.get(i); // box : [x, y, width, height, id, frame_count]
      String title = trackingResults.titles.get(i);
      int id = (int) box[4];

      float centerX = box[0] + box[2] / 2;
      float centerY = box[1] + box[3] / 2;
      float lt = centerX - box[2] * areaRateW;
      float rt = centerX + box[2] * areaRateW;
      float tp = centerY - box[3] * areaRateH;
      float bt = centerY + box[3] * areaRateH;

      float[][] checkPoints = {
        {lt,tp},
        {lt,centerY},
        {lt,bt},
        {centerX,tp},
        {centerX,centerY},
        {centerX, bt},
        {rt, tp},
        {rt, centerY},
        {rt, bt}
      };

      float top = 2.0f * ((INPUT_SIZE - box[1]) / INPUT_SIZE) - 1.0f;
      float left = 2.0f * (box[0] / INPUT_SIZE) - 1.0f;

      float[] baseTr = {0f,0f,0f};
      float[] baseRt = {0f,0f,0f,1.0f};
      int countOfValue = 0;
      for(int idx = checkPointIndexFrom; idx < checkPointIndexTo; idx++) {
        List<HitResult> hitResultList = frame.hitTest(checkPoints[i][0] / INPUT_SIZE * w, checkPoints[i][1] / INPUT_SIZE * h);
        for (HitResult hit : hitResultList) {
          Trackable trackable = hit.getTrackable();
          if (trackable instanceof DepthPoint) {
            DepthPoint pt = (DepthPoint) trackable;
            Pose pos = hit.getHitPose();
            baseTr[0] += pos.tx();
            baseTr[1] += pos.ty();
            baseTr[2] += pos.tz();
            baseRt[0] += pos.qx();
            baseRt[1] += pos.qy();
            baseRt[2] += pos.qz();
            baseRt[3] += pos.qw();
            countOfValue++;
            break;
          }
        }
      }

      GuardObject obj = (GuardObject) objectMapper.get(id);

      if(countOfValue > 0) {
        baseTr[0] /= countOfValue;
        baseTr[1] /= countOfValue;
        baseTr[2] /= countOfValue;
        baseRt[0] /= countOfValue;
        baseRt[1] /= countOfValue;
        baseRt[2] /= countOfValue;
        baseRt[3] /= countOfValue;
        Pose pos = new Pose(baseTr, baseRt);
        float posX = pos.tx() - cameraPos.tx();
        float posY = pos.ty() - cameraPos.ty();
        float posZ = pos.tz() - cameraPos.tz();
        if(obj == null) {
          obj = new GuardObject(FRAME_UNIT_NUM);
          int sndId = sound.make3Dsound(posX, posY, posZ, 1);
          obj.setSndId(sndId);
        }
        else {
          obj.update(pos.tx() - cameraPos.tx(), pos.ty() - cameraPos.ty(), pos.tz() - cameraPos.tz());
          sound.updatePos(obj.getSndId(), posX, posY, posZ);
        }

        if(frame_count > DISCARD_FRAME_NUM && frame_count % FRAME_UNIT_NUM == 0) {
          float speed = obj.speed();
          float angle = obj.angle();

          if(angle < Math.cos(Math.PI / 180.0f * 150.0f)) {
//            obj.setInfo("[" + id + "] " + speed + " " + angle, 0xff, 0, 0);
            if(speed > 10f) {
              obj.setInfo(id + ". " + title, 0xff, 0, 0);
              checkDangerous = true;
            }
            else
              obj.setInfo(id + ". " + title, 0, 0, 0xff);
          }
          else {
//            obj.setInfo("[" + id + "] " + speed + " " + angle, 0xff, 0xff, 0xff);
            obj.setInfo(id + ". " + title, 0x00, 0x00, 0x00);
          }
          obj.clear();
        }
        drawText(render,obj.getInfo(), left, top, obj.getRed(), obj.getGreen(), obj.getBlue());

        newMap.put(id, obj);
      }
    }

//    checker.alarmObject(checkDangerous);

    //check released object
    objectMapper.forEach((key, value) -> {
      if (!newMap.containsKey(key)) {
        GuardObject obj = (GuardObject) value;
        if(obj.decTolerance() <= 0) {
          sound.stop3DSound(obj.getSndId());
        }
        else {
          newMap.put(key, value);
        }
      }
      else {
        GuardObject obj = (GuardObject) value;
        obj.setTolerance(3);
      }
    });
    objectMapper = newMap;
    sound.update();
  }

  private void drawResultRect(SampleRender render) {
    GLES30.glLineWidth(12.0f);

    for(int i = 0; i < trackingResults.size(); ++i) {
      float[] box = trackingResults.boxResults.get(i); // box : [x, y, width, height, id, frame_count]
      String title = trackingResults.titles.get(i);
      int id = (int) box[4];

      float top = 2.0f * ((INPUT_SIZE - box[1]) / INPUT_SIZE) - 1.0f;
      float bottom = 2.0f * ((INPUT_SIZE - (box[1] + box[3])) / INPUT_SIZE) - 1.0f;
      float left = 2.0f * (box[0] / INPUT_SIZE) - 1.0f;
      float right = 2.0f * ((box[0] + box[2]) / INPUT_SIZE) - 1.0f;

      FloatBuffer test = ByteBuffer.allocateDirect(2 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
      test.put(new float[] {
              left, top,
              right, top,
              right, bottom,
              left, bottom});

      boxVertexBuffer.set(test);
      render.draw(boxMesh, boxShader);

//      drawText(render,"[" + id + "] " + title, left, top);
    }

    GLES30.glLineWidth(1.0f);
  }

  private void drawText(SampleRender render, String str, float x, float y, int r, int g, int b) {

    Paint textPaint = new Paint();
    textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
    textPaint.setTextSize(45);
    textPaint.setAntiAlias(true);
    textPaint.setARGB(0xff, r, g, b);
    textPaint.setTextAlign(Paint.Align.LEFT);
    textPaint.setTextScaleX(1.2f);
    Rect rect = new Rect();
//    str += "   ";
    textPaint.getTextBounds(str, 0, str.length(), rect);

    Bitmap bitmap = Bitmap.createBitmap(rect.right + 20, rect.height() + 15, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
//    bitmap.eraseColor(Color.TRANSPARENT);
    canvas.drawText(str, rect.left + 15, -rect.top + 15, textPaint);

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

  public void updateUI() {
    switch (recording.currentState.get()) {
      case IDLE:
        startRecordingButton.setVisibility(View.VISIBLE);
        startRecordingButton.setEnabled(true);
        stopRecordingButton.setVisibility(View.GONE);
        stopRecordingButton.setEnabled(false);
        stopPlaybackButton.setVisibility(View.INVISIBLE);
        stopPlaybackButton.setEnabled(false);
        startPlaybackButton.setVisibility(View.VISIBLE);
        startPlaybackButton.setEnabled(recording.playbackDatasetPath != null);
        exploreButton.setVisibility(View.VISIBLE);
        exploreButton.setEnabled(true);
        recordingPlaybackPathTextView.setText(
                getResources()
                        .getString(
                                R.string.playback_path_text,
                                recording.playbackDatasetPath == null ? "" : recording.playbackDatasetPath));
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
                                recording.lastRecordingDatasetPath == null ? "" : recording.lastRecordingDatasetPath));
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

  private void configureSession() {
    Config config = session.getConfig();
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    } else {
      config.setDepthMode(Config.DepthMode.DISABLED);
    }

    session.configure(config);
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