package com.architectica.socialcomponents.main.postDetails;

import android.os.Bundle;

import com.architectica.socialcomponents.R;
import com.architectica.socialcomponents.adapters.RewardsAppliedAdapter;
import com.architectica.socialcomponents.model.Profile;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import static com.architectica.socialcomponents.main.postDetails.RewardDetailsActivity.REWARD_ID_EXTRA_KEY;

public class RewardsAppliedPeople extends AppCompatActivity {

    RecyclerView peopleApplied;

    private RewardsAppliedAdapter adapter;

    private String postId;

    List<Profile> profiles = new ArrayList<>();

    List<String> profileids = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people_applied);

        peopleApplied = findViewById(R.id.applied_people);

        postId = getIntent().getStringExtra(REWARD_ID_EXTRA_KEY);

        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                DataSnapshot dataSnapshot1;

                dataSnapshot1 = dataSnapshot.child("rewards/" + postId + "/requests");

                if (dataSnapshot1.exists()){

                    profiles.clear();
                    profileids.clear();

                    for (DataSnapshot snapshot : dataSnapshot1.getChildren()){

                        String id = snapshot.child("userid").getValue(String.class);

                        profileids.add(id);

                    }

                    DataSnapshot dataSnapshot2 = dataSnapshot.child("profiles");

                    for (int j=0;j<profileids.size();j++){

                        DataSnapshot dataSnapshot3 = dataSnapshot2.child(profileids.get(j));

                        Profile profile = dataSnapshot3.getValue(Profile.class);

                        profile.setId(profileids.get(j));

                        profiles.add(profile);

                    }

                    initPeopleApplied(profiles);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void initPeopleApplied(List<Profile> profiles){

        adapter = new RewardsAppliedAdapter(this,profiles,postId);
        peopleApplied.setLayoutManager(new LinearLayoutManager(this));
        ((SimpleItemAnimator) peopleApplied.getItemAnimator()).setSupportsChangeAnimations(false);
        peopleApplied.setAdapter(adapter);

    }

}
