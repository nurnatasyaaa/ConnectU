package com.example.connectu;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditReplyActivity extends AppCompatActivity {

    EditText etReply;
    Button btnSaveReply;

    FirebaseFirestore db;

    String discussionId, replyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reply);

        // 1. Initialize Edit Reply page components
        etReply = findViewById(R.id.etReply);
        btnSaveReply = findViewById(R.id.btnSaveReply);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve discussion ID and reply ID
        discussionId = getIntent().getStringExtra("discussionId");
        replyId = getIntent().getStringExtra("replyId");

        // 4. Display the existing reply text
        etReply.setText(getIntent().getStringExtra("replyText"));

        // 5. Save the updated reply
        btnSaveReply.setOnClickListener(v -> updateReply());
    }

    private void updateReply() {
        // 6. Retrieve the updated reply text
        String replyText = etReply.getText().toString().trim();
        // 7. Ensure the reply is not empty
        if (replyText.isEmpty()) {
            Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        // 8. Prepare the updated reply information
        Map<String, Object> updates = new HashMap<>();
        updates.put("replyText", replyText);
        updates.put("isEdited", true);
        updates.put("editedAt", FieldValue.serverTimestamp());
        // 9. Update the reply in the Firestore replies subcollection
        db.collection("discussion")
                .document(discussionId)
                .collection("replies")
                .document(replyId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    // 10. Display success message and close the page
                    Toast.makeText(this, "Reply updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->

                        // 11. Display an error message if the update fails
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}