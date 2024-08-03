package com.meranel.codediction;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements TextToSpeech.OnInitListener {

    private Map<String, Word> dictionary;
    private ImageButton speechToTextButton;
    private ImageButton searchButton;
    private ImageButton speakButton;

    private TextView wordTextView;
    private RecyclerView authorRecyclerView;
    private TextView firestoreMeaningTextView;
    private TextView meaningTextView;
    private TextView viewsTextView;
    private EditText newMeaningEditText;

    private Button submitMeaningButton;
    private ImageButton addNewMeaningButton; // Added this line
    private static final int SPEECH_REQUEST_CODE = 100;
    private TextToSpeech textToSpeech;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textToSpeech = new TextToSpeech(getActivity(), this);
        sharedPreferences = requireContext().getSharedPreferences("bookmarks", Context.MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(requireContext());
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        loadDictionaryFromJson();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeUI(view);
        return view;
    }

    private void initializeUI(View view) {
        firestoreMeaningTextView = view.findViewById(R.id.firestore_meaning_text_view);

        wordTextView = view.findViewById(R.id.word_text_view);
        meaningTextView = view.findViewById(R.id.meaning_text_view);
        viewsTextView = view.findViewById(R.id.views_text_view); // Added views TextView
        speechToTextButton = view.findViewById(R.id.speech_to_text_button);
        searchButton = view.findViewById(R.id.search_button);
        speakButton = view.findViewById(R.id.speak_button);
        ImageButton bookmarkButton = view.findViewById(R.id.bookmark_button);

        newMeaningEditText = view.findViewById(R.id.new_meaning_edit_text);
        submitMeaningButton = view.findViewById(R.id.submit_meaning_button);

        AutoCompleteTextView searchEditText = view.findViewById(R.id.search_edit_text);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(dictionary.keySet()));
        searchEditText.setAdapter(adapter);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.getText().toString().trim());
                return true;
            }
            return false;
        });

        speechToTextButton.setOnClickListener(v -> startSpeechToText());
        searchButton.setOnClickListener(v -> performSearch(searchEditText.getText().toString().trim()));
        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        speakButton.setOnClickListener(v -> speakWord());

        // Handle submission of new meaning
        addNewMeaningButton = view.findViewById(R.id.add_new_meaning_button); // Modified this line
        addNewMeaningButton.setOnClickListener(v -> showNewMeaningDialog());

        // Initialize RecyclerView
        authorRecyclerView = view.findViewById(R.id.authorRecyclerView);
        authorRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize FloatingActionButton
        FloatingActionButton fabAddWord = view.findViewById(R.id.fab_add_word);
        fabAddWord.setOnClickListener(v -> showAddWordDialog());

        // Fetch words and meanings from Firestore
        fetchWordsAndMeaningsFromFirestore();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getContext(), "Language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNewMeaningDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_meaning, null);

        // Find views in the dialog layout
        newMeaningEditText = dialogView.findViewById(R.id.new_meaning_edit_text);
        Button submitButton = dialogView.findViewById(R.id.submit_meaning_button);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        builder.setTitle("Add New Meaning");

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle submit button click
        submitButton.setOnClickListener(v -> {
            if (newMeaningEditText != null) {
                String newMeaning = newMeaningEditText.getText().toString().trim();
                if (!newMeaning.isEmpty()) {
                    // Call submitNewMeaning method with the new meaning
                    submitNewMeaning(newMeaning);
                    dialog.dismiss(); // Dismiss the dialog after submission
                } else {
                    Toast.makeText(getContext(), "Please enter a meaning", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle null reference gracefully
                Toast.makeText(getContext(), "Error: EditText is null", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddWordDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_word, null);

        // Find views in the dialog layout
        EditText newWordEditText = dialogView.findViewById(R.id.et_word);
        EditText newMeaningEditText = dialogView.findViewById(R.id.et_meaning);
        Button submitButton = dialogView.findViewById(R.id.btn_add);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        builder.setTitle("Add New Word");

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle submit button click
        submitButton.setOnClickListener(v -> {
            String newWord = newWordEditText.getText().toString().trim();
            String newMeaning = newMeaningEditText.getText().toString().trim();
            if (!newWord.isEmpty() && !newMeaning.isEmpty()) {
                submitNewWord(newWord, newMeaning);
                dialog.dismiss(); // Dismiss the dialog after submission
            } else {
                Toast.makeText(getContext(), "Please enter a word and a meaning", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitNewWord(String word, String meaning) {
        String currentUserEmail = mAuth.getCurrentUser().getEmail();

        if (currentUserEmail != null) {
            Map<String, Object> newWordMap = new HashMap<>();
            newWordMap.put("word", word);
            newWordMap.put("meaning", meaning);
            newWordMap.put("author", currentUserEmail);
            newWordMap.put("status", "pending");

            firestore.collection("pendingAddWordMeaning").add(newWordMap)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Word submitted for approval", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error submitting word", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "Error: User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDictionaryFromJson() {
        dictionary = new HashMap<>();
        try (InputStream inputStream = getActivity().getAssets().open("dictionary.json")) {
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            JSONArray wordsArray = jsonObject.getJSONArray("words");
            for (int i = 0; i < wordsArray.length(); i++) {
                JSONObject wordObject = wordsArray.getJSONObject(i);
                String word = wordObject.getString("word");
                String meaning = wordObject.getString("meaning");
                Word newWord = new Word(word, meaning);
                newWord.setBookmarked(sharedPreferences.getBoolean(word, false));
                dictionary.put(word.toLowerCase(), newWord);
            }
        } catch (IOException | JSONException e) {
            Toast.makeText(getContext(), "Error loading dictionary", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveDictionaryToJson() {
        try {
            JSONArray wordsArray = new JSONArray();
            for (Word word : dictionary.values()) {
                JSONObject wordObject = new JSONObject();
                wordObject.put("word", word.getWord());
                wordObject.put("meaning", word.getMeaning());
                wordsArray.put(wordObject);
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("words", wordsArray);

            String jsonString = jsonObject.toString();

            try (FileOutputStream outputStream = getActivity().openFileOutput("dictionary.json", Context.MODE_PRIVATE)) {
                outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
            }
        } catch (JSONException | IOException e) {
            Toast.makeText(getContext(), "Error saving dictionary", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void fetchWordsAndMeaningsFromFirestore() {
        firestore.collection("wordandmeanings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String word = document.getString("word");
                            String meaning = document.getString("meaning");

                            if (word != null && meaning != null && !dictionary.containsKey(word.toLowerCase())) {
                                Word newWord = new Word(word, meaning);
                                dictionary.put(word.toLowerCase(), newWord);
                            }
                        }
                        saveDictionaryToJson();
                    } else {
                        Toast.makeText(getContext(), "Error fetching words from Firestore", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                performSearch(result.get(0));
            }
        }
    }

    private void performSearch(String searchTerm) {
        // Trim and check the searchTerm
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            Log.e("Search", "Invalid search term: " + searchTerm);
            return;
        }

        searchTerm = searchTerm.trim();

        // Clear the previous views
        authorRecyclerView.setAdapter(null);

        // Create tasks for both collections
        Task<QuerySnapshot> approvedMeaningsTask = firestore.collection("approvedMeanings")
                .whereEqualTo("word", searchTerm)
                .get();
        Task<QuerySnapshot> wordAndMeaningsTask = firestore.collection("wordandmeanings")
                .whereEqualTo("word", searchTerm)
                .get();

        // Combine both tasks
        String finalSearchTerm = searchTerm;
        Tasks.whenAllSuccess(approvedMeaningsTask, wordAndMeaningsTask)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Object> results = (List<Object>) task.getResult();
                        QuerySnapshot approvedMeaningsResult = (QuerySnapshot) results.get(0);
                        QuerySnapshot wordAndMeaningsResult = (QuerySnapshot) results.get(1);

                        List<AuthorMeaning> authorMeaningList = new ArrayList<>();
                        long totalViews = 0;
                        int meaningsCount = 0;

                        // Process approvedMeanings collection
                        for (QueryDocumentSnapshot document : approvedMeaningsResult) {
                            String newMeaning = document.getString("newMeaning");
                            String author = document.getString("author");
                            String id = document.getId();
                            Long views = document.getLong("views");
                            totalViews += (views != null ? views : 0);
                            meaningsCount++;

                            // Check if author is null, set to "by: Administrator"
                            if (author == null) {
                                author = "by: Administrator";
                            }

                            AuthorMeaning authorMeaning = new AuthorMeaning(id, author, newMeaning, 0, 0);
                            authorMeaningList.add(authorMeaning);
                            incrementWordView(document.getReference());
                        }

                        // Process wordandmeanings collection
                        for (QueryDocumentSnapshot document : wordAndMeaningsResult) {
                            String newMeaning = document.getString("meaning");
                            String author = document.getString("author");
                            String id = document.getId();
                            Long views = 0L; // Assuming no views field in this collection

                            // Check if author is null, set to "by: Administrator"
                            if (author == null) {
                                author = "by: Administrator";
                            }

                            AuthorMeaning authorMeaning = new AuthorMeaning(id, author, newMeaning, 0, 0);
                            authorMeaningList.add(authorMeaning);
                        }

                        if (!authorMeaningList.isEmpty()) {
                            double averageViews = meaningsCount > 0 ? (double) totalViews / meaningsCount : 0;
                            String viewsText = "Views: " + String.format(Locale.getDefault(), "%.2f", averageViews);

                            // Fetch and display ratings, then sort by rating
                            fetchAndSortRatings(authorMeaningList, finalSearchTerm);

                            displayLocalWord(finalSearchTerm);

                            // Update view count in Firestore
                            updateViewsInFirestore(finalSearchTerm);

                            // Fetch and display view count
                            displayViewCount(finalSearchTerm);
                        } else {
                            handleNoResults(finalSearchTerm);
                        }

                        hideKeyboard();
                        showAddMeaningButton(true); // Show the button after search

                        return;
                    }

                    handleNoResults(finalSearchTerm);
                });
    }

    private void handleNoResults(String searchTerm) {
        displayLocalWord(searchTerm);
        showAddMeaningButton(true); // Show the button even if no results are found
        viewsTextView.setVisibility(View.GONE); // Clear the views text
        firestoreMeaningTextView.setVisibility(View.GONE); // Hide the firestore meaning text view
        hideKeyboard();
    }


    @SuppressLint("StringFormatInvalid")
    private void displayLocalWord(String searchTerm) {
        Word localWord = dictionary.get(searchTerm.toLowerCase());
        if (localWord != null) {
            String localWordText = localWord.getWord();
            String localMeaning = localWord.getMeaning();
            wordTextView.setText(localWordText);
            meaningTextView.setText(localMeaning);
            firestoreMeaningTextView.setVisibility(View.GONE);
            viewsTextView.setVisibility(View.VISIBLE); // Ensure viewsTextView is visible when there is a local word
        } else {
            wordTextView.setText("");
            meaningTextView.setText(getString(R.string.no_results_found, searchTerm));
            viewsTextView.setVisibility(View.GONE); // Hide viewsTextView when no local word is found
        }
    }

    private void fetchAndSortRatings(List<AuthorMeaning> authorMeaningList, String word) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("ratingsPerMeaning")
                .whereEqualTo("word", word)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String meaningId = document.getString("meaningId");
                            Long rating = document.getLong("rating");

                            for (AuthorMeaning authorMeaning : authorMeaningList) {
                                if (authorMeaning.getId().equals(meaningId)) {
                                    authorMeaning.setRating(rating != null ? rating.intValue() : 0);
                                    break;
                                }
                            }
                        }

                        // Fetch rating counts from approvedMeanings collection
                        firestore.collection("approvedMeanings")
                                .whereEqualTo("word", word)
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
                                        AuthorMeaningAdapter adapter = new AuthorMeaningAdapter(requireContext(), authorMeaningList);
                                        authorRecyclerView.setAdapter(adapter);
                                    }
                                });
                    }
                });
    }

    private void incrementWordView(DocumentReference wordRef) {
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(wordRef);
            Long currentViews = snapshot.getLong("views");
            long newViewCount = (currentViews == null ? 0 : currentViews) + 1;

            transaction.update(wordRef, "views", newViewCount);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("View Increment", "View count incremented successfully");
        }).addOnFailureListener(e -> {
            Log.e("View Increment", "Error incrementing view count", e);
        });
    }

    private void updateViewsInFirestore(String word) {
        if (word == null || word.trim().isEmpty()) {
            Log.e("Firestore", "Invalid word: " + word);
            return;
        }

        // Ensure the word is trimmed and lowercased for consistency
        String documentId = word.trim().toLowerCase(Locale.getDefault());
        Log.d("Firestore", "Updating views for document ID: " + documentId);

        // Creating the document reference with a valid path
        DocumentReference wordRef = firestore.collection("viewsPerWord").document(documentId);

        wordRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long currentViews = documentSnapshot.getLong("views");
                long newViewCount = (currentViews == null ? 0 : currentViews) + 1;
                wordRef.update("views", newViewCount);
            } else {
                Map<String, Object> viewsData = new HashMap<>();
                viewsData.put("views", 1);
                wordRef.set(viewsData);
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error updating views", e);
        });
    }

    private void displayViewCount(String word) {
        DocumentReference wordRef = firestore.collection("viewsPerWord").document(word.toLowerCase());
        wordRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long views = documentSnapshot.getLong("views");
                viewsTextView.setText("Views: " + (views != null ? views : 0));
            } else {
                viewsTextView.setText("Views: 0");
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error fetching view count", e);
            viewsTextView.setText("Views: N/A");
        });
    }

    private void updateBookmarkButtonState(Word word) {
        ImageButton bookmarkButton = getView().findViewById(R.id.bookmark_button);
        if (word.isBookmarked()) {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled);
        } else {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark);
        }
    }

    private void toggleBookmark() {
        String wordText = wordTextView.getText().toString();
        Word word = dictionary.get(wordText.toLowerCase());
        if (word != null) {
            word.setBookmarked(!word.isBookmarked());
            if (word.isBookmarked()) {
                databaseHelper.insertBookmark(requireContext(), word.getWord(), word.getMeaning());
            } else {
                databaseHelper.deleteBookmark(word.getWord());
            }
            updateBookmarkButtonState(word);
        }
    }

    private void submitNewMeaning(String meaningText) {
        String word = wordTextView.getText().toString();
        if (!word.isEmpty() && !meaningText.isEmpty()) {
            String currentUserEmail = mAuth.getCurrentUser().getEmail();

            if (currentUserEmail != null) {
                Map<String, Object> newMeaningMap = new HashMap<>();
                newMeaningMap.put("word", word);
                newMeaningMap.put("newMeaning", meaningText);
                newMeaningMap.put("author", currentUserEmail);
                newMeaningMap.put("status", "pending");

                firestore.collection("pendingMeanings").add(newMeaningMap)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(getContext(), "Meaning submitted for approval", Toast.LENGTH_SHORT).show();
                            newMeaningEditText.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error submitting meaning", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getContext(), "Error: User not authenticated", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Please enter a word and a meaning", Toast.LENGTH_SHORT).show();
        }
    }

    private void speakWord() {
        String word = wordTextView.getText().toString();
        if (!word.isEmpty()) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Toast.makeText(getContext(), "Nothing to speak", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(wordTextView.getWindowToken(), 0);
    }

    private void showAddMeaningButton(boolean show) {
        if (addNewMeaningButton != null) {
            addNewMeaningButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
