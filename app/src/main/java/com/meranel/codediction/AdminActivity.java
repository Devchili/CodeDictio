package com.meranel.codediction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private Button btnPending, btnApproved, btnDeclined;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        btnPending = findViewById(R.id.btn_pending);
        btnApproved = findViewById(R.id.btn_approved);
        btnDeclined = findViewById(R.id.btn_declined);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        firestore = FirebaseFirestore.getInstance();

        btnPending.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, PendingActivity.class)));
        btnApproved.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, ApprovedActivity.class)));
        btnDeclined.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, DeclinedActivity.class)));

        fabAdd.setOnClickListener(v -> showAddWordDialog());

        // Load counts and set button texts
        loadCounts();
    }

    private void showAddWordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_word, null);

        EditText etWord = view.findViewById(R.id.et_word);
        EditText etMeaning = view.findViewById(R.id.et_meaning);
        Button btnAdd = view.findViewById(R.id.btn_add);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnAdd.setOnClickListener(v -> {
            String word = etWord.getText().toString().trim();
            String meaning = etMeaning.getText().toString().trim();

            if (!word.isEmpty() && !meaning.isEmpty()) {
                addWordToFirestore(word, meaning);
                dialog.dismiss();
            } else {
                if (word.isEmpty()) {
                    etWord.setError("Word is required");
                }
                if (meaning.isEmpty()) {
                    etMeaning.setError("Meaning is required");
                }
            }
        });

        dialog.show();
    }

    private void addWordToFirestore(String word, String meaning) {
        Map<String, String> wordMap = new HashMap<>();
        wordMap.put("word", word);
        wordMap.put("meaning", meaning);
        wordMap.put("author", "admin@gmail.com");

        firestore.collection("wordandmeanings")
                .add(wordMap)
                .addOnSuccessListener(documentReference -> {
                    // Word added successfully
                    loadApprovedCount(); // Update the approved count after adding a new word
                })
                .addOnFailureListener(e -> {
                    // Error adding word
                });
    }

    private void loadCounts() {
        loadPendingCount();
        loadApprovedCount();
        loadDeclinedCount();
    }

    private void loadPendingCount() {
        firestore.collection("pendingMeanings").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int pendingMeaningsCount = task.getResult().size();
                        firestore.collection("pendingAddWordMeaning").get()
                                .addOnCompleteListener(addWordTask -> {
                                    if (addWordTask.isSuccessful() && addWordTask.getResult() != null) {
                                        int pendingAddWordMeaningCount = addWordTask.getResult().size();
                                        int totalCount = pendingMeaningsCount + pendingAddWordMeaningCount;
                                        btnPending.setText("Pending\n\n\n" + totalCount);
                                    } else {
                                        btnPending.setText("Pending\n\n\n" + pendingMeaningsCount);
                                    }
                                });
                    } else {
                        firestore.collection("pendingAddWordMeaning").get()
                                .addOnCompleteListener(addWordTask -> {
                                    if (addWordTask.isSuccessful() && addWordTask.getResult() != null) {
                                        int pendingAddWordMeaningCount = addWordTask.getResult().size();
                                        btnPending.setText("Pending\n\n\n" + pendingAddWordMeaningCount);
                                    } else {
                                        btnPending.setText("Pending\n\n\n0");
                                    }
                                });
                    }
                });
    }

    private void loadApprovedCount() {
        firestore.collection("approvedMeanings").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int approvedMeaningsCount = task.getResult().size();
                        firestore.collection("wordandmeanings").get()
                                .addOnCompleteListener(wordAndMeaningsTask -> {
                                    if (wordAndMeaningsTask.isSuccessful() && wordAndMeaningsTask.getResult() != null) {
                                        int wordAndMeaningsCount = wordAndMeaningsTask.getResult().size();
                                        int totalCount = approvedMeaningsCount + wordAndMeaningsCount;
                                        btnApproved.setText("Approved\n\n\n" + totalCount);
                                    } else {
                                        btnApproved.setText("Approved\n\n\n" + approvedMeaningsCount);
                                    }
                                });
                    } else {
                        firestore.collection("wordandmeanings").get()
                                .addOnCompleteListener(wordAndMeaningsTask -> {
                                    if (wordAndMeaningsTask.isSuccessful() && wordAndMeaningsTask.getResult() != null) {
                                        int wordAndMeaningsCount = wordAndMeaningsTask.getResult().size();
                                        btnApproved.setText("Approved\n\n\n" + wordAndMeaningsCount);
                                    } else {
                                        btnApproved.setText("Approved\n\n\n0");
                                    }
                                });
                    }
                });
    }

    private void loadDeclinedCount() {
        firestore.collection("DeclinedMeanings").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int declinedMeaningsCount = task.getResult().size();
                        firestore.collection("declinedMeanings").get()
                                .addOnCompleteListener(wordMeaningTask -> {
                                    if (wordMeaningTask.isSuccessful() && wordMeaningTask.getResult() != null) {
                                        int declinedWordMeaningCount = wordMeaningTask.getResult().size();
                                        int totalCount = declinedMeaningsCount + declinedWordMeaningCount;
                                        btnDeclined.setText("Declined\n\n\n" + totalCount);
                                    } else {
                                        btnDeclined.setText("Declined\n\n\n" + declinedMeaningsCount);
                                    }
                                });
                    } else {
                        firestore.collection("declinedMeanings").get()
                                .addOnCompleteListener(wordMeaningTask -> {
                                    if (wordMeaningTask.isSuccessful() && wordMeaningTask.getResult() != null) {
                                        int declinedWordMeaningCount = wordMeaningTask.getResult().size();
                                        btnDeclined.setText("Declined\n\n\n" + declinedWordMeaningCount);
                                    } else {
                                        btnDeclined.setText("Declined\n\n\n0");
                                    }
                                });
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_logout) {
            logout();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(AdminActivity.this, Login.class));
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
