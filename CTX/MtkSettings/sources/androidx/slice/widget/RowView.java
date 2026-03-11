package androidx.slice.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;
import androidx.slice.widget.SliceView;
import java.io.FileNotFoundException;
import java.util.List;

public class RowView extends SliceChildView implements View.OnClickListener {
    private LinearLayout mContent;
    private View mDivider;
    private LinearLayout mEndContainer;
    private List<SliceAction> mHeaderActions;
    private int mIconSize;
    private int mImageSize;
    private boolean mIsHeader;
    private boolean mIsSingleItem;
    private TextView mLastUpdatedText;
    private TextView mPrimaryText;
    private ProgressBar mRangeBar;
    private int mRangeHeight;
    private LinearLayout mRootView;
    private SliceActionImpl mRowAction;
    private RowContent mRowContent;
    private int mRowIndex;
    private TextView mSecondaryText;
    private View mSeeMoreView;
    private LinearLayout mStartContainer;
    private ArrayMap<SliceActionImpl, SliceActionView> mToggles;

    public RowView(Context context) {
        super(context);
        this.mToggles = new ArrayMap<>();
        this.mIconSize = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        this.mImageSize = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
        this.mRootView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.abc_slice_small_template, (ViewGroup) this, false);
        addView(this.mRootView);
        this.mStartContainer = (LinearLayout) findViewById(R.id.icon_frame);
        this.mContent = (LinearLayout) findViewById(android.R.id.content);
        this.mPrimaryText = (TextView) findViewById(android.R.id.title);
        this.mSecondaryText = (TextView) findViewById(android.R.id.summary);
        this.mLastUpdatedText = (TextView) findViewById(R.id.last_updated);
        this.mDivider = findViewById(R.id.divider);
        this.mEndContainer = (LinearLayout) findViewById(android.R.id.widget_frame);
        this.mRangeHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_row_range_height);
    }

    public void setSingleItem(boolean isSingleItem) {
        this.mIsSingleItem = isSingleItem;
    }

    @Override
    public int getSmallHeight() {
        if (this.mRowContent == null || !this.mRowContent.isValid()) {
            return 0;
        }
        return this.mRowContent.getSmallHeight();
    }

    @Override
    public int getActualHeight() {
        if (this.mIsSingleItem) {
            return getSmallHeight();
        }
        if (this.mRowContent == null || !this.mRowContent.isValid()) {
            return 0;
        }
        return this.mRowContent.getActualHeight();
    }

    private int getRowContentHeight() {
        int rowHeight;
        if (getMode() == 1 || this.mIsSingleItem) {
            rowHeight = getSmallHeight();
        } else {
            rowHeight = getActualHeight();
        }
        if (this.mRangeBar != null) {
            return rowHeight - this.mRangeHeight;
        }
        return rowHeight;
    }

    @Override
    public void setTint(int tintColor) throws FileNotFoundException {
        super.setTint(tintColor);
        if (this.mRowContent != null) {
            populateViews();
        }
    }

    @Override
    public void setSliceActions(List<SliceAction> actions) throws FileNotFoundException {
        this.mHeaderActions = actions;
        if (this.mRowContent != null) {
            populateViews();
        }
    }

    @Override
    public void setShowLastUpdated(boolean showLastUpdated) throws FileNotFoundException {
        super.setShowLastUpdated(showLastUpdated);
        if (this.mRowContent != null) {
            populateViews();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int totalHeight = getMode() == 1 ? getSmallHeight() : getActualHeight();
        int rowHeight = getRowContentHeight();
        if (rowHeight != 0) {
            this.mRootView.setVisibility(0);
            int heightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(rowHeight, 1073741824);
            measureChild(this.mRootView, widthMeasureSpec, heightMeasureSpec2);
        } else {
            this.mRootView.setVisibility(8);
        }
        if (this.mRangeBar != null) {
            int rangeMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mRangeHeight, 1073741824);
            measureChild(this.mRangeBar, widthMeasureSpec, rangeMeasureSpec);
        }
        int totalHeightSpec = View.MeasureSpec.makeMeasureSpec(totalHeight, 1073741824);
        super.onMeasure(widthMeasureSpec, totalHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mRootView.layout(0, 0, this.mRootView.getMeasuredWidth(), getRowContentHeight());
        if (this.mRangeBar != null) {
            this.mRangeBar.layout(0, getRowContentHeight(), this.mRangeBar.getMeasuredWidth(), getRowContentHeight() + this.mRangeHeight);
        }
    }

    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader, int index, int rowCount, SliceView.OnSliceActionListener observer) throws FileNotFoundException {
        setSliceActionListener(observer);
        this.mRowIndex = index;
        this.mIsHeader = ListContent.isValidHeader(slice);
        this.mHeaderActions = null;
        this.mRowContent = new RowContent(getContext(), slice, this.mIsHeader);
        populateViews();
    }

    private void populateViews() throws FileNotFoundException {
        SliceItem subtitleItem;
        SliceItem endItem;
        resetView();
        if (this.mRowContent.getLayoutDirItem() != null) {
            setLayoutDirection(this.mRowContent.getLayoutDirItem().getInt());
        }
        if (this.mRowContent.isDefaultSeeMore()) {
            showSeeMore();
            return;
        }
        CharSequence contentDescr = this.mRowContent.getContentDescription();
        if (contentDescr != null) {
            this.mContent.setContentDescription(contentDescr);
        }
        SliceItem startItem = this.mRowContent.getStartItem();
        boolean z = false;
        boolean showStart = startItem != null && this.mRowIndex > 0;
        if (showStart) {
            showStart = addItem(startItem, this.mTintColor, true);
        }
        this.mStartContainer.setVisibility(showStart ? 0 : 8);
        SliceItem titleItem = this.mRowContent.getTitleItem();
        if (titleItem != null) {
            this.mPrimaryText.setText(titleItem.getText());
        }
        this.mPrimaryText.setTextSize(0, this.mIsHeader ? this.mHeaderTitleSize : this.mTitleSize);
        this.mPrimaryText.setTextColor(this.mTitleColor);
        this.mPrimaryText.setVisibility(titleItem != null ? 0 : 8);
        if (getMode() == 1) {
            subtitleItem = this.mRowContent.getSummaryItem();
        } else {
            subtitleItem = this.mRowContent.getSubtitleItem();
        }
        addSubtitle(subtitleItem);
        SliceItem primaryAction = this.mRowContent.getPrimaryAction();
        if (primaryAction != null && primaryAction != startItem) {
            this.mRowAction = new SliceActionImpl(primaryAction);
            if (this.mRowAction.isToggle()) {
                addAction(this.mRowAction, this.mTintColor, this.mEndContainer, false);
                setViewClickable(this.mRootView, true);
                return;
            }
        }
        SliceItem range = this.mRowContent.getRange();
        if (range != null) {
            if (this.mRowAction != null) {
                setViewClickable(this.mRootView, true);
            }
            addRange(range);
            return;
        }
        List endItems = this.mRowContent.getEndItems();
        if (this.mHeaderActions != null && this.mHeaderActions.size() > 0) {
            endItems = this.mHeaderActions;
        }
        boolean firstItemIsADefaultToggle = false;
        SliceItem endAction = null;
        int endItemCount = 0;
        int endItemCount2 = 0;
        while (endItemCount2 < endItems.size()) {
            if (endItems.get(endItemCount2) instanceof SliceItem) {
                endItem = (SliceItem) endItems.get(endItemCount2);
            } else {
                endItem = ((SliceActionImpl) endItems.get(endItemCount2)).getSliceItem();
            }
            if (endItemCount < 3 && addItem(endItem, this.mTintColor, z)) {
                if (endAction == null && SliceQuery.find(endItem, "action") != null) {
                    SliceItem endAction2 = endItem;
                    endAction = endAction2;
                }
                endItemCount++;
                if (endItemCount == 1) {
                    boolean firstItemIsADefaultToggle2 = !this.mToggles.isEmpty() && SliceQuery.find(endItem.getSlice(), "image") == null;
                    firstItemIsADefaultToggle = firstItemIsADefaultToggle2;
                }
            }
            endItemCount2++;
            z = false;
        }
        this.mDivider.setVisibility((this.mRowAction == null || !firstItemIsADefaultToggle) ? 8 : 0);
        boolean hasStartAction = (startItem == null || SliceQuery.find(startItem, "action") == null) ? false : true;
        boolean hasEndItemAction = endAction != null;
        if (this.mRowAction != null) {
            setViewClickable((hasEndItemAction || hasStartAction) ? this.mContent : this.mRootView, true);
            return;
        }
        if (hasEndItemAction != hasStartAction) {
            if (endItemCount == 1 || hasStartAction) {
                if (!this.mToggles.isEmpty()) {
                    this.mRowAction = this.mToggles.keySet().iterator().next();
                } else {
                    this.mRowAction = new SliceActionImpl(endAction != null ? endAction : startItem);
                }
                setViewClickable(this.mRootView, true);
            }
        }
    }

    private void addSubtitle(SliceItem subtitleItem) {
        CharSequence subtitleTimeString = null;
        boolean subtitleExists = true;
        if (this.mShowLastUpdated && this.mLastUpdated != -1) {
            subtitleTimeString = getResources().getString(R.string.abc_slice_updated, SliceViewUtil.getRelativeTimeString(this.mLastUpdated));
        }
        CharSequence subtitle = subtitleItem != null ? subtitleItem.getText() : null;
        if (TextUtils.isEmpty(subtitle) && (subtitleItem == null || !subtitleItem.hasHint("partial"))) {
            subtitleExists = false;
        }
        if (subtitleExists) {
            this.mSecondaryText.setText(subtitle);
            this.mSecondaryText.setTextSize(0, this.mIsHeader ? this.mHeaderSubtitleSize : this.mSubtitleSize);
            this.mSecondaryText.setTextColor(this.mSubtitleColor);
            int verticalPadding = this.mIsHeader ? this.mVerticalHeaderTextPadding : this.mVerticalTextPadding;
            this.mSecondaryText.setPadding(0, verticalPadding, 0, 0);
        }
        if (subtitleTimeString != null) {
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleTimeString = " · " + ((Object) subtitleTimeString);
            }
            SpannableString sp = new SpannableString(subtitleTimeString);
            sp.setSpan(new StyleSpan(2), 0, subtitleTimeString.length(), 0);
            this.mLastUpdatedText.setText(sp);
            this.mLastUpdatedText.setTextSize(0, this.mIsHeader ? this.mHeaderSubtitleSize : this.mSubtitleSize);
            this.mLastUpdatedText.setTextColor(this.mSubtitleColor);
        }
        this.mLastUpdatedText.setVisibility(TextUtils.isEmpty(subtitleTimeString) ? 8 : 0);
        this.mSecondaryText.setVisibility(subtitleExists ? 0 : 8);
        this.mSecondaryText.requestLayout();
        this.mLastUpdatedText.requestLayout();
    }

    private void addRange(final SliceItem range) {
        ProgressBar progressBar;
        Drawable d;
        boolean isSeekBar = "action".equals(range.getFormat());
        if (isSeekBar) {
            progressBar = new SeekBar(getContext());
        } else {
            progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        }
        Drawable progressDrawable = DrawableCompat.wrap(progressBar.getProgressDrawable());
        if (this.mTintColor != -1 && progressDrawable != null) {
            DrawableCompat.setTint(progressDrawable, this.mTintColor);
            progressBar.setProgressDrawable(progressDrawable);
        }
        SliceItem min = SliceQuery.findSubtype(range, "int", "min");
        int minValue = 0;
        if (min != null) {
            minValue = min.getInt();
        }
        SliceItem max = SliceQuery.findSubtype(range, "int", "max");
        if (max != null) {
            progressBar.setMax(max.getInt() - minValue);
        }
        SliceItem progress = SliceQuery.findSubtype(range, "int", "value");
        if (progress != null) {
            progressBar.setProgress(progress.getInt() - minValue);
        }
        progressBar.setVisibility(0);
        addView(progressBar);
        this.mRangeBar = progressBar;
        if (isSeekBar) {
            SliceItem thumb = this.mRowContent.getInputRangeThumb();
            SeekBar seekBar = (SeekBar) this.mRangeBar;
            if (thumb != null && (d = thumb.getIcon().loadDrawable(getContext())) != null) {
                seekBar.setThumb(d);
            }
            Drawable thumbDrawable = DrawableCompat.wrap(seekBar.getThumb());
            if (this.mTintColor != -1 && thumbDrawable != null) {
                DrawableCompat.setTint(thumbDrawable, this.mTintColor);
                seekBar.setThumb(thumbDrawable);
            }
            final int finalMinValue = minValue;
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar2, int progress2, boolean fromUser) {
                    try {
                        range.fireAction(RowView.this.getContext(), new Intent().putExtra("android.app.slice.extra.RANGE_VALUE", progress2 + finalMinValue));
                    } catch (PendingIntent.CanceledException e) {
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar2) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar2) {
                }
            });
        }
    }

    private void addAction(SliceActionImpl sliceActionImpl, int i, ViewGroup viewGroup, boolean z) throws FileNotFoundException {
        SliceActionView sliceActionView = new SliceActionView(getContext());
        viewGroup.addView(sliceActionView);
        boolean zIsToggle = sliceActionImpl.isToggle();
        EventInfo eventInfo = new EventInfo(getMode(), !zIsToggle ? 1 : 0, zIsToggle ? 3 : 0, this.mRowIndex);
        if (z) {
            eventInfo.setPosition(0, 0, 1);
        }
        sliceActionView.setAction(sliceActionImpl, eventInfo, this.mObserver, i);
        if (zIsToggle) {
            this.mToggles.put(sliceActionImpl, sliceActionView);
        }
    }

    private boolean addItem(SliceItem sliceItem, int color, boolean isStart) throws FileNotFoundException {
        IconCompat icon = null;
        boolean zHasHint = false;
        SliceItem timeStamp = null;
        ViewGroup container = isStart ? this.mStartContainer : this.mEndContainer;
        if ("slice".equals(sliceItem.getFormat()) || "action".equals(sliceItem.getFormat())) {
            if (sliceItem.hasHint("shortcut")) {
                addAction(new SliceActionImpl(sliceItem), color, container, isStart);
                return true;
            }
            sliceItem = sliceItem.getSlice().getItems().get(0);
        }
        if ("image".equals(sliceItem.getFormat())) {
            icon = sliceItem.getIcon();
            zHasHint = sliceItem.hasHint("no_tint");
        } else if ("long".equals(sliceItem.getFormat())) {
            timeStamp = sliceItem;
        }
        View addedView = null;
        if (icon != null) {
            boolean isIcon = !zHasHint;
            ImageView iv = new ImageView(getContext());
            iv.setImageDrawable(icon.loadDrawable(getContext()));
            if (isIcon && color != -1) {
                iv.setColorFilter(color);
            }
            container.addView(iv);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
            lp.width = this.mImageSize;
            lp.height = this.mImageSize;
            iv.setLayoutParams(lp);
            int p = isIcon ? this.mIconSize / 2 : 0;
            iv.setPadding(p, p, p, p);
            addedView = iv;
        } else if (timeStamp != null) {
            TextView tv = new TextView(getContext());
            tv.setText(SliceViewUtil.getRelativeTimeString(sliceItem.getTimestamp()));
            tv.setTextSize(0, this.mSubtitleSize);
            tv.setTextColor(this.mSubtitleColor);
            container.addView(tv);
            addedView = tv;
        }
        return addedView != null;
    }

    private void showSeeMore() {
        Button b = (Button) LayoutInflater.from(getContext()).inflate(R.layout.abc_slice_row_show_more, (ViewGroup) this, false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (RowView.this.mObserver != null) {
                        EventInfo info = new EventInfo(RowView.this.getMode(), 4, 0, RowView.this.mRowIndex);
                        RowView.this.mObserver.onSliceAction(info, RowView.this.mRowContent.getSlice());
                    }
                    RowView.this.mRowContent.getSlice().fireAction(null, null);
                } catch (PendingIntent.CanceledException e) {
                    Log.e("RowView", "PendingIntent for slice cannot be sent", e);
                }
            }
        });
        if (this.mTintColor != -1) {
            b.setTextColor(this.mTintColor);
        }
        this.mSeeMoreView = b;
        this.mRootView.addView(this.mSeeMoreView);
    }

    @Override
    public void onClick(View view) {
        if (this.mRowAction != null && this.mRowAction.getActionItem() != null) {
            if (this.mRowAction.isToggle() && !(view instanceof SliceActionView)) {
                SliceActionView sav = this.mToggles.get(this.mRowAction);
                if (sav != null) {
                    sav.toggle();
                    return;
                }
                return;
            }
            try {
                this.mRowAction.getActionItem().fireAction(null, null);
                if (this.mObserver != null) {
                    EventInfo info = new EventInfo(getMode(), 3, 0, this.mRowIndex);
                    this.mObserver.onSliceAction(info, this.mRowAction.getSliceItem());
                }
            } catch (PendingIntent.CanceledException e) {
                Log.e("RowView", "PendingIntent for slice cannot be sent", e);
            }
        }
    }

    private void setViewClickable(View layout, boolean isClickable) {
        Drawable drawable = null;
        layout.setOnClickListener(isClickable ? this : null);
        if (isClickable) {
            drawable = SliceViewUtil.getDrawable(getContext(), android.R.attr.selectableItemBackground);
        }
        layout.setBackground(drawable);
        layout.setClickable(isClickable);
    }

    @Override
    public void resetView() {
        this.mRootView.setVisibility(0);
        setLayoutDirection(2);
        setViewClickable(this.mRootView, false);
        setViewClickable(this.mContent, false);
        this.mStartContainer.removeAllViews();
        this.mEndContainer.removeAllViews();
        this.mPrimaryText.setText((CharSequence) null);
        this.mSecondaryText.setText((CharSequence) null);
        this.mLastUpdatedText.setText((CharSequence) null);
        this.mLastUpdatedText.setVisibility(8);
        this.mToggles.clear();
        this.mRowAction = null;
        this.mDivider.setVisibility(8);
        if (this.mRangeBar != null) {
            removeView(this.mRangeBar);
            this.mRangeBar = null;
        }
        if (this.mSeeMoreView != null) {
            this.mRootView.removeView(this.mSeeMoreView);
            this.mSeeMoreView = null;
        }
    }
}
