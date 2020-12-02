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
import com.architectica.socialcomponents.adapters.holders.ProjectsImageViewHolder;
import com.architectica.socialcomponents.adapters.holders.ProjectsTextViewHolder;
import com.architectica.socialcomponents.adapters.holders.ProjectsVideoViewHolder;
import com.architectica.socialcomponents.adapters.holders.ProjectsViewHolder;
import com.architectica.socialcomponents.controllers.LikeController;
import com.architectica.socialcomponents.enums.ItemType;
import com.architectica.socialcomponents.main.main.MainActivity;
import com.architectica.socialcomponents.managers.PostManager;
import com.architectica.socialcomponents.managers.listeners.OnPostListChangedListener;
import com.architectica.socialcomponents.model.Post;
import com.architectica.socialcomponents.model.PostListResult;
import com.architectica.socialcomponents.utils.PreferencesUtil;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ProjectsAdapter extends BaseProjectsAdapter {
    public static final String TAG = PostsAdapter.class.getSimpleName();

    private Callback callback;
    private boolean isLoading = false;
    private boolean isMoreDataAvailable = true;
    private long lastLoadedItemCreatedDate;
    private SwipeRefreshLayout swipeContainer;
    private MainActivity mainActivity;

    public ProjectsAdapter(final MainActivity activity, SwipeRefreshLayout swipeContainer) {
        super(activity);
        this.mainActivity = activity;
        this.swipeContainer = swipeContainer;
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
            loadProjectsFirstPage();
            cleanSelectedPostInformation();
        } else {
            swipeContainer.setRefreshing(false);
            mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_TEXT){
            return new ProjectsTextViewHolder(inflater.inflate(R.layout.text_post_list_item, parent, false),
                    createTextOnClickListener(), activity);
        }
        else if (viewType == TYPE_VIDEO){
            return new ProjectsVideoViewHolder(inflater.inflate(R.layout.video_post_list_item, parent, false),
                    createVideoOnClickListener(), activity);
        }
        else if (viewType == TYPE_IMAGE){
            return new ProjectsImageViewHolder(inflater.inflate(R.layout.image_post_list_item, parent, false),
                    createImageOnClickListener(), activity);
        }
        else {
            return new LoadViewHolder(inflater.inflate(R.layout.loading_view, parent, false));
        }
    }

    private ProjectsTextViewHolder.OnClickListener createTextOnClickListener(){

        return new ProjectsTextViewHolder.OnClickListener() {
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

    private ProjectsImageViewHolder.OnClickListener createImageOnClickListener(){

        return new ProjectsImageViewHolder.OnClickListener() {
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

    private ProjectsVideoViewHolder.OnClickListener createVideoOnClickListener(){

        return new ProjectsVideoViewHolder.OnClickListener() {
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
                        //postList.add(new Post(ItemType.LOAD));
                        //notifyItemInserted(postList.size());
                        loadProjectsNextPage(lastLoadedItemCreatedDate - 1);
                    } else {
                        mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });


        }

        if (getItemViewType(position) == TYPE_TEXT) {
            ((ProjectsTextViewHolder) holder).bindData(postList.get(position));
        }

        if (getItemViewType(position) == TYPE_IMAGE) {
            ((ProjectsImageViewHolder) holder).bindData(postList.get(position));
        }

        if (getItemViewType(position) == TYPE_VIDEO) {
            StorageReference videoRef = PostManager.getInstance(mainActivity.getApplicationContext()).getOriginImageStorageRef(postList.get(position).getImageTitle());
            videoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Log.i("mediauri","" + uri);
                    ((ProjectsVideoViewHolder) holder).bind(uri);
                    ((ProjectsVideoViewHolder) holder).bindData(postList.get(position));
                }
            });
        }


    }

    private void addList(List<Post> list) {
        this.postList.addAll(list);
        notifyDataSetChanged();
        isLoading = false;
    }

    public void loadProjectsFirstPage(){

        loadProjectsNextPage(0);
        PostManager.getInstance(mainActivity.getApplicationContext()).clearNewPostsCounter();

    }

    private void loadProjectsNextPage(final long nextItemCreatedDate) {

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

        if (FirebaseAuth.getInstance().getCurrentUser() != null){

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference("profiles/" + userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if ("RiftAdmin".equals(dataSnapshot.child("username").getValue(String.class))){

                        PostManager.getInstance(activity).getAllProjectsList(onPostsDataChangedListener, nextItemCreatedDate);

                    }
                    else {

                        PostManager.getInstance(activity).getProjectsList(onPostsDataChangedListener, nextItemCreatedDate);

                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
        else{

            PostManager.getInstance(activity).getProjectsList(onPostsDataChangedListener, nextItemCreatedDate);

        }

    }


    private void hideProgress() {
        if (!postList.isEmpty() && getItemViewType(postList.size() - 1) == ItemType.LOAD.getTypeCode()) {
            postList.remove(postList.size() - 1);
            notifyItemRemoved(postList.size() - 1);
        }
    }

    public void removeSelectedPost() {
        postList.remove(selectedPostPosition);
        notifyItemRemoved(selectedPostPosition);
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

