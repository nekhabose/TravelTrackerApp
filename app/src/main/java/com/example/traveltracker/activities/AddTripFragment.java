package com.example.traveltracker.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions; // Import NavOptions
import androidx.navigation.Navigation;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.example.traveltracker.R;
import com.example.traveltracker.databinding.FragmentAddTripBinding;
import com.example.traveltracker.models.Trip;
import com.example.traveltracker.utils.GeocoderUtil;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AddTripFragment extends Fragment {

    private static final String TAG = "AddTripFragment";
    private FragmentAddTripBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private NavController navController;
    private String existingTripId = null;
    private Trip tripToEdit = null;

    private Calendar selectedDateCalendar = Calendar.getInstance();
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference("trips");

        if (getArguments() != null) {
            if (getArguments().containsKey("tripToEdit")) {
                try {
                    tripToEdit = (Trip) getArguments().getSerializable("tripToEdit");
                    if (tripToEdit != null) {
                        existingTripId = tripToEdit.getTripId();
                        Log.d(TAG, "Editing existing trip with ID: " + existingTripId);
                    } else { Log.w(TAG, "Received null tripToEdit object."); }
                } catch (ClassCastException e) { Log.e(TAG, "Argument type mismatch for tripToEdit", e); }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddTripBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            if(navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.loginFragment) {
                navController.popBackStack(R.id.loginFragment, true);
            }
            return;
        }

        setupDatePicker();
        binding.buttonSaveTrip.setOnClickListener(v -> attemptSaveTrip());

        if (tripToEdit != null) {
            Log.d(TAG, "Pre-populating fields for editing.");
            binding.editTextCity.setText(tripToEdit.getCity());
            binding.editTextState.setText(tripToEdit.getState());
            binding.editTextDuration.setText(tripToEdit.getDuration());
            setCalendarDateFromString(tripToEdit.getDateVisited());
            updateLabel();
            binding.buttonSaveTrip.setText(R.string.update_trip);
        } else {
            Log.d(TAG, "Adding new trip.");
            binding.buttonSaveTrip.setText(R.string.save_trip);
        }
    }

    private void setCalendarDateFromString(String dateString) {
        if (dateString == null || dateString.isEmpty()) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            selectedDateCalendar.setTime(sdf.parse(dateString));
        } catch (java.text.ParseException e) {
            Log.e(TAG, "Error parsing date string: " + dateString, e);
        }
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (dpview, year, monthOfYear, dayOfMonth) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, monthOfYear);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        };
        if (binding == null) return;
        binding.editTextDate.setFocusable(false);
        binding.editTextDate.setClickable(true);
        binding.editTextDate.setOnClickListener(v -> showDatePickerDialog(dateSetListener));
        binding.textFieldDate.setEndIconOnClickListener(v -> showDatePickerDialog(dateSetListener));
    }

    private void showDatePickerDialog(DatePickerDialog.OnDateSetListener listener) {
        new DatePickerDialog(requireContext(), listener,
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateLabel() {
        if (binding == null) return;
        String myFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        binding.editTextDate.setText(sdf.format(selectedDateCalendar.getTime()));
    }

    private void attemptSaveTrip() {
        if (binding == null || currentUser == null) return;

        String city = binding.editTextCity.getText().toString().trim();
        String state = binding.editTextState.getText().toString().trim();
        String dateVisited = binding.editTextDate.getText().toString().trim();
        String duration = binding.editTextDuration.getText().toString().trim();

        boolean valid = true;
        if (TextUtils.isEmpty(city)) { binding.textFieldCity.setError("City is required"); binding.editTextCity.requestFocus(); valid = false; } else { binding.textFieldCity.setError(null); }
        if (TextUtils.isEmpty(state)) { binding.textFieldState.setError("State/Province is required"); if(valid) binding.editTextState.requestFocus(); valid = false; } else { binding.textFieldState.setError(null); }
        if (TextUtils.isEmpty(dateVisited)) { binding.textFieldDate.setError("Date visited is required"); if(valid) binding.textFieldDate.requestFocus(); valid = false; } else { binding.textFieldDate.setError(null); }
        if (TextUtils.isEmpty(duration)) { binding.textFieldDuration.setError("Duration is required"); if(valid) binding.editTextDuration.requestFocus(); valid = false; } else { binding.textFieldDuration.setError(null); }
        if (!valid) return;

        showLoading(true);

        backgroundExecutor.execute(() -> {
            Log.d(TAG, "Starting geocoding in background...");
            LatLng coordinates = null;
            try { coordinates = GeocoderUtil.getLatLngFromAddress(requireContext(), city, state); }
            catch(Exception e) { Log.e(TAG, "Geocoding failed", e); }
            final LatLng finalCoordinates = coordinates;
            Log.d(TAG, "Geocoding finished. Coords: " + finalCoordinates);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    saveTripToDatabase(city, state, dateVisited, duration, finalCoordinates);
                });
            } else {
                Log.w(TAG, "Activity detached during geocoding.");

            }
        });
    }

    private void saveTripToDatabase(String city, String state, String dateVisited, String duration, LatLng coordinates) {
        if (currentUser == null) { if (getActivity() != null) getActivity().runOnUiThread(() -> showLoading(false)); return; }
        String userId = currentUser.getUid();

        Trip tripData = new Trip(city, state, dateVisited, duration, userId);
        if (coordinates != null) {
            tripData.setLatitude(coordinates.latitude);
            tripData.setLongitude(coordinates.longitude);
        } else {
            Log.w(TAG, "Saving logged trip with null coordinates for " + city);
        }

        Task<Void> saveTask;
        String successMessage;
        String failureMessagePrefix;
        String idToUse;

        if (existingTripId != null) {
            idToUse = existingTripId;
            tripData.setTripId(idToUse);
            Log.d(TAG, "Updating trip data at /trips/" + userId + "/" + idToUse);
            saveTask = mDatabase.child(userId).child(idToUse).setValue(tripData);
            successMessage = getString(R.string.trip_updated_success);
            failureMessagePrefix = getString(R.string.trip_update_failed);
        } else {
            idToUse = mDatabase.child(userId).push().getKey();
            if (idToUse == null) {
                Log.e(TAG, "Null push key");
                if(getActivity() != null) getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Error: Cannot generate trip ID.", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            tripData.setTripId(idToUse);
            Log.d(TAG, "Saving new trip data to /trips/" + userId + "/" + idToUse);
            saveTask = mDatabase.child(userId).child(idToUse).setValue(tripData);
            successMessage = getString(R.string.save_trip);
            failureMessagePrefix = "Failed to save trip:";
        }

        saveTask.addOnCompleteListener(requireActivity(), task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Log.d(TAG, successMessage);
                Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
                // *** Navigate Back To Dashboard ***
                if (navController != null && getView() != null) {
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.dashboardFragment, false)
                            .build();
                    try {
                        navController.navigate(R.id.dashboardFragment, null, navOptions);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Could not navigate back to Dashboard from AddTrip.", e);
                        navController.popBackStack();
                    }
                }
            } else {
                Log.w(TAG, failureMessagePrefix, task.getException());
                Toast.makeText(getContext(), failureMessagePrefix + " " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (binding == null) return;
        binding.progressBarAddTrip.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonSaveTrip.setEnabled(!isLoading);

        binding.editTextCity.setEnabled(!isLoading);
        binding.editTextState.setEnabled(!isLoading);
        binding.editTextDate.setEnabled(!isLoading);
        binding.editTextDuration.setEnabled(!isLoading);
        binding.textFieldCity.setEnabled(!isLoading);
        binding.textFieldState.setEnabled(!isLoading);
        binding.textFieldDate.setEnabled(!isLoading);
        binding.textFieldDuration.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

