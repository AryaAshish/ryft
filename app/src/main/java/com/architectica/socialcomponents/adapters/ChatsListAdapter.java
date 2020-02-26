package com.architectica.socialcomponents.adapters;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.architectica.socialcomponents.R;
import com.architectica.socialcomponents.main.Chat.ChatActivity;
import com.architectica.socialcomponents.model.Profile;
import com.architectica.socialcomponents.views.CircularImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChatsListAdapter extends RecyclerView.Adapter<ChatsListAdapter.ChatsListViewHolder> {

    private Context context;
    private List<Profile> profiles;

    public ChatsListAdapter(Context context,List<Profile> profiles){

        this.context = context;
        this.profiles = profiles;
    }

    @Override
    public ChatsListAdapter.ChatsListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.users_single_layout,parent, false);

        return new ChatsListAdapter.ChatsListViewHolder(v);

    }

    @Override
    public void onBindViewHolder(@NonNull ChatsListViewHolder chatsListViewHolder, int i) {

        chatsListViewHolder.displayName.setText(profiles.get(i).getUsername());
        Picasso.with(chatsListViewHolder.profileImage.getContext()).load(profiles.get(i).getPhotoUrl())
                .placeholder(R.drawable.default_avatar).into(chatsListViewHolder.profileImage);


        chatsListViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent chatIntent = new Intent(context, ChatActivity.class);
                chatIntent.putExtra("ChatUser",profiles.get(i).getId());
                chatIntent.putExtra("UserName",profiles.get(i).getUsername());
                context.startActivity(chatIntent);

            }
        });

    }

    @Override
    public int getItemCount() {

        return profiles.size();

    }

    public class ChatsListViewHolder extends RecyclerView.ViewHolder {

        public CircularImageView profileImage;
        public TextView displayName;

        public ChatsListViewHolder(View view) {
            super(view);

            profileImage = (CircularImageView) view.findViewById(R.id.user_single_image);
            displayName = (TextView) view.findViewById(R.id.user_single_name);

        }
    }
}
