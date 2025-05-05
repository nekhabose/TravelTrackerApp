package com.example.traveltracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.traveltracker.R;
import com.example.traveltracker.models.Trip;

import java.util.ArrayList;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList = new ArrayList<>();
    private final OnTripClickListener listener;
    private final Context context;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public TripAdapter(Context context, OnTripClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTrips(List<Trip> trips) {
        this.tripList = trips != null ? trips : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip currentTrip = tripList.get(position);
        holder.bind(currentTrip, listener, context);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewDestination;
        private final TextView textViewDateVisited;
        private final TextView textViewDuration;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewDestination = itemView.findViewById(R.id.textViewDestination);
            textViewDateVisited = itemView.findViewById(R.id.textViewDateVisited);
            textViewDuration = itemView.findViewById(R.id.textViewDuration);
        }

        void bind(final Trip trip, final OnTripClickListener listener, Context context) {
            String destination = trip.getCity() + ", " + trip.getState();
            textViewDestination.setText(destination);
            textViewDateVisited.setText(context.getString(R.string.visited_prefix, trip.getDateVisited()));
            textViewDuration.setText(context.getString(R.string.duration_prefix, trip.getDuration()));



            itemView.setOnClickListener(v -> {
                if(listener != null) listener.onTripClick(trip);
            });
        }
    }
}