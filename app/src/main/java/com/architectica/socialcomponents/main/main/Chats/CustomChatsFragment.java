package com.architectica.socialcomponents.main.main.Chats;

import com.architectica.socialcomponents.R;

import sdk.chat.ui.fragments.PrivateThreadsFragment;

public class CustomChatsFragment extends PrivateThreadsFragment {
    @Override
    protected int getLayout() {
        return R.layout.fragment_threads;
    }
}
