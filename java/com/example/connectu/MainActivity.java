package com.example.connectu;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.app.AlertDialog;
import android.content.Intent;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    String studentId;

    boolean notificationPopupShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        studentId = getIntent().getStringExtra("studentId");

        HomeFragment firstHomeFragment = new HomeFragment();
        Bundle firstBundle = new Bundle();
        firstBundle.putString("studentId", studentId);
        firstHomeFragment.setArguments(firstBundle);

        loadFragment(firstHomeFragment);

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                HomeFragment newHomeFragment = new HomeFragment();
                Bundle newBundle = new Bundle();
                newBundle.putString("studentId", studentId);
                newHomeFragment.setArguments(newBundle);
                selectedFragment = newHomeFragment;

            } else if (item.getItemId() == R.id.nav_repository) {

                RepositoryFragment repositoryFragment = new RepositoryFragment();
                Bundle bundle = new Bundle();
                bundle.putString("studentId", studentId);
                repositoryFragment.setArguments(bundle);
                selectedFragment = repositoryFragment;

            } else if (item.getItemId() == R.id.nav_community) {

                CommunityFragment communityFragment = new CommunityFragment();
                Bundle bundle = new Bundle();
                bundle.putString("studentId", studentId);
                communityFragment.setArguments(bundle);
                selectedFragment = communityFragment;

            } else if (item.getItemId() == R.id.nav_profile) {

                ProfileFragment profileFragment = new ProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("studentId", studentId);
                profileFragment.setArguments(bundle);
                selectedFragment = profileFragment;
            }

            return loadFragment(selectedFragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .commit();
            checkNotificationPopup();
            return true;
        }
        return false;
    }

    private void checkNotificationPopup() {

        if (notificationPopupShown) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("notifications")
                .whereEqualTo("recipientStudentId", studentId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(query -> {

                    if (!query.isEmpty()) {

                        notificationPopupShown = true;

                        int count = query.size();

                        new AlertDialog.Builder(this)
                                .setTitle("New Notifications")
                                .setMessage("You have " + count + " new notification(s).")
                                .setPositiveButton("View", (dialog, which) -> {
                                    Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                                    startActivity(intent);
                                })
                                .setNegativeButton("Later", null)
                                .show();
                    }
                });
    }
}