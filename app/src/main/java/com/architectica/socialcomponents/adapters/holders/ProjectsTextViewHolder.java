package com.architectica.socialcomponents.adapters.holders;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.architectica.socialcomponents.Constants;
import com.architectica.socialcomponents.R;
import com.architectica.socialcomponents.controllers.LikeController;
import com.architectica.socialcomponents.main.Hashtags.HashtagProjectsActivity;
import com.architectica.socialcomponents.main.base.BaseActivity;
import com.architectica.socialcomponents.main.interactors.PostInteractor;
import com.architectica.socialcomponents.managers.PostManager;
import com.architectica.socialcomponents.managers.ProfileManager;
import com.architectica.socialcomponents.managers.listeners.OnObjectChangedListener;
import com.architectica.socialcomponents.managers.listeners.OnObjectChangedListenerSimple;
import com.architectica.socialcomponents.managers.listeners.OnObjectExistListener;
import com.architectica.socialcomponents.model.Like;
import com.architectica.socialcomponents.model.Post;
import com.architectica.socialcomponents.model.Profile;
import com.architectica.socialcomponents.utils.FormatterUtil;
import com.architectica.socialcomponents.utils.GlideApp;
import com.architectica.socialcomponents.utils.ProjectImageUtil;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hendraanggrian.appcompat.widget.SocialTextView;
import com.hendraanggrian.appcompat.widget.SocialView;

import androidx.recyclerview.widget.RecyclerView;

public class ProjectsTextViewHolder extends RecyclerView.ViewHolder {
    public static final String TAG = PostViewHolder.class.getSimpleName();

    private LinearLayout countersLayout;
    protected Context context;
    private TextView titleTextView;
    private SocialTextView detailsTextView;
    private TextView likeCounterTextView,userName,userSkill;
    private ImageView likesImageView;
    private TextView commentsCountTextView;
    private TextView watcherCounterTextView;
    private TextView dateTextView;
    private ImageView authorImageView;
    private ViewGroup likeViewGroup;

    private ProfileManager profileManager;
    protected PostManager postManager;
    protected PostInteractor postInteractor;

    private LikeController likeController;
    private BaseActivity baseActivity;

    public ProjectsTextViewHolder(View view, final OnClickListener onClickListener, BaseActivity activity) {
        this(view, onClickListener, activity, true);
    }

    public ProjectsTextViewHolder(View view, final OnClickListener onClickListener, BaseActivity activity, boolean isAuthorNeeded) {
        super(view);
        this.context = view.getContext();
        this.baseActivity = activity;

        userName=view.findViewById(R.id.user);
        userSkill=view.findViewById(R.id.userInfo);
        likeCounterTextView = view.findViewById(R.id.likeCounterTextView);
        likesImageView = view.findViewById(R.id.likesImageView);
        commentsCountTextView = view.findViewById(R.id.commentsCountTextView);
        watcherCounterTextView = view.findViewById(R.id.watcherCounterTextView);
        dateTextView = view.findViewById(R.id.dateTextView);
        titleTextView = view.findViewById(R.id.titleTextView);
        detailsTextView = view.findViewById(R.id.detailsTextView);
        countersLayout = view.findViewById(R.id.countersContainer);

        countersLayout.setVisibility(View.VISIBLE);

        detailsTextView.setHashtagColor(Color.RED);
        detailsTextView.setOnHashtagClickListener(new SocialView.OnClickListener() {
            @Override
            public void onClick(SocialView view, CharSequence text) {

                //Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

                Log.i("text","" + text);

                Intent intent = new Intent(context, HashtagProjectsActivity.class);
                intent.putExtra("hashtag","" + text);
                context.startActivity(intent);

            }
        });

        authorImageView = view.findViewById(R.id.authorImageView);
        likeViewGroup = view.findViewById(R.id.likesContainer);

        authorImageView.setVisibility(isAuthorNeeded ? View.VISIBLE : View.GONE);

        profileManager = ProfileManager.getInstance(context.getApplicationContext());
        postManager = PostManager.getInstance(context.getApplicationContext());
        postInteractor = PostInteractor.getInstance(context.getApplicationContext());
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        view.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                onClickListener.onItemClick(getAdapterPosition(), v);
            }
        });

        likeViewGroup.setOnClickListener(view1 -> {
            int position = getAdapterPosition();
            if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                onClickListener.onLikeClick(likeController, position);
            }
        });

        authorImageView.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                onClickListener.onAuthorClick(getAdapterPosition(), v);
            }
        });

    }

    public void bindData(Post post) {

        likeController = new LikeController(context, post, likeCounterTextView, likesImageView, true);

        String title = removeNewLinesDividers(post.getTitle());
        titleTextView.setText(title);
        String description = removeNewLinesDividers(post.getDescription());
        detailsTextView.setText(description);
        likeCounterTextView.setText(String.valueOf(post.getLikesCount()));
        commentsCountTextView.setText(String.valueOf(post.getCommentsCount()));
        watcherCounterTextView.setText(String.valueOf(post.getWatchersCount()));

        CharSequence date = FormatterUtil.getRelativeTimeSpanStringShort(context, post.getCreatedDate());
        dateTextView.setText(date);

        if (post.getAuthorId() != null) {
            ProfileManager profileManager=new ProfileManager(context);
            profileManager.getProfileDetail(post.getAuthorId(),userName,userSkill);
            profileManager.getProfileSingleValue(post.getAuthorId(), createProfileChangeListener(authorImageView));
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            postInteractor.hasCurrentUserProjectLikeSingleValue(post.getId(), firebaseUser.getUid(), createOnLikeObjectExistListener());
        }
    }

    private String removeNewLinesDividers(String text) {
        int decoratedTextLength = text.length() < Constants.Post.MAX_TEXT_LENGTH_IN_LIST ?
                text.length() : Constants.Post.MAX_TEXT_LENGTH_IN_LIST;
        return text.substring(0, decoratedTextLength).replaceAll("\n", " ").trim();
    }

    private OnObjectChangedListener<Profile> createProfileChangeListener(final ImageView authorImageView) {
        return new OnObjectChangedListenerSimple<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (obj != null && obj.getPhotoUrl() != null) {
                    if (!baseActivity.isFinishing() && !baseActivity.isDestroyed()) {
                        ProjectImageUtil.loadImage(GlideApp.with(baseActivity), obj.getPhotoUrl(), authorImageView);
                    }
                }
            }
        };
    }

    private OnObjectExistListener<Like> createOnLikeObjectExistListener() {
        return exist -> likeController.initLike(exist);
    }

    public interface OnClickListener {
        void onItemClick(int position, View view);

        void onLikeClick(LikeController likeController, int position);

        void onAuthorClick(int position, View view);
    }
}

