package com.example.connectu;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

public class NewDiscussionActivity extends AppCompatActivity {

    TextView btnBack, btnPostDiscussion, tvSemesterInfo;
    EditText etTitle, etContent;
    Spinner spinnerSubject;

    FirebaseFirestore db;

    int selectedSemester = 1;
    String studentId = "";
    String currentUserName = "";

    String selectedSubjectCode = "";
    String selectedSubjectName = "";

    List<String> subjectDisplayList = new ArrayList<>();
    List<String> subjectCodeList = new ArrayList<>();
    List<String> subjectNameList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_discussion);

        // 1. Initialize New Discussion page components
        btnBack = findViewById(R.id.btnBack);
        btnPostDiscussion = findViewById(R.id.btnPostDiscussion);
        tvSemesterInfo = findViewById(R.id.tvSemesterInfo);

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        spinnerSubject = findViewById(R.id.spinnerSubject);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve the selected semester and current student ID
        selectedSemester = getIntent().getIntExtra("semester", 1);
        studentId = getIntent().getStringExtra("studentId");

        // 4. Display the selected semester forum title
        tvSemesterInfo.setText("Semester " + selectedSemester + " Forum");

        // 5. Load the current student's name and subjects for the selected semester
        loadCurrentUserName();
        loadSubjectsBySemester();

        // 6. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 7. Post the new discussion
        btnPostDiscussion.setOnClickListener(v -> postDiscussion());

        // 8. Store the selected subject information
        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView selectedText = (TextView) view;

                if (position == 0) {
                    selectedText.setTextColor(0xFF9CA3AF);
                    selectedSubjectCode = "";
                    selectedSubjectName = "";
                    return;
                }

                selectedText.setTextColor(0xFF111827);
                selectedSubjectCode = subjectCodeList.get(position);
                selectedSubjectName = subjectNameList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadCurrentUserName() {
        // 9. Stop if the current student ID is unavailable
        if (studentId == null || studentId.isEmpty()) return;
        // 10. Retrieve the current student's name from Firestore
        db.collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("name");
                    }
                });
    }

    private void loadSubjectsBySemester() {
        // 11. Clear the previous subject lists
        subjectDisplayList.clear();
        subjectCodeList.clear();
        subjectNameList.clear();
        // 12. Add the default subject selection option
        subjectDisplayList.add("Choose subject");
        subjectCodeList.add("");
        subjectNameList.add("");
        // 13. Retrieve subjects based on the selected semester and programme
        db.collection("courses")
                .whereEqualTo("semester", selectedSemester)
                .whereEqualTo("program", "CDCS240")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String courseCode = document.getString("courseCode");
                        String courseName = document.getString("courseName");

                        subjectDisplayList.add(courseCode + " - " + courseName);
                        subjectCodeList.add(courseCode == null ? "" : courseCode);
                        subjectNameList.add(courseName == null ? "" : courseName);
                    }
                    // 14. Display the retrieved subjects in the subject spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            subjectDisplayList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSubject.setAdapter(adapter);
                });
    }

    private void postDiscussion() {

        // 15. Retrieve the entered discussion title and content
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        // 16. Ensure all required discussion information is completed
        if (title.isEmpty()
                || selectedSubjectCode.isEmpty()
                || content.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please fill in title, subject and content",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // 17. Prepare the new discussion information
        Map<String, Object> discussion = new HashMap<>();

        discussion.put("title", title);
        discussion.put("content", content);

        // Store the selected subject information
        discussion.put("courseCode", selectedSubjectCode);
        discussion.put("courseName", selectedSubjectName);

        discussion.put(
                "postedBy",
                currentUserName == null || currentUserName.isEmpty()
                        ? "Unknown"
                        : currentUserName
        );

        discussion.put("postedById", studentId);
        discussion.put("postedAt", FieldValue.serverTimestamp());
        discussion.put("semester", selectedSemester);
        discussion.put("program", "CDCS240");
        discussion.put("replyCount", 0);
        discussion.put("likedBy", new ArrayList<String>());

        // 18. Save the new discussion into Firestore
        db.collection("discussion")
                .add(discussion)
                .addOnSuccessListener(documentReference -> {

                    // 19. Display success message and close the page
                    Toast.makeText(
                            this,
                            "Discussion posted",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e ->

                        // 20. Display an error message if posting fails
                        Toast.makeText(
                                this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}