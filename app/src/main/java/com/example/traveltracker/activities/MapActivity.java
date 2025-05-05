package com.example.traveltracker.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.traveltracker.R;
import com.example.traveltracker.utils.Constants;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String destinationName = "Destination";
    private Handler mainThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mainThreadHandler = new Handler(Looper.getMainLooper());

        Toolbar toolbarMap = findViewById(R.id.toolbarMap);
        setSupportActionBar(toolbarMap);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_map);
        }

        float latFloat = 0.0f;
        float lonFloat = 0.0f;

        if (getIntent().hasExtra(Constants.EXTRA_LATITUDE) &&
                getIntent().hasExtra(Constants.EXTRA_LONGITUDE)) {

            latFloat = getIntent().getFloatExtra(Constants.EXTRA_LATITUDE, 0.0f);
            lonFloat = getIntent().getFloatExtra(Constants.EXTRA_LONGITUDE, 0.0f);
            destinationName = getIntent().getStringExtra(Constants.EXTRA_DESTINATION_NAME);

            latitude = (double) latFloat;
            longitude = (double) lonFloat;

            Log.i(TAG, "-------------------------------------------");
            Log.i(TAG, "MapActivity received Intent Extras:");
            Log.i(TAG, "Lat (float extra): " + latFloat);
            Log.i(TAG, "Lon (float extra): " + lonFloat);
            Log.i(TAG, "Lat (double internal): " + latitude);
            Log.i(TAG, "Lon (double internal): " + longitude);
            Log.i(TAG, "Name: " + destinationName);
            Log.i(TAG, "-------------------------------------------");

            if (destinationName != null && !destinationName.isEmpty() && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(destinationName);
            }
        } else {
            Log.e(TAG, "Latitude or Longitude EXTRA missing in Intent for MapActivity.");
            Toast.makeText(this, "Error: Location data missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_container);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.map_fragment_container, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready.");

        if (Math.abs(latitude) > 0.0001 || Math.abs(longitude) > 0.0001) {
            final LatLng destinationLocation = new LatLng(latitude, longitude);
            Log.d(TAG, "Valid coordinates found. Adding marker for: " + destinationName + " at " + destinationLocation);


            mMap.addMarker(new MarkerOptions()
                    .position(destinationLocation)
                    .title(destinationName));

            // Enable zoom controls
            mMap.getUiSettings().setZoomControlsEnabled(true);


            mainThreadHandler.post(() -> {
                if (mMap != null) {
                    Log.d(TAG, "Moving camera (deferred).");
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLocation, 12f));
                }
            });

        } else {
            Log.e(TAG, "Invalid coordinates received for map display (Lat: " + latitude + ", Lon: " + longitude + ")");
            Toast.makeText(this, "Could not display location: Invalid coordinates received.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mainThreadHandler != null) {
            mainThreadHandler.removeCallbacksAndMessages(null);
        }
    }
}