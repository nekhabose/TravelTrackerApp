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
import com.example.traveltracker.databinding.FragmentFutureTripDetailBinding;
import com.example.traveltracker.models.FutureTrip;
import com.example.traveltracker.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FutureTripDetailFragment extends Fragment {

    private static final String TAG = "FutureTripDetailFrag";
    private FragmentFutureTripDetailBinding binding;
    private FutureTrip currentFutureTrip;
    private NavController navController;
    private DatabaseReference userFutureTripsRef;
    private FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userFutureTripsRef = FirebaseDatabase.getInstance().getReference("future_trips").child(currentUser.getUid());
        } else {
            handleLoadError("Error: User not authenticated.");
            return;
        }

        if (getArguments() != null) {
            if (getArguments().containsKey(Constants.EXTRA_FUTURE_TRIP)) {
                try { currentFutureTrip = (FutureTrip) getArguments().getSerializable(Constants.EXTRA_FUTURE_TRIP); }
                catch (ClassCastException e) { Log.e(TAG, "Argument type mismatch", e); currentFutureTrip = null; }
            }
            if (currentFutureTrip == null || currentFutureTrip.getFutureTripId() == null) {
                handleLoadError("Error: FutureTrip data not found or invalid in arguments.");
            }
        } else {
            handleLoadError("Error: No arguments passed to FutureTripDetailFragment.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFutureTripDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (currentFutureTrip != null) {
            populateDetails();
            setupButtonListeners();
        } else {
            binding.textViewFutureDetailDestination.setText(R.string.error_loading_trip);
            binding.buttonViewFutureOnMap.setEnabled(false);
            binding.buttonEditFutureTrip.setEnabled(false);
            binding.buttonDeleteFutureTrip.setEnabled(false);
            binding.textViewItineraryLabel.setVisibility(View.GONE);
            binding.scrollViewItinerary.setVisibility(View.GONE);
        }
    }

    private void populateDetails() {
        String destination = currentFutureTrip.getCity() + ", " + currentFutureTrip.getState();
        binding.textViewFutureDetailDestination.setText(destination);
        binding.textViewFutureDetailDate.setText(getString(R.string.planned_prefix, currentFutureTrip.getPlannedDate()));
        binding.textViewFutureDetailDuration.setText(getString(R.string.duration_prefix, currentFutureTrip.getDuration()));

        if (currentFutureTrip.getGeneratedItinerary() != null && !currentFutureTrip.getGeneratedItinerary().isEmpty()
                && !currentFutureTrip.getGeneratedItinerary().contains("failed")
                && !currentFutureTrip.getGeneratedItinerary().contains("skipped")) {
            binding.textViewGeneratedItinerary.setText(currentFutureTrip.getGeneratedItinerary());
            binding.textViewItineraryLabel.setVisibility(View.VISIBLE);
            binding.scrollViewItinerary.setVisibility(View.VISIBLE);
        } else {
            binding.textViewItineraryLabel.setVisibility(View.GONE);
            binding.scrollViewItinerary.setVisibility(View.GONE);
        }

        boolean hasValidCoords = Math.abs(currentFutureTrip.getLatitude()) > 0.0001 || Math.abs(currentFutureTrip.getLongitude()) > 0.0001;
        binding.buttonViewFutureOnMap.setEnabled(hasValidCoords);
        binding.buttonViewFutureOnMap.setAlpha(hasValidCoords ? 1.0f : 0.5f);
        binding.buttonEditFutureTrip.setEnabled(true);
        binding.buttonDeleteFutureTrip.setEnabled(true);
    }

    private void setupButtonListeners() {
        binding.buttonViewFutureOnMap.setOnClickListener(v -> navigateToMap());
        binding.buttonEditFutureTrip.setOnClickListener(v -> navigateToEdit());
        binding.buttonDeleteFutureTrip.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void navigateToMap() {
        if (currentFutureTrip == null || (Math.abs(currentFutureTrip.getLatitude()) < 0.0001 && Math.abs(currentFutureTrip.getLongitude()) < 0.0001)) {
            Toast.makeText(getContext(), "Location coordinates not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle args = new Bundle();
        args.putFloat("latitude", (float) currentFutureTrip.getLatitude());
        args.putFloat("longitude", (float) currentFutureTrip.getLongitude());
        args.putString("destination_name", currentFutureTrip.getCity() + ", " + currentFutureTrip.getState());
        try { navController.navigate(R.id.action_futureTripDetailFragment_to_mapActivity, args); }
        catch (IllegalArgumentException e) { Log.e(TAG, "Navigation action not found.", e); }
    }

    private void navigateToEdit() {
        if (currentFutureTrip == null || navController == null || getView() == null) return;
        Log.d(TAG, "Navigating to edit future trip: " + currentFutureTrip.getFutureTripId());
        Bundle args = new Bundle();
        args.putSerializable("futureTripToEdit", currentFutureTrip);
        try {
            navController.navigate(R.id.action_futureTripDetailFragment_to_addFutureTripFragment, args);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Could not navigate to edit fragment", e);
            Toast.makeText(getContext(), "Error opening edit screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        if (getContext() == null || !isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirmation_title)
                .setMessage(R.string.delete_future_trip_confirmation_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.delete_yes, (dialog, which) -> deleteFutureTrip())
                .setNegativeButton(R.string.delete_no, null)
                .show();
    }

    private void deleteFutureTrip() {
        if (currentFutureTrip == null || currentFutureTrip.getFutureTripId() == null || userFutureTripsRef == null || navController == null || getView() == null) {
            Toast.makeText(getContext(), "Error: Cannot delete plan.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Delete failed: missing future trip data or user reference.");
            return;
        }

        Log.d(TAG, "Deleting future trip: " + currentFutureTrip.getFutureTripId());
        userFutureTripsRef.child(currentFutureTrip.getFutureTripId()).removeValue()
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Future trip deleted successfully.");
                        Toast.makeText(getContext(), R.string.future_plan_deleted_success, Toast.LENGTH_SHORT).show();
                        navController.popBackStack();
                    } else {
                        Log.w(TAG, "Failed to delete future trip", task.getException());
                        Toast.makeText(getContext(), getString(R.string.future_plan_delete_failed) + " " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
