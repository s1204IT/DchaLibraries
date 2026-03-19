package com.mediatek.camera.feature.setting.matrixdisplay;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.widget.Rotatable;
import com.mediatek.camera.common.widget.RotateLayout;
import com.mediatek.camera.feature.setting.matrixdisplay.MatrixDisplayView;
import java.util.List;

public class MatrixDisplayViewManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MatrixDisplayViewManager.class.getSimpleName());
    private static final String[] mEffectName = {"none", "mono", "negative", "solarize", "sepia", "posterize", "whiteboard", "blackboard", "aqua", "sepiagreen", "sepiablue", "nashville", "hefe", "valencia", "xproll", "lofi", "sierra", "kelvin", "walden", "f1977", "num"};
    private Activity mActivity;
    private MyAdapter mAdapter;
    private List<CharSequence> mEffectEntries;
    private List<CharSequence> mEffectEntryValues;
    private EffectUpdateListener mEffectUpdateListener;
    private ViewGroup mEffectsLayout;
    private Animation mFadeIn;
    private Animation mFadeOut;
    private MatrixDisplayView mGridView;
    private ItemClickListener mItemClickListener;
    private SurfaceAvailableListener mSurfaceAvailableListener;
    private ViewStateCallback mViewStateCallback;
    private Surface[] mSurfaceList = new Surface[12];
    private int mNumsOfEffect = 0;
    private int mDisplayWidth = 0;
    private int mDisplayHeight = 0;
    private int mOrientation = 0;
    private int mSelectedPosition = 0;
    private int mDisplayOrientation = 0;
    private boolean mMirror = false;
    private boolean mEffectsDone = false;
    private boolean mShowEffects = false;
    private boolean mNeedScrollToFirstPosition = false;
    private boolean mSizeChanged = false;
    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            LogHelper.d(MatrixDisplayViewManager.TAG, "[handleMessage],msg.what:" + message.what);
            switch (message.what) {
                case 0:
                    if (MatrixDisplayViewManager.this.mGridView != null) {
                        MatrixDisplayViewManager.this.mGridView.removeAllViews();
                    }
                    if (MatrixDisplayViewManager.this.mEffectsLayout != null && MatrixDisplayViewManager.this.mEffectsLayout.getParent() != null) {
                        MatrixDisplayViewManager.this.mEffectsLayout.removeAllViews();
                        ((ViewGroup) MatrixDisplayViewManager.this.mEffectsLayout.getParent()).removeView(MatrixDisplayViewManager.this.mEffectsLayout);
                    }
                    MatrixDisplayViewManager.this.mGridView = null;
                    MatrixDisplayViewManager.this.mEffectsLayout = null;
                    MatrixDisplayViewManager.this.mEffectsDone = false;
                    if (MatrixDisplayViewManager.this.mViewStateCallback != null) {
                        MatrixDisplayViewManager.this.mViewStateCallback.onViewDestroyed();
                    }
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    MatrixDisplayViewManager.this.rotateGridViewItem(MatrixDisplayViewManager.this.mOrientation);
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    if (MatrixDisplayViewManager.this.mEffectsLayout != null) {
                        MatrixDisplayViewManager.this.startFadeInAnimation(MatrixDisplayViewManager.this.mEffectsLayout);
                        MatrixDisplayViewManager.this.mEffectsLayout.setAlpha(1.0f);
                        MatrixDisplayViewManager.this.mEffectsLayout.setVisibility(0);
                    }
                    break;
            }
        }
    };

    public interface EffectUpdateListener {
        void onEffectUpdated(int i, int i2);
    }

    public interface ItemClickListener {
        boolean onItemClicked(String str);
    }

    public interface SurfaceAvailableListener {
        void onSurfaceAvailable(Surface surface, int i, int i2, int i3);
    }

    public interface ViewStateCallback {
        void onViewCreated();

        void onViewDestroyed();

        void onViewHidden();

        void onViewScrollOut();
    }

    public MatrixDisplayViewManager(Activity activity) {
        this.mActivity = activity;
    }

    public void setViewStateCallback(ViewStateCallback viewStateCallback) {
        this.mViewStateCallback = viewStateCallback;
    }

    public void setSurfaceAvailableListener(SurfaceAvailableListener surfaceAvailableListener) {
        this.mSurfaceAvailableListener = surfaceAvailableListener;
    }

    public void setEffectUpdateListener(EffectUpdateListener effectUpdateListener) {
        this.mEffectUpdateListener = effectUpdateListener;
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    public void setDisplayOrientation(int i) {
        this.mDisplayOrientation = i;
    }

    public void setOrientation(int i) {
        if (this.mOrientation != i) {
            LogHelper.d(TAG, "<setOrientation> mOrientation:" + this.mOrientation + ",orientation:" + i);
            this.mOrientation = i;
            rotateGridViewItem(i);
        }
    }

    public void setEffectEntriesAndEntryValues(List<CharSequence> list, List<CharSequence> list2) {
        this.mEffectEntries = list;
        this.mEffectEntryValues = list2;
        this.mNumsOfEffect = this.mEffectEntryValues.size();
    }

    public void setSelectedEffect(String str) {
        this.mSelectedPosition = this.mEffectEntryValues.indexOf(str);
    }

    public void setMirror(boolean z) {
        this.mMirror = z;
    }

    public void showView() {
        LogHelper.d(TAG, "[showView]..., start");
        this.mMainHandler.removeMessages(0);
        this.mShowEffects = true;
        if (this.mGridView != null && this.mNeedScrollToFirstPosition) {
            this.mNeedScrollToFirstPosition = false;
            this.mGridView.scrollToSelectedPosition(this.mSelectedPosition);
        }
        if (this.mGridView != null) {
            this.mGridView.showSelectedBorder(this.mSelectedPosition);
        }
        if (this.mEffectsLayout == null) {
            initialEffect();
        }
        if (this.mEffectsDone) {
            this.mMainHandler.sendEmptyMessageDelayed(2, 500L);
        }
        LogHelper.d(TAG, "[showEffect]..., end");
    }

    public void hideView(boolean z, int i) {
        hideEffect(z, i);
    }

    public void setLayoutSize(int i, int i2) {
        LogHelper.d(TAG, "[setLayoutSize], inputSize, width:" + i + ", height:" + i2 + "displayOrientation:" + this.mDisplayOrientation + ", mOrientation:" + this.mOrientation);
        this.mDisplayWidth = Math.max(i, i2);
        this.mDisplayHeight = Math.min(i, i2);
        if (this.mGridView != null) {
            this.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogHelper.d(MatrixDisplayViewManager.TAG, "setDisplaySize(" + MatrixDisplayViewManager.this.mDisplayWidth + "," + MatrixDisplayViewManager.this.mDisplayHeight + ")");
                    if (MatrixDisplayViewManager.this.mGridView != null) {
                        MatrixDisplayViewManager.this.mGridView.setLayoutSize(MatrixDisplayViewManager.this.mDisplayWidth, MatrixDisplayViewManager.this.mDisplayHeight);
                    }
                }
            });
        }
        LogHelper.d(TAG, "onSizeChanged(), outputSize, mDisplayWidth:" + this.mDisplayWidth + ", mDisplayHeight:" + this.mDisplayHeight);
        this.mSizeChanged = true;
    }

    private class MyAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;

        public MyAdapter(Context context) {
            this.mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return MatrixDisplayViewManager.this.mNumsOfEffect;
        }

        @Override
        public Object getItem(int i) {
            return MatrixDisplayViewManager.this.mSurfaceList[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private class ViewHolder {
            int mPosition;
            RotateLayout mRotateLayout;
            TextView mTextView;
            TextureView mTextureView;

            private ViewHolder() {
            }
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            LogHelper.d(MatrixDisplayViewManager.TAG, "convertView:" + view + ", position:" + i);
            if (MatrixDisplayViewManager.this.mEffectsLayout == null) {
                LogHelper.d(MatrixDisplayViewManager.TAG, "mEffectsLayout is null");
                return null;
            }
            int effectId = MatrixDisplayViewManager.this.getEffectId(((CharSequence) MatrixDisplayViewManager.this.mEffectEntryValues.get(i)).toString());
            if (i > 8 && i < 12 && view == null) {
                effectId = -1;
            }
            if (i >= 12) {
                MatrixDisplayViewManager.this.mEffectUpdateListener.onEffectUpdated(i - 12, effectId);
            } else {
                MatrixDisplayViewManager.this.mEffectUpdateListener.onEffectUpdated(i, effectId);
            }
            if (view == null) {
                view = this.mLayoutInflater.inflate(R.layout.lomo_effects_item, (ViewGroup) null);
                viewHolder = new ViewHolder();
                viewHolder.mTextureView = (TextureView) view.findViewById(R.id.textureview);
                viewHolder.mTextView = (TextView) view.findViewById(R.id.effects_name);
                viewHolder.mRotateLayout = (RotateLayout) view.findViewById(R.id.rotate);
                viewHolder.mTextureView.setLayoutParams(new RelativeLayout.LayoutParams((int) Math.ceil(((double) MatrixDisplayViewManager.this.mDisplayWidth) / 3.0d), (int) Math.ceil(((double) MatrixDisplayViewManager.this.mDisplayHeight) / 3.0d)));
                int paddingLeft = view.getPaddingLeft();
                if (MatrixDisplayViewManager.this.mDisplayOrientation == 270 || MatrixDisplayViewManager.this.mDisplayOrientation == 180) {
                    float f = paddingLeft;
                    viewHolder.mTextureView.setPivotX((((ViewGroup.LayoutParams) r0).width / 2.0f) - f);
                    viewHolder.mTextureView.setPivotY((((ViewGroup.LayoutParams) r0).height / 2.0f) - f);
                    viewHolder.mTextureView.setRotation(180.0f);
                }
                if (MatrixDisplayViewManager.this.mMirror) {
                    float f2 = paddingLeft;
                    viewHolder.mTextureView.setPivotX((((ViewGroup.LayoutParams) r0).width / 2.0f) - f2);
                    viewHolder.mTextureView.setPivotY((((ViewGroup.LayoutParams) r0).height / 2.0f) - f2);
                    viewHolder.mTextureView.setRotationY(180.0f);
                }
                viewHolder.mTextureView.setSurfaceTextureListener(MatrixDisplayViewManager.this.new LomoSurfaceTextureListener(i));
                viewHolder.mPosition = i;
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.mTextView.setText((CharSequence) MatrixDisplayViewManager.this.mEffectEntries.get(i));
            int iComputeRotation = MatrixDisplayViewManager.this.computeRotation(MatrixDisplayViewManager.this.mActivity, MatrixDisplayViewManager.this.mOrientation, 270);
            MatrixDisplayViewManager.this.layoutByOrientation(viewHolder.mRotateLayout, iComputeRotation);
            MatrixDisplayViewManager.this.setOrientation(viewHolder.mRotateLayout, iComputeRotation, true);
            return view;
        }
    }

    private int getEffectId(String str) {
        for (int i = 0; i < mEffectName.length; i++) {
            if (equals(str, mEffectName[i])) {
                LogHelper.d(TAG, "effectName:" + str + ", effetId:" + i);
                return i;
            }
        }
        LogHelper.d(TAG, "effectName:" + str + ", effetId: -1");
        return -1;
    }

    private boolean equals(Object obj, Object obj2) {
        return obj == obj2 || (obj != null && obj.equals(obj2));
    }

    private class LomoSurfaceTextureListener implements TextureView.SurfaceTextureListener {
        private int mPosition;

        public LomoSurfaceTextureListener(int i) {
            this.mPosition = i;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
            LogHelper.d(MatrixDisplayViewManager.TAG, "onSurfacetTextureAvailable(), surface:" + surfaceTexture + ", width:" + i + ", height:" + i2 + ", mPosition:" + this.mPosition);
            MatrixDisplayViewManager.this.mSurfaceList[this.mPosition] = new Surface(surfaceTexture);
            if (MatrixDisplayViewManager.this.mSurfaceAvailableListener != null) {
                MatrixDisplayViewManager.this.mSurfaceAvailableListener.onSurfaceAvailable(MatrixDisplayViewManager.this.mSurfaceList[this.mPosition], i, i2, this.mPosition);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            LogHelper.d(MatrixDisplayViewManager.TAG, "onSurfaceTextureDestroyed(), surface:" + surfaceTexture + "and mPosition:" + this.mPosition);
            MatrixDisplayViewManager.this.mSurfaceList[this.mPosition] = null;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    }

    private int computeRotation(Context context, int i, int i2) {
        if (context.getResources().getConfiguration().orientation == 1) {
            return ((i - i2) + 360) % 360;
        }
        return i;
    }

    private void layoutByOrientation(RotateLayout rotateLayout, int i) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) rotateLayout.getLayoutParams();
        if (i == 0) {
            layoutParams.addRule(11);
            layoutParams.addRule(12);
            layoutParams.removeRule(9);
            layoutParams.removeRule(10);
        } else if (i == 90) {
            layoutParams.addRule(11);
            layoutParams.addRule(10);
            layoutParams.removeRule(9);
            layoutParams.removeRule(12);
        } else if (i == 180) {
            layoutParams.addRule(9);
            layoutParams.addRule(10);
            layoutParams.removeRule(11);
            layoutParams.removeRule(12);
        } else if (i == 270) {
            layoutParams.addRule(9);
            layoutParams.addRule(12);
            layoutParams.removeRule(11);
            layoutParams.removeRule(10);
        }
        rotateLayout.setLayoutParams(layoutParams);
        rotateLayout.requestLayout();
    }

    private void setOrientation(View view, int i, boolean z) {
        if (view == 0) {
            LogHelper.d(TAG, "[setOrientation]view is null,return.");
            return;
        }
        if (view instanceof Rotatable) {
            ((Rotatable) view).setOrientation(i, z);
            return;
        }
        if (view instanceof ViewGroup) {
            int childCount = view.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                setOrientation(view.getChildAt(i2), i, z);
            }
        }
    }

    private void hideEffect(boolean z, long j) {
        LogHelper.d(TAG, "hideEffect(), animation:" + z + ", mEffectsLayout:" + this.mEffectsLayout);
        if (this.mEffectsLayout != null) {
            this.mMainHandler.removeMessages(0);
            this.mShowEffects = false;
            if (z) {
                startFadeOutAnimation(this.mEffectsLayout);
            }
            this.mEffectsLayout.setVisibility(8);
            this.mMainHandler.sendEmptyMessageDelayed(0, j);
            if (this.mViewStateCallback != null) {
                this.mViewStateCallback.onViewHidden();
            }
        }
    }

    private void startFadeOutAnimation(View view) {
        if (this.mFadeOut == null) {
            this.mFadeOut = AnimationUtils.loadAnimation(this.mActivity, R.anim.grid_effects_fade_out);
            this.mFadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
        }
        if (view != null) {
            view.startAnimation(this.mFadeOut);
            this.mFadeOut = null;
        }
    }

    private void rotateGridViewItem(int i) {
        LogHelper.d(TAG, "rotateGridViewItem(), orientation:" + i);
        int iComputeRotation = computeRotation(this.mActivity, i, 270);
        if (this.mGridView != null) {
            int childCount = this.mGridView.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                ViewGroup viewGroup = (ViewGroup) this.mGridView.getChildAt(i2);
                if (viewGroup != null) {
                    int childCount2 = viewGroup.getChildCount();
                    for (int i3 = 0; i3 < childCount2; i3++) {
                        View childAt = viewGroup.getChildAt(i3);
                        if (childAt != null) {
                            RotateLayout rotateLayout = (RotateLayout) childAt.findViewById(R.id.rotate);
                            layoutByOrientation(rotateLayout, iComputeRotation);
                            setOrientation(rotateLayout, iComputeRotation, true);
                        }
                    }
                }
            }
        }
    }

    private void startFadeInAnimation(View view) {
        if (this.mFadeIn == null) {
            this.mFadeIn = AnimationUtils.loadAnimation(this.mActivity, R.anim.gird_effects_fade_in);
        }
        if (view != null && this.mFadeIn != null) {
            view.startAnimation(this.mFadeIn);
            this.mFadeIn = null;
        }
    }

    private void initialEffect() {
        LogHelper.d(TAG, "[initialEffect]mEffectsLayout:" + this.mEffectsLayout + ", mSizeChanged:" + this.mSizeChanged + ", mMirror:" + this.mMirror);
        if (this.mEffectsLayout == null) {
            LogHelper.d(TAG, "nums of effect:" + this.mNumsOfEffect + ",mViewStateCallback" + this.mViewStateCallback);
            this.mEffectsLayout = (ViewGroup) this.mActivity.getLayoutInflater().inflate(R.layout.lomo_effects, (ViewGroup) null);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-1, -1);
            ((ViewGroup.MarginLayoutParams) layoutParams).topMargin = 0;
            this.mActivity.addContentView(this.mEffectsLayout, layoutParams);
            if (this.mViewStateCallback != null) {
                this.mViewStateCallback.onViewCreated();
            }
            this.mGridView = (MatrixDisplayView) this.mEffectsLayout.findViewById(R.id.lomo_effect_gridview);
            int i = this.mDisplayWidth % 3 == 0 ? this.mDisplayWidth / 3 : (this.mDisplayWidth / 3) + 1;
            this.mGridView.setGridWidth(i);
            this.mGridView.setGridHeight(this.mDisplayHeight / 3);
            this.mGridView.setGridCountInColumn(3);
            this.mGridView.setLayoutSize(i * 3, this.mDisplayHeight);
            this.mAdapter = new MyAdapter(this.mActivity);
            this.mGridView.setAdapter(this.mAdapter);
            this.mGridView.setOnItemClickListener(new MyOnItemClickListener());
            this.mGridView.setSelector(R.drawable.lomo_effect_selector);
            this.mGridView.setOverScrollMode(0);
            this.mGridView.setOnScrollListener(new MyOnScrollListener());
            this.mGridView.scrollToSelectedPosition(this.mSelectedPosition);
            this.mGridView.showSelectedBorder(this.mSelectedPosition);
            this.mSizeChanged = false;
            this.mEffectsLayout.setAlpha(0.0f);
            return;
        }
        if (this.mSizeChanged) {
            this.mGridView.setLayoutSize(this.mDisplayWidth, this.mDisplayHeight);
            this.mSizeChanged = false;
        }
    }

    private class MyOnScrollListener implements MatrixDisplayView.OnScrollListener {
        private MyOnScrollListener() {
        }

        @Override
        public void onScrollOut(MatrixDisplayView matrixDisplayView, int i) {
            LogHelper.d(MatrixDisplayViewManager.TAG, "onScrollOut()");
            if (i == 1) {
                MatrixDisplayViewManager.this.mNeedScrollToFirstPosition = true;
                if (MatrixDisplayViewManager.this.mViewStateCallback != null) {
                    MatrixDisplayViewManager.this.mViewStateCallback.onViewScrollOut();
                }
            }
        }

        @Override
        public void onScrollDone(MatrixDisplayView matrixDisplayView, int i, int i2) {
            LogHelper.d(MatrixDisplayViewManager.TAG, "onScrollDone(), startPosition:" + i + ", endPosition:" + i2);
            for (int i3 = i; i3 < i2; i3++) {
                int effectId = MatrixDisplayViewManager.this.getEffectId(((CharSequence) MatrixDisplayViewManager.this.mEffectEntryValues.get(i3)).toString());
                int i4 = i3 % 12;
                if (MatrixDisplayViewManager.this.mEffectUpdateListener != null) {
                    MatrixDisplayViewManager.this.mEffectUpdateListener.onEffectUpdated(i4, effectId);
                }
            }
            if (i == 0) {
                for (int i5 = i2; i5 < i2 + 3; i5++) {
                    if (MatrixDisplayViewManager.this.mEffectUpdateListener != null) {
                        MatrixDisplayViewManager.this.mEffectUpdateListener.onEffectUpdated(i5, -1);
                    }
                }
                return;
            }
            for (int i6 = i - 1; i6 >= i - 3; i6--) {
                if (MatrixDisplayViewManager.this.mEffectUpdateListener != null) {
                    MatrixDisplayViewManager.this.mEffectUpdateListener.onEffectUpdated(i6, -1);
                }
            }
        }
    }

    private class MyOnItemClickListener implements MatrixDisplayView.OnItemClickListener {
        private MyOnItemClickListener() {
        }

        @Override
        public boolean onItemClick(View view, int i) {
            LogHelper.d(MatrixDisplayViewManager.TAG, "[onItemClick], position:" + i);
            if (MatrixDisplayViewManager.this.mEffectsDone && MatrixDisplayViewManager.this.mItemClickListener != null) {
                return MatrixDisplayViewManager.this.mItemClickListener.onItemClicked(((CharSequence) MatrixDisplayViewManager.this.mEffectEntryValues.get(i)).toString());
            }
            return false;
        }
    }
}
