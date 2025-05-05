
package com.example.traveltracker.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GeocoderUtil {

    private static final String TAG = "GeocoderUtil";

    public static LatLng getLatLngFromAddress(Context context, String city, String state) {
        if (!Geocoder.isPresent()) {
            Log.w(TAG, "Geocoder not present on this device.");
            return null;
        }

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        String searchAddress = city + ", " + state;
        LatLng latLng = null;

        try {
            // Get max 1 result
            List<Address> addresses = geocoder.getFromLocationName(searchAddress, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                latLng = new LatLng(address.getLatitude(), address.getLongitude());
                Log.i(TAG, "Geocoded '" + searchAddress + "' to: " + latLng.latitude + ", " + latLng.longitude);
            } else {
                Log.w(TAG, "No address found for: " + searchAddress);
            }
        } catch (IOException e) {
            // Network error or other I/O problem
            Log.e(TAG, "Geocoder IOException for: " + searchAddress, e);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Invalid address format
            Log.e(TAG, "Geocoder IllegalArgumentException for: " + searchAddress, illegalArgumentException);
        }

        return latLng;
    }
}