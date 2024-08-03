package com.meranel.codediction;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookmarksFragment extends Fragment implements OnBookmarkClickListener {

    private ListView bookmarksListView;
    private List<String> bookmarkedWords;
    private BookmarkAdapter adapter;
    private DatabaseHelper databaseHelper;
    private FirebaseFirestore firestore;
    private Map<String, String> localDictionary;

    public BookmarksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());
        firestore = FirebaseFirestore.getInstance();
        bookmarkedWords = new ArrayList<>(); // Initialize the list here
        loadBookmarkedWords();
        loadDictionaryFromJson();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmarks, container, false);
        bookmarksListView = view.findViewById(R.id.bookmarks_list_view);
        adapter = new BookmarkAdapter(requireContext(), R.layout.list_item_bookmark, bookmarkedWords, databaseHelper, this);
        bookmarksListView.setAdapter(adapter);

        return view;
    }

    private void loadBookmarkedWords() {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                List<String> words = new ArrayList<>();
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT word FROM bookmarks", null);
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            @SuppressLint("Range") String word = cursor.getString(cursor.getColumnIndex("word"));
                            words.add(word);
                        } while (cursor.moveToNext());
                    }
                } finally {
                    cursor.close();
                    db.close();
                }
                return words;
            }

            @Override
            protected void onPostExecute(List<String> words) {
                bookmarkedWords.clear();
                bookmarkedWords.addAll(words);
                adapter.notifyDataSetChanged();
            }
        }.execute();
    }

    private void loadDictionaryFromJson() {
        localDictionary = new HashMap<>();
        try (InputStream inputStream = requireActivity().getAssets().open("dictionary.json")) {
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            JSONArray wordsArray = jsonObject.getJSONArray("words");
            for (int i = 0; i < wordsArray.length(); i++) {
                JSONObject wordObject = wordsArray.getJSONObject(i);
                String word = wordObject.getString("word");
                String meaning = wordObject.getString("meaning");
                localDictionary.put(word.toLowerCase(), meaning);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayMeanings(String selectedWord) {
        StringBuilder meanings = new StringBuilder();

        // Retrieve and display meanings from local dictionary
        String localMeaning = localDictionary.get(selectedWord.toLowerCase());
        if (localMeaning != null) {
            meanings.append(localMeaning).append("\n\n");
        }

        // Create tasks for both Firestore collections
        Task<QuerySnapshot> approvedMeaningsTask = firestore.collection("approvedMeanings")
                .whereEqualTo("word", selectedWord)
                .get();
        Task<QuerySnapshot> wordAndMeaningsTask = firestore.collection("wordandmeanings")
                .whereEqualTo("word", selectedWord)
                .get();

        // Combine both tasks
        Tasks.whenAllSuccess(approvedMeaningsTask, wordAndMeaningsTask)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Process approvedMeanings collection
                        QuerySnapshot approvedMeaningsResult = (QuerySnapshot) task.getResult().get(0);
                        for (QueryDocumentSnapshot document : approvedMeaningsResult) {
                            String newMeaning = document.getString("newMeaning");
                            if (newMeaning != null) {
                                meanings.append(newMeaning).append("\n\n");
                            }
                        }

                        // Process wordandmeanings collection
                        QuerySnapshot wordAndMeaningsResult = (QuerySnapshot) task.getResult().get(1);
                        for (QueryDocumentSnapshot document : wordAndMeaningsResult) {
                            String meaning = document.getString("meaning");
                            if (meaning != null) {
                                meanings.append(meaning).append("\n\n");
                            }
                        }

                        // Display the meanings in a dialog
                        if (meanings.length() > 0) {
                            showMeaningDialog(selectedWord, meanings.toString());
                        } else {
                            showMeaningDialog(selectedWord, getString(R.string.no_meanings_found));
                        }
                    } else {
                        showMeaningDialog(selectedWord, getString(R.string.error_fetching_meanings));
                    }
                });
    }

    private void showMeaningDialog(String word, String meanings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(word);
        builder.setMessage(meanings);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void onBookmarkClick(String word) {
        displayMeanings(word);
    }

    private static class BookmarkAdapter extends ArrayAdapter<String> {

        private final Context context;
        private final int resource;
        private final List<String> bookmarkedWords;
        private final DatabaseHelper databaseHelper;
        private final OnBookmarkClickListener clickListener;

        public BookmarkAdapter(@NonNull Context context, int resource, @NonNull List<String> objects, DatabaseHelper databaseHelper, OnBookmarkClickListener clickListener) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.bookmarkedWords = objects;
            this.databaseHelper = databaseHelper;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                itemView = inflater.inflate(resource, parent, false);
            }

            // Bind data to views
            String word = getItem(position);
            TextView textViewWord = itemView.findViewById(R.id.text_bookmark_word);
            textViewWord.setText(word);

            // Set click listener to show meanings dialog
            itemView.setOnClickListener(v -> clickListener.onBookmarkClick(word));

            // Remove bookmark button
            ImageButton removeBookmarkButton = itemView.findViewById(R.id.remove_bookmark_button);
            removeBookmarkButton.setOnClickListener(v -> removeBookmark(word));

            return itemView;
        }

        private void removeBookmark(String word) {
            // Remove the bookmark from the database
            databaseHelper.deleteBookmark(word);
            // Remove the word from the list and update the adapter
            bookmarkedWords.remove(word);
            notifyDataSetChanged();
        }
    }
}
