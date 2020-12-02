package com.architectica.socialcomponents.adapters;

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
import com.architectica.socialcomponents.enums.ItemType;
import com.architectica.socialcomponents.main.Hashtags.HashtagPostsActivity;
import com.architectica.socialcomponents.managers.PostManager;
import com.architectica.socialcomponents.managers.listeners.OnPostListChangedListener;
import com.architectica.socialcomponents.model.Post;
import com.architectica.socialcomponents.model.PostListResult;
import com.architectica.socialcomponents.utils.PreferencesUtil;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class HashtagPostsAdapter extends BasePostsAdapter{

    public static final String TAG = PostsAdapter.class.getSimpleName();

    private HashtagPostsAdapter.Callback callback;
    private boolean isLoading = false;
    private boolean isMoreDataAvailable = true;
    private long lastLoadedItemCreatedDate;
    private SwipeRefreshLayout swipeContainer;
    private HashtagPostsActivity mainActivity;
    private String hashtag;

    public HashtagPostsAdapter(String hashtag,final HashtagPostsActivity activity, SwipeRefreshLayout swipeContainer) {
        super(activity);
        this.mainActivity = activity;
        this.swipeContainer = swipeContainer;
        this.hashtag = hashtag;
        initRefreshLayout();
        setHasStableIds(true);
    }

    private void initRefreshLayout() {
        if (swipeContainer != null) {
            this.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    onRefreshAction();
                }
            });
        }
    }

    private void onRefreshAction() {
        if (activity.hasInternetConnection()) {
            loadHashtagFirstPage();
            cleanSelectedPostInformation();
        } else {
            swipeContainer.setRefreshing(false);
            mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    public void setCallback(HashtagPostsAdapter.Callback callback) {
        this.callback = callback;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
                if (callback != null) {
                    selectedPostPosition = position;
                    callback.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                Post post = getItemByPosition(position);
                likeController.handleLikeClickAction(activity, post);
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callback != null) {
                    callback.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };
    }

    private PostImageViewHolder.OnClickListener createImageOnClickListener() {
        return new PostImageViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callback != null) {
                    selectedPostPosition = position;
                    callback.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                Post post = getItemByPosition(position);
                likeController.handleLikeClickAction(activity, post);
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callback != null) {
                    callback.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };
    }

    private PostVideoViewHolder.OnClickListener createVideoOnClickListener(){

        return new PostVideoViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callback != null) {
                    selectedPostPosition = position;
                    callback.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                Post post = getItemByPosition(position);
                likeController.handleLikeClickAction(activity, post);
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callback != null) {
                    callback.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position >= getItemCount() - 1 && isMoreDataAvailable && !isLoading) {
            android.os.Handler mHandler = activity.getWindow().getDecorView().getHandler();
            mHandler.post(new Runnable() {
                public void run() {
                    //change adapter contents
                    if (activity.hasInternetConnection()) {
                        isLoading = true;
                        postList.add(new Post(ItemType.LOAD));
                        notifyItemInserted(postList.size());
                        loadHashtagNextPage(lastLoadedItemCreatedDate - 1);
                    } else {
                        mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });


        }

        if (getItemViewType(position) == TYPE_TEXT) {
            ((PostTextViewHolder) holder).bindData(postList.get(position));
        }

        if (getItemViewType(position) == TYPE_IMAGE) {
            ((PostImageViewHolder) holder).bindData(postList.get(position));
        }

        if (getItemViewType(position) == TYPE_VIDEO) {
            StorageReference videoRef = PostManager.getInstance(mainActivity.getApplicationContext()).getOriginImageStorageRef(postList.get(position).getImageTitle());
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

    private void addList(List<Post> list) {

        Log.i("result",list.toString());

        this.postList.addAll(list);
        notifyDataSetChanged();
        isLoading = false;
    }

    public void loadHashtagFirstPage(){

        loadHashtagNextPage(0);
        PostManager.getInstance(mainActivity.getApplicationContext()).clearNewPostsCounter();

    }

    private void loadHashtagNextPage(final long nextItemCreatedDate) {

        if (!PreferencesUtil.isPostWasLoadedAtLeastOnce(mainActivity) && !activity.hasInternetConnection()) {
            mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
            hideProgress();
            callback.onListLoadingFinished();
            return;
        }

        OnPostListChangedListener<Post> onPostsDataChangedListener = new OnPostListChangedListener<Post>() {
            @Override
            public void onListChanged(PostListResult result) {
                lastLoadedItemCreatedDate = result.getLastItemCreatedDate();
                isMoreDataAvailable = result.isMoreDataAvailable();
                List<Post> list = result.getPosts();

                Log.i("list",list.toString());

                if (nextItemCreatedDate == 0) {
                    postList.clear();
                    notifyDataSetChanged();
                    swipeContainer.setRefreshing(false);
                }

                hideProgress();

                if (!list.isEmpty()) {
                    addList(list);

                    if (!PreferencesUtil.isPostWasLoadedAtLeastOnce(mainActivity)) {
                        PreferencesUtil.setPostWasLoadedAtLeastOnce(mainActivity, true);
                    }
                } else {
                    isLoading = false;
                }

                callback.onListLoadingFinished();
            }

            @Override
            public void onCanceled(String message) {
                callback.onCanceled(message);
            }
        };

        PostManager.getInstance(activity).getHashtagPostsList(hashtag,onPostsDataChangedListener, nextItemCreatedDate);

    }


    private void hideProgress() {
        if (!postList.isEmpty() && getItemViewType(postList.size() - 1) == ItemType.LOAD.getTypeCode()) {
            postList.remove(postList.size() - 1);
            notifyItemRemoved(postList.size() - 1);
        }
    }

    @Override
    public long getItemId(int position) {
        return getItemByPosition(position).getId().hashCode();
    }

    public interface Callback {
        void onItemClick(Post post, View view);
        void onListLoadingFinished();
        void onAuthorClick(String authorId, View view);
        void onCanceled(String message);
    }

}
