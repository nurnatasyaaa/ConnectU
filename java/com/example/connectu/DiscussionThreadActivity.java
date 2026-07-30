package com.example.connectu;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.content.Intent;

import android.content.SharedPreferences;

public class DiscussionThreadActivity extends AppCompatActivity {

    TextView btnBack, btnMore, tvTitle, tvMeta, tvContent, tvReplyCount, tvLikeCount, btnSendReply;
    EditText etReply;
    LinearLayout replyContainer, bottomReplyBar;

    FirebaseFirestore db;
    String discussionId;
    String studentId = "";
    String currentUserName = "";
    String postedById = "";
    List<String> discussionLikedBy = new ArrayList<>();

    long currentLikes = 0;
    long currentReplies = 0;
    boolean liked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Initialize the discussion thread page
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion_thread);

        // 2. Connect Java variables with interface components
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        btnMore.setVisibility(android.view.View.GONE);
        tvTitle = findViewById(R.id.tvTitle);
        tvMeta = findViewById(R.id.tvMeta);
        tvContent = findViewById(R.id.tvContent);
        tvReplyCount = findViewById(R.id.tvReplyCount);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        etReply = findViewById(R.id.etReply);
        btnSendReply = findViewById(R.id.btnSendReply);
        replyContainer = findViewById(R.id.replyContainer);
        bottomReplyBar = findViewById(R.id.bottomReplyBar);
        setupKeyboardListener();

        // 3. Initialize Firestore and retrieve discussion and student information
        db = FirebaseFirestore.getInstance();
        discussionId = getIntent().getStringExtra("discussionId");
        studentId = getIntent().getStringExtra("studentId");
        loadCurrentUserName();

        // 4. Set actions for back, reply, like, and edit functions
        btnBack.setOnClickListener(v -> finish());
        btnSendReply.setOnClickListener(v -> postReply());
        tvLikeCount.setOnClickListener(v -> toggleLike());

        btnMore.setOnClickListener(v -> {
            Intent intent = new Intent(DiscussionThreadActivity.this, EditDiscussionActivity.class);
            intent.putExtra("discussionId", discussionId);
            intent.putExtra("title", tvTitle.getText().toString());
            intent.putExtra("content", tvContent.getText().toString());
            startActivity(intent);
        });

        loadDiscussion();
        loadReplies();
    }

    // 5. Retrieve the current student name from Firestore
    private void loadCurrentUserName() {
        if (studentId == null || studentId.isEmpty()) return;

        db.collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("name");
                    }
                });
    }

    // 6. Retrieve and display the selected discussion in real time
    private void loadDiscussion() {
        db.collection("discussion")
                .document(discussionId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                        return;
                    }
                    String title = documentSnapshot.getString("title");
                    String content = documentSnapshot.getString("content");
                    String postedBy = documentSnapshot.getString("postedBy");

                    postedById = documentSnapshot.getString("postedById");

                    Timestamp postedAt = documentSnapshot.getTimestamp("postedAt");

                    Long replyCount = documentSnapshot.getLong("replyCount");

                    List<String> likedBy = (List<String>) documentSnapshot.get("likedBy");
                    if (likedBy == null) {
                        likedBy = new ArrayList<>();
                    }
                    discussionLikedBy = likedBy;
                    currentReplies = replyCount == null ? 0 : replyCount;
                    currentLikes = discussionLikedBy.size();
                    liked = discussionLikedBy.contains(studentId);

                    tvMeta.setText(postedBy + "\n" + getTimeAgo(postedAt));
                    tvTitle.setText(title);
                    tvContent.setText(content);

                    updateCounts();

                    if (postedById != null && postedById.equals(studentId)) {
                        btnMore.setVisibility(android.view.View.VISIBLE);
                    } else {
                        btnMore.setVisibility(android.view.View.GONE);
                    }
                });
    }

    // 7. Update the displayed reply and like counts
    private void updateCounts() {
        tvReplyCount.setText("💬 " + currentReplies + " replies");

        if (liked) {
            tvLikeCount.setText("♥ " + currentLikes + " likes");
            tvLikeCount.setTextColor(0xFF6366F1);
        } else {
            tvLikeCount.setText("♡ " + currentLikes + " likes");
            tvLikeCount.setTextColor(0xFF111827);
        }
    }

    // 8. Add or remove the current student from the discussion like list
    private void toggleLike() {
        if (studentId == null || studentId.isEmpty()) return;

        if (discussionLikedBy.contains(studentId)) {
            db.collection("discussion")
                    .document(discussionId)
                    .update("likedBy", FieldValue.arrayRemove(studentId));
        } else {
            db.collection("discussion")
                    .document(discussionId)
                    .update("likedBy", FieldValue.arrayUnion(studentId))
                    .addOnSuccessListener(unused -> {
                        createLikeNotification();
                    });
        }
    }

    // 9. Convert the Firestore timestamp into a readable time format
    private String getTimeAgo(Timestamp timestamp) {
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

    // 10. Retrieve and display replies for the selected discussion
    private void loadReplies() {
        db.collection("discussion")
                .document(discussionId)
                .collection("replies")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) {
                        return;
                    }
                    replyContainer.removeAllViews();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        String replyId = document.getId();
                        String replyText = document.getString("replyText");
                        String repliedBy = document.getString("repliedBy");
                        String repliedById = document.getString("repliedById");

                        Timestamp repliedAt =
                                document.getTimestamp("repliedAt");

                        List<String> likedBy =
                                (List<String>) document.get("likedBy");

                        if (likedBy == null) {
                            likedBy = new ArrayList<>();
                        }
                        addReplyCard(
                                replyId,
                                replyText,
                                repliedBy,
                                repliedById,
                                repliedAt,
                                likedBy
                        );
                    }
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyReply();
                    }
                });
    }

    // 11. Validate and save a new reply into Firestore
    private void postReply() {
        String reply = etReply.getText().toString().trim();
        if (reply.isEmpty()) {
            Toast.makeText(this, "Please write a reply first", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> replyData = new HashMap<>();
        replyData.put("replyText", reply);
        replyData.put("repliedBy", currentUserName == null || currentUserName.isEmpty() ? "Unknown" : currentUserName);
        replyData.put("repliedById", studentId);
        replyData.put("repliedAt", FieldValue.serverTimestamp());
        replyData.put("likedBy", new ArrayList<String>());

        db.collection("discussion")
                .document(discussionId)
                .collection("replies")
                .add(replyData)
                .addOnSuccessListener(documentReference -> {
                    db.collection("discussion")
                            .document(discussionId)
                            .update("replyCount", FieldValue.increment(1));

                    createReplyNotification();

                    etReply.setText("");
                    Toast.makeText(this, "Reply posted", Toast.LENGTH_SHORT).show();
                });
    }

    // 12. Create and display each reply card dynamically
    private void addReplyCard(String replyId,
                              String replyText,
                              String repliedBy,
                              String repliedById,
                              Timestamp repliedAt,
                              List<String> likedBy) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 16, 18, 16);
        card.setBackgroundResource(R.drawable.bg_course_card);
        card.setElevation(8);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView name = new TextView(this);
        name.setText(repliedBy);
        name.setTextSize(14);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setTextColor(0xFF111827);

        TextView time = new TextView(this);
        time.setText(getTimeAgo(repliedAt));
        time.setTextSize(13);
        time.setTextColor(0xFF6B7280);

        row.addView(name,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1));

        row.addView(time);

        TextView content = new TextView(this);
        content.setText(replyText);
        content.setTextSize(14);
        content.setTextColor(0xFF111827);
        content.setPadding(0, 12, 0, 12);

        boolean alreadyLiked =
                likedBy.contains(studentId);

        long likeCount =
                likedBy.size();

        TextView likeText = new TextView(this);

        likeText.setText(
                (alreadyLiked ? "♥ " : "♡ ")
                        + likeCount
                        + " likes"
        );

        likeText.setTextSize(12);

        likeText.setTextColor(
                alreadyLiked
                        ? 0xFF6366F1
                        : 0xFF111827
        );

        // 13. Add or remove a like from the selected reply
        likeText.setOnClickListener(v -> {

            if (studentId == null || studentId.isEmpty()) {
                return;
            }

            if (likedBy.contains(studentId)) {

                db.collection("discussion")
                        .document(discussionId)
                        .collection("replies")
                        .document(replyId)
                        .update(
                                "likedBy",
                                FieldValue.arrayRemove(studentId)
                        );

            } else {

                db.collection("discussion")
                        .document(discussionId)
                        .collection("replies")
                        .document(replyId)
                        .update("likedBy", FieldValue.arrayUnion(studentId))
                        .addOnSuccessListener(unused -> {
                            createReplyLikeNotification(repliedById, replyText);
                        });
            }
        });

        card.addView(row);
        card.addView(content);
        card.addView(likeText);

        // 14. Show the Edit option only to the reply owner
        if (studentId != null && studentId.equals(repliedById)) {
            TextView editReply = new TextView(this);
            editReply.setText("Edit");
            editReply.setTextSize(12);
            editReply.setTextColor(0xFF6366F1);
            editReply.setPadding(0, 10, 0, 0);

            editReply.setOnClickListener(v -> {
                Intent intent = new Intent(DiscussionThreadActivity.this, EditReplyActivity.class);
                intent.putExtra("discussionId", discussionId);
                intent.putExtra("replyId", replyId);
                intent.putExtra("replyText", replyText);
                startActivity(intent);
            });

            card.addView(editReply);
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 18);
        card.setLayoutParams(params);

        replyContainer.addView(card);
    }

    // 15. Display a message when no replies are available
    private void showEmptyReply () {
        TextView empty = new TextView(this);
        empty.setText("No replies yet.\nBe the first to help!");
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 30, 20, 30);

        replyContainer.addView(empty);
    }

    // 16. Create a notification when another student replies to a discussion
    private void createReplyNotification () {

        if (postedById == null || postedById.isEmpty()) return;
        if (studentId == null || studentId.isEmpty()) return;

        // Do not notify yourself
        if (postedById.equals(studentId)) return;

        String senderName = currentUserName;
        if (senderName == null || senderName.isEmpty()) {
            senderName = "Someone";
        }

        Map<String, Object> notification = new HashMap<>();

        notification.put("recipientStudentId", postedById);
        notification.put("message", senderName + " replied to your discussion: " + tvTitle.getText().toString());
        notification.put("type", "reply");
        notification.put("discussionId", discussionId);
        notification.put("isRead", false);
        notification.put("createdAt", FieldValue.serverTimestamp());

        db.collection("notifications").add(notification);
    }

    // 17. Create a notification when another student likes a discussion
    private void createLikeNotification () {
        if (postedById == null || postedById.isEmpty()) {
            Toast.makeText(this, "No discussion owner found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "No current student found", Toast.LENGTH_SHORT).show();
            return;
        }
        // Do not notify yourself
        if (postedById.equals(studentId)) return;

        String senderName = currentUserName;
        if (senderName == null || senderName.isEmpty()) {
            senderName = "Someone";
        }
        Map<String, Object> notification = new HashMap<>();

        notification.put("recipientStudentId", postedById);
        notification.put("message", senderName + " liked your discussion: " + tvTitle.getText().toString());
        notification.put("type", "like");
        notification.put("discussionId", discussionId);
        notification.put("isRead", false);
        notification.put("createdAt", FieldValue.serverTimestamp());

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Like notification created", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Notification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // 18. Create a notification when another student likes a reply
    private void createReplyLikeNotification(String replyOwnerId, String replyText) {

        if (replyOwnerId == null || replyOwnerId.isEmpty()) return;
        if (studentId == null || studentId.isEmpty()) return;

        // Do not notify yourself
        if (replyOwnerId.equals(studentId)) return;

        String senderName = currentUserName;
        if (senderName == null || senderName.isEmpty()) {
            senderName = "Someone";
        }

        String preview = replyText;
        if (preview != null && preview.length() > 40) {
            preview = preview.substring(0, 40) + "...";
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientStudentId", replyOwnerId);
        notification.put("message", senderName + " liked your reply: " + preview);
        notification.put("type", "like");
        notification.put("discussionId", discussionId);
        notification.put("isRead", false);
        notification.put("createdAt", FieldValue.serverTimestamp());

        db.collection("notifications").add(notification);
    }

    // 19. Move the reply bar above the keyboard when typing
    private void setupKeyboardListener() {
        final View rootView = findViewById(android.R.id.content);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect rect = new android.graphics.Rect();
            rootView.getWindowVisibleDisplayFrame(rect);

            int screenHeight = rootView.getRootView().getHeight();
            int keyboardHeight = screenHeight - rect.bottom;

            if (keyboardHeight > screenHeight * 0.15) {
                bottomReplyBar.setTranslationY(-(keyboardHeight - 40));
            } else {
                bottomReplyBar.setTranslationY(0);
            }
        });
    }
}