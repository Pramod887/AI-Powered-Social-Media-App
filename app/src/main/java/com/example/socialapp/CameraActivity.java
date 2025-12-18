package com.example.socialapp;

import static android.os.Environment.getExternalStoragePublicDirectory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.AREventListener;
import ai.deepar.ar.ARTouchInfo;
import ai.deepar.ar.ARTouchType;
import ai.deepar.ar.DeepAR;
import ai.deepar.ar.DeepARImageFormat;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, AREventListener {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int NUMBER_OF_BUFFERS = 2;
    private static final int REQUEST_PICK_IMAGE = 200;

    private DeepAR deepAR;
    private SurfaceView surfaceView;
    private ImageButton recordButton;
    private ImageButton switchCamera;
    private ImageButton btnSettings;
    private ImageButton previousMask;
    private ImageButton nextMask;
    private ImageButton btnGallery;

    private boolean useExternalCameraTexture = true;
    private int currentEffect = 0;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int width, height;
    private int currentBuffer = 0;
    private ByteBuffer[] buffers;
    private ARSurfaceProvider surfaceProvider;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private List<String> effects = Arrays.asList(
        "burning_effect.deepar",
        "butterfly-headband.deepar",
        "cute-virtual-pet.deepar",
        "Elephant_Trunk.deepar",
        "Emotion_Meter.deepar",
        "Emotions_Exaggerator.deepar",
        "Fire_Effect.deepar",
        "flower_face.deepar",
        "galaxy_background.deepar",
        "Hope.deepar",
        "Humanoid.deepar",
        "lightbulb.deepar",
        "MakeupLook.deepar",
        "Neon_Devil_Horns.deepar",
        "Ping_Pong.deepar",
        "Pixel_Hearts.deepar",
        "Snail.deepar",
        "Split_View_Look.deepar",
        "Stallone.deepar",
        "Vendetta_Mask.deepar",
        "viking_helmet.deepar"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Request permissions
        if (!checkPermissions()) {
            requestPermissions();
        }

        initViews();
        initializeDeepAR();
    }

    private void initViews() {
        surfaceView = findViewById(R.id.surfaceView);
        recordButton = findViewById(R.id.recordButton);
        switchCamera = findViewById(R.id.switchCamera);
        btnSettings = findViewById(R.id.btnSettings);
        previousMask = findViewById(R.id.previousMask);
        nextMask = findViewById(R.id.nextMask);
        btnGallery = findViewById(R.id.btnGallery);

        // Set up click listeners
        recordButton.setOnClickListener(v -> takeScreenshot());
        switchCamera.setOnClickListener(v -> switchCamera());
        btnSettings.setOnClickListener(v -> openSettings());
        previousMask.setOnClickListener(v -> gotoPrevious());
        nextMask.setOnClickListener(v -> gotoNext());
        btnGallery.setOnClickListener(v -> openGallery());

        // Set up surface view
        surfaceView.getHolder().addCallback(this);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeDeepAR();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void takeScreenshot() {
        if (deepAR != null) {
            deepAR.takeScreenshot();
        }
    }

    private void switchCamera() {
        lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK ? 
                    CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        setupCamera();
    }

    private void openSettings() {
        // TODO: Implement settings
        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                Toast.makeText(this, "Selected image: " + selectedImage.toString(), Toast.LENGTH_SHORT).show();
                // You can add code here to display the image or process it as needed
            }
        }
    }

    private void initializeDeepAR() {
        deepAR = new DeepAR(this);
        deepAR.setLicenseKey("ec1e4c7e1ea416b90b2afe6a9f045e62b8329836f5e1729a19a85d7e665ed5e701e5bf23e917bfe8");
        deepAR.initialize(this, this);
        setupCamera();
    }

    private void setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        int width;
        int height;
        int orientation = getScreenOrientation();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation ==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            width = 1920;
            height = 1080;
        } else {
            width = 1080;
            height = 1920;
        }

        Size cameraResolution = new Size(width, height);
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        if(useExternalCameraTexture) {
            Preview preview = new Preview.Builder()
                    .setTargetResolution(cameraResolution)
                    .build();

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview);
            if(surfaceProvider == null) {
                surfaceProvider = new ARSurfaceProvider(this, deepAR);
            }
            preview.setSurfaceProvider(surfaceProvider);
            surfaceProvider.setMirror(lensFacing == CameraSelector.LENS_FACING_FRONT);
        } else {
            buffers = new ByteBuffer[NUMBER_OF_BUFFERS];
            for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
                buffers[i] = ByteBuffer.allocateDirect(width * height * 4);
                buffers[i].order(ByteOrder.nativeOrder());
                buffers[i].position(0);
            }

            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(cameraResolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer);
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis);
        }
    }

    private ImageAnalysis.Analyzer imageAnalyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            buffer.rewind();
            buffers[currentBuffer].put(buffer);
            buffers[currentBuffer].position(0);
            if (deepAR != null) {
                deepAR.receiveFrame(buffers[currentBuffer],
                        image.getWidth(), image.getHeight(),
                        image.getImageInfo().getRotationDegrees(),
                        lensFacing == CameraSelector.LENS_FACING_FRONT,
                        DeepARImageFormat.RGBA_8888,
                        image.getPlanes()[0].getPixelStride()
                );
            }
            currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS;
            image.close();
        }
    };

    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    private String getFilterPath(String filterName) {
        return "file:///android_asset/" + filterName;
    }

    private void gotoNext() {
        currentEffect = (currentEffect + 1) % effects.size();
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        Toast.makeText(this, getCurrentFilterName(), Toast.LENGTH_SHORT).show();
    }

    private void gotoPrevious() {
        currentEffect = (currentEffect - 1 + effects.size()) % effects.size();
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        Toast.makeText(this, getCurrentFilterName(), Toast.LENGTH_SHORT).show();
    }

    private String getCurrentFilterName() {
        String filename = effects.get(currentEffect);
        String name = filename.replace(".deepar", "").replace("_", " ").replace("-", " ");
        // Capitalize first letter of each word
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    protected void onStop() {
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(surfaceProvider != null) {
            surfaceProvider.stop();
            surfaceProvider = null;
        }
        deepAR.release();
        deepAR = null;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(surfaceProvider != null) {
            surfaceProvider.stop();
        }
        if (deepAR == null) {
            return;
        }
        deepAR.setAREventListener(null);
        deepAR.release();
        deepAR = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If we are using on screen rendering we have to set surface view where DeepAR will render
        deepAR.setRenderSurface(holder.getSurface(), width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (deepAR != null) {
            deepAR.setRenderSurface(null, 0, 0);
        }
    }

    @Override
    public void screenshotTaken(Bitmap bitmap) {
        CharSequence now = DateFormat.format("yyyy_MM_dd_hh_mm_ss", new Date());
        try {
            File imageFile = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "image_" + now + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(imageFile);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("uri", contentUri);
            setResult(RESULT_OK, resultIntent);
            finish();
            Toast.makeText(CameraActivity.this, "Screenshot " + imageFile.getName() + " saved.", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdownFinished() {

    }

    @Override
    public void videoRecordingStarted() {
        // Not used - photo only mode
    }

    @Override
    public void videoRecordingFinished() {
        // Not used - photo only mode
    }

    @Override
    public void videoRecordingFailed() {
        // Not used - photo only mode
    }

    @Override
    public void videoRecordingPrepared() {
        // Not used - photo only mode
    }

    @Override
    public void initialized() {
        // Restore effect state after deepar release
        if (deepAR != null && effects != null && effects.size() > currentEffect) {
            deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        }
    }

    @Override
    public void faceVisibilityChanged(boolean b) {

    }

    @Override
    public void imageVisibilityChanged(String s, boolean b) {

    }

    @Override
    public void frameAvailable(Image image) {

    }

    @Override
    public void error(ARErrorType arErrorType, String s) {
        Log.e("CameraActivity", "DeepAR Error: " + arErrorType + " - " + s);
        runOnUiThread(() -> {
            Toast.makeText(this, "Camera Error: " + s, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void effectSwitched(String s) {

    }
}
