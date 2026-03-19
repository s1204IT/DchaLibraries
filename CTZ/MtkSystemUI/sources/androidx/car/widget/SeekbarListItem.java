package androidx.car.widget;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsUtils;
import androidx.car.widget.ListItem;
import androidx.car.widget.SeekbarListItem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SeekbarListItem extends ListItem<ViewHolder> {
    private final Context mContext;
    private int mMax;
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;
    private Drawable mPrimaryActionIconDrawable;
    private View.OnClickListener mPrimaryActionIconOnClickListener;
    private int mProgress;
    private int mSecondaryProgress;
    private boolean mShowSupplementalIconDivider;
    private Drawable mSupplementalIconDrawable;
    private View.OnClickListener mSupplementalIconOnClickListener;
    private String mText;
    private boolean mIsEnabled = true;
    private final List<ListItem.ViewBinder<ViewHolder>> mBinders = new ArrayList();
    private int mPrimaryActionType = 0;
    private int mSupplementalActionType = 0;

    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public SeekbarListItem(Context context) {
        this.mContext = context;
        markDirty();
    }

    @Override
    public int getViewType() {
        return 2;
    }

    public void setMax(int max) {
        this.mMax = max;
        markDirty();
    }

    public void setProgress(int progress) {
        this.mProgress = progress;
        markDirty();
    }

    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        this.mOnSeekBarChangeListener = listener;
        markDirty();
    }

    @Override
    protected void resolveDirtyState() {
        this.mBinders.clear();
        setItemLayoutHeight();
        setPrimaryAction();
        setSeekBarAndText();
        setSupplementalAction();
    }

    @Override
    protected void onBind(ViewHolder viewHolder) {
        hideSubViews(viewHolder);
        Iterator<ListItem.ViewBinder<ViewHolder>> it = this.mBinders.iterator();
        while (it.hasNext()) {
            it.next().bind(viewHolder);
        }
        for (View v : viewHolder.getWidgetViews()) {
            v.setEnabled(this.mIsEnabled);
        }
    }

    private void hideSubViews(ViewHolder vh) {
        for (View v : vh.getWidgetViews()) {
            v.setVisibility(8);
        }
        vh.getSeekBar().setVisibility(0);
    }

    private void setItemLayoutHeight() {
        final int minHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_single_line_list_item_height);
        this.mBinders.add(new ListItem.ViewBinder() {
            @Override
            public final void bind(Object obj) {
                SeekbarListItem.lambda$setItemLayoutHeight$41(minHeight, (SeekbarListItem.ViewHolder) obj);
            }
        });
    }

    static void lambda$setItemLayoutHeight$41(int i, ViewHolder vh) {
        vh.itemView.setMinimumHeight(i);
        vh.getContainerLayout().setMinimumHeight(i);
        ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
        layoutParams.height = -2;
        vh.itemView.requestLayout();
    }

    private void setPrimaryAction() {
        setPrimaryActionLayout();
        setPrimaryActionContent();
    }

    private void setSeekBarAndText() {
        setSeekBarAndTextContent();
        setSeekBarAndTextLayout();
    }

    private void setSupplementalAction() {
        setSupplementalActionLayout();
        setSupplementalActionContent();
    }

    private void setPrimaryActionLayout() {
        switch (this.mPrimaryActionType) {
            case 0:
            case 1:
                return;
            case 2:
                final int startMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1);
                final int iconSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_primary_icon_size);
                this.mBinders.add(new ListItem.ViewBinder() {
                    @Override
                    public final void bind(Object obj) {
                        SeekbarListItem.lambda$setPrimaryActionLayout$42(this.f$0, iconSize, startMargin, (SeekbarListItem.ViewHolder) obj);
                    }
                });
                return;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

    public static void lambda$setPrimaryActionLayout$42(SeekbarListItem seekbarListItem, int i, int i2, ViewHolder vh) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) vh.getPrimaryIcon().getLayoutParams();
        layoutParams.width = i;
        layoutParams.height = i;
        layoutParams.addRule(20);
        layoutParams.setMarginStart(i2);
        if (!TextUtils.isEmpty(seekbarListItem.mText)) {
            int itemHeight = seekbarListItem.mContext.getResources().getDimensionPixelSize(R.dimen.car_double_line_list_item_height);
            layoutParams.removeRule(15);
            layoutParams.topMargin = (itemHeight - i) / 2;
        } else {
            layoutParams.addRule(15);
            layoutParams.topMargin = 0;
        }
        vh.getPrimaryIcon().requestLayout();
    }

    private void setPrimaryActionContent() {
        switch (this.mPrimaryActionType) {
            case 0:
            case 1:
                return;
            case 2:
                this.mBinders.add(new ListItem.ViewBinder() {
                    @Override
                    public final void bind(Object obj) {
                        SeekbarListItem.lambda$setPrimaryActionContent$43(this.f$0, (SeekbarListItem.ViewHolder) obj);
                    }
                });
                return;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

    public static void lambda$setPrimaryActionContent$43(SeekbarListItem seekbarListItem, ViewHolder vh) {
        vh.getPrimaryIcon().setVisibility(0);
        vh.getPrimaryIcon().setImageDrawable(seekbarListItem.mPrimaryActionIconDrawable);
        vh.getPrimaryIcon().setOnClickListener(seekbarListItem.mPrimaryActionIconOnClickListener);
        vh.getPrimaryIcon().setClickable(seekbarListItem.mPrimaryActionIconOnClickListener != null);
    }

    private void setSeekBarAndTextContent() {
        this.mBinders.add(new ListItem.ViewBinder() {
            @Override
            public final void bind(Object obj) {
                SeekbarListItem.lambda$setSeekBarAndTextContent$44(this.f$0, (SeekbarListItem.ViewHolder) obj);
            }
        });
    }

    public static void lambda$setSeekBarAndTextContent$44(SeekbarListItem seekbarListItem, ViewHolder vh) {
        vh.getSeekBar().setMax(seekbarListItem.mMax);
        vh.getSeekBar().setProgress(seekbarListItem.mProgress);
        vh.getSeekBar().setSecondaryProgress(seekbarListItem.mSecondaryProgress);
        vh.getSeekBar().setOnSeekBarChangeListener(seekbarListItem.mOnSeekBarChangeListener);
        if (!TextUtils.isEmpty(seekbarListItem.mText)) {
            vh.getText().setVisibility(0);
            vh.getText().setText(seekbarListItem.mText);
            vh.getText().setTextAppearance(seekbarListItem.getTitleTextAppearance());
        }
    }

    private void setSeekBarAndTextLayout() {
        this.mBinders.add(new ListItem.ViewBinder() {
            @Override
            public final void bind(Object obj) {
                SeekbarListItem.lambda$setSeekBarAndTextLayout$45(this.f$0, (SeekbarListItem.ViewHolder) obj);
            }
        });
    }

    public static void lambda$setSeekBarAndTextLayout$45(SeekbarListItem seekbarListItem, ViewHolder vh) {
        ViewGroup.MarginLayoutParams seekBarLayoutParams = (ViewGroup.MarginLayoutParams) vh.getSeekBar().getLayoutParams();
        seekBarLayoutParams.topMargin = TextUtils.isEmpty(seekbarListItem.mText) ? 0 : seekbarListItem.mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_1);
        vh.getSeekBar().requestLayout();
        seekbarListItem.setViewStartMargin(vh.getSeekBarContainer());
        seekbarListItem.setViewEndMargin(vh.getSeekBarContainer());
        RelativeLayout.LayoutParams containerLayoutParams = (RelativeLayout.LayoutParams) vh.getSeekBarContainer().getLayoutParams();
        containerLayoutParams.addRule(15);
    }

    private void setViewStartMargin(View v) {
        int startMarginResId;
        switch (this.mPrimaryActionType) {
            case 0:
                startMarginResId = R.dimen.car_keyline_1;
                break;
            case 1:
            case 2:
                startMarginResId = R.dimen.car_keyline_3;
                break;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        layoutParams.setMarginStart(this.mContext.getResources().getDimensionPixelSize(startMarginResId));
        v.requestLayout();
    }

    private void setViewEndMargin(View v) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
        int endMargin = 0;
        switch (this.mSupplementalActionType) {
            case 0:
                layoutParams.addRule(21);
                layoutParams.removeRule(16);
                layoutParams.setMarginEnd(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1));
                v.requestLayout();
                return;
            case 1:
                layoutParams.addRule(16, R.id.supplemental_icon_divider);
                layoutParams.removeRule(21);
                layoutParams.setMarginEnd(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_4));
                v.requestLayout();
                return;
            case 3:
                int endMargin2 = 0 + this.mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_4);
                endMargin = endMargin2 + this.mContext.getResources().getDimensionPixelSize(R.dimen.car_vertical_line_divider_width);
            case 2:
                int endMargin3 = endMargin + this.mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_4) + this.mContext.getResources().getDimensionPixelSize(R.dimen.car_primary_icon_size) + this.mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1);
                layoutParams.addRule(21);
                layoutParams.removeRule(16);
                layoutParams.setMarginEnd(endMargin3);
                v.requestLayout();
                return;
            default:
                throw new IllegalStateException("Unknown supplemental action type.");
        }
    }

    private void setSupplementalActionLayout() {
        final int keyline1 = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1);
        final int padding4 = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_4);
        this.mBinders.add(new ListItem.ViewBinder() {
            @Override
            public final void bind(Object obj) {
                SeekbarListItem.lambda$setSupplementalActionLayout$46(keyline1, padding4, (SeekbarListItem.ViewHolder) obj);
            }
        });
    }

    static void lambda$setSupplementalActionLayout$46(int i, int i2, ViewHolder vh) {
        RelativeLayout.LayoutParams iconLayoutParams = (RelativeLayout.LayoutParams) vh.getSupplementalIcon().getLayoutParams();
        iconLayoutParams.addRule(21);
        iconLayoutParams.setMarginEnd(i);
        iconLayoutParams.addRule(15);
        vh.getSupplementalIcon().requestLayout();
        RelativeLayout.LayoutParams dividerLayoutParams = (RelativeLayout.LayoutParams) vh.getSupplementalIconDivider().getLayoutParams();
        dividerLayoutParams.addRule(16, R.id.supplemental_icon);
        dividerLayoutParams.setMarginEnd(i2);
        dividerLayoutParams.addRule(15);
        vh.getSupplementalIconDivider().requestLayout();
    }

    private void setSupplementalActionContent() {
        switch (this.mSupplementalActionType) {
            case 0:
            case 2:
            case 3:
                return;
            case 1:
                this.mBinders.add(new ListItem.ViewBinder() {
                    @Override
                    public final void bind(Object obj) {
                        SeekbarListItem.lambda$setSupplementalActionContent$47(this.f$0, (SeekbarListItem.ViewHolder) obj);
                    }
                });
                return;
            default:
                throw new IllegalStateException("Unknown supplemental action type.");
        }
    }

    public static void lambda$setSupplementalActionContent$47(SeekbarListItem seekbarListItem, ViewHolder vh) {
        vh.getSupplementalIcon().setVisibility(0);
        if (seekbarListItem.mShowSupplementalIconDivider) {
            vh.getSupplementalIconDivider().setVisibility(0);
        }
        vh.getSupplementalIcon().setImageDrawable(seekbarListItem.mSupplementalIconDrawable);
        vh.getSupplementalIcon().setOnClickListener(seekbarListItem.mSupplementalIconOnClickListener);
        vh.getSupplementalIcon().setClickable(seekbarListItem.mSupplementalIconOnClickListener != null);
    }

    public void setPrimaryActionIcon(Drawable drawable) {
        this.mPrimaryActionType = 2;
        this.mPrimaryActionIconDrawable = drawable;
        markDirty();
    }

    @Deprecated
    public void setSupplementalIcon(Drawable drawable, boolean showSupplementalIconDivider, View.OnClickListener listener) {
        this.mSupplementalActionType = 1;
        this.mSupplementalIconDrawable = drawable;
        this.mShowSupplementalIconDivider = showSupplementalIconDivider;
        this.mSupplementalIconOnClickListener = listener;
        markDirty();
    }

    public void setSupplementalEmptyIcon(boolean seekbarOffsetDividerWidth) {
        this.mSupplementalActionType = seekbarOffsetDividerWidth ? 3 : 2;
        markDirty();
    }

    public static class ViewHolder extends ListItem.ViewHolder {
        private RelativeLayout mContainerLayout;
        private ImageView mPrimaryIcon;
        private SeekBar mSeekBar;
        private LinearLayout mSeekBarContainer;
        private ImageView mSupplementalIcon;
        private View mSupplementalIconDivider;
        private TextView mText;
        private final View[] mWidgetViews;

        public ViewHolder(View itemView) {
            super(itemView);
            this.mContainerLayout = (RelativeLayout) itemView.findViewById(R.id.container);
            this.mPrimaryIcon = (ImageView) itemView.findViewById(R.id.primary_icon);
            this.mSeekBarContainer = (LinearLayout) itemView.findViewById(R.id.seek_bar_container);
            this.mText = (TextView) itemView.findViewById(R.id.text);
            this.mSeekBar = (SeekBar) itemView.findViewById(R.id.seek_bar);
            this.mSupplementalIcon = (ImageView) itemView.findViewById(R.id.supplemental_icon);
            this.mSupplementalIconDivider = itemView.findViewById(R.id.supplemental_icon_divider);
            int minTouchSize = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.car_touch_target_size);
            MinTouchTargetHelper.ensureThat(this.mSupplementalIcon).hasMinTouchSize(minTouchSize);
            this.mWidgetViews = new View[]{this.mPrimaryIcon, this.mSeekBar, this.mText, this.mSupplementalIcon, this.mSupplementalIconDivider};
        }

        @Override
        protected void applyUxRestrictions(CarUxRestrictions restrictions) {
            CarUxRestrictionsUtils.apply(this.itemView.getContext(), restrictions, getText());
        }

        public RelativeLayout getContainerLayout() {
            return this.mContainerLayout;
        }

        public ImageView getPrimaryIcon() {
            return this.mPrimaryIcon;
        }

        public LinearLayout getSeekBarContainer() {
            return this.mSeekBarContainer;
        }

        public TextView getText() {
            return this.mText;
        }

        public SeekBar getSeekBar() {
            return this.mSeekBar;
        }

        public ImageView getSupplementalIcon() {
            return this.mSupplementalIcon;
        }

        public View getSupplementalIconDivider() {
            return this.mSupplementalIconDivider;
        }

        public View[] getWidgetViews() {
            return this.mWidgetViews;
        }
    }
}
