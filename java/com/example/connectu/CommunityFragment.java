package com.example.connectu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

public class CommunityFragment extends Fragment {

    LinearLayout semesterContainer;
    TextView btnNotification;
    View redDot;

    FirebaseFirestore db;
    String studentId;

    // 1. Store the number of active members for each semester
    int[] members = {120, 93, 94, 87, 100, 85, 132};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_community, container, false);

        // 2. Initialize Community page components
        semesterContainer = view.findViewById(R.id.semesterContainer);
        btnNotification = view.findViewById(R.id.btnNotification);
        redDot = view.findViewById(R.id.redDot);

        // 3. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 4. Retrieve the logged-in student ID from SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("ConnectU", requireActivity().MODE_PRIVATE);

        studentId = prefs.getString("studentId", "");

        // 5. Open the Notification page
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(
                    getActivity(),
                    NotificationActivity.class
            );

            startActivity(intent);
        });
        // 6. Check unread notifications and display semester cards
        checkUnreadNotifications();
        showSemesterCards();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 7. Check unread notifications again when returning to this page
        if (db != null) {
            checkUnreadNotifications();
        }
    }

    private void checkUnreadNotifications() {
        // 8. Retrieve unread notifications for the current student
        db.collection("notifications")
                .whereEqualTo("recipientStudentId", studentId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(query -> {
                    // 9. Show or hide the notification red dot
                    if (query.isEmpty()) {
                        redDot.setVisibility(View.GONE);
                    } else {
                        redDot.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showSemesterCards() {
        // 10. Generate forum cards for Semester 1 until Semester 7
        semesterContainer.removeAllViews();

        for (int i = 1; i <= 7; i++) {
            addSemesterCard(i, members[i - 1]);
        }
    }

    private void addSemesterCard(int semester, int activeMembers) {
        // 11. Create a semester forum card dynamically
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 24, 24, 24);
        card.setBackgroundResource(R.drawable.bg_course_card);
        card.setElevation(8);
        // 12. Display the semester title
        TextView title = new TextView(getContext());
        title.setText("Semester " + semester);
        title.setTextSize(20);
        title.setTextColor(0xFF111827);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        // 13. Display the number of active members
        TextView subtitle = new TextView(getContext());
        subtitle.setText(activeMembers + " active members");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF6B7280);
        // 14. Create the Join Forum button
        TextView button = new TextView(getContext());
        button.setText("Join Forum →");
        button.setTextSize(13);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setGravity(android.view.Gravity.CENTER);
        button.setBackgroundColor(0xFF6366F1);
        button.setPadding(18, 10, 18, 10);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 18, 0, 0);
        button.setLayoutParams(btnParams);

        card.addView(title);
        card.addView(subtitle);
        card.addView(button);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 18);
        card.setLayoutParams(cardParams);
        // 15. Open the selected semester forum
        card.setOnClickListener(v -> openForum(semester));
        button.setOnClickListener(v -> openForum(semester));

        semesterContainer.addView(card);
    }

    private void openForum(int semester) {
        // 16. Open Discussion List page and pass semester and student ID
        Intent intent = new Intent(
                getContext(),
                DiscussionListActivity.class
        );
        intent.putExtra("semester", semester);
        intent.putExtra("studentId", studentId);

        startActivity(intent);
    }
}