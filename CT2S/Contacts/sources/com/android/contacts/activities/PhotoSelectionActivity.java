package com.android.contacts.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.detail.PhotoSelectionHandler;
import com.android.contacts.util.SchedulingUtils;

public class PhotoSelectionActivity extends Activity {
    private AnimatorListenerAdapter mAnimationListener;
    private boolean mAnimationPending;
    private View mBackdrop;
    private boolean mCloseActivityWhenCameBackFromSubActivity;
    private Uri mCurrentPhotoUri;
    private boolean mExpandPhoto;
    private int mExpandedPhotoSize;
    private int mHeightOffset;
    private boolean mIsDirectoryContact;
    private boolean mIsProfile;
    Rect mOriginalPos = new Rect();
    private PendingPhotoResult mPendingPhotoResult;
    private ObjectAnimator mPhotoAnimator;
    private FrameLayout.LayoutParams mPhotoEndParams;
    private PhotoHandler mPhotoHandler;
    private FrameLayout.LayoutParams mPhotoStartParams;
    private Uri mPhotoUri;
    private ImageView mPhotoView;
    private Rect mSourceBounds;
    private RawContactDeltaList mState;
    private boolean mSubActivityInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photoselection_activity);
        if (savedInstanceState != null) {
            this.mCurrentPhotoUri = (Uri) savedInstanceState.getParcelable("currentphotouri");
            this.mSubActivityInProgress = savedInstanceState.getBoolean("subinprogress");
        }
        Intent intent = getIntent();
        this.mPhotoUri = (Uri) intent.getParcelableExtra("photo_uri");
        this.mState = (RawContactDeltaList) intent.getParcelableExtra("entity_delta_list");
        this.mIsProfile = intent.getBooleanExtra("is_profile", false);
        this.mIsDirectoryContact = intent.getBooleanExtra("is_directory_contact", false);
        this.mExpandPhoto = intent.getBooleanExtra("expand_photo", false);
        this.mExpandedPhotoSize = getResources().getDimensionPixelSize(R.dimen.detail_contact_photo_expanded_size);
        this.mHeightOffset = getResources().getDimensionPixelOffset(R.dimen.expanded_photo_height_offset);
        this.mBackdrop = findViewById(R.id.backdrop);
        this.mPhotoView = (ImageView) findViewById(R.id.photo);
        this.mSourceBounds = intent.getSourceBounds();
        animateInBackground();
        this.mBackdrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoSelectionActivity.this.finish();
            }
        });
        SchedulingUtils.doAfterLayout(this.mBackdrop, new Runnable() {
            @Override
            public void run() {
                PhotoSelectionActivity.this.displayPhoto();
            }
        });
    }

    private int getAdjustedExpandedPhotoSize(View enclosingView, int heightOffset) {
        Rect bounds = new Rect();
        enclosingView.getDrawingRect(bounds);
        int boundsWidth = bounds.width();
        int boundsHeight = bounds.height() - heightOffset;
        float alpha = Math.min(boundsHeight / this.mExpandedPhotoSize, boundsWidth / this.mExpandedPhotoSize);
        return alpha < 1.0f ? (int) (this.mExpandedPhotoSize * alpha) : this.mExpandedPhotoSize;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!this.mSubActivityInProgress) {
            finishImmediatelyWithNoAnimation();
        } else {
            this.mCloseActivityWhenCameBackFromSubActivity = true;
        }
    }

    @Override
    public void finish() {
        if (!this.mSubActivityInProgress) {
            closePhotoAndFinish();
        } else {
            finishImmediatelyWithNoAnimation();
        }
    }

    private void finishImmediatelyWithNoAnimation() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mPhotoAnimator != null) {
            this.mPhotoAnimator.cancel();
            this.mPhotoAnimator = null;
        }
        if (this.mPhotoHandler != null) {
            this.mPhotoHandler.destroy();
            this.mPhotoHandler = null;
        }
    }

    private void displayPhoto() {
        int[] pos = new int[2];
        this.mBackdrop.getLocationOnScreen(pos);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(this.mSourceBounds.width(), this.mSourceBounds.height());
        this.mOriginalPos.left = this.mSourceBounds.left - pos[0];
        this.mOriginalPos.top = this.mSourceBounds.top - pos[1];
        this.mOriginalPos.right = this.mOriginalPos.left + this.mSourceBounds.width();
        this.mOriginalPos.bottom = this.mOriginalPos.top + this.mSourceBounds.height();
        layoutParams.setMargins(this.mOriginalPos.left, this.mOriginalPos.top, this.mOriginalPos.right, this.mOriginalPos.bottom);
        this.mPhotoStartParams = layoutParams;
        this.mPhotoView.setLayoutParams(layoutParams);
        this.mPhotoView.requestLayout();
        int photoWidth = getPhotoEndParams().width;
        if (this.mPhotoUri != null) {
            ContactPhotoManager.getInstance(this).loadPhoto(this.mPhotoView, this.mPhotoUri, photoWidth, false, false, null);
        } else {
            this.mPhotoView.setImageDrawable(null);
        }
        this.mPhotoView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (PhotoSelectionActivity.this.mAnimationPending) {
                    PhotoSelectionActivity.this.mAnimationPending = false;
                    PropertyValuesHolder pvhLeft = PropertyValuesHolder.ofInt("left", PhotoSelectionActivity.this.mOriginalPos.left, left);
                    PropertyValuesHolder pvhTop = PropertyValuesHolder.ofInt("top", PhotoSelectionActivity.this.mOriginalPos.top, top);
                    PropertyValuesHolder pvhRight = PropertyValuesHolder.ofInt("right", PhotoSelectionActivity.this.mOriginalPos.right, right);
                    PropertyValuesHolder pvhBottom = PropertyValuesHolder.ofInt("bottom", PhotoSelectionActivity.this.mOriginalPos.bottom, bottom);
                    ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(PhotoSelectionActivity.this.mPhotoView, pvhLeft, pvhTop, pvhRight, pvhBottom).setDuration(100L);
                    if (PhotoSelectionActivity.this.mAnimationListener != null) {
                        anim.addListener(PhotoSelectionActivity.this.mAnimationListener);
                    }
                    anim.start();
                }
            }
        });
        attachPhotoHandler();
    }

    private FrameLayout.LayoutParams getPhotoEndParams() {
        if (this.mPhotoEndParams == null) {
            this.mPhotoEndParams = new FrameLayout.LayoutParams(this.mPhotoStartParams);
            if (this.mExpandPhoto) {
                int adjustedPhotoSize = getAdjustedExpandedPhotoSize(this.mBackdrop, this.mHeightOffset);
                int widthDelta = adjustedPhotoSize - this.mPhotoStartParams.width;
                int heightDelta = adjustedPhotoSize - this.mPhotoStartParams.height;
                if (widthDelta >= 1 || heightDelta >= 1) {
                    this.mPhotoEndParams.width = adjustedPhotoSize;
                    this.mPhotoEndParams.height = adjustedPhotoSize;
                    this.mPhotoEndParams.topMargin = Math.max(this.mPhotoStartParams.topMargin - heightDelta, 0);
                    this.mPhotoEndParams.leftMargin = Math.max(this.mPhotoStartParams.leftMargin - widthDelta, 0);
                    this.mPhotoEndParams.bottomMargin = 0;
                    this.mPhotoEndParams.rightMargin = 0;
                }
            }
        }
        return this.mPhotoEndParams;
    }

    private void animatePhotoOpen() {
        this.mAnimationListener = new AnimatorListenerAdapter() {
            private void capturePhotoPos() {
                PhotoSelectionActivity.this.mPhotoView.requestLayout();
                PhotoSelectionActivity.this.mOriginalPos.left = PhotoSelectionActivity.this.mPhotoView.getLeft();
                PhotoSelectionActivity.this.mOriginalPos.top = PhotoSelectionActivity.this.mPhotoView.getTop();
                PhotoSelectionActivity.this.mOriginalPos.right = PhotoSelectionActivity.this.mPhotoView.getRight();
                PhotoSelectionActivity.this.mOriginalPos.bottom = PhotoSelectionActivity.this.mPhotoView.getBottom();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                capturePhotoPos();
                if (PhotoSelectionActivity.this.mPhotoHandler != null) {
                    PhotoSelectionActivity.this.mPhotoHandler.onClick(PhotoSelectionActivity.this.mPhotoView);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                capturePhotoPos();
            }
        };
        animatePhoto(getPhotoEndParams());
    }

    private void closePhotoAndFinish() {
        this.mAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(PhotoSelectionActivity.this.mPhotoView, "alpha", 0.0f).setDuration(50L);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation2) {
                        PhotoSelectionActivity.this.finishImmediatelyWithNoAnimation();
                    }
                });
                anim.start();
            }
        };
        animatePhoto(this.mPhotoStartParams);
        animateAwayBackground();
    }

    private void animatePhoto(ViewGroup.MarginLayoutParams to) {
        if (this.mPhotoAnimator != null) {
            this.mPhotoAnimator.cancel();
        }
        this.mPhotoView.setLayoutParams(to);
        this.mAnimationPending = true;
        this.mPhotoView.requestLayout();
    }

    private void animateInBackground() {
        ObjectAnimator.ofFloat(this.mBackdrop, "alpha", 0.0f, 0.5f).setDuration(100L).start();
    }

    private void animateAwayBackground() {
        ObjectAnimator.ofFloat(this.mBackdrop, "alpha", 0.0f).setDuration(100L).start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("currentphotouri", this.mCurrentPhotoUri);
        outState.putBoolean("subinprogress", this.mSubActivityInProgress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (this.mPhotoHandler != null) {
            this.mSubActivityInProgress = false;
            if (this.mPhotoHandler.handlePhotoActivityResult(requestCode, resultCode, data)) {
                this.mPendingPhotoResult = null;
                return;
            } else if (this.mCloseActivityWhenCameBackFromSubActivity) {
                finishImmediatelyWithNoAnimation();
                return;
            } else {
                this.mPhotoHandler.onClick(this.mPhotoView);
                return;
            }
        }
        this.mPendingPhotoResult = new PendingPhotoResult(requestCode, resultCode, data);
    }

    private void attachPhotoHandler() {
        int mode = this.mPhotoUri == null ? 4 : 14;
        this.mPhotoHandler = new PhotoHandler(this, this.mPhotoView, mode & (-3), this.mState);
        if (this.mPendingPhotoResult == null) {
            SchedulingUtils.doAfterLayout(this.mBackdrop, new Runnable() {
                @Override
                public void run() {
                    PhotoSelectionActivity.this.animatePhotoOpen();
                }
            });
        } else {
            this.mPhotoHandler.handlePhotoActivityResult(this.mPendingPhotoResult.mRequestCode, this.mPendingPhotoResult.mResultCode, this.mPendingPhotoResult.mData);
            this.mPendingPhotoResult = null;
        }
    }

    private final class PhotoHandler extends PhotoSelectionHandler {
        private final PhotoSelectionHandler.PhotoActionListener mListener;

        private PhotoHandler(Context context, View photoView, int photoMode, RawContactDeltaList state) {
            super(context, photoView, photoMode, PhotoSelectionActivity.this.mIsDirectoryContact, state);
            this.mListener = new PhotoListener();
        }

        @Override
        public PhotoSelectionHandler.PhotoActionListener getListener() {
            return this.mListener;
        }

        @Override
        public void startPhotoActivity(Intent intent, int requestCode, Uri photoUri) {
            PhotoSelectionActivity.this.mSubActivityInProgress = true;
            PhotoSelectionActivity.this.mCurrentPhotoUri = photoUri;
            PhotoSelectionActivity.this.startActivityForResult(intent, requestCode);
        }

        private final class PhotoListener extends PhotoSelectionHandler.PhotoActionListener {
            private PhotoListener() {
                super();
            }

            @Override
            public void onPhotoSelected(Uri uri) {
                RawContactDeltaList delta = PhotoHandler.this.getDeltaForAttachingPhotoToContact();
                long rawContactId = PhotoHandler.this.getWritableEntityId();
                Intent intent = ContactSaveService.createSaveContactIntent(PhotoHandler.this.mContext, delta, (RawContactDeltaList) null, "", 0, PhotoSelectionActivity.this.mIsProfile, (Class<? extends Activity>) null, (String) null, rawContactId, uri);
                PhotoSelectionActivity.this.startService(intent);
                PhotoSelectionActivity.this.finish();
            }

            @Override
            public Uri getCurrentPhotoUri() {
                return PhotoSelectionActivity.this.mCurrentPhotoUri;
            }

            @Override
            public void onPhotoSelectionDismissed() {
                if (!PhotoSelectionActivity.this.mSubActivityInProgress) {
                    PhotoSelectionActivity.this.finish();
                }
            }
        }
    }

    private static class PendingPhotoResult {
        private final Intent mData;
        private final int mRequestCode;
        private final int mResultCode;

        private PendingPhotoResult(int requestCode, int resultCode, Intent data) {
            this.mRequestCode = requestCode;
            this.mResultCode = resultCode;
            this.mData = data;
        }
    }
}
