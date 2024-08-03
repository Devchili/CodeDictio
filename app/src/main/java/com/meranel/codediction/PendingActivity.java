package com.meranel.codediction;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PendingActivity extends AppCompatActivity {

    private ListView requestsListView;
    private RequestAdapter adapter;
    private ArrayList<String> requestList;
    private ArrayList<DocumentReference> documentReferences;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending);

        requestsListView = findViewById(R.id.requests_list_view);
        firestore = FirebaseFirestore.getInstance();

        requestList = new ArrayList<>();
        documentReferences = new ArrayList<>();
        adapter = new RequestAdapter(this, requestList, this);
        requestsListView.setAdapter(adapter);

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        // Fetch from pendingMeanings collection
        firestore.collection("pendingMeanings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        requestList.clear();
                        documentReferences.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String word = document.getString("word");
                            String newMeaning = document.getString("newMeaning");
                            String author = document.getString("author");
                            requestList.add("Meaning: " + word + ": " + newMeaning + " (by " + author + ")");
                            documentReferences.add(document.getReference());
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(PendingActivity.this, "Error loading requests", Toast.LENGTH_SHORT).show();
                    }
                });

        // Fetch from pendingAddWordMeaning collection
        firestore.collection("pendingAddWordMeaning")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String word = document.getString("word");
                            String meaning = document.getString("meaning");
                            String author = document.getString("author");
                            requestList.add("Word: " + word + ": " + meaning + " (by " + author + ")");
                            documentReferences.add(document.getReference());
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(PendingActivity.this, "Error loading requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void onAcceptClick(View view, int position) {
        DocumentReference documentReference = documentReferences.get(position);
        documentReference.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String collectionName;
                        Map<String, Object> approvedData = new HashMap<>();

                        if (documentSnapshot.contains("newMeaning")) {
                            // It comes from pendingMeanings
                            String word = documentSnapshot.getString("word");
                            String newMeaning = documentSnapshot.getString("newMeaning");
                            String author = documentSnapshot.getString("author");

                            approvedData.put("word", word);
                            approvedData.put("newMeaning", newMeaning);
                            approvedData.put("author", author);

                            collectionName = "approvedMeanings";
                        } else {
                            // It comes from pendingAddWordMeaning
                            String word = documentSnapshot.getString("word");
                            String meaning = documentSnapshot.getString("meaning");
                            String author = documentSnapshot.getString("author");

                            approvedData.put("word", word);
                            approvedData.put("meaning", meaning);
                            approvedData.put("author", author);

                            collectionName = "wordandmeanings";
                        }

                        firestore.collection(collectionName)
                                .add(approvedData)
                                .addOnSuccessListener(documentReference1 -> {
                                    documentReference.delete();
                                    loadPendingRequests();
                                    Toast.makeText(PendingActivity.this, "Request accepted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(PendingActivity.this, "Error accepting request", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(PendingActivity.this, "Document does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(PendingActivity.this, "Error getting document", Toast.LENGTH_SHORT).show());
    }

    public void onDeclineClick(View view, int position) {
        DocumentReference documentReference = documentReferences.get(position);
        documentReference.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> declinedData = new HashMap<>();
                        declinedData.put("word", documentSnapshot.getString("word"));
                        declinedData.put("author", documentSnapshot.getString("author"));
                        declinedData.put("declinedAt", new java.util.Date());

                        if (documentSnapshot.contains("newMeaning")) {
                            // It comes from pendingMeanings
                            declinedData.put("newMeaning", documentSnapshot.getString("newMeaning"));
                            firestore.collection("declinedMeanings").add(declinedData)
                                    .addOnSuccessListener(documentReference1 -> {
                                        documentReference.delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    loadPendingRequests();
                                                    Toast.makeText(PendingActivity.this, "Request declined and archived", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(PendingActivity.this, "Error deleting the original request", Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(PendingActivity.this, "Error saving declined request", Toast.LENGTH_SHORT).show());
                        } else {
                            // It comes from pendingAddWordMeaning
                            declinedData.put("meaning", documentSnapshot.getString("meaning"));
                            firestore.collection("declinedWordMeaning").add(declinedData)
                                    .addOnSuccessListener(documentReference1 -> {
                                        documentReference.delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    loadPendingRequests();
                                                    Toast.makeText(PendingActivity.this, "Request declined and archived", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(PendingActivity.this, "Error deleting the original request", Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(PendingActivity.this, "Error saving declined request", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(PendingActivity.this, "Document does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(PendingActivity.this, "Error retrieving document", Toast.LENGTH_SHORT).show());
    }
}
