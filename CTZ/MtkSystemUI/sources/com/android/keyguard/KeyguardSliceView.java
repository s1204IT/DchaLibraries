package com.android.keyguard;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Trace;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceViewManager;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;
import androidx.slice.widget.RowContent;
import androidx.slice.widget.SliceLiveData;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.ColorUtils;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.keyguard.KeyguardSliceProvider;
import com.android.systemui.statusbar.AlphaOptimizedTextView;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.wakelock.KeepAwakeAnimationListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

public class KeyguardSliceView extends LinearLayout implements Observer<Slice>, View.OnClickListener, ConfigurationController.ConfigurationListener, TunerService.Tunable {
    private final HashMap<View, PendingIntent> mClickActions;
    private Runnable mContentChangeListener;
    private float mDarkAmount;
    private boolean mHasHeader;
    private int mIconSize;
    private Uri mKeyguardSliceUri;
    private LiveData<Slice> mLiveData;
    private boolean mPulsing;
    private Row mRow;
    private Slice mSlice;
    private int mTextColor;

    @VisibleForTesting
    TextView mTitle;

    public KeyguardSliceView(Context context) {
        this(context, null, 0);
    }

    public KeyguardSliceView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardSliceView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDarkAmount = 0.0f;
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "keyguard_slice_uri");
        this.mClickActions = new HashMap<>();
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setStagger(0, 275L);
        layoutTransition.setDuration(2, 550L);
        layoutTransition.setDuration(3, 275L);
        layoutTransition.disableTransitionType(0);
        layoutTransition.disableTransitionType(1);
        layoutTransition.setInterpolator(2, Interpolators.FAST_OUT_SLOW_IN);
        layoutTransition.setInterpolator(3, Interpolators.ALPHA_OUT);
        layoutTransition.setAnimateParentHierarchy(false);
        layoutTransition.addTransitionListener(new SliceViewTransitionListener());
        setLayoutTransition(layoutTransition);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTitle = (TextView) findViewById(com.android.systemui.R.id.title);
        this.mRow = (Row) findViewById(com.android.systemui.R.id.row);
        this.mTextColor = Utils.getColorAttr(this.mContext, com.android.systemui.R.attr.wallpaperTextColor);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mLiveData.observeForever(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mLiveData.removeObserver(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    private void showSlice() throws FileNotFoundException {
        PendingIntent action;
        Drawable drawableLoadDrawable;
        Trace.beginSection("KeyguardSliceView#showSlice");
        if (this.mPulsing || this.mSlice == null) {
            this.mTitle.setVisibility(8);
            this.mRow.setVisibility(8);
            if (this.mContentChangeListener != null) {
                this.mContentChangeListener.run();
                return;
            }
            return;
        }
        ListContent listContent = new ListContent(getContext(), this.mSlice);
        this.mHasHeader = listContent.hasHeader();
        ?? arrayList = new ArrayList();
        int i = 0;
        for (int i2 = 0; i2 < listContent.getRowItems().size(); i2++) {
            SliceItem sliceItem = listContent.getRowItems().get(i2);
            if (!"content://com.android.systemui.keyguard/action".equals(sliceItem.getSlice().getUri().toString())) {
                arrayList.add(sliceItem);
            }
        }
        if (this.mHasHeader) {
            this.mTitle.setVisibility(0);
            SliceItem titleItem = new RowContent(getContext(), (SliceItem) arrayList.get(0), true).getTitleItem();
            this.mTitle.setText(titleItem != null ? titleItem.getText() : null);
        } else {
            this.mTitle.setVisibility(8);
        }
        this.mClickActions.clear();
        int size = arrayList.size();
        int textColor = getTextColor();
        boolean z = this.mHasHeader;
        this.mRow.setVisibility(size > 0 ? 0 : 8);
        for (?? r7 = z; r7 < size; r7++) {
            SliceItem sliceItem2 = (SliceItem) arrayList.get(r7);
            RowContent rowContent = new RowContent(getContext(), sliceItem2, true);
            Uri uri = sliceItem2.getSlice().getUri();
            KeyguardSliceButton keyguardSliceButton = (KeyguardSliceButton) this.mRow.findViewWithTag(uri);
            if (keyguardSliceButton == null) {
                keyguardSliceButton = new KeyguardSliceButton(this.mContext);
                keyguardSliceButton.setTextColor(textColor);
                keyguardSliceButton.setTag(uri);
                this.mRow.addView(keyguardSliceButton, r7 - (this.mHasHeader ? 1 : 0));
            }
            if (rowContent.getPrimaryAction() != null) {
                action = rowContent.getPrimaryAction().getAction();
            } else {
                action = null;
            }
            this.mClickActions.put(keyguardSliceButton, action);
            SliceItem titleItem2 = rowContent.getTitleItem();
            keyguardSliceButton.setText(titleItem2 == null ? null : titleItem2.getText());
            keyguardSliceButton.setContentDescription(rowContent.getContentDescription());
            SliceItem sliceItemFind = SliceQuery.find(sliceItem2.getSlice(), "image");
            if (sliceItemFind != null) {
                drawableLoadDrawable = sliceItemFind.getIcon().loadDrawable(this.mContext);
                drawableLoadDrawable.setBounds(0, 0, Math.max((int) ((drawableLoadDrawable.getIntrinsicWidth() / drawableLoadDrawable.getIntrinsicHeight()) * this.mIconSize), 1), this.mIconSize);
            } else {
                drawableLoadDrawable = null;
            }
            keyguardSliceButton.setCompoundDrawables(drawableLoadDrawable, null, null, null);
            keyguardSliceButton.setOnClickListener(this);
            keyguardSliceButton.setClickable(action != null);
        }
        while (i < this.mRow.getChildCount()) {
            View childAt = this.mRow.getChildAt(i);
            if (!this.mClickActions.containsKey(childAt)) {
                this.mRow.removeView(childAt);
                i--;
            }
            i++;
        }
        if (this.mContentChangeListener != null) {
            this.mContentChangeListener.run();
        }
        Trace.endSection();
    }

    public void setPulsing(boolean z, boolean z2) throws FileNotFoundException {
        this.mPulsing = z;
        LayoutTransition layoutTransition = getLayoutTransition();
        if (!z2) {
            setLayoutTransition(null);
        }
        showSlice();
        if (!z2) {
            setLayoutTransition(layoutTransition);
        }
    }

    public static CharSequence findBestLineBreak(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            return charSequence;
        }
        String string = charSequence.toString();
        if (string.contains("\n") || !string.contains(" ")) {
            return string;
        }
        String[] strArrSplit = string.split(" ");
        StringBuilder sb = new StringBuilder(string.length());
        int i = 0;
        while (sb.length() < string.length() - sb.length()) {
            sb.append(strArrSplit[i]);
            if (i < strArrSplit.length - 1) {
                sb.append(" ");
            }
            i++;
        }
        sb.append("\n");
        for (int i2 = i; i2 < strArrSplit.length; i2++) {
            sb.append(strArrSplit[i2]);
            if (i < strArrSplit.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public void setDarkAmount(float f) {
        this.mDarkAmount = f;
        this.mRow.setDarkAmount(f);
        updateTextColors();
    }

    private void updateTextColors() {
        int textColor = getTextColor();
        this.mTitle.setTextColor(textColor);
        int childCount = this.mRow.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mRow.getChildAt(i);
            if (childAt instanceof Button) {
                ((Button) childAt).setTextColor(textColor);
            }
        }
    }

    @Override
    public void onClick(View view) {
        PendingIntent pendingIntent = this.mClickActions.get(view);
        if (pendingIntent != null) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.i("KeyguardSliceView", "Pending intent cancelled, nothing to launch", e);
            }
        }
    }

    public void setContentChangeListener(Runnable runnable) {
        this.mContentChangeListener = runnable;
    }

    public boolean hasHeader() {
        return this.mHasHeader;
    }

    @Override
    public void onChanged(Slice slice) throws FileNotFoundException {
        this.mSlice = slice;
        showSlice();
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        setupUri(str2);
    }

    public void setupUri(String str) {
        if (str == null) {
            str = "content://com.android.systemui.keyguard/main";
        }
        boolean z = false;
        if (this.mLiveData != null && this.mLiveData.hasActiveObservers()) {
            z = true;
            this.mLiveData.removeObserver(this);
        }
        this.mKeyguardSliceUri = Uri.parse(str);
        this.mLiveData = SliceLiveData.fromUri(this.mContext, this.mKeyguardSliceUri);
        if (z) {
            this.mLiveData.observeForever(this);
        }
    }

    @VisibleForTesting
    int getTextColor() {
        return ColorUtils.blendARGB(this.mTextColor, -1, this.mDarkAmount);
    }

    @VisibleForTesting
    void setTextColor(int i) {
        this.mTextColor = i;
        updateTextColors();
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        this.mIconSize = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.widget_icon_size);
    }

    public void refresh() throws FileNotFoundException {
        Slice sliceBindSlice;
        Trace.beginSection("KeyguardSliceView#refresh");
        if ("content://com.android.systemui.keyguard/main".equals(this.mKeyguardSliceUri.toString())) {
            KeyguardSliceProvider attachedInstance = KeyguardSliceProvider.getAttachedInstance();
            if (attachedInstance != null) {
                sliceBindSlice = attachedInstance.onBindSlice(this.mKeyguardSliceUri);
            } else {
                Log.w("KeyguardSliceView", "Keyguard slice not bound yet?");
                sliceBindSlice = null;
            }
        } else {
            sliceBindSlice = SliceViewManager.getInstance(getContext()).bindSlice(this.mKeyguardSliceUri);
        }
        onChanged(sliceBindSlice);
        Trace.endSection();
    }

    public static class Row extends LinearLayout {
        private float mDarkAmount;
        private final Animation.AnimationListener mKeepAwakeListener;

        public Row(Context context) {
            this(context, null);
        }

        public Row(Context context, AttributeSet attributeSet) {
            this(context, attributeSet, 0);
        }

        public Row(Context context, AttributeSet attributeSet, int i) {
            this(context, attributeSet, i, 0);
        }

        public Row(Context context, AttributeSet attributeSet, int i, int i2) {
            super(context, attributeSet, i, i2);
            this.mKeepAwakeListener = new KeepAwakeAnimationListener(this.mContext);
        }

        @Override
        protected void onFinishInflate() {
            LayoutTransition layoutTransition = new LayoutTransition();
            layoutTransition.setDuration(550L);
            ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(null, PropertyValuesHolder.ofInt("left", 0, 1), PropertyValuesHolder.ofInt("right", 0, 1));
            layoutTransition.setAnimator(0, objectAnimatorOfPropertyValuesHolder);
            layoutTransition.setAnimator(1, objectAnimatorOfPropertyValuesHolder);
            layoutTransition.setInterpolator(0, Interpolators.ACCELERATE_DECELERATE);
            layoutTransition.setInterpolator(1, Interpolators.ACCELERATE_DECELERATE);
            layoutTransition.setStartDelay(0, 550L);
            layoutTransition.setStartDelay(1, 550L);
            layoutTransition.setAnimator(2, ObjectAnimator.ofFloat((Object) null, "alpha", 0.0f, 1.0f));
            layoutTransition.setInterpolator(2, Interpolators.ALPHA_IN);
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat((Object) null, "alpha", 1.0f, 0.0f);
            layoutTransition.setInterpolator(3, Interpolators.ALPHA_OUT);
            layoutTransition.setDuration(3, 137L);
            layoutTransition.setAnimator(3, objectAnimatorOfFloat);
            layoutTransition.setAnimateParentHierarchy(false);
            setLayoutTransition(layoutTransition);
        }

        @Override
        protected void onMeasure(int i, int i2) {
            int size = View.MeasureSpec.getSize(i);
            int childCount = getChildCount();
            for (int i3 = 0; i3 < childCount; i3++) {
                View childAt = getChildAt(i3);
                if (childAt instanceof KeyguardSliceButton) {
                    ((KeyguardSliceButton) childAt).setMaxWidth(size / childCount);
                }
            }
            super.onMeasure(i, i2);
        }

        public void setDarkAmount(float f) {
            boolean z = f != 0.0f;
            if (z == (this.mDarkAmount != 0.0f)) {
                return;
            }
            this.mDarkAmount = f;
            setLayoutAnimationListener(z ? null : this.mKeepAwakeListener);
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
    }

    @VisibleForTesting
    static class KeyguardSliceButton extends Button implements ConfigurationController.ConfigurationListener {
        public KeyguardSliceButton(Context context) {
            super(context, null, 0, 2131886477);
            onDensityOrFontScaleChanged();
            setEllipsize(TextUtils.TruncateAt.END);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
        }

        @Override
        public void onDensityOrFontScaleChanged() {
            updatePadding();
        }

        @Override
        public void setText(CharSequence charSequence, TextView.BufferType bufferType) {
            super.setText(charSequence, bufferType);
            updatePadding();
        }

        private void updatePadding() {
            boolean z = !TextUtils.isEmpty(getText());
            int dimension = ((int) getContext().getResources().getDimension(com.android.systemui.R.dimen.widget_horizontal_padding)) / 2;
            setPadding(dimension, 0, (z ? 1 : -1) * dimension, 0);
            setCompoundDrawablePadding((int) this.mContext.getResources().getDimension(com.android.systemui.R.dimen.widget_icon_padding));
        }

        @Override
        public void setTextColor(int i) {
            super.setTextColor(i);
            updateDrawableColors();
        }

        @Override
        public void setCompoundDrawables(Drawable drawable, Drawable drawable2, Drawable drawable3, Drawable drawable4) {
            super.setCompoundDrawables(drawable, drawable2, drawable3, drawable4);
            updateDrawableColors();
            updatePadding();
        }

        private void updateDrawableColors() {
            int currentTextColor = getCurrentTextColor();
            for (Drawable drawable : getCompoundDrawables()) {
                if (drawable != null) {
                    drawable.setTint(currentTextColor);
                }
            }
        }
    }

    static class TitleView extends AlphaOptimizedTextView {
        public TitleView(Context context) {
            super(context);
        }

        public TitleView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public TitleView(Context context, AttributeSet attributeSet, int i) {
            super(context, attributeSet, i);
        }

        public TitleView(Context context, AttributeSet attributeSet, int i, int i2) {
            super(context, attributeSet, i, i2);
        }

        @Override
        protected void onMeasure(int i, int i2) {
            boolean z;
            super.onMeasure(i, i2);
            Layout layout = getLayout();
            int lineCount = layout.getLineCount();
            if (layout.getEllipsisCount(lineCount - 1) == 0) {
                z = false;
            } else {
                z = true;
            }
            if (lineCount > 0 && !z) {
                CharSequence text = getText();
                CharSequence charSequenceFindBestLineBreak = KeyguardSliceView.findBestLineBreak(text);
                if (!TextUtils.equals(text, charSequenceFindBestLineBreak)) {
                    setText(charSequenceFindBestLineBreak);
                    super.onMeasure(i, i2);
                }
            }
        }
    }

    private class SliceViewTransitionListener implements LayoutTransition.TransitionListener {
        private SliceViewTransitionListener() {
        }

        @Override
        public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            switch (i) {
                case 2:
                    view.setTranslationY(KeyguardSliceView.this.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.pulsing_notification_appear_translation));
                    view.animate().translationY(0.0f).setDuration(550L).setInterpolator(Interpolators.ALPHA_IN).start();
                    break;
                case 3:
                    if (view == KeyguardSliceView.this.mTitle) {
                        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) KeyguardSliceView.this.mTitle.getLayoutParams();
                        KeyguardSliceView.this.mTitle.setTranslationY((-KeyguardSliceView.this.mTitle.getHeight()) - (layoutParams.topMargin + layoutParams.bottomMargin));
                    }
                    break;
            }
        }

        @Override
        public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
        }
    }
}
