package com.example.connectu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;
import android.app.AlertDialog;

public class UploadHistoryActivity extends AppCompatActivity {

    TextView btnBack, tvSubtitle;
    LinearLayout historyContainer;

    FirebaseFirestore db;
    String studentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_history);

        // 1. Initialize Upload History page components
        btnBack = findViewById(R.id.btnBack);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        historyContainer = findViewById(R.id.historyContainer);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve the current student name
        studentName = getIntent().getStringExtra("studentName");

        // 4. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 5. Use a default name if the student name is unavailable
        if (studentName == null || studentName.isEmpty()) {
            studentName = "Unknown";
        }

        // 6. Display the upload history subtitle
        tvSubtitle.setText("All resources uploaded by " + studentName);

        // 7. Load the student's upload history
        loadUploadHistory();
    }

    private void loadUploadHistory() {
        // 8. Clear the previous upload history
        historyContainer.removeAllViews();
        // 9. Retrieve all resources uploaded by the selected student
        db.collection("resources")
                .whereEqualTo("uploadedBy", studentName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 10. Display a message if no uploaded resources are found
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyMessage();
                        return;
                    }
                    // 11. Retrieve each uploaded resource
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String documentId = document.getId();
                        String title = document.getString("title");
                        String fileName = document.getString("fileName");
                        String courseCode = document.getString("courseCode");
                        String materialType = document.getString("materialType");
                        String fileUrl = document.getString("fileUrl");
                        Timestamp uploadDate = document.getTimestamp("uploadDate");
                        addHistoryCard(documentId, title, fileName, courseCode, materialType, fileUrl, uploadDate);
                    }
                });
    }

    private void addHistoryCard(String documentId,
                                String title,
                                String fileName,
                                String courseCode,
                                String materialType,
                                String fileUrl,
                                Timestamp uploadDate) {

        // 12. Create an upload history card dynamically
        TextView card = new TextView(this);

        // 13. Prepare safe resource information for display
        String safeTitle = title == null ? "Untitled Resource" : title;
        String safeCourse = courseCode == null ? "-" : courseCode;
        String safeType = materialType == null ? "-" : materialType;
        String time = getTimeAgo(uploadDate);

        // 14. Prepare the resource card text
        String text =
                safeTitle + "\n" +
                        safeCourse + " • " + safeType + "\n" +
                        time + "\n" +
                        "View options";

        android.text.SpannableString spannable =
                new android.text.SpannableString(text);

        int start = text.indexOf("View options");

        // 15. Highlight the View options text
        if (start >= 0) {
            spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(0xFF6366F1),
                    start,
                    text.length(),
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        card.setText(spannable);
        card.setTextSize(14);
        card.setTextColor(0xFF111827);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundResource(R.drawable.bg_course_card);
        card.setElevation(6);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        // 16. Display View, Edit, or Delete options for the selected resource
        card.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(safeTitle)
                    .setItems(new String[]{"View Resource", "Edit Resource", "Delete Resource"}, (dialog, which) -> {

                        // 17. Open the selected resource
                        if (which == 0) {
                            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                                Toast.makeText(this, "No file link found", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
                            startActivity(intent);

                            // 18. Open the Edit Resource page
                        } else if (which == 1) {
                            Intent intent = new Intent(UploadHistoryActivity.this, EditResourceActivity.class);
                            intent.putExtra("documentId", documentId);
                            intent.putExtra("title", safeTitle);
                            intent.putExtra("fileName", fileName);
                            intent.putExtra("fileUrl", fileUrl);
                            startActivity(intent);

                            // 19. Display the delete confirmation dialog
                        } else {
                            confirmDelete(documentId, safeTitle);
                        }
                    })
                    .show();
        });

        historyContainer.addView(card);
    }

    private void confirmDelete(String documentId, String title) {
        // 20. Ask the user to confirm resource deletion
        new AlertDialog.Builder(this)
                .setTitle("Delete Resource")
                .setMessage("Are you sure you want to delete \"" + title + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // 21. Delete the selected resource from Firestore
                    db.collection("resources")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                // 22. Display success message and refresh upload history
                                Toast.makeText(this, "Resource deleted", Toast.LENGTH_SHORT).show();
                                loadUploadHistory();
                            })
                            .addOnFailureListener(e ->
                                    // 23. Display an error message if deletion fails
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getTimeAgo(Timestamp timestamp) {

        // 24. Convert the upload timestamp into readable time
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

    private void showEmptyMessage() {

        // 25. Display a message when no uploads are found
        TextView empty = new TextView(this);
        empty.setText("No uploads found.");
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 40, 20, 40);

        historyContainer.addView(empty);
    }
}