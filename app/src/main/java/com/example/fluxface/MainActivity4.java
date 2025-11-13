package com.example.fluxface;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
// import android.media.MediaPlayer; // REMOVED
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity4 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // --- Configuration ---
    private static final String SERVER_URL = "http://192.168.1.4:5000/predict";
    private static final int CAMERA_PERMISSION_CODE = 123;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    // --- UI Elements ---
    private ImageButton btnPlay, btnMenu, btnSettings, btnAbout;
    private Dialog loadingDialog;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navHeaderName;
    private View navHeaderView;

    // --- CameraX ---
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    // --- Networking ---
    private final OkHttpClient client = new OkHttpClient();

    // --- Audio ---
    // REMOVED: private MediaPlayer musicPlayer;

    // --- ADDED: Music and Navigation Handling from your new code ---
    private boolean navigatingInternally = false;
    private Handler pauseHandler = new Handler(Looper.getMainLooper());
    private final Runnable pauseRunnable = () -> MusicManager.pause();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        // --- Initialize ALL UI Elements ---
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btnMenu);
        btnPlay = findViewById(R.id.btnPlay);

        btnSettings = findViewById(R.id.btnSettings);
        btnAbout = findViewById(R.id.btnAbout);

        // --- Setup Menu Header ---
        navHeaderView = navigationView.getHeaderView(0);
        navHeaderName = navHeaderView.findViewById(R.id.navHeaderName);

        // --- Set Menu Click Listener ---
        navigationView.setNavigationItemSelectedListener(this);

        // --- Setup Button Listeners ---
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        btnPlay.setOnClickListener(v -> takePhoto());

        btnSettings.setOnClickListener(v -> {
            navigatingInternally = true;
            // ADDED: Cancel any scheduled pause
            pauseHandler.removeCallbacks(pauseRunnable);
            Intent intent = new Intent(MainActivity4.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnAbout.setOnClickListener(v -> {
            navigatingInternally = true;
            // ADDED: Cancel any scheduled pause
            pauseHandler.removeCallbacks(pauseRunnable);
            Intent intent = new Intent(MainActivity4.this, InfoActivity.class);
            startActivity(intent);
        });

        // --- REMOVED old music logic from here ---

        // Set up the custom loading Dialog
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.setCancelable(false);

        // --- Camera Setup ---
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            initializeCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, CAMERA_PERMISSION_CODE);
        }
    }

    private void updateNavHeader() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userName = prefs.getString("userName", null);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        MenuItem loginItem = navigationView.getMenu().findItem(R.id.nav_login);
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.nav_logout);

        if (isLoggedIn && userName != null) {
            navHeaderName.setText("Hi, " + userName);
            loginItem.setVisible(false);
            logoutItem.setVisible(true);
        } else {
            navHeaderName.setText("Hi, Guest");
            loginItem.setVisible(true);
            logoutItem.setVisible(false);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        navigatingInternally = true;
        // ADDED: Cancel any scheduled pause
        pauseHandler.removeCallbacks(pauseRunnable);

        if (id == R.id.nav_stats) {
            startActivity(new Intent(MainActivity4.this, StatisticsActivity.class));
        } else if (id == R.id.nav_login) {
            startActivity(new Intent(MainActivity4.this, MainActivity2.class));
        } else if (id == R.id.nav_logout) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().remove("userName").remove("userEmail").remove("isLoggedIn").apply();
            updateNavHeader();
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
            navigatingInternally = false;
        }

        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    // --- REPLACED: Music and Activity Lifecycle Methods ---

    @Override
    protected void onStart() {
        super.onStart();
        // Use the MusicManager to start the music
        MusicManager.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset internal navigation flag and cancel any pending pause
        navigatingInternally = false;
        pauseHandler.removeCallbacks(pauseRunnable);
        updateNavHeader(); // Update "Hi, User" text
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Schedule a delayed pause if we are not navigating internally
        if (!navigatingInternally) {
            pauseHandler.postDelayed(pauseRunnable, 200);
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Schedule a delayed pause if the user is leaving the app (e.g. home button)
        if (!navigatingInternally) {
            pauseHandler.postDelayed(pauseRunnable, 200);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        // REMOVED old MediaPlayer release logic
    }

    // --- (The rest of your code remains unchanged) ---

    private Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width = bmpOriginal.getWidth();
        int height = bmpOriginal.getHeight();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready, please wait.", Toast.LENGTH_SHORT).show();
            if (!allPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, CAMERA_PERMISSION_CODE);
            }
            return;
        }

        loadingDialog.show(); // Show the loading dialog immediately

        // 🔹 NEW: Add a 1.5 second delay before taking the picture
        // This gives the camera time to auto-focus and the user to "hold the smile"
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

            // All the original logic now runs *after* the delay
            imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    analyzeImageEmotion(bitmap);
                    image.close();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("CameraX", "Photo capture failed: " + exception.getMessage(), exception);
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity4.this, "Failed to capture image.", Toast.LENGTH_SHORT).show();
                    });
                }
            });

        }, 1500); // 1500 milliseconds = 1.5 seconds
    }
    private void analyzeImageEmotion(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "captured_image.jpg",
                        RequestBody.create(byteArray, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder().url(SERVER_URL).post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("EmotionAPI", "Connection to server failed", e);
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(MainActivity4.this, "Cannot connect to server.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> loadingDialog.dismiss());
                if (!response.isSuccessful()) {
                    Log.e("EmotionAPI", "Server returned unsuccessful response: " + response.code());
                    return;
                }
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);
                    String emotion = jsonObject.optString("emotion", "neutral");
                    launchGame(emotion);
                } catch (JSONException e) {
                    Log.e("EmotionAPI", "Error parsing response", e);
                }
            }
        });
    }

    private void launchGame(String emotion) {
        String difficulty;
        switch (emotion.toLowerCase()) {
            case "happy":
                difficulty = "HARD";
                break;
            case "angry":
            case "sad":
            case "fear":
            case "disgust":
                difficulty = "EASY";
                break;
            default:
                difficulty = "NORMAL";
                break;
        }

        runOnUiThread(() -> {
            pauseHandler.removeCallbacks(pauseRunnable);
            Intent intent = new Intent(MainActivity4.this, GameActivity.class);
            intent.putExtra("DIFFICULTY", difficulty);
            startActivity(intent);
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (allPermissionsGranted()) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to play.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error initializing camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
}
