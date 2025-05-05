package com.example.traveltracker;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;


public class TravelTrackerApp extends Application {

    private static final String TAG = "TravelTrackerApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable StrictMode for debug builds ONLY
        enableStrictMode();

        // Initialize Firebase and App Check
        initializeFirebaseAndAppCheck();

        Log.d(TAG, "TravelTrackerApp onCreate finished.");
    }

    private void initializeFirebaseAndAppCheck() {
        try {
            FirebaseApp.initializeApp(/*context=*/ this);
            Log.i(TAG, "FirebaseApp initialized successfully.");

            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance());
            Log.i(TAG, "Firebase App Check installed with Play Integrity provider.");

        } catch (IllegalStateException e) {
            Log.w(TAG, "FirebaseApp initialization error (might already be initialized): " + e.getMessage());
            try {
                FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
                firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance());
                Log.i(TAG, "Firebase App Check installed (attempted after Firebase init warning).");
            } catch (Exception appCheckError) {
                Log.e(TAG, "Error initializing App Check after Firebase init warning", appCheckError);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during Firebase/AppCheck initialization", e);
        }
    }

    private void enableStrictMode() {

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Enabling StrictMode...");
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()      // Detect blocking disk reads on main thread
                    .detectDiskWrites()     // Detect blocking disk writes on main thread
                    .detectNetwork()        // Detect blocking network calls on main thread

                    .penaltyLog()           // Log violations to Logcat

                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()

                    .build());
            Log.i(TAG, "StrictMode enabled.");
        } else {
            Log.d(TAG, "StrictMode not enabled (release build).");
        }
    }
}