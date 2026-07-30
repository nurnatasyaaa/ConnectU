package com.example.connectu;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class EditResourceActivity extends AppCompatActivity {

    TextView btnBack, btnSaveChanges;
    EditText etFileName, etFileUrl;

    FirebaseFirestore db;

    String documentId, title, fileName, fileUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_resource);

        // 1. Initialize Edit Resource page components
        btnBack = findViewById(R.id.btnBack);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        etFileName = findViewById(R.id.etFileName);
        etFileUrl = findViewById(R.id.etFileUrl);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve the selected resource information
        documentId = getIntent().getStringExtra("documentId");
        title = getIntent().getStringExtra("title");
        fileName = getIntent().getStringExtra("fileName");
        fileUrl = getIntent().getStringExtra("fileUrl");

        // 4. Display the existing resource file name and URL
        etFileName.setText(fileName);
        etFileUrl.setText(fileUrl);

        // 5. Return to the previous page
        btnBack.setOnClickListener(v -> finish());

        // 6. Save the updated resource information
        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        // 7. Retrieve the updated file name and URL
        String newFileName = etFileName.getText().toString().trim();
        String newFileUrl = etFileUrl.getText().toString().trim();
        // 8. Ensure all fields are completed
        if (newFileName.isEmpty() || newFileUrl.isEmpty()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        // 9. Ensure only Google Drive or Google Docs links are used
        if (!(newFileUrl.startsWith("https://drive.google.com/")
                || newFileUrl.startsWith("https://docs.google.com/"))) {
            Toast.makeText(this,
                    "Only Google Drive links are allowed.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        // 10. Update the selected resource in Firestore
        db.collection("resources")
                .document(documentId)
                .update(
                        "fileName", newFileName,
                        "title", newFileName,
                        "fileUrl", newFileUrl
                )
                .addOnSuccessListener(unused -> {
                    // 11. Display success message and close the page
                    Toast.makeText(this, "Resource updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        // 12. Display an error message if the update fails
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}