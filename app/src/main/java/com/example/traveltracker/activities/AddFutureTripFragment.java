package com.example.traveltracker.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.traveltracker.BuildConfig;
import com.example.traveltracker.R;
import com.example.traveltracker.databinding.FragmentAddFutureTripBinding;
import com.example.traveltracker.models.FutureTrip;
import com.example.traveltracker.utils.GeocoderUtil;
import com.google.android.gms.maps.model.LatLng;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Candidate;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AddFutureTripFragment extends Fragment {

    private static final String TAG = "AddFutureTripFragment";
    private FragmentAddFutureTripBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private NavController navController;
    private String existingFutureTripId = null;
    private FutureTrip futureTripToEdit = null;

    private Calendar selectedDateCalendar = Calendar.getInstance();
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private GenerativeModelFutures geminiModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference("future_trips");
        initializeGemini();

        if (getArguments() != null) {
            if (getArguments().containsKey("futureTripToEdit")) {
                try {
                    futureTripToEdit = (FutureTrip) getArguments().getSerializable("futureTripToEdit");
                    if (futureTripToEdit != null) {
                        existingFutureTripId = futureTripToEdit.getFutureTripId();
                        Log.d(TAG, "Editing existing future trip with ID: " + existingFutureTripId);
                    } else {
                        Log.w(TAG, "Received null futureTripToEdit object.");
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "Argument type mismatch for futureTripToEdit", e);
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddFutureTripBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.loginFragment) {
                navController.popBackStack(R.id.loginFragment, true);
            }
            return;
        }

        setupDatePicker();
        binding.buttonSaveFutureTrip.setOnClickListener(v -> attemptSaveFutureTrip());

        if (futureTripToEdit != null) {
            Log.d(TAG, "Pre-populating future trip fields for editing.");
            binding.editTextCityFuture.setText(futureTripToEdit.getCity());
            binding.editTextStateFuture.setText(futureTripToEdit.getState());
            binding.editTextDurationFuture.setText(futureTripToEdit.getDuration());
            setCalendarDateFromString(futureTripToEdit.getPlannedDate());
            updateLabel();
            binding.buttonSaveFutureTrip.setText(R.string.update_future_trip);
        } else {
            Log.d(TAG, "Adding new future trip.");
            binding.buttonSaveFutureTrip.setText(R.string.save_future_trip);
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
        binding.editTextDateFuture.setFocusable(false);
        binding.editTextDateFuture.setClickable(true);
        binding.editTextDateFuture.setOnClickListener(v -> showDatePickerDialog(dateSetListener));
        binding.textFieldDateFuture.setEndIconOnClickListener(v -> showDatePickerDialog(dateSetListener));
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
        binding.editTextDateFuture.setText(sdf.format(selectedDateCalendar.getTime()));
    }

    private void initializeGemini() {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_PLACEHOLDER")) {
            Log.e(TAG, "Gemini API Key not found or is placeholder.");
            geminiModel = null;
            return;
        }
        try {
            GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
            geminiModel = GenerativeModelFutures.from(gm);
            Log.d(TAG, "Gemini Model Initialized for Future Trips");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Gemini Model", e);
            Toast.makeText(getContext(), "Failed to init AI features.", Toast.LENGTH_SHORT).show();
            geminiModel = null;
        }
    }

    private void attemptSaveFutureTrip() {
        if (binding == null || currentUser == null) return;

        String city = binding.editTextCityFuture.getText().toString().trim();
        String state = binding.editTextStateFuture.getText().toString().trim();
        String plannedDate = binding.editTextDateFuture.getText().toString().trim();
        String duration = binding.editTextDurationFuture.getText().toString().trim();

        boolean valid = true;
        if (TextUtils.isEmpty(city)) { binding.textFieldCityFuture.setError("City required"); binding.editTextCityFuture.requestFocus(); valid = false; } else { binding.textFieldCityFuture.setError(null); }
        if (TextUtils.isEmpty(state)) { binding.textFieldStateFuture.setError("State required"); if(valid) binding.editTextStateFuture.requestFocus(); valid = false; } else { binding.textFieldStateFuture.setError(null); }
        if (TextUtils.isEmpty(plannedDate)) { binding.textFieldDateFuture.setError("Date required"); if(valid) binding.textFieldDateFuture.requestFocus(); valid = false; } else { binding.textFieldDateFuture.setError(null); }
        if (TextUtils.isEmpty(duration)) { binding.textFieldDurationFuture.setError("Duration required"); if(valid) binding.editTextDurationFuture.requestFocus(); valid = false; } else { binding.textFieldDurationFuture.setError(null); }

        if (!valid) return;

        showLoading(true);

        AtomicReference<LatLng> coordinatesRef = new AtomicReference<>();
        AtomicReference<String> itineraryRef = new AtomicReference<>("");
        AtomicInteger tasksCompleted = new AtomicInteger(0);
        boolean isEditing = (existingFutureTripId != null);
        boolean shouldGenerateItinerary = geminiModel != null;
        final int TOTAL_TASKS = shouldGenerateItinerary ? 2 : 1;

        if (!shouldGenerateItinerary) {
            itineraryRef.set("AI Itinerary feature not available or failed to initialize.");
        } else {
            itineraryRef.set("Itinerary generation failed or skipped.");
        }

        Runnable checkCompletion = () -> {
            if (tasksCompleted.incrementAndGet() >= TOTAL_TASKS) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "All background tasks complete. Saving to DB.");
                        saveFutureTripToDatabase(city, state, plannedDate, duration,
                                coordinatesRef.get(), itineraryRef.get());
                    });
                } else { Log.w(TAG, "Activity detached after background tasks."); }
            }
        };

        backgroundExecutor.execute(() -> {
            Log.d(TAG, "Starting geocoding for future trip...");
            coordinatesRef.set(GeocoderUtil.getLatLngFromAddress(requireContext(), city, state));
            Log.d(TAG, "Geocoding complete. Coords: " + coordinatesRef.get());
            checkCompletion.run();
        });

        if (shouldGenerateItinerary) {
            backgroundExecutor.execute(() -> {
                Log.d(TAG, "Starting itinerary generation (Edit Mode: " + isEditing + ")...");
                generateItinerary(city, state, duration, new FutureCallback<String>() {
                    @Override public void onSuccess(@Nullable String result) {
                        Log.d(TAG, "Itinerary generation success.");
                        itineraryRef.set(result != null && !result.isEmpty() ? result : "No suggestions generated.");
                        checkCompletion.run();
                    }
                    @Override public void onFailure(@NonNull Throwable t) {
                        Log.e(TAG, "Itinerary generation failed", t);
                        itineraryRef.set("Could not generate itinerary: " + t.getMessage());
                        checkCompletion.run();
                    }
                });
            });
        }
    }

    private void generateItinerary(String city, String state, String duration, FutureCallback<String> callback) {
        if (geminiModel == null) { callback.onFailure(new IllegalStateException("Gemini model not initialized.")); return; }
        String prompt = String.format(Locale.US, "Create a sample itinerary suggestion list for a trip to %s, %s lasting %s. Focus on popular tourist attractions and activities. Format it as a simple list (e.g., using bullet points or numbered days if duration suggests it).", city, state, duration);
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = geminiModel.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse result) {
                String resultText = "Could not parse AI response.";
                try {
                    if (result != null && result.getText() != null) { resultText = result.getText(); }
                    else if (result != null && result.getCandidates() != null && !result.getCandidates().isEmpty()){
                        Candidate firstCandidate = result.getCandidates().get(0);
                        if (firstCandidate != null && firstCandidate.getContent() != null && !firstCandidate.getContent().getParts().isEmpty()){
                            Part firstPart = firstCandidate.getContent().getParts().get(0);
                            if (firstPart instanceof TextPart) { resultText = ((TextPart) firstPart).getText(); }
                        }
                    }
                } catch (Exception e) { Log.e(TAG,"Error parsing Gemini response", e); }
                callback.onSuccess(resultText);
            }
            @Override public void onFailure(@NonNull Throwable t) { callback.onFailure(t); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }


    private void saveFutureTripToDatabase(String city, String state, String plannedDate, String duration, LatLng coordinates, String itinerary) {
        if (currentUser == null) { if (getActivity() != null) getActivity().runOnUiThread(() -> showLoading(false)); return; }
        String userId = currentUser.getUid();

        FutureTrip tripData = new FutureTrip(userId, city, state, plannedDate, duration);
        if (coordinates != null) { tripData.setLatitude(coordinates.latitude); tripData.setLongitude(coordinates.longitude); }
        tripData.setGeneratedItinerary(itinerary != null ? itinerary : "");

        Task<Void> saveTask;
        String successMessage;
        String failureMessagePrefix;
        String idToUse;

        if (existingFutureTripId != null) {
            idToUse = existingFutureTripId;
            tripData.setFutureTripId(idToUse);
            Log.d(TAG, "Updating future trip data to /future_trips/" + userId + "/" + idToUse);
            saveTask = mDatabase.child(userId).child(idToUse).setValue(tripData);
            successMessage = getString(R.string.future_plan_updated_success);
            failureMessagePrefix = getString(R.string.future_plan_update_failed);
        } else {
            idToUse = mDatabase.child(userId).push().getKey();
            if (idToUse == null) { Log.e(TAG, "Null push key"); if(getActivity()!=null) getActivity().runOnUiThread(()->{ showLoading(false); Toast.makeText(getContext(), "Error: Cannot generate plan ID.", Toast.LENGTH_SHORT).show(); }); return; }
            tripData.setFutureTripId(idToUse);
            Log.d(TAG, "Saving new future trip data to /future_trips/" + userId + "/" + idToUse);
            saveTask = mDatabase.child(userId).child(idToUse).setValue(tripData);
            successMessage = getString(R.string.save_future_trip);
            failureMessagePrefix = "Failed to save plan:";
        }

        saveTask.addOnCompleteListener(requireActivity(), task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Log.d(TAG, successMessage);
                Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();

                if (navController != null && getView() != null) {
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.dashboardFragment, false)
                            .build();
                    try {
                        navController.navigate(R.id.dashboardFragment, null, navOptions);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Could not navigate back to Dashboard from AddFutureTrip.", e);
                        navController.popBackStack();
                    }
                }
            } else {
                Log.w(TAG, failureMessagePrefix, task.getException());
                Toast.makeText(getContext(), failureMessagePrefix + " " + (task.getException() != null ? task.getException().getMessage() : "Unknown"), Toast.LENGTH_LONG).show();
            }
        });
    }


    private void showLoading(boolean isLoading) {
        if (binding == null) return;
        binding.progressBarAddFutureTrip.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonSaveFutureTrip.setEnabled(!isLoading);
        binding.editTextCityFuture.setEnabled(!isLoading);
        binding.editTextStateFuture.setEnabled(!isLoading);
        binding.editTextDateFuture.setEnabled(!isLoading);
        binding.editTextDurationFuture.setEnabled(!isLoading);
        binding.textFieldCityFuture.setEnabled(!isLoading);
        binding.textFieldStateFuture.setEnabled(!isLoading);
        binding.textFieldDateFuture.setEnabled(!isLoading);
        binding.textFieldDurationFuture.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

