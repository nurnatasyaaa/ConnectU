package com.example.connectu;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class RepositoryFragment extends Fragment {

    TextView cardNotes, cardQuizzes, cardFinalPapers, btnUpload;
    String studentId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repository, container, false);
        // 1. Retrieve student ID passed from MainActivity
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
        }
        // 2. Initialize repository category cards
        cardNotes = view.findViewById(R.id.cardNotes);
        cardQuizzes = view.findViewById(R.id.cardQuizzes);
        cardFinalPapers = view.findViewById(R.id.cardFinalPapers);
        // 3. Open semester selection based on selected material type
        cardNotes.setOnClickListener(v -> openSemesterSelection("Notes"));
        cardQuizzes.setOnClickListener(v -> openSemesterSelection("Quizzes"));
        cardFinalPapers.setOnClickListener(v -> openSemesterSelection("Final Papers"));
        // 4. Initialize upload button
        btnUpload = view.findViewById(R.id.btnUpload);
        // 5. Open Upload page and pass the current student ID
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UploadActivity.class);
            intent.putExtra("studentId", studentId);
            startActivity(intent);
        });
        return view;
    }

    private void openSemesterSelection(String materialType) {
        // 6. Open Semester Selection page and pass the selected material type
        Intent intent = new Intent(getContext(), SemesterSelectionActivity.class);
        intent.putExtra("materialType", materialType);
        startActivity(intent);
    }
}