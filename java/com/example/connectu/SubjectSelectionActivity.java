package com.example.connectu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SubjectSelectionActivity extends AppCompatActivity {

    TextView btnBack, tvTitle, tvSubtitle;
    TextView tabAll, tabCreative, tabInfra, tabBigData;
    LinearLayout filterContainer, subjectContainer;
    String materialType;

    FirebaseFirestore db;

    int semester;
    String selectedTrack = "all";

    List<CourseItem> allCourses = new ArrayList<>();

    // 1. Model class to store course information
    static class CourseItem {
        String documentId, courseCode, courseName, track;

        CourseItem(String documentId, String courseCode, String courseName, String track) {
            this.documentId = documentId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.track = track;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_selection);

        // 2. Initialize UI components
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        filterContainer = findViewById(R.id.filterContainer);
        subjectContainer = findViewById(R.id.subjectContainer);

        tabAll = findViewById(R.id.tabAll);
        tabCreative = findViewById(R.id.tabCreative);
        tabInfra = findViewById(R.id.tabInfra);
        tabBigData = findViewById(R.id.tabBigData);

        // 3. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 4. Retrieve selected semester and material type
        semester = getIntent().getIntExtra("semester", 1);
        materialType = getIntent().getStringExtra("materialType");
        tvTitle.setText("Choose your subject");
        tvSubtitle.setText("Semester " + semester);

        // 5. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 6. Display track filters only for Semester 5 and Semester 6
        if (semester == 5 || semester == 6) {
            filterContainer.setVisibility(android.view.View.VISIBLE);
        } else {
            filterContainer.setVisibility(android.view.View.GONE);
        }

        // 7. Filter subjects based on selected track
        tabAll.setOnClickListener(v -> {
            selectedTrack = "all";
            updateTabs();
            displayCourses(allCourses);
        });
        tabCreative.setOnClickListener(v -> {
            selectedTrack = "creative_it";
            updateTabs();
            filterByTrack("creative_it");
        });
        tabInfra.setOnClickListener(v -> {
            selectedTrack = "it_infra";
            updateTabs();
            filterByTrack("it_infra");
        });
        tabBigData.setOnClickListener(v -> {
            selectedTrack = "big_data";
            updateTabs();
            filterByTrack("big_data");
        });
        updateTabs();
        loadCourses();
    }

    private void loadCourses() {
        // 8. Retrieve courses based on semester and programme from Firestore
        allCourses.clear();
        subjectContainer.removeAllViews();

        db.collection("courses")
                .whereEqualTo("semester", semester)
                .whereEqualTo("program", "CDCS240")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        String documentId = document.getId();
                        String courseCode = document.getString("courseCode");
                        String courseName = document.getString("courseName");
                        String track = document.getString("track");

                        allCourses.add(new CourseItem(
                                documentId,
                                courseCode,
                                courseName,
                                track
                        ));
                    }
                    displayCourses(allCourses);
                    if (allCourses.isEmpty()) {
                        showEmptyMessage("No subjects found.");
                    }
                });
    }

    private void filterByTrack(String trackName) {
        // 9. Filter course list according to the selected track
        List<CourseItem> filtered = new ArrayList<>();

        for (CourseItem course : allCourses) {
            if (course.track == null) continue;

            if (course.track.equals("all")
                    || course.track.equals("general")
                    || course.track.equals(trackName)) {
                filtered.add(course);
            }
        }

        displayCourses(filtered);

        if (filtered.isEmpty()) {
            showEmptyMessage("No subjects found for this track.");
        }
    }

    private void displayCourses(List<CourseItem> courses) {
        // 10. Display course cards in the subject container
        subjectContainer.removeAllViews();
        for (CourseItem course : courses) {
            addCourseCard(course);
        }
    }

    private void addCourseCard(CourseItem course) {
        // 11. Create subject card dynamically
        TextView card = new TextView(this);

        String code = course.courseCode == null ? course.documentId : course.courseCode;
        String name = course.courseName == null ? "Course name not added yet" : course.courseName;
        String trackText = formatTrack(course.track);

        card.setText(code + "\n" + name + "\n" + trackText);
        card.setTextSize(15);
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
        // 12. Open Resource List page and pass selected subject information
        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SubjectSelectionActivity.this,
                    ResourceListActivity.class
            );
            intent.putExtra("materialType", materialType);
            intent.putExtra("courseDocumentId", course.documentId);
            intent.putExtra("courseCode", code);
            intent.putExtra("courseName", name);
            startActivity(intent);
        });

        subjectContainer.addView(card);
    }

    private void updateTabs() {
        // 13. Update the appearance of track filter tabs
        setTabStyle(tabAll, selectedTrack.equals("all"));
        setTabStyle(tabCreative, selectedTrack.equals("creative_it"));
        setTabStyle(tabInfra, selectedTrack.equals("it_infra"));
        setTabStyle(tabBigData, selectedTrack.equals("big_data"));
    }

    private void setTabStyle(TextView tab, boolean selected) {
        // 14. Apply selected or unselected style to each tab
        if (selected) {
            tab.setTextColor(0xFFFFFFFF);
            tab.setBackgroundColor(0xFF6366F1);
        } else {
            tab.setTextColor(0xFF6B7280);
            tab.setBackgroundColor(0xFFFFFFFF);
        }
    }

    private String formatTrack(String track) {
        // 15. Convert track values into readable labels
        if (track == null) return "General";
        switch (track) {
            case "all":
                return "Shared Subject";
            case "creative_it":
                return "Creative IT";
            case "it_infra":
                return "IT Infrastructure";
            case "big_data":
                return "Big Data";
            case "general":
                return "General";
            case "internship":
                return "Internship";
            default:
                return track;
        }
    }

    private void showEmptyMessage(String message) {
        // 16. Display a message when no subjects are found
        subjectContainer.removeAllViews();
        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextSize(15);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(20, 40, 20, 40);
        subjectContainer.addView(empty);
    }
}