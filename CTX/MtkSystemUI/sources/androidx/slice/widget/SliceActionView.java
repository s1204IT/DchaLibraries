package androidx.slice.widget;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.widget.SliceView;
import java.io.FileNotFoundException;

public class SliceActionView extends FrameLayout implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final int[] STATE_CHECKED = {R.attr.state_checked};
    private View mActionView;
    private EventInfo mEventInfo;
    private int mIconSize;
    private int mImageSize;
    private SliceView.OnSliceActionListener mObserver;
    private SliceActionImpl mSliceAction;

    public SliceActionView(Context context) {
        super(context);
        Resources res = getContext().getResources();
        this.mIconSize = res.getDimensionPixelSize(androidx.slice.view.R.dimen.abc_slice_icon_size);
        this.mImageSize = res.getDimensionPixelSize(androidx.slice.view.R.dimen.abc_slice_small_image_size);
    }

    public void setAction(SliceActionImpl action, EventInfo info, SliceView.OnSliceActionListener listener, int color) throws FileNotFoundException {
        CharSequence contentDescription;
        this.mSliceAction = action;
        this.mEventInfo = info;
        this.mObserver = listener;
        this.mActionView = null;
        if (action.isDefaultToggle()) {
            Switch switchView = new Switch(getContext());
            addView(switchView);
            switchView.setChecked(action.isChecked());
            switchView.setOnCheckedChangeListener(this);
            switchView.setMinimumHeight(this.mImageSize);
            switchView.setMinimumWidth(this.mImageSize);
            this.mActionView = switchView;
        } else if (action.getIcon() != null) {
            if (action.isToggle()) {
                ImageToggle imageToggle = new ImageToggle(getContext());
                imageToggle.setChecked(action.isChecked());
                this.mActionView = imageToggle;
            } else {
                this.mActionView = new ImageView(getContext());
            }
            addView(this.mActionView);
            Drawable d = this.mSliceAction.getIcon().loadDrawable(getContext());
            ((ImageView) this.mActionView).setImageDrawable(d);
            if (color != -1 && this.mSliceAction.getImageMode() == 0 && d != null) {
                DrawableCompat.setTint(d, color);
            }
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mActionView.getLayoutParams();
            lp.width = this.mImageSize;
            lp.height = this.mImageSize;
            this.mActionView.setLayoutParams(lp);
            int p = action.getImageMode() == 0 ? this.mIconSize / 2 : 0;
            this.mActionView.setPadding(p, p, p, p);
            int touchFeedbackAttr = R.attr.selectableItemBackground;
            if (Build.VERSION.SDK_INT >= 21) {
                touchFeedbackAttr = R.attr.selectableItemBackgroundBorderless;
            }
            this.mActionView.setBackground(SliceViewUtil.getDrawable(getContext(), touchFeedbackAttr));
            this.mActionView.setOnClickListener(this);
        }
        if (this.mActionView != null) {
            if (action.getContentDescription() != null) {
                contentDescription = action.getContentDescription();
            } else {
                contentDescription = action.getTitle();
            }
            this.mActionView.setContentDescription(contentDescription);
        }
    }

    public void toggle() {
        if (this.mActionView != null && this.mSliceAction != null && this.mSliceAction.isToggle()) {
            ((Checkable) this.mActionView).toggle();
        }
    }

    @Override
    public void onClick(View v) {
        if (this.mSliceAction == null || this.mActionView == null) {
            return;
        }
        sendAction();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.mSliceAction == null || this.mActionView == null) {
            return;
        }
        sendAction();
    }

    private void sendAction() {
        try {
            if (this.mSliceAction.isToggle()) {
                boolean isChecked = ((Checkable) this.mActionView).isChecked();
                Intent i = new Intent().putExtra("android.app.slice.extra.TOGGLE_STATE", isChecked);
                this.mSliceAction.getActionItem().fireAction(getContext(), i);
                if (this.mEventInfo != null) {
                    this.mEventInfo.state = isChecked ? 1 : 0;
                }
            } else {
                this.mSliceAction.getActionItem().fireAction(null, null);
            }
            if (this.mObserver != null && this.mEventInfo != null) {
                this.mObserver.onSliceAction(this.mEventInfo, this.mSliceAction.getSliceItem());
            }
        } catch (PendingIntent.CanceledException e) {
            if (this.mActionView instanceof Checkable) {
                this.mActionView.setSelected(true ^ ((Checkable) this.mActionView).isChecked());
            }
            Log.e("SliceActionView", "PendingIntent for slice cannot be sent", e);
        }
    }

    private static class ImageToggle extends ImageView implements View.OnClickListener, Checkable {
        private boolean mIsChecked;
        private View.OnClickListener mListener;

        ImageToggle(Context context) {
            super(context);
            super.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            toggle();
        }

        @Override
        public void toggle() {
            setChecked(!isChecked());
        }

        @Override
        public void setChecked(boolean checked) {
            if (this.mIsChecked != checked) {
                this.mIsChecked = checked;
                refreshDrawableState();
                if (this.mListener != null) {
                    this.mListener.onClick(this);
                }
            }
        }

        @Override
        public void setOnClickListener(View.OnClickListener listener) {
            this.mListener = listener;
        }

        @Override
        public boolean isChecked() {
            return this.mIsChecked;
        }

        @Override
        public int[] onCreateDrawableState(int extraSpace) {
            int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
            if (this.mIsChecked) {
                mergeDrawableStates(drawableState, SliceActionView.STATE_CHECKED);
            }
            return drawableState;
        }
    }
}
