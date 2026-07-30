package com.example.connectu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DiscussionListActivity extends AppCompatActivity {

    TextView btnBack, tvForumTitle, btnNewDiscussion, btnNotification;
    TextView tabAll, tabMyQuestions;

    EditText etSearchDiscussion;
    Spinner spinnerSubject;
    LinearLayout listContainer;

    FirebaseFirestore db;

    int semester;
    String currentTab = "All";
    String studentId = "";
    String currentUserName = "";

    // Stores the currently selected subject filter
    String selectedSubject = "All Subjects";

    List<DiscussionItem> allDiscussions = new ArrayList<>();

    // Lists used by the subject filter
    List<String> subjectOptions = new ArrayList<>();
    ArrayAdapter<String> subjectAdapter;

    // 1. Model class to store discussion information
    static class DiscussionItem {

        String discussionId;
        String title;
        String content;
        String postedBy;
        String postedById;

        String courseCode;
        String courseName;

        Timestamp postedAt;
        long replyCount;
        List<String> likedBy;

        DiscussionItem(
                String discussionId,
                String title,
                String content,
                String postedBy,
                String postedById,
                String courseCode,
                String courseName,
                Timestamp postedAt,
                long replyCount,
                List<String> likedBy
        ) {

            this.discussionId = discussionId;
            this.title = title;
            this.content = content;
            this.postedBy = postedBy;
            this.postedById = postedById;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.postedAt = postedAt;
            this.replyCount = replyCount;
            this.likedBy = likedBy;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion_list);

        // 2. Initialize UI components
        btnBack = findViewById(R.id.btnBack);
        tvForumTitle = findViewById(R.id.tvForumTitle);
        btnNewDiscussion = findViewById(R.id.btnNewDiscussion);
        btnNotification = findViewById(R.id.btnNotification);

        tabAll = findViewById(R.id.tabAll);
        tabMyQuestions = findViewById(R.id.tabMyQuestions);

        etSearchDiscussion = findViewById(R.id.etSearchDiscussion);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        listContainer = findViewById(R.id.listContainer);

        // 3. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 4. Retrieve semester and current student ID
        semester = getIntent().getIntExtra("semester", 1);
        studentId = getIntent().getStringExtra("studentId");

        tvForumTitle.setText("Semester " + semester);

        loadCurrentUserName();

        // 5. Set up the subject filter
        setupSubjectSpinner();

        // 6. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 7. Open Notification page
        btnNotification.setOnClickListener(v -> {

            Intent intent = new Intent(
                    DiscussionListActivity.this,
                    NotificationActivity.class
            );

            startActivity(intent);
        });

        // 8. Open New Discussion page
        btnNewDiscussion.setOnClickListener(v -> {

            Intent intent = new Intent(
                    DiscussionListActivity.this,
                    NewDiscussionActivity.class
            );

            intent.putExtra("semester", semester);
            intent.putExtra("studentId", studentId);

            startActivity(intent);
        });

        // 9. Display all discussions
        tabAll.setOnClickListener(v -> {

            currentTab = "All";

            updateTabs();
            applyFilters();
        });

        // 10. Display only discussions posted by the current student
        tabMyQuestions.setOnClickListener(v -> {

            currentTab = "My Questions";

            updateTabs();
            applyFilters();
        });

        // 11. Filter discussions whenever the user enters a keyword
        etSearchDiscussion.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        applyFilters();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );

        updateTabs();
        loadDiscussions();
    }

    private void setupSubjectSpinner() {

        // 12. Prepare the subject filter
        subjectOptions.clear();
        subjectOptions.add("All Subjects");

        subjectAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                subjectOptions
        );

        subjectAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSubject.setAdapter(subjectAdapter);

        // 13. Filter discussions when a subject is selected
        spinnerSubject.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedSubject =
                                parent.getItemAtPosition(position)
                                        .toString();

                        applyFilters();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        // 14. Load the complete subject list from Firestore
        loadSubjectsForFilter();
    }

    private void loadSubjectsForFilter() {

        // 15. Retrieve all subjects for the selected semester and programme
        db.collection("courses")
                .whereEqualTo("semester", semester)
                .whereEqualTo("program", "CDCS240")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    subjectOptions.clear();
                    subjectOptions.add("All Subjects");

                    for (QueryDocumentSnapshot document :
                            queryDocumentSnapshots) {

                        String courseCode =
                                document.getString("courseCode");

                        if (courseCode != null
                                && !courseCode.trim().isEmpty()
                                && !subjectOptions.contains(courseCode)) {

                            subjectOptions.add(courseCode);
                        }
                    }

                    subjectAdapter.notifyDataSetChanged();
                    spinnerSubject.setSelection(0);
                });
    }

    private void loadCurrentUserName() {

        // 14. Retrieve the current student's name from Firestore
        if (studentId == null || studentId.isEmpty()) {
            return;
        }

        db.collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        currentUserName =
                                documentSnapshot.getString("name");
                    }
                });
    }

    private void loadDiscussions() {

        // 15. Retrieve discussions for the selected semester
        db.collection("discussion")
                .whereEqualTo("semester", semester)
                .addSnapshotListener(
                        (queryDocumentSnapshots, error) -> {

                            if (error != null
                                    || queryDocumentSnapshots == null) {

                                return;
                            }

                            allDiscussions.clear();

                            for (QueryDocumentSnapshot document :
                                    queryDocumentSnapshots) {

                                String discussionId =
                                        document.getId();

                                String title =
                                        document.getString("title");

                                String content =
                                        document.getString("content");

                                String postedBy =
                                        document.getString("postedBy");

                                String postedById =
                                        document.getString("postedById");

                                // Retrieve the new field names
                                String courseCode =
                                        document.getString("subject");

                                String courseName =
                                        document.getString("subjectName");

                                // Retrieve the old field names for existing discussions
                                if (courseCode == null || courseCode.trim().isEmpty()) {
                                    courseCode = document.getString("subject");
                                }

                                if (courseName == null || courseName.trim().isEmpty()) {
                                    courseName = document.getString("subjectName");
                                }

                                // Only discussions with no subject at all become General
                                if (courseCode == null || courseCode.trim().isEmpty()) {
                                    courseCode = "General";
                                }

                                if (courseName == null || courseName.trim().isEmpty()) {
                                    courseName = "General Discussion";
                                }

                                Timestamp postedAt =
                                        document.getTimestamp("postedAt");

                                Long replyCount =
                                        document.getLong("replyCount");

                                List<String> likedBy =
                                        (List<String>) document.get("likedBy");

                                if (likedBy == null) {
                                    likedBy = new ArrayList<>();
                                }

                                // Support older discussions that do not have a subject
                                if (courseCode == null
                                        || courseCode.trim().isEmpty()) {

                                    courseCode = "General";
                                }

                                if (courseName == null
                                        || courseName.trim().isEmpty()) {

                                    courseName = "General Discussion";
                                }

                                allDiscussions.add(
                                        new DiscussionItem(
                                                discussionId,
                                                title,
                                                content,
                                                postedBy,
                                                postedById,
                                                courseCode,
                                                courseName,
                                                postedAt,
                                                replyCount == null
                                                        ? 0
                                                        : replyCount,
                                                likedBy
                                        )
                                );
                            }

                            // 17. Apply the selected filters
                            applyFilters();

                            if (allDiscussions.isEmpty()) {

                                showEmptyMessage(
                                        "No discussions yet.\n"
                                                + "Start the first question!"
                                );
                            }
                        }
                );
    }

    private void applyFilters() {

        // 18. Combine subject, tab, and search filters
        List<DiscussionItem> filtered =
                new ArrayList<>();

        String search =
                etSearchDiscussion.getText()
                        .toString()
                        .toLowerCase()
                        .trim();

        for (DiscussionItem item : allDiscussions) {

            // Check subject filter
            boolean subjectMatches =
                    selectedSubject.equals("All Subjects")
                            || (
                            item.courseCode != null
                                    && item.courseCode.equals(selectedSubject)
                    );

            // Check All or My Questions tab
            boolean tabMatches =
                    currentTab.equals("All")
                            || (
                            item.postedById != null
                                    && item.postedById.equals(studentId)
                    );

            String title =
                    item.title == null
                            ? ""
                            : item.title.toLowerCase();

            String content =
                    item.content == null
                            ? ""
                            : item.content.toLowerCase();

            String postedBy =
                    item.postedBy == null
                            ? ""
                            : item.postedBy.toLowerCase();

            String courseCode =
                    item.courseCode == null
                            ? ""
                            : item.courseCode.toLowerCase();

            String courseName =
                    item.courseName == null
                            ? ""
                            : item.courseName.toLowerCase();

            // Check search keyword
            boolean searchMatches =
                    search.isEmpty()
                            || title.contains(search)
                            || content.contains(search)
                            || postedBy.contains(search)
                            || courseCode.contains(search)
                            || courseName.contains(search);

            if (subjectMatches
                    && tabMatches
                    && searchMatches) {

                filtered.add(item);
            }
        }

        displayDiscussions(filtered);

        if (filtered.isEmpty() && !allDiscussions.isEmpty()) {

            showEmptyMessage(
                    "No matching discussion found."
            );
        }
    }

    private void displayDiscussions(
            List<DiscussionItem> discussions
    ) {

        // 19. Display discussion cards in the list
        listContainer.removeAllViews();

        for (DiscussionItem item : discussions) {
            addDiscussionCard(item);
        }
    }

    private String getTimeAgo(Timestamp timestamp) {

        // 20. Convert discussion timestamp into readable time
        if (timestamp == null) {
            return "Just now";
        }

        long diff =
                new Date().getTime()
                        - timestamp.toDate().getTime();

        long minutes =
                diff / (1000 * 60);

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

    private void addDiscussionCard(DiscussionItem item) {

        // 21. Create discussion card dynamically
        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(22, 22, 22, 22);
        card.setBackgroundResource(
                R.drawable.bg_course_card
        );
        card.setElevation(8);

        // 22. Display the subject code and subject name
        TextView subjectView =
                new TextView(this);

        subjectView.setText(
                item.courseCode
                        + " • "
                        + item.courseName
        );

        subjectView.setTextSize(12);
        subjectView.setTextColor(0xFF6366F1);
        subjectView.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        // 23. Display discussion author and posting time
        TextView meta =
                new TextView(this);

        String author =
                item.postedBy == null
                        ? "Unknown"
                        : item.postedBy;

        meta.setText(
                author
                        + "\n"
                        + getTimeAgo(item.postedAt)
        );

        meta.setTextSize(13);
        meta.setTextColor(0xFF6B7280);
        meta.setPadding(0, 8, 0, 0);

        // 24. Display discussion title
        TextView title =
                new TextView(this);

        title.setText(
                item.title == null
                        ? "Untitled Discussion"
                        : item.title
        );

        title.setTextSize(16);
        title.setTextColor(0xFF111827);

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        title.setPadding(0, 14, 0, 8);

        // 25. Display shortened discussion content
        String preview =
                item.content == null
                        ? ""
                        : item.content;

        if (preview.length() > 90) {

            preview =
                    preview.substring(0, 90)
                            + "... Show more";
        }

        TextView content =
                new TextView(this);

        content.setText(preview);
        content.setTextSize(14);
        content.setTextColor(0xFF111827);

        LinearLayout actionRow =
                new LinearLayout(this);

        actionRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        actionRow.setPadding(0, 18, 0, 0);

        // 26. Display the number of replies
        TextView replies =
                new TextView(this);

        replies.setText(
                "💬  "
                        + item.replyCount
                        + " replies"
        );

        replies.setTextSize(13);
        replies.setTextColor(0xFF111827);
        replies.setPadding(0, 0, 30, 0);

        // 27. Check whether the current student liked the discussion
        boolean alreadyLiked =
                item.likedBy.contains(studentId);

        long likeCount =
                item.likedBy.size();

        TextView likes =
                new TextView(this);

        likes.setText(
                (alreadyLiked ? "♥  " : "♡  ")
                        + likeCount
                        + " likes"
        );

        likes.setTextSize(13);

        likes.setTextColor(
                alreadyLiked
                        ? 0xFFE53935
                        : 0xFF111827
        );

        // 28. Add or remove the student's like
        likes.setOnClickListener(v -> {

            if (studentId == null
                    || studentId.isEmpty()) {

                return;
            }

            if (item.likedBy.contains(studentId)) {

                db.collection("discussion")
                        .document(item.discussionId)
                        .update(
                                "likedBy",
                                FieldValue.arrayRemove(studentId)
                        );

            } else {

                db.collection("discussion")
                        .document(item.discussionId)
                        .update(
                                "likedBy",
                                FieldValue.arrayUnion(studentId)
                        );
            }
        });

        actionRow.addView(replies);
        actionRow.addView(likes);

        card.addView(subjectView);
        card.addView(meta);
        card.addView(title);
        card.addView(content);
        card.addView(actionRow);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 22);
        card.setLayoutParams(params);

        // 29. Open the selected discussion thread
        card.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            DiscussionListActivity.this,
                            DiscussionThreadActivity.class
                    );

            intent.putExtra(
                    "discussionId",
                    item.discussionId
            );

            intent.putExtra(
                    "studentId",
                    studentId
            );

            startActivity(intent);
        });

        listContainer.addView(card);
    }

    private void updateTabs() {

        // 30. Update the selected tab colour
        tabAll.setBackgroundColor(
                currentTab.equals("All")
                        ? 0xFF6366F1
                        : 0xFF9CA3AF
        );

        tabMyQuestions.setBackgroundColor(
                currentTab.equals("My Questions")
                        ? 0xFF6366F1
                        : 0xFF9CA3AF
        );
    }

    private void showEmptyMessage(String message) {

        // 31. Display a message when no discussions are available
        listContainer.removeAllViews();

        TextView empty =
                new TextView(this);

        empty.setText(message);
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 40, 20, 40);

        listContainer.addView(empty);
    }
}