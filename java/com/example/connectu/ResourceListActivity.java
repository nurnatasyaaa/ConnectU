package com.example.connectu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResourceListActivity extends AppCompatActivity {

    TextView btnBack, tvTitle, tvSubtitle;
    EditText etSearchResource;
    LinearLayout resourceContainer;

    FirebaseFirestore db;

    String materialType, courseDocumentId, courseCode, courseName;

    List<ResourceItem> allResources = new ArrayList<>();

    // 1. Model class to store resource information
    static class ResourceItem {
        String documentId, title, fileName, author, fileUrl;
        Timestamp uploadDate;

        ResourceItem(String documentId, String title, String fileName,
                     String author, String fileUrl, Timestamp uploadDate) {
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
        setContentView(R.layout.activity_resource_list);

        // 2. Initialize UI components
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        etSearchResource = findViewById(R.id.etSearchResource);
        resourceContainer = findViewById(R.id.resourceContainer);

        // 3. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 4. Retrieve selected material and course information
        materialType = getIntent().getStringExtra("materialType");
        courseDocumentId = getIntent().getStringExtra("courseDocumentId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        // 5. Display the selected course and material type
        tvTitle.setText(courseCode + " " + getShortMaterialName(materialType));
        tvSubtitle.setText(materialType + " • " + courseName);

        btnBack.setOnClickListener(v -> finish());

        // 6. Search resources based on user input
        etSearchResource.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterResources(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadResources();
    }

    private void loadResources() {
        // 7. Retrieve resources based on course and material type from Firestore
        allResources.clear();
        resourceContainer.removeAllViews();
        db.collection("resources")
                .whereEqualTo("courseDocumentId", courseDocumentId)
                .whereEqualTo("materialType", materialType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String documentId = document.getId();
                        String title = document.getString("title");
                        String fileName = document.getString("fileName");
                        String uploadedBy = document.getString("uploadedBy");
                        String fileUrl = document.getString("fileUrl");
                        Timestamp uploadDate = document.getTimestamp("uploadDate");
                        allResources.add(new ResourceItem(
                                documentId,
                                title,
                                fileName,
                                uploadedBy,
                                fileUrl,
                                uploadDate
                        ));
                    }
                    displayResources(allResources);
                    if (allResources.isEmpty()) {
                        showEmptyMessage("No resources uploaded yet.");
                    }
                });
    }

    private void filterResources(String query) {
        // 8. Filter resources by title, file name, or uploader
        List<ResourceItem> filtered = new ArrayList<>();
        String search = query.toLowerCase().trim();

        if (search.isEmpty()) {
            displayResources(allResources);
            return;
        }

        for (ResourceItem resource : allResources) {
            String title = resource.title == null ? "" : resource.title;
            String fileName = resource.fileName == null ? "" : resource.fileName;
            String author = resource.author == null ? "" : resource.author;

            if (title.toLowerCase().contains(search)
                    || fileName.toLowerCase().contains(search)
                    || author.toLowerCase().contains(search)) {
                filtered.add(resource);
            }
        }

        displayResources(filtered);

        if (filtered.isEmpty()) {
            showEmptyMessage("No matching resources found.");
        }
    }

    private String getTimeAgo(Timestamp timestamp) {
        // 9. Convert upload date into a readable time format
        if (timestamp == null) return "Just now";

        long diff = new Date().getTime() - timestamp.toDate().getTime();
        long minutes = diff / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        if (hours < 24) return hours + " hours ago";
        return days + " days ago";
    }

    private void displayResources(List<ResourceItem> resources) {
        // 10. Display resource cards in the resource container
        resourceContainer.removeAllViews();

        for (ResourceItem resource : resources) {
            addResourceCard(resource);
        }
    }

    private void addResourceCard(ResourceItem resource) {
        // 11. Create resource card dynamically
        TextView card = new TextView(this);

        String title = resource.title == null ? "Untitled Resource" : resource.title;
        String author = resource.author == null ? "Unknown" : resource.author;
        String time = getTimeAgo(resource.uploadDate);

        String text = title + "\n" +
                time + "\n" +
                "Uploaded by " + author + "\n" +
                "View options";

        android.text.SpannableString spannable =
                new android.text.SpannableString(text);

        int start = text.indexOf("View options");

        spannable.setSpan(
                new android.text.style.ForegroundColorSpan(0xFF6366F1),
                start,
                text.length(),
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        card.setText(spannable);
        card.setTextSize(14);
        card.setTextColor(0xFF111827);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundResource(R.drawable.bg_course_card);
        card.setElevation(6);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        // 12. Open selected resource when the resource card is clicked
        card.setOnClickListener(v -> openResource(resource));

        resourceContainer.addView(card);
    }

    private void openResource(ResourceItem resource) {
        // 13. Validate the resource file link
        if (resource.fileUrl == null || resource.fileUrl.trim().isEmpty()) {
            Toast.makeText(this, "No file link found", Toast.LENGTH_SHORT).show();
            return;
        }

        // 14. Increase the resource download count in Firestore
        db.collection("resources")
                .document(resource.documentId)
                .update("downloadCount", FieldValue.increment(1));

        // 15. Open the resource file using its URL
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resource.fileUrl)
        );
        startActivity(intent);
    }

    private void showEmptyMessage(String message) {
        // 16. Display a message when no resources are found
        resourceContainer.removeAllViews();

        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 40, 20, 40);

        resourceContainer.addView(empty);
    }

    private String getShortMaterialName(String materialType) {
        // 17. Display the correct material category name
        if (materialType.equals("Notes")) {
            return "Notes";
        } else if (materialType.equals("Quizzes")) {
            return "Quizzes";
        } else {
            return "Final Papers";
        }
    }
}