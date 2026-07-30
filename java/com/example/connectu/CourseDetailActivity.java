package com.example.connectu;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CourseDetailActivity extends AppCompatActivity {

    TextView btnBack, tvCourseCode, tvCourseName;
    TextView tvResourceCount;
    TextView tabNotes, tabQuizzes, tabFinalPapers;
    EditText etSearchResource;
    LinearLayout resourceContainer;

    FirebaseFirestore db;

    String courseDocumentId, courseCode, courseName;
    String selectedMaterialType = "Notes";

    List<ResourceItem> allResources = new ArrayList<>();

    // 1. Model class to store resource information retrieved from Firestore
    static class ResourceItem {

        String documentId;
        String title;
        String fileName;
        String author;
        String fileUrl;
        Timestamp uploadDate;

        ResourceItem(String documentId,
                     String title,
                     String fileName,
                     String author,
                     String fileUrl,
                     Timestamp uploadDate) {

            this.documentId = documentId;
            this.title = title;
            this.fileName = fileName;
            this.author = author;
            this.fileUrl = fileUrl;
            this.uploadDate = uploadDate;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        // 2. Initialize UI components
        btnBack = findViewById(R.id.btnBack);
        tvCourseCode = findViewById(R.id.tvCourseCode);
        tvCourseName = findViewById(R.id.tvCourseName);
        tvResourceCount = findViewById(R.id.tvResourceCount);

        tabNotes = findViewById(R.id.tabNotes);
        tabQuizzes = findViewById(R.id.tabQuizzes);
        tabFinalPapers = findViewById(R.id.tabFinalPapers);

        etSearchResource = findViewById(R.id.etSearchResource);
        resourceContainer = findViewById(R.id.resourceContainer);

        // 3. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 4. Retrieve selected course information from HomeFragment
        courseDocumentId = getIntent().getStringExtra("courseDocumentId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        // 5. Display selected course information
        tvCourseCode.setText(courseCode);
        tvCourseName.setText(courseName);

        // 6. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 7. Allow users to switch between resource categories
        tabNotes.setOnClickListener(v -> selectTab("Notes"));
        tabQuizzes.setOnClickListener(v -> selectTab("Quizzes"));
        tabFinalPapers.setOnClickListener(v -> selectTab("Final Papers"));

        // 8. Search resources as the user enters a keyword
        etSearchResource.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {
            }
            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {

                filterResources(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        // 9. Display Notes as the default resource category
        selectTab("Notes");
    }

    private void selectTab(String materialType) {
        // 10. Store the selected material category
        selectedMaterialType = materialType;
        // Reset the appearance of all category tabs
        resetTabs();
        // Highlight the currently selected category tab
        if (materialType.equals("Notes")) {
            tabNotes.setTextColor(0xFFFFFFFF);
            tabNotes.setBackgroundColor(0xFF6366F1);
        } else if (materialType.equals("Quizzes")) {
            tabQuizzes.setTextColor(0xFFFFFFFF);
            tabQuizzes.setBackgroundColor(0xFF6366F1);
        } else {
            tabFinalPapers.setTextColor(0xFFFFFFFF);
            tabFinalPapers.setBackgroundColor(0xFF6366F1);
        }
        // 11. Load resources that match the selected category
        loadResources(materialType);
    }

    private void resetTabs() {
        // 12. Reset all resource tabs to their default appearance
        tabNotes.setTextColor(0xFF6B7280);
        tabNotes.setBackgroundColor(0xFFFFFFFF);
        tabQuizzes.setTextColor(0xFF6B7280);
        tabQuizzes.setBackgroundColor(0xFFFFFFFF);
        tabFinalPapers.setTextColor(0xFF6B7280);
        tabFinalPapers.setBackgroundColor(0xFFFFFFFF);
    }

    private void loadResources(String materialType) {
        // 13. Clear previous resource data before loading new results
        allResources.clear();
        resourceContainer.removeAllViews();
        // 14. Retrieve resources from Firestore based on course and material type
        db.collection("resources")
                .whereEqualTo("courseDocumentId", courseDocumentId)
                .whereEqualTo("materialType", materialType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document :
                            queryDocumentSnapshots) {
                        String documentId =
                                document.getId();
                        String title =
                                document.getString("title");
                        String fileName =
                                document.getString("fileName");
                        String uploadedBy =
                                document.getString("uploadedBy");
                        String fileUrl =
                                document.getString("fileUrl");
                        Timestamp uploadDate =
                                document.getTimestamp("uploadDate");
                        // Store each retrieved resource in the resource list
                        allResources.add(
                                new ResourceItem(
                                        documentId,
                                        title,
                                        fileName,
                                        uploadedBy,
                                        fileUrl,
                                        uploadDate
                                )
                        );
                    }
                    // 15. Display the total number of available resources
                    tvResourceCount.setText(
                            String.valueOf(allResources.size())
                    );
                    // 16. Display all retrieved resources
                    displayResources(allResources);
                    // Display an empty message if no resources are available
                    if (allResources.isEmpty()) {
                        showEmptyMessage();
                    }
                });
    }

    private void filterResources(String query) {
        // 17. Filter resources using the entered search keyword
        List<ResourceItem> filtered = new ArrayList<>();
        String search = query.toLowerCase().trim();
        for (ResourceItem resource : allResources) {
            String title =
                    resource.title == null ? "" :
                            resource.title.toLowerCase();
            String author =
                    resource.author == null ? "" :
                            resource.author.toLowerCase();
            String fileName =
                    resource.fileName == null ? "" :
                            resource.fileName.toLowerCase();
            // Match keyword with title, uploader name, or file name
            if (title.contains(search)
                    || author.contains(search)
                    || fileName.contains(search)) {
                filtered.add(resource);
            }
        }
        // Display resources that match the search keyword
        displayResources(filtered);
        // Display a message when no matching resources are found
        if (filtered.isEmpty()) {
            showNoSearchResult();
        }
    }

    private String getTimeAgo(Timestamp timestamp) {

        // 18. Convert Firestore upload timestamp into readable relative time
        if (timestamp == null) {
            return "Just now";
        }
        long difference =
                new Date().getTime()
                        - timestamp.toDate().getTime();
        long minutes =
                difference / (1000 * 60);
        long hours =
                minutes / 60;
        long days =
                hours / 24;
        if (minutes < 1) {
            return "Just now";
        }
        if (minutes < 60) {
            return minutes + " mins ago";
        }
        if (hours < 24) {
            return hours + " hours ago";
        }
        return days + " days ago";
    }

    private void displayResources(List<ResourceItem> resources) {
        // 19. Display resource cards in the resource container
        resourceContainer.removeAllViews();
        for (ResourceItem resource : resources) {
            addResourceCard(resource);
        }
    }

    private void addResourceCard(ResourceItem resource) {
        // 20. Create a resource card dynamically
        TextView card = new TextView(this);
        String title =
                resource.title == null
                        ? "Untitled Resource"
                        : resource.title;
        String author =
                resource.author == null
                        ? "Unknown"
                        : resource.author;
        String time =
                getTimeAgo(resource.uploadDate);
        String text =
                title + "\n"
                        + time + "\n"
                        + "Uploaded by " + author + "\n"
                        + "View options";

        android.text.SpannableString spannable =
                new android.text.SpannableString(text);
        int start =
                text.indexOf("View options");
        // 21. Highlight the View options text using the application colour
        if (start >= 0) {
            spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(
                            0xFF6366F1
                    ),
                    start,
                    text.length(),
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        card.setText(spannable);
        card.setTextSize(14);
        card.setTextColor(0xFF111827);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundResource(
                R.drawable.bg_course_card
        );
        card.setElevation(6);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);
        // 22. Open the selected resource when its card is clicked
        card.setOnClickListener(
                v -> openResource(resource)
        );
        resourceContainer.addView(card);
    }

    private void openResource(ResourceItem resource) {
        // 23. Validate that the resource contains a file link
        if (resource.fileUrl == null
                || resource.fileUrl.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No file link found",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        // 24. Open the selected Google Drive resource using an external application
        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(resource.fileUrl)
                );
        startActivity(intent);
    }

    private void showEmptyMessage() {
        // 25. Display a message when no resources have been uploaded
        TextView empty =
                new TextView(this);
        empty.setText(
                "No resources uploaded yet."
        );
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 40, 20, 40);
        resourceContainer.addView(empty);
    }

    private void showNoSearchResult() {
        // 26. Display a message when no resource matches the search keyword
        resourceContainer.removeAllViews();
        TextView empty =
                new TextView(this);
        empty.setText(
                "No matching resources found."
        );
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 40, 20, 40);
        resourceContainer.addView(empty);
    }
}