//package com.example.vedroid1;
//
//import android.content.Context;
//import android.hardware.camera2.CameraAccessException;
//import android.hardware.camera2.CameraManager;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import androidx.appcompat.app.AppCompatActivity;
//
//public class main extends AppCompatActivity {
//
//    private boolean isFlashlightOn = false;
//    private CameraManager cameraManager;
//    private String cameraId;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_helloact);
//
//        Button toggleButton = findViewById(R.id.buttonToggle);
//
//        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        try {
//            cameraId = cameraManager.getCameraIdList()[0];
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//
//        toggleButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                toggleFlashlight(v);
//            }
//        });
//    }
//
//    public void toggleFlashlight(View view) {
//        try {
//            if (isFlashlightOn) {
//                // Выключение фонарика
//                cameraManager.setTorchMode(cameraId, false);
//                isFlashlightOn = false;
//            } else {
//                // Включение фонарика
//                cameraManager.setTorchMode(cameraId, true);
//                isFlashlightOn = true;
//            }
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (isFlashlightOn) {
//            // При закрытии приложения выключаем фонарик, если он был включен
//            toggleFlashlight(findViewById(R.id.buttonToggle));
//        }
//    }
//}

package com.example.vedroid1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class main extends AppCompatActivity {
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 200;
    private static final int FLASHLIGHT_PERMISSION_REQUEST_CODE = 201;
    private boolean isMonitoring = false;
    private MediaRecorder mediaRecorder;
    private CameraManager cameraManager;
    private TextView textTriggerValue, textCurrentValue;
    private String cameraId;
    private int maxVolume = 10000;
    private int amplitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helloact);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        textTriggerValue = findViewById(R.id.triggerValue);
        textCurrentValue = findViewById(R.id.currentValue);
        Button startStopButton = findViewById(R.id.button);

        textTriggerValue.setText("Фонарик включится, если громкость привысит отметку в " + maxVolume);
        textCurrentValue.setText("Текущее значение: Отключено");

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMonitoring) {
                    startMonitoring();
                    startStopButton.setText("Остановить");
                } else {
                    stopMonitoring();
                    startStopButton.setText("Начать");
                }
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    FLASHLIGHT_PERMISSION_REQUEST_CODE);
        }
    }

    private void startMonitoring() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isMonitoring = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(isMonitoring) {
                    try {
                        amplitude = mediaRecorder.getMaxAmplitude();
                        if (amplitude > maxVolume) {

                            cameraManager.setTorchMode(cameraId, true);
                        } else {
                            cameraManager.setTorchMode(cameraId, false);
                        }
                        textCurrentValue.post(new Runnable() {
                            @Override
                            public void run() {
                                textCurrentValue.setText("Текущее значение: " + amplitude);
                            }
                        });
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopMonitoring() {
        if (mediaRecorder != null) {
            isMonitoring = false;
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            textCurrentValue.setText("Текущее значение: Отключено");
        }
    }
}
