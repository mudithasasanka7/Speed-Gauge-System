package com.example.speedgauge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.github.anastr.speedviewlib.PointerSpeedometer;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private PointerSpeedometer speedometer;
    private TextView speedValue;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        animatePointerOnStart();
        hideNavigationBar();

        // Initialize Views
        speedometer = findViewById(R.id.speedometer);
        speedValue = findViewById(R.id.speedValue);

        // Initialize Location Manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Check if GPS is enabled and request Location Updates
//        checkAndEnableGPS();
    }

    private void animatePointerOnStart() {
        // Move pointer to maximum speed and back to 0 with animation
        new Handler().postDelayed(() -> speedometer.speedTo(180, 2000), 500); // Go to 180 in 2 seconds after 0.5s delay
        new Handler().postDelayed(() -> speedometer.speedTo(0, 2000), 3000);  // Back to 0 in 2 seconds after reaching 180
    }

    private void hideNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hides the navigation bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN    // Hides the status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Ensures navigation bar stays hidden
        );
    }

    private void checkAndEnableGPS() {
        if (!isGPSEnabled()) {
            // If GPS is disabled, prompt user to enable it
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        requestLocationUpdates();
    }

    private boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Start receiving location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
    }

    private final LocationListener locationListener = new LocationListener() {


        @Override
        public void onLocationChanged(@NonNull Location location) {

            if (location.hasSpeed()) {
                // Convert speed from m/s to km/h
                float speedInKmH = location.getSpeed() * 3.6f;
                // Update Speedometer and TextView
                speedometer.speedTo(speedInKmH);
                speedValue.setText("Speed: " + Math.round(speedInKmH) + " km/h");
                if (speedInKmH > 40) {
                    // Change color to orange or red if speed > 40
                    if (speedInKmH > 80) {
                        speedometer.setBackgroundCircleColor(Color.RED);  // Speed > 80, red color
                    } else {
                        speedometer.setBackgroundCircleColor(Color.parseColor("#FFA500"));  // Speed > 40 but <= 80, orange color
                    }
                } else {
                    // Default color for speed <= 40
                    speedometer.setBackgroundCircleColor(Color.parseColor("#1E1E1E"));  // Original color
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Deprecated but required for older Android versions
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Toast.makeText(MainActivity.this, "GPS Enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Toast.makeText(MainActivity.this, "GPS Disabled", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required to get speed data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove location updates when activity is destroyed
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeSpeedometerWidth(300);
            Toast.makeText(this, "Landscape mode", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeSpeedometerWidth(350);
            Toast.makeText(this, "Portrait mode", Toast.LENGTH_SHORT).show();
        }
    }
    private void changeSpeedometerWidth(int widthInDp) {
        // Convert dp to pixels
        int widthInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthInDp, getResources().getDisplayMetrics());

        // Get current layout params of the speedometer
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) speedometer.getLayoutParams();
        layoutParams.width = widthInPx;  // Set the new width in pixels
        speedometer.setLayoutParams(layoutParams);  // Apply the new layout parameters
    }
}
