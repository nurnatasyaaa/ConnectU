package com.example.connectu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class ProfileFragment extends Fragment {

    de.hdodenhof.circleimageview.CircleImageView imgProfile;

    TextView tvProfileName, tvStudentId,
            tvProgramSemester, tvTrack,
            btnViewAllUploads, btnLogout,
            tvUploadCount, btnSettingsTop;

    LinearLayout uploadHistoryContainer;

    FirebaseFirestore db;
    String studentId;

    TextView btnChangePhoto;
    Uri selectedImageUri;
    ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 1. Initialize Profile page components
        imgProfile = view.findViewById(R.id.imgProfile);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileName.setText("");
        tvStudentId = view.findViewById(R.id.tvStudentId);
        tvStudentId.setText("");
        tvProgramSemester = view.findViewById(R.id.tvProgramSemester);
        tvProgramSemester.setText("");
        tvTrack = view.findViewById(R.id.tvTrack);
        tvTrack.setText("");
        tvUploadCount = view.findViewById(R.id.tvUploadCount);

        btnViewAllUploads = view.findViewById(R.id.btnViewAllUploads);
        btnLogout = view.findViewById(R.id.btnLogout);

        uploadHistoryContainer = view.findViewById(R.id.uploadHistoryContainer);

        // 2. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 3. Retrieve the current student ID and load profile data
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");

            loadStudentData();
        }

        // 4. Open the Upload History page
        btnViewAllUploads.setOnClickListener(v -> {

            Intent intent =
                    new Intent(getActivity(),
                            UploadHistoryActivity.class);

            intent.putExtra(
                    "studentName",
                    tvProfileName.getText().toString()
            );

            startActivity(intent);
        });

        // 5. Remove saved login information and return to Login page
        btnLogout.setOnClickListener(v -> {

            SharedPreferences prefs =
                    requireActivity().getSharedPreferences("ConnectU",
                            requireActivity().MODE_PRIVATE);

            prefs.edit()
                    .remove("studentId")
                    .apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });

        btnSettingsTop = view.findViewById(R.id.btnSettingsTop);

        // 6. Display a message for the future Settings feature
        btnSettingsTop.setOnClickListener(v -> {
            Toast.makeText(getContext(),
                    "Settings will be available in future version",
                    Toast.LENGTH_SHORT).show();
        });

        // 7. Handle the selected profile image from the gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();

                        if (selectedImageUri != null) {
                            requireActivity().getContentResolver().takePersistableUriPermission(
                                    selectedImageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                            imgProfile.setImageURI(selectedImageUri);

                            requireActivity()
                                    .getSharedPreferences("ConnectU", requireActivity().MODE_PRIVATE)
                                    .edit()
                                    .putString("profilePhoto_" + studentId, selectedImageUri.toString())
                                    .apply();

                            Toast.makeText(getContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 8. Open the gallery when the photo or Change Photo button is selected
        btnChangePhoto.setOnClickListener(v -> openGallery());
        imgProfile.setOnClickListener(v -> openGallery());
        return view;
    }

    private void loadStudentData() {
        // 9. Retrieve the current student's profile information from Firestore
        db.collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name =
                                documentSnapshot.getString("name");
                        String id =
                                documentSnapshot.getString("studentId");
                        String program =
                                documentSnapshot.getString("program");
                        Long semester =
                                documentSnapshot.getLong("semester");
                        String track =
                                documentSnapshot.getString("track");
                        // 10. Display student profile information
                        tvProfileName.setText(name);
                        tvStudentId.setText(id);
                        tvProgramSemester.setText(
                                program + " • Semester " + semester
                        );
                        tvTrack.setText(formatTrack(track));
                        // 11. Retrieve the saved profile photo
                        String savedPhoto = requireActivity()
                                .getSharedPreferences("ConnectU",
                                        requireActivity().MODE_PRIVATE)
                                .getString("profilePhoto_" + studentId, "");
                        if (!savedPhoto.isEmpty()) {
                            imgProfile.setImageURI(Uri.parse(savedPhoto));
                        }
                        // 12. Load recent uploads and upload statistics
                        loadRecentUploads(name);
                        loadUploadStats(name);
                    }
                });
    }

    private void loadRecentUploads(String studentName) {
        // 13. Clear the previous upload history
        uploadHistoryContainer.removeAllViews();
        // 14. Retrieve the three most recent resources uploaded by the student
        db.collection("resources")
                .whereEqualTo("uploadedBy", studentName)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 15. Display a message if the student has no uploads
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No uploads yet.");
                        empty.setTextColor(0xFF6B7280);
                        uploadHistoryContainer.addView(empty);
                        return;
                    }
                    // 16. Retrieve each uploaded resource
                    for (QueryDocumentSnapshot document
                            : queryDocumentSnapshots) {
                        String documentId = document.getId();
                        String fileName = document.getString("fileName");
                        String courseCode = document.getString("courseCode");
                        String category = document.getString("materialType");
                        String fileUrl = document.getString("fileUrl");
                        addUploadItem(
                                documentId,
                                fileName,
                                courseCode,
                                category,
                                fileUrl
                        );
                    }
                });
    }

    private void addUploadItem(String documentId,
                               String fileName,
                               String courseCode,
                               String category,
                               String fileUrl) {

        // 17. Create an upload history card dynamically
        LinearLayout item = new LinearLayout(getContext());

        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(20, 20, 20, 20);
        item.setBackgroundResource(R.drawable.bg_course_card);

        // 18. Display the uploaded file name
        TextView title = new TextView(getContext());

        title.setText("📄 " + fileName);
        title.setTextSize(15);
        title.setTextColor(0xFF111827);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        // 19. Display the course code and material category
        TextView subtitle = new TextView(getContext());

        subtitle.setText(courseCode + " • " + category);
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFF6B7280);

        TextView openHint = new TextView(getContext());
        openHint.setText("Tap to open");
        openHint.setTextSize(12);
        openHint.setTextColor(0xFF6366F1);

        item.addView(title);
        item.addView(subtitle);
        item.addView(openHint);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 12);
        item.setLayoutParams(params);

        // 20. Open the selected uploaded resource
        item.setOnClickListener(v -> {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                Toast.makeText(getContext(), "No file link found", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
            startActivity(intent);
        });

        uploadHistoryContainer.addView(item);
    }

    private void loadUploadStats(String studentName) {
        // 21. Retrieve the total number of resources uploaded by the student
        db.collection("resources")
                .whereEqualTo("uploadedBy", studentName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int uploadCount = queryDocumentSnapshots.size();
                    // 22. Display the total upload count
                    tvUploadCount.setText(String.valueOf(uploadCount));
                });
    }

    private String formatTrack(String track) {
        // 23. Convert track values into readable labels
        if (track == null) return "";
        switch (track) {
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

    private void openGallery() {
        // 24. Open the device gallery to select a profile image
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        imagePickerLauncher.launch(intent);
    }
}