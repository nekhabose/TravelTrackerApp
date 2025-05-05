package com.example.traveltracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.traveltracker.R;
import com.example.traveltracker.models.FutureTrip;
import java.util.ArrayList;
import java.util.List;


public class FutureTripAdapter extends RecyclerView.Adapter<FutureTripAdapter.FutureTripViewHolder> {

    private List<FutureTrip> futureTripList = new ArrayList<>();
    private final OnFutureTripClickListener listener;
    private final Context context;


    public interface OnFutureTripClickListener {
        void onFutureTripClick(FutureTrip futureTrip);
    }


    public FutureTripAdapter(Context context, OnFutureTripClickListener listener) {
        this.context = context;
        this.listener = listener;
    }


    public void setFutureTrips(List<FutureTrip> futureTrips) {

        this.futureTripList = (futureTrips != null) ? futureTrips : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FutureTripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single future trip item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_future_trip, parent, false);
        return new FutureTripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FutureTripViewHolder holder, int position) {
        // Get the FutureTrip object for the current position
        FutureTrip currentFutureTrip = futureTripList.get(position);
        // Bind the data to the ViewHolder
        holder.bind(currentFutureTrip, listener, context);
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the list
        return futureTripList.size();
    }


    static class FutureTripViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewFutureDestination;
        private final TextView textViewFutureDatePlanned;
        private final TextView textViewFutureDuration;

        public FutureTripViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewFutureDestination = itemView.findViewById(R.id.textViewFutureDestination);
            textViewFutureDatePlanned = itemView.findViewById(R.id.textViewFutureDatePlanned);
            textViewFutureDuration = itemView.findViewById(R.id.textViewFutureDuration);
        }


        void bind(final FutureTrip futureTrip, final OnFutureTripClickListener listener, Context context) {
            // Construct the destination string
            String destination = futureTrip.getCity() + ", " + futureTrip.getState();
            textViewFutureDestination.setText(destination);

            // Set the text using string resources for formatting
            textViewFutureDatePlanned.setText(context.getString(R.string.planned_prefix, futureTrip.getPlannedDate()));
            textViewFutureDuration.setText(context.getString(R.string.duration_prefix, futureTrip.getDuration()));

            // Set the click listener for the entire item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFutureTripClick(futureTrip);
                }
            });
        }
    }
}