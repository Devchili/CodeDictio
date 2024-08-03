package com.meranel.codediction;

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ApprovedActivity extends AppCompatActivity {
    private ListView listView;
    private ApprovedAdapter adapter;
    private ArrayList<String> listItems;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approved);
        listView = findViewById(R.id.approved_list_view);
        listItems = new ArrayList<>();
        adapter = new ApprovedAdapter(this, listItems);
        listView.setAdapter(adapter);
        firestore = FirebaseFirestore.getInstance();
        loadApprovedMeanings();
    }

    private void loadApprovedMeanings() {
        // First query for approvedMeanings collection
        firestore.collection("approvedMeanings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listItems.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String word = document.getString("word");
                            String newMeaning = document.getString("newMeaning");
                            String author = document.getString("author"); // Retrieving the author field

                            // Constructing the display text to include the author
                            String displayText = word + ": " + newMeaning + " (by " + author + ")";
                            listItems.add(displayText);
                        }
                        loadWordAndMeanings(); // Load the second collection after the first one
                    }
                });
    }

    private void loadWordAndMeanings() {
        // Second query for wordandmeanings collection
        firestore.collection("wordandmeanings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String word = document.getString("word");
                            String meaning = document.getString("meaning");
                            String author = document.getString("author"); // Retrieving the author field

                            // Constructing the display text to include the author
                            String displayText = word + ": " + meaning + " (by " + author + ")";
                            listItems.add(displayText);
                        }
                        adapter.notifyDataSetChanged(); // Notify adapter after both collections are loaded
                    }
                });
    }
}
