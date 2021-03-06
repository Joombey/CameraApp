package it.mirea.cameraapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private int LENS_FACING = 1;

    private PreviewView previewView;

    private final int PERMISSION_REQUEST_CAMERA = 123123;
    private final int PERMISSION_REQUEST_AUDIO = 120;

    private VideoCapture videoCapture;
    private ImageCapture imageCapture;


    private Button bRecord;
    private Button bCapture;
    private Button bRotate;

    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        previewView = findViewById(R.id.previewView);
        bRecord = findViewById(R.id.bRecord);
        bCapture = findViewById(R.id.bCapture);
        bRotate = findViewById(R.id.bRotate);

        bRecord.setOnClickListener(this);
        bCapture.setOnClickListener(this);
        bRotate.setOnClickListener(this);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
               != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},
                   this.PERMISSION_REQUEST_CAMERA);
        }
        InitCamera();
    }

    private void InitCamera() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission to use camera has been granted", Toast.LENGTH_SHORT).show();
        }
        else if(requestCode == PERMISSION_REQUEST_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission to record an audio has been granted", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();
        CameraSelector cameraSelector;
        cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(LENS_FACING)
                    .build();
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setCameraSelector(cameraSelector)
                        .build();
        // Video capture use case
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview,
                                        imageCapture, videoCapture);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bCapture:
                checkWritePermission();
                capturePhoto();
                break;
            case R.id.bRecord:
                recordVideo();
                break;
            case R.id.bRotate:
                if(this.LENS_FACING == 1){
                    this.LENS_FACING = 0;
                    bRotate.setText("BACK");
                } else{
                    this.LENS_FACING = 1;
                    bRotate.setText("FRONT");
                }
                InitCamera();
                break;
        }
    }

    private void checkWritePermission() {
    }

    public void capturePhoto() {
        File photoDir = new File(getExternalMediaDirs()[0]  + "/CameraXPhotos");
        if(!photoDir.exists())
            photoDir.mkdir();
        Date date = new Date();
        String timestamp = String.valueOf(date.getTime());
        String photoFilePath = photoDir.getAbsolutePath() + "/" + timestamp + ".jpg";
        File photoFile = new File(photoFilePath);

        ImageCapture.OutputFileOptions image = new ImageCapture.OutputFileOptions.Builder(photoFile)
                .build();

        imageCapture.takePicture(image, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "Photo has been saved successfully.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                );
    }
    @SuppressLint("RestrictedAPI")
    public void recordVideo(){
        if(videoCapture != null){
            File movieDir = new File(getExternalMediaDirs()[0]  + "/CameraXVideos");

            if(!movieDir.exists())
                movieDir.mkdir();

            Date date = new Date();
            String timestamp = String.valueOf(date.getTime());
            String vidFilePath = movieDir.getAbsolutePath() + "/" + timestamp + ".mp4";
            File vidFile = new File(vidFilePath);

            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_AUDIO);
                }
                if(bRecord.getText() == "stop R"){
                    bRecord.setText("start R");
                    videoCapture.stopRecording();
                } else{
                    bRecord.setText("stop R");
                    videoCapture.startRecording(
                            new VideoCapture.OutputFileOptions.Builder(vidFile).build(),
                            ContextCompat.getMainExecutor(this),
                            new VideoCapture.OnVideoSavedCallback() {
                                @Override
                                public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                                    Toast.makeText(MainActivity.this, "Video has been saved successfully.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                    Toast.makeText(MainActivity.this, "Error saving video: " + message, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}