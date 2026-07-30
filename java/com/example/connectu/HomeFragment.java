package com.example.connectu;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    TextView tvName, tvInfo;
    LinearLayout courseContainer;
    FirebaseFirestore db;
    String studentId;

    List<CourseItem> allCourseItems = new ArrayList<>();

    // 1. Model class to store course information
    static class CourseItem {
        String documentId, code, name, imageName;

        CourseItem(String documentId, String code, String name, String imageName) {
            this.documentId = documentId;
            this.code = code;
            this.name = name;
            this.imageName = imageName;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 2. Initialize UI components
        tvName = view.findViewById(R.id.tvName);
        tvName.setText("");
        tvInfo = view.findViewById(R.id.tvInfo);
        tvInfo.setText("");
        courseContainer = view.findViewById(R.id.courseContainer);
        EditText etSearch = view.findViewById(R.id.etSearch);

        // 3. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 4. Search courses based on user input
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCourses(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 5. Retrieve student ID passed from MainActivity
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
            loadStudentData();
        }

        return view;
    }

    private void loadStudentData() {
        // 6. Retrieve student profile and enrolled courses from Firestore
        db.collection("students").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        String name = documentSnapshot.getString("name");
                        String program = documentSnapshot.getString("program");
                        Long semester = documentSnapshot.getLong("semester");
                        List<String> courseDocumentIds = (List<String>) documentSnapshot.get("courses");

                        tvName.setText("Hello, " + name + "!");
                        tvInfo.setText(program + " • Semester " + semester);

                        loadCourseDetails(courseDocumentIds);
                    }
                });
    }

    private void loadCourseDetails(List<String> courseDocumentIds) {
        // 7. Retrieve detailed course information using course document IDs
        allCourseItems.clear();
        courseContainer.removeAllViews();

        if (courseDocumentIds == null || courseDocumentIds.isEmpty()) {
            showNoResultMessage("No courses found for this student.");
            return;
        }

        for (String courseDocId : courseDocumentIds) {

            String cleanDocId = courseDocId.trim();

            db.collection("courses").document(cleanDocId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        String code = cleanDocId;
                        String name = "Course name not added yet";
                        String imageName = "connectu_logo";

                        if (documentSnapshot.exists()) {
                            code = documentSnapshot.getString("courseCode");
                            name = documentSnapshot.getString("courseName");
                            imageName = documentSnapshot.getString("imageName");
                        }

                        allCourseItems.add(new CourseItem(cleanDocId, code, name, imageName));
                        displayCourses(allCourseItems);
                    });
        }
    }

    private void filterCourses(String query) {
        // 8. Filter course list based on course code, document ID, or course name
        List<CourseItem> filtered = new ArrayList<>();
        String search = query.toLowerCase().trim();

        if (search.isEmpty()) {
            displayCourses(allCourseItems);
            return;
        }

        for (CourseItem course : allCourseItems) {
            String code = course.code.toLowerCase();
            String name = course.name.toLowerCase();
            String documentId = course.documentId.toLowerCase();

            boolean codeMatch = code.contains(search);
            boolean documentIdMatch = documentId.contains(search);
            boolean nameWordMatch = false;

            String[] words = name.split("\\s+");
            for (String word : words) {
                if (word.startsWith(search)) {
                    nameWordMatch = true;
                    break;
                }
            }

            if (codeMatch || documentIdMatch || nameWordMatch) {
                filtered.add(course);
            }
        }

        displayCourses(filtered);

        if (filtered.isEmpty()) {
            showNoResultMessage("No course found.\nTry a different keyword.");
        }
    }

    private void displayCourses(List<CourseItem> courses) {
        // 9. Display all course cards in the home page
        courseContainer.removeAllViews();

        for (CourseItem course : courses) {
            addCourseCard(course.documentId, course.code, course.name, course.imageName);
        }
    }

    private void showNoResultMessage(String message) {
        // 10. Display message when no course is found
        courseContainer.removeAllViews();

        TextView emptyView = new TextView(getContext());
        emptyView.setText(message);
        emptyView.setTextSize(15);
        emptyView.setTextColor(0xFF6B7280);
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(24, 40, 24, 40);

        courseContainer.addView(emptyView);
    }

    private void addCourseCard(String documentId,
                               String code,
                               String name,
                               String imageName) {

        // 11. Create a course card that highlights the course information
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(24, 22, 24, 22);
        card.setBackgroundResource(R.drawable.bg_course_card);
        card.setElevation(6);

        // 12. Display a small supporting subject icon
        ImageView image = new ImageView(getContext());

        int imageResId = getResources().getIdentifier(
                imageName,
                "drawable",
                requireContext().getPackageName()
        );

        if (imageResId == 0) {
            imageResId = R.drawable.uitm_logo;
        }

        image.setImageResource(imageResId);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        dpToPx(64),
                        dpToPx(64)
                );

        imageParams.setMargins(0, 0, dpToPx(18), 0);
        image.setLayoutParams(imageParams);

        // 13. Create a text section for the course code and course name
        LinearLayout courseInfoLayout = new LinearLayout(getContext());
        courseInfoLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        courseInfoLayout.setLayoutParams(infoParams);

        // 14. Highlight the course code
        TextView codeView = new TextView(getContext());
        codeView.setText(code);
        codeView.setTextSize(18);
        codeView.setTextColor(0xFF6366F1);
        codeView.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        // 15. Display the full course name as the main card content
        TextView nameView = new TextView(getContext());
        nameView.setText(name);
        nameView.setTextSize(15);
        nameView.setTextColor(0xFF111827);
        nameView.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        nameView.setMaxLines(3);
        nameView.setPadding(0, dpToPx(5), 0, dpToPx(5));

        // 16. Add a navigation label
        TextView viewCourseText = new TextView(getContext());
        viewCourseText.setText("View course resources");
        viewCourseText.setTextSize(12);
        viewCourseText.setTextColor(0xFF6B7280);

        courseInfoLayout.addView(codeView);
        courseInfoLayout.addView(nameView);
        courseInfoLayout.addView(viewCourseText);

        card.addView(image);
        card.addView(courseInfoLayout);

        // 17. Set flexible card dimensions
        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dpToPx(14)
        );

        card.setLayoutParams(cardParams);

        courseContainer.addView(card);

        // 18. Open the selected course detail page
        card.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            getContext(),
                            CourseDetailActivity.class
                    );

            intent.putExtra(
                    "courseDocumentId",
                    documentId
            );

            intent.putExtra(
                    "courseCode",
                    code
            );

            intent.putExtra(
                    "courseName",
                    name
            );

            startActivity(intent);
        });
    }

    // 19. Convert density-independent pixels into screen pixels
    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}