package com.example.traveltracker.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.traveltracker.R;
import com.example.traveltracker.adapters.FutureTripAdapter;
import com.example.traveltracker.adapters.TripAdapter;
import com.example.traveltracker.databinding.FragmentDashboardBinding;
import com.example.traveltracker.models.FutureTrip;
import com.example.traveltracker.models.Trip;
import com.example.traveltracker.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DashboardFragment extends Fragment implements TripAdapter.OnTripClickListener, FutureTripAdapter.OnFutureTripClickListener {

    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mPastTripsDbRef;
    private DatabaseReference mFutureTripsDbRef;
    private FirebaseUser currentUser;
    private NavController navController;

    private TripAdapter pastTripAdapter;
    private FutureTripAdapter futureTripAdapter;
    private final List<Trip> pastTripList = new ArrayList<>();
    private final List<FutureTrip> futureTripList = new ArrayList<>();

    private ValueEventListener pastTripsListener;
    private ValueEventListener futureTripsListener;

    // Track if listeners have returned to hide progress bar accurately
    private final AtomicBoolean pastTripsLoaded = new AtomicBoolean(false);
    private final AtomicBoolean futureTripsLoaded = new AtomicBoolean(false);


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User is null in DashboardFragment onCreate.");

            return;
        }
        String userId = currentUser.getUid();
        mPastTripsDbRef = FirebaseDatabase.getInstance().getReference("trips").child(userId);
        mFutureTripsDbRef = FirebaseDatabase.getInstance().getReference("future_trips").child(userId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (currentUser == null) return;

        setupRecyclerViews();

        // FAB click listeners
        binding.fabAddPastTrip.setOnClickListener(v -> {
            Log.d(TAG, "FAB Add Past Trip clicked");
            navController.navigate(R.id.action_dashboardFragment_to_addTripFragment);
        });
        binding.fabAddFutureTrip.setOnClickListener(v -> {
            Log.d(TAG, "FAB Add Future Trip clicked");
            navController.navigate(R.id.action_dashboardFragment_to_addFutureTripFragment);
        });

        Log.d(TAG, "DashboardFragment view created.");

    }

    private void setupRecyclerViews() {
        // Past Trips
        pastTripAdapter = new TripAdapter(requireContext(), this);
        binding.recyclerViewPastTrips.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPastTrips.setAdapter(pastTripAdapter);
        binding.recyclerViewPastTrips.setNestedScrollingEnabled(false);

        // Future Trips
        futureTripAdapter = new FutureTripAdapter(requireContext(), this);
        binding.recyclerViewFutureTrips.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewFutureTrips.setAdapter(futureTripAdapter);
        binding.recyclerViewFutureTrips.setNestedScrollingEnabled(false);
    }

    private void loadPastTrips() {
        if (mPastTripsDbRef == null) {
            Log.e(TAG, "mPastTripsDbRef is null, cannot load past trips.");
            pastTripsLoaded.set(true);
            checkHideLoading();
            return;
        }
        Log.d(TAG, "loadPastTrips called.");
        showLoading(true);
        pastTripsLoaded.set(false);

        if (pastTripsListener != null) {
            mPastTripsDbRef.removeEventListener(pastTripsListener);
        }

        pastTripsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "loadPastTrips: onDataChange CALLED. Has children: " + dataSnapshot.hasChildren());
                pastTripList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Trip trip = snapshot.getValue(Trip.class);
                        if (trip != null) {
                            trip.setTripId(snapshot.getKey());

                            pastTripList.add(trip);
                        } else {
                            Log.w(TAG, "Parsed null Trip object for key: " + snapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing past trip: " + snapshot.getKey(), e);
                    }
                }
                Collections.reverse(pastTripList);
                pastTripAdapter.setTrips(pastTripList);
                updatePastTripVisibility();
                pastTripsLoaded.set(true);
                checkHideLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadPastTrips: onCancelled CALLED", databaseError.toException());
                Toast.makeText(getContext(), "Failed to load past trips: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                updatePastTripVisibility();
                pastTripsLoaded.set(true);
                checkHideLoading();
            }
        };
        mPastTripsDbRef.addValueEventListener(pastTripsListener);
    }

    private void loadFutureTrips() {
        if (mFutureTripsDbRef == null) {
            Log.e(TAG, "mFutureTripsDbRef is null, cannot load future trips.");
            futureTripsLoaded.set(true);
            checkHideLoading();
            return;
        }
        Log.d(TAG, "loadFutureTrips called.");
        showLoading(true);
        futureTripsLoaded.set(false);

        if (futureTripsListener != null) {
            mFutureTripsDbRef.removeEventListener(futureTripsListener);
        }

        futureTripsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "loadFutureTrips: onDataChange CALLED. Has children: " + dataSnapshot.hasChildren());
                futureTripList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        FutureTrip trip = snapshot.getValue(FutureTrip.class);
                        if (trip != null) {
                            trip.setFutureTripId(snapshot.getKey());
                            futureTripList.add(trip);
                        } else {
                            Log.w(TAG, "Parsed null FutureTrip object for key: " + snapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing future trip: " + snapshot.getKey(), e);
                    }
                }

                futureTripAdapter.setFutureTrips(futureTripList);
                updateFutureTripVisibility();
                futureTripsLoaded.set(true);
                checkHideLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadFutureTrips: onCancelled CALLED", error.toException());
                Toast.makeText(getContext(), "Failed to load future trips: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                updateFutureTripVisibility();
                futureTripsLoaded.set(true);
                checkHideLoading();
            }
        };
        mFutureTripsDbRef.addValueEventListener(futureTripsListener);
    }

    private void updatePastTripVisibility() {
        if (binding == null) return;
        boolean isEmpty = pastTripList.isEmpty();
        Log.d(TAG, "Updating past trip visibility. Is empty: " + isEmpty);
        binding.textViewEmptyPast.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewPastTrips.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateFutureTripVisibility() {
        if (binding == null) return;
        boolean isEmpty = futureTripList.isEmpty();
        Log.d(TAG, "Updating future trip visibility. Is empty: " + isEmpty);
        binding.textViewEmptyFuture.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewFutureTrips.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // Hide loading only when BOTH data loads have completed (or failed)
    private void checkHideLoading() {
        if (pastTripsLoaded.get() && futureTripsLoaded.get()) {
            Log.d(TAG, "Both trip lists loaded (or failed), hiding loading indicator.");
            showLoading(false);
        } else {
            Log.d(TAG, "Still waiting for data... Past loaded: " + pastTripsLoaded.get() + ", Future loaded: " + futureTripsLoaded.get());
        }
    }

    private void showLoading(boolean isLoading) {
        if (binding == null) return;
        Log.d(TAG, "showLoading called with: " + isLoading);
        binding.progressBarDashboard.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {

        }
    }

    // Click listener for Past Trips
    @Override
    public void onTripClick(Trip trip) {
        Log.d(TAG, "Clicked past trip: " + trip.getCity());
        Bundle args = new Bundle();
        args.putSerializable(Constants.EXTRA_TRIP, trip);

        if (navController != null && navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getAction(R.id.action_dashboardFragment_to_tripDetailFragment) != null) {
            navController.navigate(R.id.action_dashboardFragment_to_tripDetailFragment, args);
        } else {
            Log.e(TAG, "Cannot navigate to Trip Detail, NavController or Action invalid.");
            Toast.makeText(getContext(), "Error opening trip details.", Toast.LENGTH_SHORT).show();
        }
    }

    // Click listener for Future Trips
    @Override
    public void onFutureTripClick(FutureTrip futureTrip) {
        Log.d(TAG, "Clicked future trip: " + futureTrip.getCity());
        Bundle args = new Bundle();
        args.putSerializable(Constants.EXTRA_FUTURE_TRIP, futureTrip);

        if (navController != null && navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getAction(R.id.action_dashboardFragment_to_futureTripDetailFragment) != null) {
            navController.navigate(R.id.action_dashboardFragment_to_futureTripDetailFragment, args);
        } else {
            Log.e(TAG, "Cannot navigate to Future Trip Detail, NavController or Action invalid.");
            Toast.makeText(getContext(), "Error opening future trip details.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called. Loading data...");
        if (currentUser != null) {

            pastTripsLoaded.set(false);
            futureTripsLoaded.set(false);
            loadPastTrips();
            loadFutureTrips();
        } else {
            Log.w(TAG, "User is null in onStart, cannot load data.");

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called. Removing listeners.");
        if (mPastTripsDbRef != null && pastTripsListener != null) {
            mPastTripsDbRef.removeEventListener(pastTripsListener);
            pastTripsListener = null;
        }
        if (mFutureTripsDbRef != null && futureTripsListener != null) {
            mFutureTripsDbRef.removeEventListener(futureTripsListener);
            futureTripsListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d(TAG, "onDestroyView called.");
    }
}