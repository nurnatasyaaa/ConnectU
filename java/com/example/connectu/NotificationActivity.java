package com.example.connectu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class NotificationActivity extends AppCompatActivity {

    TextView btnBack;
    LinearLayout listContainer;

    FirebaseFirestore db;
    String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // 1. Initialize Notification page components
        btnBack = findViewById(R.id.btnBack);
        listContainer = findViewById(R.id.listContainer);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve the current student ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("ConnectU", MODE_PRIVATE);
        studentId = prefs.getString("studentId", "");

        // 4. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 5. Load notifications and mark unread notifications as read
        loadNotifications();
        markAllAsRead();
    }

    private void loadNotifications() {
        // 6. Clear existing notifications before loading updated data
        listContainer.removeAllViews();
        // 7. Retrieve notifications for the current student
        db.collection("notifications")
                .whereEqualTo("recipientStudentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 8. Display a message if no notifications are available
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyMessage();
                        return;
                    }
                    // 9. Retrieve each notification from Firestore
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String message = document.getString("message");
                        String type = document.getString("type");
                        Timestamp createdAt = document.getTimestamp("createdAt");
                        String createdAtText = getTimeAgo(createdAt);
                        String discussionId = document.getString("discussionId");
                        Boolean isRead = document.getBoolean("isRead");

                        addNotificationCard(
                                message,
                                type,
                                createdAtText,
                                isRead != null && isRead,
                                discussionId
                        );
                    }
                });
    }

    private void addNotificationCard(String message,
                                     String type,
                                     String createdAtText,
                                     boolean isRead,
                                     String discussionId) {

        // 10. Create a notification card dynamically
        TextView card = new TextView(this);

        String icon = "🔔";

        // 11. Display a different icon based on notification type
        if ("like".equals(type)) {
            icon = "❤️";
        } else if ("reply".equals(type)) {
            icon = "💬";
        }

        // 12. Display notification message and time
        card.setText(icon + " " + message + "\n" + createdAtText);
        card.setTextSize(14);
        card.setTextColor(0xFF111827);
        card.setPadding(20, 20, 20, 20);

        // 13. Apply different background based on read status
        if (isRead) {
            card.setBackgroundResource(R.drawable.bg_course_card);
        } else {
            card.setBackgroundColor(0xFFFFFFFF);
        }

        card.setElevation(6);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        // 14. Open the related discussion when notification is selected
        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    NotificationActivity.this,
                    DiscussionThreadActivity.class
            );

            intent.putExtra("discussionId", discussionId);
            intent.putExtra("studentId", studentId);

            startActivity(intent);
        });

        listContainer.addView(card);
    }

    private void markAllAsRead() {

        // 15. Retrieve all unread notifications for the current student
        db.collection("notifications")
                .whereEqualTo("recipientStudentId", studentId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 16. Update every unread notification as read
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().update("isRead", true);
                    }
                });
    }

    private void showEmptyMessage() {

        // 17. Display a message when no notifications are available
        TextView empty = new TextView(this);
        empty.setText("No notifications yet.");
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 40, 20, 40);

        listContainer.addView(empty);
    }

    private String getTimeAgo(Timestamp timestamp) {

        // 18. Convert the notification timestamp into readable time
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
}