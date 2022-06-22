package me.chayan.rtmppublisher;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.takusemba.rtmppublisher.Publisher;
import com.takusemba.rtmppublisher.PublisherListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements PublisherListener {

    public static int REQUEST_CAMERA = 201;

    private static final String[] PERMISSIONS_CAMERA = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO"};

    private Publisher publisher;
    private GLSurfaceView glView;
    private Button publishButton;
    private ImageView cameraButton;
    private TextView label;

    private final String url = BuildConfig.STREAMING_URL;
    private final Handler handler = new Handler();
    private Thread thread;
    private boolean isCounting = false;
    private Long startedAt, updatedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glView = findViewById(R.id.surface_view);
        publishButton = findViewById(R.id.toggle_publish);
        cameraButton = findViewById(R.id.toggle_camera);
        label = findViewById(R.id.live_label);

        checkPermission();
    }

    private void initializePublisher() {
        // VISIBLE GLSurfaceView after getting Camera Permission
        // otherwise you will get NullPointerException
        glView.setVisibility(View.VISIBLE);

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_SHORT).show();
        } else {
            publisher = new Publisher.Builder(this)
                    .setGlView(glView)
                    .setUrl(url)
                    .setSize(Publisher.Builder.DEFAULT_WIDTH, Publisher.Builder.DEFAULT_HEIGHT)
                    .setAudioBitrate(Publisher.Builder.DEFAULT_AUDIO_BITRATE)
                    .setVideoBitrate(Publisher.Builder.DEFAULT_VIDEO_BITRATE)
                    .setCameraMode(Publisher.Builder.DEFAULT_MODE)
                    .setListener(this)
                    .build();

            publishButton.setOnClickListener(view -> {
                if (publisher.isPublishing()) {
                    publisher.stopPublishing();
                } else {
                    publisher.startPublishing();
                }
            });

            cameraButton.setOnClickListener(view -> publisher.switchCamera());

            updateControls();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (publisher != null && !TextUtils.isEmpty(url)) {
            updateControls();
        }
    }

    @Override
    public void onStarted() {
        Toast.makeText(this, R.string.started_publishing, Toast.LENGTH_SHORT).show();
        updateControls();
        startCounting();
    }

    @Override
    public void onStopped() {
        Toast.makeText(this, R.string.stopped_publishing, Toast.LENGTH_SHORT).show();
        updateControls();
        stopCounting();
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, R.string.disconnected_publishing, Toast.LENGTH_SHORT).show();
        updateControls();
        stopCounting();
    }

    @Override
    public void onFailedToConnect() {
        Toast.makeText(this, R.string.failed_publishing, Toast.LENGTH_SHORT).show();
        updateControls();
        stopCounting();
    }

    private void updateControls() {
        publishButton.setText(getString(publisher.isPublishing() ? R.string.stop_publishing : R.string.start_publishing));
    }

    private void startCounting() {
        isCounting = true;
        label.setText(getString(R.string.publishing_label, format(0L), format(0L)));
        label.setVisibility(View.VISIBLE);
        startedAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
        thread = new Thread(() -> {
            while (isCounting) {
                if (System.currentTimeMillis() - updatedAt > 1000) {
                    updatedAt = System.currentTimeMillis();
                    handler.post(() -> {
                        long diff = System.currentTimeMillis() - startedAt;
                        long second = diff / 1000 % 60;
                        long min = diff / 1000 / 60;
                        label.setText(getString(R.string.publishing_label, format(min), format(second)));
                    });
                }
            }
        });
        thread.start();
    }

    private void stopCounting() {
        isCounting = false;
        label.setText("");
        label.setVisibility(View.GONE);
        thread.interrupt();
    }

    private String format(Long value) {
        return String.format(Locale.ENGLISH, "%02d", value);
    }

    // Function to check and request permission.
    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, REQUEST_CAMERA);
        } else {
            initializePublisher();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializePublisher();
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}