/*
 *  Copyright 2017 Rozdoum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.architectica.socialcomponents;

import com.architectica.socialcomponents.adapters.CustomEditThreadDetailsActivity;
import com.architectica.socialcomponents.main.Chat.CustomChatActivity;
import com.architectica.socialcomponents.main.main.Chats.CustomChatsFragment;
import com.architectica.socialcomponents.main.main.MainActivity;

import sdk.chat.app.firebase.ChatSDKFirebase;
import sdk.chat.core.session.ChatSDK;

public class Application extends android.app.Application {

    public static final String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            ChatSDKFirebase.quickStart(this,"chatsdk","api key",true);
            ChatSDK.ui().setMainActivity(MainActivity.class);
            //ChatSDK.ui().setThreadDetailsActivity(CustomThreadDetailsActivity.class);
            //ChatSDK.ui().setThreadEditDetailsActivity(CustomEditThreadDetailsActivity.class);
            //ChatSDK.ui().setChatActivity(CustomChatActivity.class);
            ChatSDK.ui().setPrivateThreadsFragment(new CustomChatsFragment());
            ApplicationHelper.initDatabaseHelper(this);
            // PostInteractor.getInstance(this).subscribeToNewPosts();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
