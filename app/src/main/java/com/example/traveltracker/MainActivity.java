package com.example.traveltracker;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private FirebaseAuth mAuth;
    private Toolbar toolbarMain;
    private BottomNavigationView bottomNavView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate started.");

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        toolbarMain = findViewById(R.id.toolbarMain);
        bottomNavView = findViewById(R.id.bottom_navigation);

        // Set up the Toolbar as the ActionBar for this Activity
        setSupportActionBar(toolbarMain);

        // Find NavHostFragment and setup Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_container);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            Log.d(TAG, "NavController obtained.");


            Set<Integer> topLevelDestinations = new HashSet<>();
            topLevelDestinations.add(R.id.loginFragment);      // Login screen
            topLevelDestinations.add(R.id.dashboardFragment);  // Main dashboard screen

            appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();


            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


            NavigationUI.setupWithNavController(bottomNavView, navController);


            if (savedInstanceState == null) {
                handleInitialNavigation();
            }

            // Add a listener to react to navigation events (destination changes)
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d(TAG, "Navigated to destination: " + destination.getLabel() + " (ID: " + destination.getId() + ")");

                // --- Control UI Element Visibility based on Destination ---
                if (destination.getId() == R.id.loginFragment) {
                    // Hide Toolbar and Bottom Navigation on the Login screen
                    bottomNavView.setVisibility(View.GONE);
                    if(getSupportActionBar() != null) getSupportActionBar().hide();
                    Log.d(TAG, "UI Update: Hiding Toolbar and BottomNav for LoginFragment");
                } else {
                    // Show Toolbar and Bottom Navigation on all other screens
                    bottomNavView.setVisibility(View.VISIBLE);
                    if(getSupportActionBar() != null) getSupportActionBar().show();
                    Log.d(TAG, "UI Update: Showing Toolbar and BottomNav");
                }


                if (getSupportActionBar() != null && destination.getLabel() != null) {
                    getSupportActionBar().setTitle(destination.getLabel());
                }
            });


            bottomNavView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                Log.d(TAG, "BottomNav item selected: " + item.getTitle() + ", ID: " + itemId);

                if (itemId == R.id.action_logout_bottom) {
                    Log.d(TAG, "Logout item clicked, calling logoutUser().");
                    logoutUser();
                    return true;
                } else {

                    boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                    Log.d(TAG, "NavigationUI handled click for item " + item.getTitle() + ": " + handled);

                    return handled;
                }
            });

        } else {

            Log.e(TAG, "FATAL ERROR: NavHostFragment R.id.nav_host_fragment_container not found in activity_main.xml!");
            Toast.makeText(this, "Critical Error: Navigation setup failed.", Toast.LENGTH_LONG).show();
            finish();
        }
    }


    private void handleInitialNavigation() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (navController == null) {
            Log.e(TAG, "NavController is null during initial navigation check.");
            return;
        }

        int currentDestinationId = navController.getCurrentDestination() != null ?
                navController.getCurrentDestination().getId() : 0;

        if (currentUser == null) {
            // User is NOT logged in. Make sure we are on the Login screen.
            Log.d(TAG, "handleInitialNavigation: User not logged in.");
            if (currentDestinationId != R.id.loginFragment) {
                Log.d(TAG, "handleInitialNavigation: Not on Login, navigating to Login and clearing stack.");

                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), true)
                        .build();
                navController.navigate(R.id.loginFragment, null, navOptions);
            } else {
                Log.d(TAG, "handleInitialNavigation: Already on Login screen.");
            }
        } else {

            Log.d(TAG, "handleInitialNavigation: User is logged in (UID: " + currentUser.getUid() + ")");
            if (currentDestinationId == R.id.loginFragment) {
                Log.d(TAG, "handleInitialNavigation: On Login screen, navigating to Dashboard.");

                navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
            } else {
                Log.d(TAG, "handleInitialNavigation: User logged in and not on Login screen. Current Dest ID: " + currentDestinationId);

            }
        }
    }


    @Override
    public boolean onSupportNavigateUp() {

        if (navController == null) {
            Log.e(TAG,"onSupportNavigateUp called but NavController is null!");
            return super.onSupportNavigateUp();
        }

        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    private void logoutUser() {
        Log.d(TAG, "Inside logoutUser() method.");
        mAuth.signOut(); // Sign out from Firebase
        Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();

        if (navController != null) {
            Log.d(TAG, "NavController available, attempting navigation to loginFragment.");

            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(navController.getGraph().getId(), true)
                    .setLaunchSingleTop(true)
                    .build();
            try {
                // Navigate to the login fragment using its ID from the nav graph
                navController.navigate(R.id.loginFragment, null, navOptions);
                Log.d(TAG,"Navigation to loginFragment attempted successfully.");
            } catch (IllegalArgumentException e){

                Log.e(TAG, "Could not navigate to loginFragment (ID: " + R.id.loginFragment + "). Is it defined correctly in nav_graph?", e);
                fallbackToRestart(); // Use fallback if navigation fails unexpectedly
            }
        } else {

            fallbackToRestart();
        }
    }


    private void fallbackToRestart() {
        Log.e(TAG, "NavController was null or navigation failed during logout, restarting MainActivity via Intent.");
        Intent intent = new Intent(this, MainActivity.class);
        // Flags to clear the existing task and start a new one
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


}