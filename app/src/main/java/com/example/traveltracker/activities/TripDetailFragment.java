package com.example.traveltracker.fragments;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.example.traveltracker.R;
import com.example.traveltracker.databinding.FragmentTripDetailBinding;
import com.example.traveltracker.models.Trip;
import com.example.traveltracker.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TripDetailFragment extends Fragment {

    private static final String TAG = "TripDetailFragment";
    private FragmentTripDetailBinding binding;
    private Trip currentTrip;
    private NavController navController;
    private DatabaseReference userTripsRef;
    private FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userTripsRef = FirebaseDatabase.getInstance().getReference("trips").child(currentUser.getUid());
        } else {
            handleLoadError("Error: User not authenticated.");
            return; // Can't proceed without user
        }

        if (getArguments() != null) {
            if (getArguments().containsKey(Constants.EXTRA_TRIP)) {
                try { currentTrip = (Trip) getArguments().getSerializable(Constants.EXTRA_TRIP); }
                catch (ClassCastException e) { Log.e(TAG, "Argument type mismatch", e); currentTrip = null; }
            }
            if (currentTrip == null || currentTrip.getTripId() == null) {
                handleLoadError("Error: Trip data not found or invalid in arguments.");
            }
        } else {
            handleLoadError("Error: No arguments passed to TripDetailFragment.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTripDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (currentTrip != null) {
            populateTripDetails();
            setupButtonListeners();
        } else {
            binding.textViewDetailDestination.setText(R.string.error_loading_trip);
            binding.buttonViewOnMap.setEnabled(false);
            binding.buttonEditTrip.setEnabled(false);
            binding.buttonDeleteTrip.setEnabled(false);
        }
    }

    private void populateTripDetails() {
        String destination = currentTrip.getCity() + ", " + currentTrip.getState();
        binding.textViewDetailDestination.setText(destination);
        binding.textViewDetailDate.setText(getString(R.string.visited_prefix, currentTrip.getDateVisited()));
        binding.textViewDetailDuration.setText(getString(R.string.duration_prefix, currentTrip.getDuration()));

        boolean hasValidCoords = Math.abs(currentTrip.getLatitude()) > 0.0001 || Math.abs(currentTrip.getLongitude()) > 0.0001;
        binding.buttonViewOnMap.setEnabled(hasValidCoords);
        binding.buttonViewOnMap.setAlpha(hasValidCoords ? 1.0f : 0.5f);
        binding.buttonEditTrip.setEnabled(true);
        binding.buttonDeleteTrip.setEnabled(true);
    }

    private void setupButtonListeners() {
        binding.buttonViewOnMap.setOnClickListener(v -> navigateToMap());
        binding.buttonEditTrip.setOnClickListener(v -> navigateToEdit());
        binding.buttonDeleteTrip.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void navigateToMap() {
        if (currentTrip == null || (Math.abs(currentTrip.getLatitude()) < 0.0001 && Math.abs(currentTrip.getLongitude()) < 0.0001)) {
            Toast.makeText(getContext(), "Location coordinates not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = currentTrip.getLatitude(); double lon = currentTrip.getLongitude();
        String destName = currentTrip.getCity() + ", " + currentTrip.getState();
        Log.d(TAG, "Navigating to Map. Lat: " + lat + ", Lon: " + lon + ", Name: " + destName);
        Bundle args = new Bundle();
        args.putFloat("latitude", (float)lat);
        args.putFloat("longitude", (float)lon);
        args.putString("destination_name", destName);
        try { navController.navigate(R.id.action_tripDetailFragment_to_mapActivity, args); }
        catch (IllegalArgumentException e) { Log.e(TAG, "Navigation action not found.", e); }
    }

    private void navigateToEdit() {
        if (currentTrip == null || navController == null || getView() == null) return;
        Log.d(TAG, "Navigating to edit trip: " + currentTrip.getTripId());
        Bundle args = new Bundle();
        args.putSerializable("tripToEdit", currentTrip);
        try {
            navController.navigate(R.id.action_tripDetailFragment_to_addTripFragment, args);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Could not navigate to edit fragment", e);
            Toast.makeText(getContext(), "Error opening edit screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        if (getContext() == null || !isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirmation_title)
                .setMessage(R.string.delete_trip_confirmation_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.delete_yes, (dialog, which) -> deleteTrip())
                .setNegativeButton(R.string.delete_no, null)
                .show();
    }

    private void deleteTrip() {
        if (currentTrip == null || currentTrip.getTripId() == null || userTripsRef == null || navController == null || getView() == null) {
            Toast.makeText(getContext(), "Error: Cannot delete trip.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Delete failed: missing trip data or user reference.");
            return;
        }

        Log.d(TAG, "Deleting trip: " + currentTrip.getTripId());
        userTripsRef.child(currentTrip.getTripId()).removeValue()
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Trip deleted successfully.");
                        Toast.makeText(getContext(), R.string.trip_deleted_success, Toast.LENGTH_SHORT).show();
                        navController.popBackStack();
                    } else {
                        Log.w(TAG, "Failed to delete trip", task.getException());
                        Toast.makeText(getContext(), getString(R.string.trip_delete_failed) + " " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleLoadError(String message) {
        Log.e(TAG, message);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        if (navController != null && getView() != null) {
            navController.popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

