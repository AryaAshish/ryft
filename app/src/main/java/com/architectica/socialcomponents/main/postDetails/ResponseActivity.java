package com.architectica.socialcomponents.main.postDetails;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import com.architectica.socialcomponents.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.disposables.Disposable;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.User;
import sdk.chat.core.interfaces.UserListItem;
import sdk.chat.core.session.ChatSDK;
import sdk.guru.common.RX;

import static com.architectica.socialcomponents.main.postDetails.ProjectDetailsActivity.PROJECT_ID_EXTRA_KEY;

public class ResponseActivity extends AppCompatActivity {

    private String postId;

    private String appliedUserId;

    private LinearLayout responseLayout;

    private Button select;

    private List<String> quesids = new ArrayList<>();

    private List<String> questions = new ArrayList<>();

    private List<String> answers = new ArrayList<>();

    private LinearLayout pointsLayout;

    private EditText pointsEditText;

    private String userPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);

        postId = getIntent().getStringExtra(PROJECT_ID_EXTRA_KEY);

        appliedUserId = getIntent().getStringExtra("userid");

        responseLayout = findViewById(R.id.responseLayout);

        pointsLayout = findViewById(R.id.pointsLayout);

        pointsEditText = findViewById(R.id.pointsEditText);

        select = findViewById(R.id.select);

        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                pointsLayout.setVisibility(View.GONE);

                DataSnapshot dataSnapshot1 = dataSnapshot.child("projects/" + postId + "/responses/" + appliedUserId);

                if (dataSnapshot1.hasChild("answers")){

                    //answers are there..so show the response

                    quesids.clear();
                    questions.clear();
                    answers.clear();

                    DataSnapshot dataSnapshot2 = dataSnapshot1.child("answers");

                    for(DataSnapshot snapshot : dataSnapshot2.getChildren()){

                        quesids.add(snapshot.child("questionid").getValue(String.class));

                        answers.add(snapshot.child("answer").getValue(String.class));


                    }

                    DataSnapshot dataSnapshot3 = dataSnapshot.child("projects/" + postId + "/questions");

                    for (int i=0;i<quesids.size();i++){

                        String question = dataSnapshot3.child(quesids.get(i)).child("question").getValue(String.class);

                        questions.add(question);

                        LinearLayout newView = (LinearLayout) getLayoutInflater().inflate(R.layout.response_card, null);

                        TextView ques = newView.findViewById(R.id.question);

                        TextView ans = newView.findViewById(R.id.answer);

                        ques.setText(question);

                        ans.setText(answers.get(i));

                        responseLayout.addView(newView);

                    }

                }
                else{

                    //no answers..just show the select button

                }

                DataSnapshot dataSnapshot2 = dataSnapshot.child("projects/" + postId + "/selectedCandidates");

                if (dataSnapshot2.hasChild(appliedUserId)){

                    select.setText("SELECTED");
                    select.setEnabled(false);

                }
                else {

                    select.setText("SELECT");
                    select.setEnabled(true);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<UserListItem> users = new ArrayList<>();

                        User user = ChatSDK.db().fetchUserWithEntityID(appliedUserId);

                        users.add(user);

                        Thread thread = ChatSDK.db().fetchThreadWithEntityID(postId);

                        ChatSDK.thread().addUsersToThread(thread, User.convertIfPossible(users))
                                .observeOn(RX.main())
                                .doFinally(() -> {
                                    HashMap<String,Object> selecteduser = new HashMap<>();

                                    selecteduser.put("userid",appliedUserId);

                                    FirebaseDatabase.getInstance().getReference("projects/" + postId).child("selectedCandidates").child(appliedUserId).setValue(selecteduser);

                                    HashMap<String,Object> selectedProject = new HashMap<>();

                                    selectedProject.put("projectid",postId);

                                    FirebaseDatabase.getInstance().getReference("profiles/" + appliedUserId + "/selections").child(postId).setValue(selectedProject);

                                    finish();
                                })
                                .subscribe(() -> {
                                    setResult(Activity.RESULT_OK);
                                }, throwable -> {
                                    Toast.makeText(ResponseActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    setResult(Activity.RESULT_CANCELED);
                                });

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

    }
}
