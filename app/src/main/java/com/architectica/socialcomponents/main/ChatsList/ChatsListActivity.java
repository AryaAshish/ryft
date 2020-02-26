package com.architectica.socialcomponents.main.ChatsList;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.architectica.socialcomponents.AllUsersList.UsersActivity;
import com.architectica.socialcomponents.R;
import com.architectica.socialcomponents.adapters.ChatsListAdapter;
import com.architectica.socialcomponents.model.Profile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatsListActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private ChatsListAdapter adapter;
    private RecyclerView recyclerView;

    FloatingActionButton allUsers;
    int i;

    List<Profile> profiles = new ArrayList<>();
    List<String>timestamplist=new ArrayList<>();

    ProgressDialog pd;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats_list);

        pd = new ProgressDialog(ChatsListActivity.this);
        pd.setMessage("Loading...");
        pd.setCanceledOnTouchOutside(false);
        pd.show();

        mToolbar = (Toolbar) findViewById(R.id.users_appBar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setTitle("Chats");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FirebaseDatabase.getInstance().getReference("messages/" + FirebaseAuth.getInstance().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    profiles.clear();

                    int count = (int) dataSnapshot.getChildrenCount();

                    i = 1;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                        String id = snapshot.getKey();


                        FirebaseDatabase.getInstance().getReference("profiles/" + id).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                Profile profile = dataSnapshot.getValue(Profile.class);
                                profile.setId(dataSnapshot.getKey());
                                DatabaseReference df= FirebaseDatabase.getInstance().getReference("messages/"+ FirebaseAuth.getInstance().getUid()+"/"+id);
                                Query lastquery =df.orderByKey().limitToLast(1);
                                lastquery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot dataSnapshot1:dataSnapshot.getChildren()){
                                            String message = dataSnapshot1.child("timestamp").getValue().toString();
                                           // timestamplist.add(message);
                                            profile.setTimestamp(Long.valueOf(message));
                                             //Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                                        }

                                        profiles.add(profile);

                                        if (i == count){

                                            initChatsList(profiles,timestamplist);

                                        }

                                        i++;

                                        pd.dismiss();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
pd.dismiss();
                                    }
                                });
pd.dismiss();

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
pd.dismiss();
                            }
                        });

                    }


                }
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
pd.dismiss();
            }
        });

        allUsers = findViewById(R.id.viewAllUsers);

        allUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatsListActivity.this,UsersActivity.class);
                startActivity(intent);
            }
        });

    }

    private void initChatsList(List<Profile> profiles, List<String> timestamplist){

        recyclerView = findViewById(R.id.conv_list);

        Collections.sort(profiles, new Comparator<Profile>() {
            @Override
            public int compare(Profile o1, Profile o2) {
                if (o1.getTimestamp() > o2.getTimestamp()){

                   return -1;
                }else if (o1.getTimestamp() < o2.getTimestamp()){

                    return 1;
                }else {
                    return 0;
                }

            }
        });

        adapter = new ChatsListAdapter(this,profiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(adapter);

        pd.dismiss();

    }

}
