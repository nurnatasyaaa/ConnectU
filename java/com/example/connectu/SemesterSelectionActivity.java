package com.example.connectu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SemesterSelectionActivity extends AppCompatActivity {

    LinearLayout semesterContainer;
    TextView btnBack;

    String materialType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_semester_selection);
        // 1. Initialize semester container and back button
        semesterContainer = findViewById(R.id.semesterContainer);
        btnBack = findViewById(R.id.btnBack);
        // 2. Retrieve selected material type from Repository page
        materialType = getIntent().getStringExtra("materialType");
        // 3. Return to the previous page
        btnBack.setOnClickListener(v -> finish());
        // 4. Display all semester cards
        showSemesters();
    }

    private void showSemesters() {
        // 5. Generate Semester 1 until Semester 7
        for (int i = 1; i <= 7; i++) {
            addSemesterCard(i);
        }
    }

    private void addSemesterCard(int semester) {
        // 6. Create semester card dynamically
        TextView card = new TextView(this);
        String detail;
        // 7. Set different descriptions based on semester
        if (semester == 5 || semester == 6) {
            detail = "Choose track after this";
        } else if (semester == 7) {
            detail = "Internship resources";
        } else {
            detail = "General academic subjects";
        }

        // 8. Set semester card design and content
        card.setText("Semester " + semester + "\n" + detail);
        card.setTextSize(16);
        card.setTextColor(0xFF111827);
        card.setPadding(24, 24, 24, 24);
        card.setBackgroundResource(R.drawable.bg_course_card);
        card.setElevation(6);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        // 9. Open Subject Selection page and pass material type and semester
        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SemesterSelectionActivity.this,
                    SubjectSelectionActivity.class
            );
            intent.putExtra("materialType", materialType);
            intent.putExtra("semester", semester);
            startActivity(intent);
        });
        semesterContainer.addView(card);
    }
}