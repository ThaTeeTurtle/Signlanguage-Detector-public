package com.example.singlanguage_detector0;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.animation.TimeAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;


import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    PreviewView vid_capture;
    FloatingActionButton btn_record_crt;
    TextView txt_rec_status;
    TextView txt_translation;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    CameraProvider cameraProvider;
    boolean is_recording = false;
    Recording recording = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vid_capture  = findViewById(R.id.video_display);
        btn_record_crt = findViewById(R.id.btn_record_crt);
        txt_rec_status = findViewById(R.id.txt_rec_status);
        txt_translation = findViewById(R.id.txt_translation);

        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        btn_record_crt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!is_recording){
                    txt_rec_status.setText("Stop Recording");
                    try {
                        start_recording(cameraProviderFuture.get());
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    is_recording = true;
                }else{
                    if(recording!=null){
                        txt_rec_status.setText("Start Recording");
                        recording.stop();
                        recording.close();
                        is_recording = false;
                    }
                }

            }
        });
    }


    private void start_recording(@NonNull ProcessCameraProvider cameraProvider){
        QualitySelector qualitySelector = QualitySelector.from(Quality.UHD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD));


        Recorder recorder = new Recorder.Builder().setExecutor(getMainExecutor()).setQualitySelector(qualitySelector).build();
        VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(recorder);
        Preview preview = get_preview();

        cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture);

        String name = "Recording-"+ Time.getCurrentTimezone() +".mp4";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, name);

        MediaStoreOutputOptions mediaoptions = new MediaStoreOutputOptions.Builder(this.getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        recording = videoCapture.getOutput()
                .prepareRecording(getApplicationContext(), mediaoptions)
                .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                        // Handle the start of a new active recording
                    } else if (videoRecordEvent instanceof VideoRecordEvent.Pause) {
                        // Handle the case where the active recording is paused
                    } else if (videoRecordEvent instanceof VideoRecordEvent.Resume) {
                        // Handles the case where the active recording is resumed
                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent =
                                (VideoRecordEvent.Finalize) videoRecordEvent;
                        // Handles a finalize event for the active recording, checking Finalize.getError()
                        int error = finalizeEvent.getError();
                        if (error != VideoRecordEvent.Finalize.ERROR_NONE) {

                        }
                    }
                });

    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = get_preview();
    }

    private Preview get_preview(){
        Preview preview = new Preview.Builder()
                .build();


        preview.setSurfaceProvider(vid_capture.getSurfaceProvider());
        return preview;
    }



    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions,
                                           @NotNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Camera Permission Granted", Toast.LENGTH_SHORT) .show();
            }
            else {
                Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT) .show();
            }
        }
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT) .show();
            }
            else {
                Toast.makeText(MainActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT) .show();
            }
        }
    }
}