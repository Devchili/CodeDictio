package com.meranel.codediction;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthorMeaningAdapter extends RecyclerView.Adapter<AuthorMeaningAdapter.AuthorMeaningViewHolder> {

    private Context context;
    private List<AuthorMeaning> authorMeaningList;

    public AuthorMeaningAdapter(Context context, List<AuthorMeaning> authorMeaningList) {
        this.context = context;
        this.authorMeaningList = authorMeaningList;
        fetchAndSortRatings();
    }

    @NonNull
    @Override
    public AuthorMeaningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_author_meaning, parent, false);
        return new AuthorMeaningViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuthorMeaningViewHolder holder, int position) {
        AuthorMeaning authorMeaning = authorMeaningList.get(position);
        holder.authorTextView.setText(authorMeaning.getAuthor());
        holder.meaningTextView.setText(authorMeaning.getMeaning());
        holder.ratingCountTextView.setText("Ratings: " + authorMeaning.getRatingCount());

        // Update button drawable based on rating
        holder.rateButton.setBackgroundResource(authorMeaning.getRating() > 0 ? R.drawable.ic_star_filled : R.drawable.ic_star);

        holder.rateButton.setOnClickListener(v -> showRatingDialog(authorMeaning, holder));
    }

    @Override
    public int getItemCount() {
        return authorMeaningList.size();
    }

    private void showRatingDialog(AuthorMeaning authorMeaning, AuthorMeaningViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        Button submitButton = dialogView.findViewById(R.id.submit_button);

        // Hide the submit button
        submitButton.setVisibility(View.GONE);

        builder.setView(dialogView)
                .setTitle("Rate this meaning")
                .setPositiveButton("Submit", (dialog, which) -> {
                    int rating = (int) ratingBar.getRating();
                    submitRating(authorMeaning, rating, holder);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitRating(AuthorMeaning authorMeaning, int rating, AuthorMeaningViewHolder holder) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("meaningId", authorMeaning.getId());
        ratingData.put("rating", rating);

        firestore.collection("ratingsPerMeaning")
                .document(authorMeaning.getId())
                .set(ratingData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Update local rating and rating count, then notify adapter
                    authorMeaning.setRating(rating);
                    authorMeaning.setRatingCount(authorMeaning.getRatingCount() + 1);
                    holder.rateButton.setBackgroundResource(R.drawable.ic_star_filled);

                    // Increment rating count in approvedMeanings collection
                    DocumentReference approvedMeaningRef = firestore.collection("approvedMeanings").document(authorMeaning.getId());
                    approvedMeaningRef.update("ratingCount", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid1 -> fetchAndSortRatings())
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating rating count", e));

                    // Increment rating count in wordandmeanings collection
                    DocumentReference wordAndMeaningsRef = firestore.collection("wordandmeanings").document(authorMeaning.getId());
                    wordAndMeaningsRef.update("ratingCount", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid1 -> fetchAndSortRatings())
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating rating count in wordandmeanings", e));
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error submitting rating", e));
    }

    private void fetchAndSortRatings() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("ratingsPerMeaning")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Long> ratingCounts = new HashMap<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            String meaningId = document.getString("meaningId");
                            Long rating = document.getLong("rating");

                            for (AuthorMeaning authorMeaning : authorMeaningList) {
                                if (authorMeaning.getId().equals(meaningId)) {
                                    authorMeaning.setRating(rating != null ? rating.intValue() : 0);
                                    ratingCounts.put(meaningId, ratingCounts.getOrDefault(meaningId, 0L) + 1);
                                    break;
                                }
                            }
                        }

                        // Update rating counts from approvedMeanings collection
                        firestore.collection("approvedMeanings")
                                .get()
                                .addOnCompleteListener(approvedMeaningsTask -> {
                                    if (approvedMeaningsTask.isSuccessful() && approvedMeaningsTask.getResult() != null) {
                                        for (DocumentSnapshot document : approvedMeaningsTask.getResult()) {
                                            String meaningId = document.getId();
                                            Long ratingCount = document.getLong("ratingCount");

                                            for (AuthorMeaning authorMeaning : authorMeaningList) {
                                                if (authorMeaning.getId().equals(meaningId)) {
                                                    authorMeaning.setRatingCount(ratingCount != null ? ratingCount : 0);
                                                    break;
                                                }
                                            }
                                        }

                                        // Sort list by rating in descending order
                                        Collections.sort(authorMeaningList, (a1, a2) -> Integer.compare(a2.getRating(), a1.getRating()));
                                        notifyDataSetChanged();
                                    }
                                });

                        // Update rating counts from wordandmeanings collection
                        firestore.collection("wordandmeanings")
                                .get()
                                .addOnCompleteListener(wordAndMeaningsTask -> {
                                    if (wordAndMeaningsTask.isSuccessful() && wordAndMeaningsTask.getResult() != null) {
                                        for (DocumentSnapshot document : wordAndMeaningsTask.getResult()) {
                                            String meaningId = document.getId();
                                            Long ratingCount = document.getLong("ratingCount");

                                            for (AuthorMeaning authorMeaning : authorMeaningList) {
                                                if (authorMeaning.getId().equals(meaningId)) {
                                                    authorMeaning.setRatingCount(ratingCount != null ? ratingCount : 0);
                                                    break;
                                                }
                                            }
                                        }

                                        // Sort list by rating in descending order
                                        Collections.sort(authorMeaningList, (a1, a2) -> Integer.compare(a2.getRating(), a1.getRating()));
                                        notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }

    public static class AuthorMeaningViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView;
        TextView meaningTextView;
        TextView ratingCountTextView;
        Button rateButton;

        public AuthorMeaningViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.author_text_view);
            meaningTextView = itemView.findViewById(R.id.meaning_text_view);
            ratingCountTextView = itemView.findViewById(R.id.rating_count_text_view);
            rateButton = itemView.findViewById(R.id.rate_button);
        }
    }
}
