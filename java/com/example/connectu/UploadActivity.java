package com.example.connectu;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadActivity extends AppCompatActivity {

    TextView btnBack, tvUploadTitle, tvProgress, btnNext;
    TextView btnNotes, btnQuizzes, btnFinalPapers, tvPreview, tvAutoTags;
    LinearLayout step1Layout, step2Layout, step3Layout, semesterContainer;

    Spinner spinnerSubject;
    EditText etFileName, etFileUrl;
    TextView tvTitlePreview;
    CheckBox cbDisclaimer;

    FirebaseFirestore db;

    // 1. Store the current upload step and selected upload information
    int currentStep = 1;
    int selectedSemester = 1;
    String selectedMaterialType = "Notes";
    String selectedCourseCode = "";
    String selectedCourseName = "";
    String studentId = "";
    String uploaderName = "";

    List<String> subjectDisplayList = new ArrayList<>();
    List<String> subjectCodeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve current student ID from the previous page
        studentId = getIntent().getStringExtra("studentId");

        if (studentId != null && !studentId.isEmpty()) {
            loadUploaderName();
        }

        // 4. Initialize UI components
        btnBack = findViewById(R.id.btnBack);
        tvUploadTitle = findViewById(R.id.tvUploadTitle);
        tvProgress = findViewById(R.id.tvProgress);
        btnNext = findViewById(R.id.btnNext);

        btnNotes = findViewById(R.id.btnNotes);
        btnQuizzes = findViewById(R.id.btnQuizzes);
        btnFinalPapers = findViewById(R.id.btnFinalPapers);
        tvPreview = findViewById(R.id.tvPreview);
        tvAutoTags = findViewById(R.id.tvAutoTags);
        cbDisclaimer = findViewById(R.id.cbDisclaimer);

        step1Layout = findViewById(R.id.step1Layout);
        step2Layout = findViewById(R.id.step2Layout);
        step3Layout = findViewById(R.id.step3Layout);
        semesterContainer = findViewById(R.id.semesterContainer);

        spinnerSubject = findViewById(R.id.spinnerSubject);

        etFileName = findViewById(R.id.etFileName);
        etFileUrl = findViewById(R.id.etFileUrl);
        tvTitlePreview = findViewById(R.id.tvTitlePreview);

        // 5. Return to the previous upload step or close the page
        btnBack.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                showStep();
            } else {
                finish();
            }
        });

        // 6. Select repository material category
        btnNotes.setOnClickListener(v -> selectMaterial("Notes"));
        btnQuizzes.setOnClickListener(v -> selectMaterial("Quizzes"));
        btnFinalPapers.setOnClickListener(v -> selectMaterial("Final Papers"));

        // 7. Store the selected subject information
        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView selectedText = (TextView) view;

                if (position == 0) {
                    selectedText.setTextColor(0xFF9CA3AF);
                    selectedCourseCode = "";
                    selectedCourseName = "";
                    return;
                }

                selectedText.setTextColor(0xFF111827);

                selectedCourseCode = subjectCodeList.get(position);

                String selectedDisplay = subjectDisplayList.get(position);

                if (selectedDisplay.contains(" - ")) {
                    selectedCourseName = selectedDisplay.substring(
                            selectedDisplay.indexOf(" - ") + 3
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 8. Navigate through the three upload steps
        btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {

                if (!validateStep1()) {
                    return;
                }

                currentStep = 2;
                showStep();

            } else if (currentStep == 2) {

                if (!validateStep2()) {
                    return;
                }

                currentStep = 3;
                updatePreviewAndTags();
                showStep();

            } else {

                if (!validateStep3()) {
                    return;
                }

                publishResource();
            }
        });

        // 9. Prepare semester buttons, subjects, material type, and first step
        createSemesterButtons();
        selectMaterial("Notes");
        loadSubjectsBySemester();
        showStep();
    }

    private void loadUploaderName() {

        // 10. Retrieve the uploader name from the students collection
        db.collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        uploaderName = documentSnapshot.getString("name");
                    }
                });
    }

    private void createSemesterButtons() {
        // 11. Generate Semester 1 until Semester 7 buttons dynamically
        semesterContainer.removeAllViews();
        for (int i = 1; i <= 7; i++) {
            TextView semBtn = new TextView(this);

            semBtn.setText(String.valueOf(i));
            semBtn.setTextSize(17);
            semBtn.setGravity(android.view.Gravity.CENTER);
            semBtn.setTypeface(null, android.graphics.Typeface.BOLD);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(58, 58);

            params.setMargins(0, 0, 10, 0);
            semBtn.setLayoutParams(params);

            int sem = i;
            // 12. Update the selected semester and retrieve its subjects
            semBtn.setOnClickListener(v -> {
                selectedSemester = sem;
                createSemesterButtons();
                loadSubjectsBySemester();
            });
            // 13. Apply selected or unselected semester button design
            if (i == selectedSemester) {
                semBtn.setTextColor(0xFFFFFFFF);
                semBtn.setBackgroundResource(R.drawable.bg_semester_selected);
            } else {
                semBtn.setTextColor(0xFF111827);
                semBtn.setBackgroundResource(R.drawable.bg_semester_unselected);
            }
            semesterContainer.addView(semBtn);
        }
    }

    private void loadSubjectsBySemester() {
        // 14. Clear the previous subject list
        subjectDisplayList.clear();
        subjectCodeList.clear();
        subjectDisplayList.add("Choose the subject");
        subjectCodeList.add("");
        // 15. Retrieve subjects based on semester and programme from Firestore
        db.collection("courses")
                .whereEqualTo("semester", selectedSemester)
                .whereEqualTo("program", "CDCS240")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String courseCode = document.getString("courseCode");
                        String courseName = document.getString("courseName");

                        subjectCodeList.add(courseCode);
                        subjectDisplayList.add(
                                courseCode + " - " + courseName
                        );
                    }
                    if (subjectDisplayList.size() == 1) {
                        subjectDisplayList.clear();
                        subjectCodeList.clear();

                        subjectDisplayList.add("No subject found");
                        subjectCodeList.add("");
                    }
                    // 16. Display the retrieved subjects in the spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            subjectDisplayList
                    );
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );
                    spinnerSubject.setAdapter(adapter);
                });
    }

    private void selectMaterial(String materialType) {

        // 17. Store and highlight the selected material category
        selectedMaterialType = materialType;

        resetMaterialButtons();

        if (materialType.equals("Notes")) {
            btnNotes.setText("Notes                                      ✓");
            btnNotes.setBackgroundColor(0xFFCFC7FF);

        } else if (materialType.equals("Quizzes")) {
            btnQuizzes.setText("Quizzes                                             ✓");
            btnQuizzes.setBackgroundColor(0xFFCFC7FF);

        } else {
            btnFinalPapers.setText("Final Papers                                      ✓");
            btnFinalPapers.setBackgroundColor(0xFFCFC7FF);
        }
    }

    private void resetMaterialButtons() {

        // 18. Reset all material category buttons
        btnNotes.setText("Notes");
        btnQuizzes.setText("Quizzes");
        btnFinalPapers.setText("Final Papers");

        btnNotes.setBackgroundResource(R.drawable.bg_course_card);
        btnQuizzes.setBackgroundResource(R.drawable.bg_course_card);
        btnFinalPapers.setBackgroundResource(R.drawable.bg_course_card);
    }

    private void showStep() {
        // 19. Display only the current upload step
        step1Layout.setVisibility(
                currentStep == 1 ? View.VISIBLE : View.GONE
        );
        step2Layout.setVisibility(
                currentStep == 2 ? View.VISIBLE : View.GONE
        );
        step3Layout.setVisibility(
                currentStep == 3 ? View.VISIBLE : View.GONE
        );
        // 20. Update the title, progress indicator, and button text
        if (currentStep == 1) {
            tvUploadTitle.setText("Upload: Step 1");
            tvProgress.setText("— • •");
            btnNext.setText("Continue →");
        } else if (currentStep == 2) {
            tvUploadTitle.setText("Upload: Step 2");
            tvProgress.setText("• — •");
            btnNext.setText("Continue →");
        } else {
            tvUploadTitle.setText("Upload: Final Step");
            tvProgress.setText("• • —");
            btnNext.setText("Publish Resource");
        }
    }

    private boolean validateStep1() {

        // 21. Ensure a valid subject is selected
        if (selectedCourseCode == null || selectedCourseCode.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select a valid subject",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    private boolean validateStep2() {
        // 22. Retrieve the entered Google Drive URL
        String fileUrl = etFileUrl.getText().toString().trim();
        // 23. Ensure all file information is completed
        if (etFileUrl.getText().toString().trim().isEmpty()
                || etFileName.getText().toString().trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Please complete file information",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }
        // 24. Ensure the entered link is a valid Google Drive link
        if (!(fileUrl.contains("drive.google.com")
                || fileUrl.contains("docs.google.com"))) {
            Toast.makeText(
                    this,
                    "Please upload a valid Google Drive link.",
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }
        // 25. Ensure the user agrees to the upload disclaimer
        if (!cbDisclaimer.isChecked()) {
            Toast.makeText(
                    this,
                    "Please confirm the disclaimer before continuing",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }
        return true;
    }

    private boolean validateStep3() {

        // 26. Final upload step validation
        return true;
    }

    private void updatePreviewAndTags() {

        // 27. Display the uploaded file preview
        String fileName = etFileName.getText().toString().trim();

        tvPreview.setText(fileName + "\nGoogle Drive file");

        tvTitlePreview.setText(
                etFileName.getText().toString().trim()
        );

        // 28. Automatically generate resource tags
        String tags = selectedCourseCode
                + ", Semester "
                + selectedSemester
                + ", "
                + selectedMaterialType;

        tvAutoTags.setText(tags);
    }

    private void publishResource() {

        // 29. Retrieve the final resource information
        String fileName = etFileName.getText().toString().trim();
        String fileUrl = etFileUrl.getText().toString().trim();
        String title = tvTitlePreview.getText().toString().trim();
        String tags = tvAutoTags.getText().toString();

        // 30. Prepare resource data for Firestore
        Map<String, Object> resource = new HashMap<>();
        resource.put("courseDocumentId", selectedCourseCode);
        resource.put("courseCode", selectedCourseCode);
        resource.put("courseName", selectedCourseName);
        resource.put("materialType", selectedMaterialType);
        resource.put("category", selectedMaterialType);
        resource.put("fileName", fileName);
        resource.put("title", title);
        resource.put("fileUrl", fileUrl);
        resource.put("tags", tags);
        resource.put(
                "uploadedBy",
                uploaderName == null || uploaderName.isEmpty()
                        ? "Unknown"
                        : uploaderName
        );
        resource.put("uploadedAtText", "Just now");
        resource.put("uploadDate", FieldValue.serverTimestamp());
        resource.put("semester", selectedSemester);
        resource.put("program", "CDCS240");
        // 31. Save the uploaded resource into Firestore
        db.collection("resources")
                .add(resource)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(
                            this,
                            "Resource published successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}