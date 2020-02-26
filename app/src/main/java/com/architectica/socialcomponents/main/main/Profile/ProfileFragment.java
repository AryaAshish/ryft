package com.architectica.socialcomponents.main.main.Profile;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.architectica.socialcomponents.R;
import com.architectica.socialcomponents.adapters.PostsByUserAdapter;
import com.architectica.socialcomponents.dialogs.UnfollowConfirmationDialog;
import com.architectica.socialcomponents.enums.FollowState;
import com.architectica.socialcomponents.main.base.BaseActivity;
import com.architectica.socialcomponents.main.base.BaseFragment;
import com.architectica.socialcomponents.main.editProfile.EditProfileActivity;
import com.architectica.socialcomponents.main.login.LoginActivity;
import com.architectica.socialcomponents.main.main.MainActivity;
import com.architectica.socialcomponents.main.post.createPost.CreatePostActivity;
import com.architectica.socialcomponents.main.postDetails.PostDetailsActivity;
import com.architectica.socialcomponents.main.usersList.UsersListActivity;
import com.architectica.socialcomponents.main.usersList.UsersListType;
import com.architectica.socialcomponents.managers.FollowManager;
import com.architectica.socialcomponents.managers.ProfileManager;
import com.architectica.socialcomponents.model.Post;
import com.architectica.socialcomponents.model.Profile;
import com.architectica.socialcomponents.utils.GlideApp;
import com.architectica.socialcomponents.utils.ImageUtil;
import com.architectica.socialcomponents.utils.LogoutHelper;
import com.architectica.socialcomponents.views.FollowButton;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends BaseFragment<ProfileView,ProfilePresenter> implements ProfileView, UnfollowConfirmationDialog.Callback{

    //private static final String TAG = ProfileActivity.class.getSimpleName();
    public static final int CREATE_POST_FROM_PROFILE_REQUEST = 22;
    public static final String USER_ID_EXTRA_KEY = "ProfileActivity.USER_ID_EXTRA_KEY";

    // UI references.
    private TextView nameEditText;
    private TextView bioTextView;
    private TextView statusTextView;
    private TextView skillTextView;
    private ImageView imageView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView postsCounterTextView;
    private ProgressBar postsProgressBar;

    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private String currentUserId;
    private String userID;

    private PostsByUserAdapter postsAdapter;
    private TextView likesCountersTextView;
    private TextView followersCounterTextView;
    private TextView followingsCounterTextView,selfEdit;
    private FollowButton followButton;

    LinearLayout linearLayout;

    public ProfileFragment(){

        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.profile_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        switch (item.getItemId()) {
            case R.id.editProfile:
                presenter.onEditProfileClick();
                return true;
            case R.id.signOut:
                if (FirebaseAuth.getInstance().getCurrentUser() != null){
                    FirebaseAuth.getInstance().signOut();
                    LogoutHelper.signOut(mGoogleApiClient, getActivity());
                    startMainActivity();
                }

                return true;
            case R.id.createPost:
                presenter.onCreatePostClick();
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public static Fragment newInstance() {

        Fragment frag = new ProfileFragment();

        Bundle args = new Bundle();

        frag.setArguments(args);

        return frag;

    }

    @Override
    public ProfilePresenter createPresenter() {
        if (presenter == null) {
            return new ProfilePresenter(getActivity());
        }
        return presenter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //userID = getIntent().getStringExtra(USER_ID_EXTRA_KEY);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userID = firebaseUser.getUid();
        }

        // Set up the login form.
        progressBar = view.findViewById(R.id.progressBar);
        imageView = view.findViewById(R.id.imageView);
        nameEditText = view.findViewById(R.id.nameEditText);
        bioTextView = view.findViewById(R.id.bio);
        statusTextView = view.findViewById(R.id.status);
        skillTextView = view.findViewById(R.id.skill);
        postsCounterTextView = view.findViewById(R.id.postsCounterTextView);
        likesCountersTextView = view.findViewById(R.id.likesCountersTextView);
        followersCounterTextView = view.findViewById(R.id.followersCounterTextView);
        followingsCounterTextView = view.findViewById(R.id.followingsCounterTextView);
        postsProgressBar = view.findViewById(R.id.postsProgressBar);
        followButton = view.findViewById(R.id.followButton);

        linearLayout=view.findViewById(R.id.msglay);
        selfEdit=view.findViewById(R.id.selfedit);

        linearLayout.setVisibility(View.GONE);
        selfEdit.setVisibility(View.VISIBLE);

        selfEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onEditProfileClick();
            }
        });

        initListeners();

        presenter.checkFollowState(userID);

        loadPostsList(view);
        getActivity().supportPostponeEnterTransition();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.loadProfile(getActivity(), userID);
        presenter.getFollowersCount(userID);
        presenter.getFollowingsCount(userID);

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        FollowManager.getInstance(getActivity()).closeListeners(getActivity());
        ProfileManager.getInstance(getActivity()).closeListeners(getActivity());

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(getActivity());
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CreatePostActivity.CREATE_NEW_POST_REQUEST:
                    postsAdapter.loadPosts();
                    showSnackBar(R.string.message_post_was_created);
                    getActivity().setResult(RESULT_OK);
                    break;

                case PostDetailsActivity.UPDATE_POST_REQUEST:
                    presenter.checkPostChanges(data);
                    break;

                case LoginActivity.LOGIN_REQUEST_CODE:
                    presenter.checkFollowState(userID);
                    break;
            }
        }
    }

    private void initListeners() {
        followButton.setOnClickListener(v -> {
            presenter.onFollowButtonClick(followButton.getState(), userID);
        });

        followingsCounterTextView.setOnClickListener(v -> {
            startUsersListActivity(UsersListType.FOLLOWINGS);
        });

        followersCounterTextView.setOnClickListener(v -> {
            startUsersListActivity(UsersListType.FOLLOWERS);
        });


    }

    private void onRefreshAction() {
        postsAdapter.loadPosts();
    }

    private void startUsersListActivity(int usersListType) {
        Intent intent = new Intent(getActivity(), UsersListActivity.class);
        intent.putExtra(UsersListActivity.USER_ID_EXTRA_KEY, userID);
        intent.putExtra(UsersListActivity.USER_LIST_TYPE, usersListType);
        startActivity(intent);
    }

    private void loadPostsList(View view) {
        if (recyclerView == null) {

            recyclerView = view.findViewById(R.id.recycler_view);
            postsAdapter = new PostsByUserAdapter((BaseActivity) getActivity(), userID);
            postsAdapter.setCallBack(new PostsByUserAdapter.CallBack() {
                @Override
                public void onItemClick(final Post post, final View view) {
                    presenter.onPostClick(post, view);
                }

                @Override
                public void onPostsListChanged(int postsCount) {
                    presenter.onPostListChanged(postsCount);
                }

                @Override
                public void onPostLoadingCanceled() {
                    hideLoadingPostsProgress();
                }
            });

            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
            recyclerView.setAdapter(postsAdapter);
            postsAdapter.loadPosts();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void openPostDetailsActivity(Post post, View v) {
        Intent intent = new Intent(getActivity(), PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());
        intent.putExtra(PostDetailsActivity.AUTHOR_ANIMATION_NEEDED_EXTRA_KEY, true);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            View imageView = v.findViewById(R.id.postImageView);

            if (imageView.getVisibility() != View.GONE){

                ActivityOptions options = ActivityOptions.
                        makeSceneTransitionAnimation(getActivity(),
                                new android.util.Pair<>(imageView, getString(R.string.post_image_transition_name))
                        );
                startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST, options.toBundle());

            }
            else {

                startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);

            }

        } else {
            startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
        }

    }

    private void scheduleStartPostponedTransition(final ImageView imageView) {
        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                getActivity().supportStartPostponedEnterTransition();
                return true;
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void startEditProfileActivity() {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivity(intent);
    }

    @Override
    public void openCreatePostActivity() {
        Intent intent = new Intent(getActivity(), CreatePostActivity.class);
        startActivityForResult(intent, CreatePostActivity.CREATE_NEW_POST_REQUEST);
    }

    @Override
    public void setProfileName(String username) {
        nameEditText.setText(username);
    }

    @Override
    public void setBio(String bio) {
        bioTextView.setText(bio);
    }

    @Override
    public void setStatus(String status) {
        if ("Not Hired".equals(status)){

            statusTextView.setText(status);
            statusTextView.setTextColor(getResources().getColor(R.color.red));

        }
        else if ("Hired".equals(status)){

            statusTextView.setText(status);
            statusTextView.setTextColor(getResources().getColor(R.color.green));

        }
    }

    @Override
    public void setSkill(String skill) {

        skillTextView.setText(skill);

    }

    @Override
    public void setProfilePhoto(String photoUrl) {
        ImageUtil.loadImage(GlideApp.with(this), photoUrl, imageView, new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                scheduleStartPostponedTransition(imageView);
                progressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                scheduleStartPostponedTransition(imageView);
                progressBar.setVisibility(View.GONE);
                return false;
            }
        });
    }

    @Override
    public void setDefaultProfilePhoto() {
        progressBar.setVisibility(View.GONE);
        imageView.setImageResource(R.drawable.ic_stub);
    }

    @Override
    public void updateLikesCounter(Spannable text) {
        likesCountersTextView.setText(text);
    }

    @Override
    public void hideLoadingPostsProgress() {

        if (postsProgressBar.getVisibility() != View.GONE) {
            postsProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void showLikeCounter(boolean show) {
        likesCountersTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void updatePostsCounter(Spannable text) {
        postsCounterTextView.setText(text);
    }

    @Override
    public void showPostCounter(boolean show) {
        postsCounterTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPostRemoved() {
        postsAdapter.removeSelectedPost();
    }

    @Override
    public void onPostUpdated() {
        postsAdapter.updateSelectedPost();
    }

    @Override
    public void showUnfollowConfirmation(@NonNull Profile profile) {
        UnfollowConfirmationDialog unfollowConfirmationDialog = new UnfollowConfirmationDialog();
        Bundle args = new Bundle();
        args.putSerializable(UnfollowConfirmationDialog.PROFILE, profile);
        unfollowConfirmationDialog.setArguments(args);
        unfollowConfirmationDialog.show(getActivity().getFragmentManager(),UnfollowConfirmationDialog.TAG);
    }

    @Override
    public void updateFollowButtonState(FollowState followState) {
        followButton.setState(followState);
    }

    @Override
    public void updateFollowersCount(int count) {
        followersCounterTextView.setVisibility(View.VISIBLE);
        String followersLabel = getResources().getQuantityString(R.plurals.followers_counter_format, count, count);
        followersCounterTextView.setText(presenter.buildCounterSpannable(followersLabel, count));
    }

    @Override
    public void updateFollowingsCount(int count) {
        followingsCounterTextView.setVisibility(View.VISIBLE);
        String followingsLabel = getResources().getQuantityString(R.plurals.followings_counter_format, count, count);
        followingsCounterTextView.setText(presenter.buildCounterSpannable(followingsLabel, count));
    }

    @Override
    public void setFollowStateChangeResultOk() {
        getActivity().setResult(UsersListActivity.UPDATE_FOLLOWING_STATE_RESULT_OK);
    }

    @Override
    public void onUnfollowButtonClicked() {
        presenter.unfollowUser(userID);
    }
}
