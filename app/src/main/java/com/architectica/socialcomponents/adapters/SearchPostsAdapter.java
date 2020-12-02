/*
 * Copyright 2017 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.architectica.socialcomponents.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.architectica.socialcomponents.R;
import com.architectica.socialcomponents.adapters.holders.LoadViewHolder;
import com.architectica.socialcomponents.adapters.holders.PostImageViewHolder;
import com.architectica.socialcomponents.adapters.holders.PostTextViewHolder;
import com.architectica.socialcomponents.adapters.holders.PostVideoViewHolder;
import com.architectica.socialcomponents.adapters.holders.PostViewHolder;
import com.architectica.socialcomponents.controllers.LikeController;
import com.architectica.socialcomponents.main.base.BaseActivity;
import com.architectica.socialcomponents.managers.PostManager;
import com.architectica.socialcomponents.model.Post;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;

import java.util.List;


public class SearchPostsAdapter extends BasePostsAdapter {
    public static final String TAG = SearchPostsAdapter.class.getSimpleName();

    private CallBack callBack;

    public SearchPostsAdapter(final BaseActivity activity) {
        super(activity);
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_TEXT){
            return new PostTextViewHolder(inflater.inflate(R.layout.text_post_list_item, parent, false),
                    createTextOnClickListener(), activity);
        }
        else if (viewType == TYPE_VIDEO){
            return new PostVideoViewHolder(inflater.inflate(R.layout.video_post_list_item, parent, false),
                    createVideoOnClickListener(), activity);
        }
        else if (viewType == TYPE_IMAGE){
            return new PostImageViewHolder(inflater.inflate(R.layout.image_post_list_item, parent, false),
                    createImageOnClickListener(), activity);
        }
        else {
            return new LoadViewHolder(inflater.inflate(R.layout.loading_view, parent, false));
        }
    }

    private PostTextViewHolder.OnClickListener createTextOnClickListener() {
        return new PostTextViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    selectedPostPosition = position;
                    callBack.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                if (callBack != null && callBack.enableClick()) {
                    Post post = getItemByPosition(position);
                    likeController.handleLikeClickAction(activity, post);
                }
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    callBack.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };
    }

    private PostImageViewHolder.OnClickListener createImageOnClickListener() {
        return new PostImageViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    selectedPostPosition = position;
                    callBack.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                if (callBack != null && callBack.enableClick()) {
                    Post post = getItemByPosition(position);
                    likeController.handleLikeClickAction(activity, post);
                }
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    callBack.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };
    }

    private PostVideoViewHolder.OnClickListener createVideoOnClickListener() {
        return new PostVideoViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    selectedPostPosition = position;
                    callBack.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                if (callBack != null && callBack.enableClick()) {
                    Post post = getItemByPosition(position);
                    likeController.handleLikeClickAction(activity, post);
                }
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    callBack.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.i("size","" + postList.size());
        Log.i("position","" + position);

        if (getItemViewType(position) == TYPE_TEXT) {
            ((PostTextViewHolder) holder).bindData(postList.get(position));
        }

        if (getItemViewType(position) == TYPE_IMAGE) {
            ((PostImageViewHolder) holder).bindData(postList.get(position));
        }

        if (getItemViewType(position) == TYPE_VIDEO) {
            if (postList.get(position).getImageTitle() != null){
                StorageReference videoRef = PostManager.getInstance(activity.getApplicationContext()).getOriginImageStorageRef(postList.get(position).getImageTitle());
                videoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.i("mediauri","" + uri);
                        ((PostVideoViewHolder) holder).bind(uri);
                        ((PostVideoViewHolder) holder).bindData(postList.get(position));
                    }
                });
            }
        }
    }

    public void setList(List<Post> list) {
        cleanSelectedPostInformation();
        postList.clear();
        postList.addAll(list);
        notifyDataSetChanged();
    }

    public void removeSelectedPost() {
        if (selectedPostPosition != RecyclerView.NO_POSITION) {
            postList.remove(selectedPostPosition);
            notifyItemRemoved(selectedPostPosition);
        }
    }

    public interface CallBack {
        void onItemClick(Post post, View view);
        void onAuthorClick(String authorId, View view);
        boolean enableClick();
    }
}
