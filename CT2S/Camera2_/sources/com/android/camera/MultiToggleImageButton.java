package com.android.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.android.camera.util.Gusterpolator;
import com.android.camera2.R;

public class MultiToggleImageButton extends ImageButton {
    public static final int ANIM_DIRECTION_HORIZONTAL = 1;
    public static final int ANIM_DIRECTION_VERTICAL = 0;
    private static final int ANIM_DURATION_MS = 250;
    private static final int UNSET = -1;
    private int mAnimDirection;
    private ValueAnimator mAnimator;
    private boolean mClickEnabled;
    private int[] mDescIds;
    private int[] mImageIds;
    private int mLevel;
    private Matrix mMatrix;
    private OnStateChangeListener mOnStateChangeListener;
    private int mParentSize;
    private int mState;

    public interface OnStateChangeListener {
        void stateChanged(View view, int i);
    }

    public MultiToggleImageButton(Context context) {
        super(context);
        this.mState = -1;
        this.mClickEnabled = true;
        this.mMatrix = new Matrix();
        init();
    }

    public MultiToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mState = -1;
        this.mClickEnabled = true;
        this.mMatrix = new Matrix();
        init();
        parseAttributes(context, attrs);
        setState(0);
    }

    public MultiToggleImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mState = -1;
        this.mClickEnabled = true;
        this.mMatrix = new Matrix();
        init();
        parseAttributes(context, attrs);
        setState(0);
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.mOnStateChangeListener = onStateChangeListener;
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int state) {
        setState(state, true);
    }

    public void setState(int state, boolean callListener) {
        setStateAnimatedInternal(state, callListener);
    }

    private void setStateAnimatedInternal(final int state, final boolean callListener) {
        if (this.mState == state || this.mState == -1) {
            setStateInternal(state, callListener);
        } else if (this.mImageIds != null) {
            new AsyncTask<Integer, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Integer... params) {
                    return MultiToggleImageButton.this.combine(params[0].intValue(), params[1].intValue());
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    int offset;
                    if (bitmap == null) {
                        MultiToggleImageButton.this.setStateInternal(state, callListener);
                        return;
                    }
                    MultiToggleImageButton.this.setImageBitmap(bitmap);
                    if (MultiToggleImageButton.this.mAnimDirection == 0) {
                        offset = (MultiToggleImageButton.this.mParentSize + MultiToggleImageButton.this.getHeight()) / 2;
                    } else if (MultiToggleImageButton.this.mAnimDirection == 1) {
                        offset = (MultiToggleImageButton.this.mParentSize + MultiToggleImageButton.this.getWidth()) / 2;
                    } else {
                        return;
                    }
                    MultiToggleImageButton.this.mAnimator.setFloatValues(-offset, 0.0f);
                    AnimatorSet s = new AnimatorSet();
                    s.play(MultiToggleImageButton.this.mAnimator);
                    s.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            MultiToggleImageButton.this.setClickEnabled(false);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            MultiToggleImageButton.this.setStateInternal(state, callListener);
                            MultiToggleImageButton.this.setClickEnabled(true);
                        }
                    });
                    s.start();
                }
            }.execute(Integer.valueOf(this.mState), Integer.valueOf(state));
        }
    }

    public void setClickEnabled(boolean enabled) {
        this.mClickEnabled = enabled;
    }

    private void setStateInternal(int state, boolean callListener) {
        this.mState = state;
        if (this.mImageIds != null) {
            setImageByState(this.mState);
        }
        if (this.mDescIds != null) {
            String oldContentDescription = String.valueOf(getContentDescription());
            String newContentDescription = getResources().getString(this.mDescIds[this.mState]);
            if (oldContentDescription != null && !oldContentDescription.isEmpty() && !oldContentDescription.equals(newContentDescription)) {
                setContentDescription(newContentDescription);
                String announceChange = getResources().getString(R.string.button_change_announcement, newContentDescription);
                announceForAccessibility(announceChange);
            }
        }
        super.setImageLevel(this.mLevel);
        if (callListener && this.mOnStateChangeListener != null) {
            this.mOnStateChangeListener.stateChanged(this, getState());
        }
    }

    private void nextState() {
        int state = this.mState + 1;
        if (state >= this.mImageIds.length) {
            state = 0;
        }
        setState(state);
    }

    protected void init() {
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MultiToggleImageButton.this.mClickEnabled) {
                    MultiToggleImageButton.this.nextState();
                }
            }
        });
        setScaleType(ImageView.ScaleType.MATRIX);
        this.mAnimator = ValueAnimator.ofFloat(0.0f, 0.0f);
        this.mAnimator.setDuration(250L);
        this.mAnimator.setInterpolator(Gusterpolator.INSTANCE);
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                MultiToggleImageButton.this.mMatrix.reset();
                if (MultiToggleImageButton.this.mAnimDirection == 0) {
                    MultiToggleImageButton.this.mMatrix.setTranslate(0.0f, ((Float) animation.getAnimatedValue()).floatValue());
                } else if (MultiToggleImageButton.this.mAnimDirection == 1) {
                    MultiToggleImageButton.this.mMatrix.setTranslate(((Float) animation.getAnimatedValue()).floatValue(), 0.0f);
                }
                MultiToggleImageButton.this.setImageMatrix(MultiToggleImageButton.this.mMatrix);
                MultiToggleImageButton.this.invalidate();
            }
        });
    }

    private void parseAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MultiToggleImageButton, 0, 0);
        int imageIds = a.getResourceId(0, 0);
        if (imageIds > 0) {
            overrideImageIds(imageIds);
        }
        int descIds = a.getResourceId(1, 0);
        if (descIds > 0) {
            overrideContentDescriptions(descIds);
        }
        a.recycle();
    }

    public void overrideImageIds(int resId) {
        TypedArray ids = null;
        try {
            ids = getResources().obtainTypedArray(resId);
            this.mImageIds = new int[ids.length()];
            for (int i = 0; i < ids.length(); i++) {
                this.mImageIds[i] = ids.getResourceId(i, 0);
            }
            if (this.mState >= 0 && this.mState < this.mImageIds.length) {
                setImageByState(this.mState);
            }
        } finally {
            if (ids != null) {
                ids.recycle();
            }
        }
    }

    public void overrideContentDescriptions(int resId) {
        TypedArray ids = null;
        try {
            ids = getResources().obtainTypedArray(resId);
            this.mDescIds = new int[ids.length()];
            for (int i = 0; i < ids.length(); i++) {
                this.mDescIds[i] = ids.getResourceId(i, 0);
            }
        } finally {
            if (ids != null) {
                ids.recycle();
            }
        }
    }

    public void setParentSize(int s) {
        this.mParentSize = s;
    }

    public void setAnimDirection(int d) {
        this.mAnimDirection = d;
    }

    @Override
    public void setImageLevel(int level) {
        super.setImageLevel(level);
        this.mLevel = level;
    }

    private void setImageByState(int state) {
        if (this.mImageIds != null) {
            setImageResource(this.mImageIds[state]);
        }
        super.setImageLevel(this.mLevel);
    }

    private Bitmap combine(int oldState, int newState) {
        if (oldState >= this.mImageIds.length) {
            return null;
        }
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        int[] enabledState = {android.R.attr.state_enabled};
        Drawable newDrawable = getResources().getDrawable(this.mImageIds[newState]).mutate();
        newDrawable.setState(enabledState);
        Drawable oldDrawable = getResources().getDrawable(this.mImageIds[oldState]).mutate();
        oldDrawable.setState(enabledState);
        if (this.mAnimDirection == 0) {
            int bitmapHeight = (height * 2) + ((this.mParentSize - height) / 2);
            int oldBitmapOffset = height + ((this.mParentSize - height) / 2);
            Bitmap bitmap = Bitmap.createBitmap(width, bitmapHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            newDrawable.setBounds(0, 0, newDrawable.getIntrinsicWidth(), newDrawable.getIntrinsicHeight());
            oldDrawable.setBounds(0, oldBitmapOffset, oldDrawable.getIntrinsicWidth(), oldDrawable.getIntrinsicHeight() + oldBitmapOffset);
            newDrawable.draw(canvas);
            oldDrawable.draw(canvas);
            return bitmap;
        }
        if (this.mAnimDirection != 1) {
            return null;
        }
        int bitmapWidth = (width * 2) + ((this.mParentSize - width) / 2);
        int oldBitmapOffset2 = width + ((this.mParentSize - width) / 2);
        Bitmap bitmap2 = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmap2);
        newDrawable.setBounds(0, 0, newDrawable.getIntrinsicWidth(), newDrawable.getIntrinsicHeight());
        oldDrawable.setBounds(oldBitmapOffset2, 0, oldDrawable.getIntrinsicWidth() + oldBitmapOffset2, oldDrawable.getIntrinsicHeight());
        newDrawable.draw(canvas2);
        oldDrawable.draw(canvas2);
        return bitmap2;
    }
}
