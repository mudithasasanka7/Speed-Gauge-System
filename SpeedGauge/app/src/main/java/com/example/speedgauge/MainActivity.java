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
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.github.anastr.speedviewlib.PointerSpeedometer;
import android.view.WindowManager;

import java.util.Locale;
public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private PointerSpeedometer speedometer;
    private TextView speedValue;
    private LocationManager locationManager;
    private Location previousLocation = null; // Stores the last known location
    private double totalDistance = 0.0;       // Tracks the total distance in meters
    private TextView distanceValue;          // TextView to display distance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distanceValue = findViewById(R.id.distanceValue); // Replace with the actual TextView ID
        distanceValue.setText("Distance: 0.0 km");        // Initial display value

        Button zeroButton = findViewById(R.id.btnZeroDistance);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        animatePointerOnStart();
        hideNavigationBar();

        // Initialize Views
        speedometer = findViewById(R.id.speedometer);
        speedValue = findViewById(R.id.speedValue);

        // Initialize Location Manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Check if GPS is enabled and request Location Updates
        checkAndEnableGPS();

    }
//    // Example: Update the displayed distance (inside your distance calculation logic)
//    private void updateDistance(double newDistance) {
//        totalDistance += newDistance; // Add the new distance
//        double displayDistance = totalDistance - zeroOffset; // Subtract the offset
//
//        TextView distanceTextView = findViewById(R.id.distanceValue);
//        distanceTextView.setText(String.format(Locale.getDefault(), "%.2f m", displayDistance));
//    }


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
//                speedValue.setText("Speed: " + Math.round(speedInKmH) + " km/h");
//                if (speedInKmH > 40) {
//                    // Change color to orange or red if speed > 40
//                    if (speedInKmH > 80) {
//                        speedometer.setBackgroundCircleColor(Color.RED);  // Speed > 80, red color
//                    } else {
//                        speedometer.setBackgroundCircleColor(Color.parseColor("#FFA500"));  // Speed > 40 but <= 80, orange color
//                    }
//                } else {
//                    // Default color for speed <= 40
//                    speedometer.setBackgroundCircleColor(Color.parseColor("#1E1E1E"));  // Original color
//                }
            }
            // Distance calculation
            if (previousLocation != null) {
                float distance = previousLocation.distanceTo(location); // Distance in meters
                totalDistance += distance; // Add to total distance
                double distanceInKm = totalDistance / 1000.0; // Convert to kilometers
                distanceValue.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceInKm));
            }

            previousLocation = location; // Update previous location
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
            //Landscape mode acticity
            changeSpeedometerWidth(300);
            adjustLayoutForLandscape();
            adjustDistanceTextForLandscape();
            changeSpeedTextSize(35);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Portrait mode acticity
            changeSpeedometerWidth(350);
            changeSpeedTextSize(45);
            adjustLayoutForPortrait();
            adjustDistanceTextForPortrait();
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
    private void changeSpeedTextSize(float textSizeInSp) {
        float textSizeInPx = textSizeInSp * getResources().getDisplayMetrics().scaledDensity;
        speedometer.setSpeedTextSize(textSizeInPx);
    }
    private void adjustLayoutForLandscape() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) speedValue.getLayoutParams();
        params.removeRule(RelativeLayout.BELOW); // Remove positioning below the gauge
        params.addRule(RelativeLayout.RIGHT_OF, R.id.speedometer); // Position to the right of the gauge
        params.setMargins(0, 20, 0, 0);
        changeSpeedTextSize(25);
        speedValue.setLayoutParams(params);
    }

    private void adjustLayoutForPortrait() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) speedValue.getLayoutParams();
        params.removeRule(RelativeLayout.RIGHT_OF); // Remove positioning to the right
        params.addRule(RelativeLayout.BELOW, R.id.speedometer); // Position below the gauge
        params.setMargins(0, 20, 0, 0); // Add top margin for spacing
        speedValue.setLayoutParams(params);
    }
    private void adjustDistanceTextForLandscape() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) distanceValue.getLayoutParams();
        params.removeRule(RelativeLayout.BELOW); // Remove positioning below the gauge
        params.addRule(RelativeLayout.RIGHT_OF, R.id.speedometer); // Position to the right of the gauge
        params.addRule(RelativeLayout.CENTER_VERTICAL); // Center vertically
        params.setMargins(20, 0, 0, 0); // Add left margin for spacing
        distanceValue.setLayoutParams(params);
    }
    private void adjustDistanceTextForPortrait() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) distanceValue.getLayoutParams();
        params.removeRule(RelativeLayout.RIGHT_OF); // Remove positioning to the right
        params.removeRule(RelativeLayout.CENTER_VERTICAL); // Remove vertical centering
        params.addRule(RelativeLayout.BELOW, R.id.speedValue); // Position below the gauge
        params.setMargins(0, 20, 0, 0); // Add top margin for spacing
        distanceValue.setLayoutParams(params);
    }
}
