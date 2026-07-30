package com.example.connectu;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditDiscussionActivity extends AppCompatActivity {

    TextView btnBack, btnSaveChanges;
    EditText etTitle, etContent;

    FirebaseFirestore db;
    String discussionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_discussion);

        // 1. Initialize Edit Discussion page components
        btnBack = findViewById(R.id.btnBack);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve the selected discussion ID
        discussionId = getIntent().getStringExtra("discussionId");

        // 4. Display the existing discussion title and content
        etTitle.setText(getIntent().getStringExtra("title"));
        etContent.setText(getIntent().getStringExtra("content"));

        // 5. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 6. Save the updated discussion information
        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        // 7. Retrieve the updated discussion title and content
        String newTitle = etTitle.getText().toString().trim();
        String newContent = etContent.getText().toString().trim();
        // 8. Ensure all fields are completed
        if (newTitle.isEmpty() || newContent.isEmpty()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        // 9. Update the discussion information in Firestore
        db.collection("discussion")
                .document(discussionId)
                .update(
                        "title", newTitle,
                        "content", newContent,
                        "editedAt", FieldValue.serverTimestamp(),
                        "isEdited", true
                )
                .addOnSuccessListener(unused -> {

                    // 10. Display success message and close the page
                    Toast.makeText(this, "Discussion updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->

                        // 11. Display error message if update fails
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}