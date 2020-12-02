package com.architectica.socialcomponents.main.Chat;

import android.view.View;

import sdk.chat.ui.activities.ChatActivity;

public class CustomChatActivity extends ChatActivity {

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void initViews() {
        super.initViews();

        // Action bar
        chatActionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //do nothing
            }
        });

    }

}
