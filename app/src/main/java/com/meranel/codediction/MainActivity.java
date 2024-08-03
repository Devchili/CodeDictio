package com.meranel.codediction;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private static final int MENU_SETTINGS = R.id.action_settings;
    private static final int MENU_LOGOUT = R.id.action_logout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Check if user is not logged in, then redirect to login activity
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, Login.class));
            finish(); // Prevent user from going back to MainActivity using back button
            return;
        }

        // Check if the logged-in user is an admin
        if (mAuth.getCurrentUser().getEmail().equals("admin@gmail.com")) {
            // Redirect to AdminActivity
            startActivity(new Intent(MainActivity.this, AdminActivity.class));
            finish(); // Prevent user from going back to MainActivity using back button
            return;
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Programmatically select the Home menu item when activity starts
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_bookmarks) {
                selectedFragment = new BookmarksFragment();
            }
            // Add else-if cases for other fragments if needed

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    // Method to navigate to HomeFragment
    public void navigateToHomeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
        // Set the "Home" menu item as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_SETTINGS) {
            // Show dialog with rating function
            showRatingDialog();
            return true;
        } else if (id == MENU_LOGOUT) {
            // Perform logout
            mAuth.signOut();
            // Navigate to LoginActivity
            startActivity(new Intent(MainActivity.this, Login.class));
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showRatingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        Button submitButton = dialogView.findViewById(R.id.submit_button); // Find the submit button in the dialog layout

        // Hide the submit button
        submitButton.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Rate this app")
                .setPositiveButton("Submit", (dialog, which) -> {
                    // Handle rating submission
                    float rating = ratingBar.getRating();
                    Toast.makeText(MainActivity.this, "Rating submitted: " + rating, Toast.LENGTH_SHORT).show();

                    // Send email
                    sendEmail(rating);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void sendEmail(float rating) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"CodeDictio@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "CodeDictio Feedback & Rating");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "User Rating: " + rating + "\n\nUser Feedback: ");

        PackageManager packageManager = getPackageManager();
        if (emailIntent.resolveActivity(packageManager) != null) {
            Intent chooserIntent = Intent.createChooser(emailIntent, "Send Email");
            if (chooserIntent != null) {
                startActivity(chooserIntent);
            } else {
                // Handle the case when no chooser is available
                Toast.makeText(MainActivity.this, "No email app found.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // If no email app can handle the intent, inform the user
            Toast.makeText(MainActivity.this, "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }
}
