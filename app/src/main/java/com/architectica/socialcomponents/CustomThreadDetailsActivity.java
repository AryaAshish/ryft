package com.architectica.socialcomponents;

import android.view.Menu;
import android.view.MenuItem;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.ui.activities.ThreadDetailsActivity;

public class CustomThreadDetailsActivity extends ThreadDetailsActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_thread_details_menu, menu);

        menu.removeItem(R.id.action_edit);

        if (!ChatSDK.thread().muteEnabled(thread)) {
            menu.removeItem(R.id.action_mute);
        }

        if (!ChatSDK.thread().canAddUsersToThread(thread)) {
            menu.removeItem(R.id.action_add_users);
        }

        if (!ChatSDK.thread().canLeaveThread(thread)) {
            menu.removeItem(R.id.action_leave);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        if (item.getItemId() == R.id.action_edit) {

        }
        if (item.getItemId() == R.id.action_mute) {
            if (thread.isMuted()) {
                ChatSDK.thread().unmute(thread).subscribe(this);
            } else {
                ChatSDK.thread().mute(thread).subscribe(this);
            }
            invalidateOptionsMenu();
        }
        if (item.getItemId() == R.id.action_add_users) {
            ChatSDK.ui().startAddUsersToThreadActivity(this, thread.getEntityID());
        }
        if (item.getItemId() == R.id.action_leave) {
            ChatSDK.thread().leaveThread(thread).doOnComplete(this::finish).subscribe(this);
        }
        return true;
    }

    @Override
    protected void initViews() {
        super.initViews();

    }
}
