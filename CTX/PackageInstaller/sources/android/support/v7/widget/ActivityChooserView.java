package android.support.v7.widget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ActionProvider;
import android.support.v7.appcompat.R;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class ActivityChooserView extends ViewGroup {
    private final View mActivityChooserContent;
    final ActivityChooserViewAdapter mAdapter;
    private final Callbacks mCallbacks;
    final FrameLayout mDefaultActivityButton;
    final FrameLayout mExpandActivityOverflowButton;
    int mInitialActivityCount;
    private boolean mIsAttachedToWindow;
    boolean mIsSelectingDefaultActivity;
    private final int mListPopupMaxWidth;
    private ListPopupWindow mListPopupWindow;
    final DataSetObserver mModelDataSetObserver;
    PopupWindow.OnDismissListener mOnDismissListener;
    private final ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
    ActionProvider mProvider;

    void showPopupUnchecked(int maxActivityCount) {
        if (this.mAdapter.getDataModel() == null) {
            throw new IllegalStateException("No data model. Did you call #setDataModel?");
        }
        getViewTreeObserver().addOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
        boolean defaultActivityButtonShown = this.mDefaultActivityButton.getVisibility() == 0;
        int activityCount = this.mAdapter.getActivityCount();
        int maxActivityCountOffset = defaultActivityButtonShown ? 1 : 0;
        if (maxActivityCount != Integer.MAX_VALUE && activityCount > maxActivityCount + maxActivityCountOffset) {
            this.mAdapter.setShowFooterView(true);
            this.mAdapter.setMaxActivityCount(maxActivityCount - 1);
        } else {
            this.mAdapter.setShowFooterView(false);
            this.mAdapter.setMaxActivityCount(maxActivityCount);
        }
        ListPopupWindow popupWindow = getListPopupWindow();
        if (!popupWindow.isShowing()) {
            if (this.mIsSelectingDefaultActivity || !defaultActivityButtonShown) {
                this.mAdapter.setShowDefaultActivity(true, defaultActivityButtonShown);
            } else {
                this.mAdapter.setShowDefaultActivity(false, false);
            }
            int contentWidth = Math.min(this.mAdapter.measureContentWidth(), this.mListPopupMaxWidth);
            popupWindow.setContentWidth(contentWidth);
            popupWindow.show();
            if (this.mProvider != null) {
                this.mProvider.subUiVisibilityChanged(true);
            }
            popupWindow.getListView().setContentDescription(getContext().getString(R.string.abc_activitychooserview_choose_application));
            popupWindow.getListView().setSelector(new ColorDrawable(0));
        }
    }

    public boolean dismissPopup() {
        if (isShowingPopup()) {
            getListPopupWindow().dismiss();
            ViewTreeObserver viewTreeObserver = getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.removeGlobalOnLayoutListener(this.mOnGlobalLayoutListener);
                return true;
            }
            return true;
        }
        return true;
    }

    public boolean isShowingPopup() {
        return getListPopupWindow().isShowing();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ActivityChooserModel dataModel = this.mAdapter.getDataModel();
        if (dataModel != null) {
            dataModel.registerObserver(this.mModelDataSetObserver);
        }
        this.mIsAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ActivityChooserModel dataModel = this.mAdapter.getDataModel();
        if (dataModel != null) {
            dataModel.unregisterObserver(this.mModelDataSetObserver);
        }
        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.removeGlobalOnLayoutListener(this.mOnGlobalLayoutListener);
        }
        if (isShowingPopup()) {
            dismissPopup();
        }
        this.mIsAttachedToWindow = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View child = this.mActivityChooserContent;
        if (this.mDefaultActivityButton.getVisibility() != 0) {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(heightMeasureSpec), 1073741824);
        }
        measureChild(child, widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(child.getMeasuredWidth(), child.getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mActivityChooserContent.layout(0, 0, right - left, bottom - top);
        if (!isShowingPopup()) {
            dismissPopup();
        }
    }

    ListPopupWindow getListPopupWindow() {
        if (this.mListPopupWindow == null) {
            this.mListPopupWindow = new ListPopupWindow(getContext());
            this.mListPopupWindow.setAdapter(this.mAdapter);
            this.mListPopupWindow.setAnchorView(this);
            this.mListPopupWindow.setModal(true);
            this.mListPopupWindow.setOnItemClickListener(this.mCallbacks);
            this.mListPopupWindow.setOnDismissListener(this.mCallbacks);
        }
        return this.mListPopupWindow;
    }

    private class Callbacks implements View.OnClickListener, View.OnLongClickListener, AdapterView.OnItemClickListener, PopupWindow.OnDismissListener {
        final ActivityChooserView this$0;

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ActivityChooserViewAdapter adapter = (ActivityChooserViewAdapter) parent.getAdapter();
            int itemViewType = adapter.getItemViewType(position);
            switch (itemViewType) {
                case DialogFragment.STYLE_NORMAL:
                    this.this$0.dismissPopup();
                    if (this.this$0.mIsSelectingDefaultActivity) {
                        if (position > 0) {
                            this.this$0.mAdapter.getDataModel().setDefaultActivity(position);
                            return;
                        }
                        return;
                    } else {
                        Intent launchIntent = this.this$0.mAdapter.getDataModel().chooseActivity(this.this$0.mAdapter.getShowDefaultActivity() ? position : position + 1);
                        if (launchIntent != null) {
                            launchIntent.addFlags(524288);
                            this.this$0.getContext().startActivity(launchIntent);
                            return;
                        }
                        return;
                    }
                case DialogFragment.STYLE_NO_TITLE:
                    this.this$0.showPopupUnchecked(Preference.DEFAULT_ORDER);
                    return;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void onClick(View view) {
            if (view != this.this$0.mDefaultActivityButton) {
                if (view == this.this$0.mExpandActivityOverflowButton) {
                    this.this$0.mIsSelectingDefaultActivity = false;
                    this.this$0.showPopupUnchecked(this.this$0.mInitialActivityCount);
                    return;
                }
                throw new IllegalArgumentException();
            }
            this.this$0.dismissPopup();
            ResolveInfo defaultActivity = this.this$0.mAdapter.getDefaultActivity();
            int index = this.this$0.mAdapter.getDataModel().getActivityIndex(defaultActivity);
            Intent launchIntent = this.this$0.mAdapter.getDataModel().chooseActivity(index);
            if (launchIntent != null) {
                launchIntent.addFlags(524288);
                this.this$0.getContext().startActivity(launchIntent);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (view == this.this$0.mDefaultActivityButton) {
                if (this.this$0.mAdapter.getCount() > 0) {
                    this.this$0.mIsSelectingDefaultActivity = true;
                    this.this$0.showPopupUnchecked(this.this$0.mInitialActivityCount);
                }
                return true;
            }
            throw new IllegalArgumentException();
        }

        @Override
        public void onDismiss() {
            notifyOnDismissListener();
            if (this.this$0.mProvider != null) {
                this.this$0.mProvider.subUiVisibilityChanged(false);
            }
        }

        private void notifyOnDismissListener() {
            if (this.this$0.mOnDismissListener != null) {
                this.this$0.mOnDismissListener.onDismiss();
            }
        }
    }

    private class ActivityChooserViewAdapter extends BaseAdapter {
        private ActivityChooserModel mDataModel;
        private boolean mHighlightDefaultActivity;
        private int mMaxActivityCount;
        private boolean mShowDefaultActivity;
        private boolean mShowFooterView;
        final ActivityChooserView this$0;

        @Override
        public int getItemViewType(int position) {
            return (this.mShowFooterView && position == getCount() - 1) ? 1 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getCount() {
            int activityCount = this.mDataModel.getActivityCount();
            if (!this.mShowDefaultActivity && this.mDataModel.getDefaultActivity() != null) {
                activityCount--;
            }
            int count = Math.min(activityCount, this.mMaxActivityCount);
            if (this.mShowFooterView) {
                return count + 1;
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            int itemViewType = getItemViewType(position);
            switch (itemViewType) {
                case DialogFragment.STYLE_NORMAL:
                    if (!this.mShowDefaultActivity && this.mDataModel.getDefaultActivity() != null) {
                        position++;
                    }
                    return this.mDataModel.getActivity(position);
                case DialogFragment.STYLE_NO_TITLE:
                    return null;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int itemViewType = getItemViewType(position);
            switch (itemViewType) {
                case DialogFragment.STYLE_NORMAL:
                    if (convertView == null || convertView.getId() != R.id.list_item) {
                        convertView = LayoutInflater.from(this.this$0.getContext()).inflate(R.layout.abc_activity_chooser_view_list_item, parent, false);
                    }
                    PackageManager packageManager = this.this$0.getContext().getPackageManager();
                    ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
                    ResolveInfo activity = (ResolveInfo) getItem(position);
                    iconView.setImageDrawable(activity.loadIcon(packageManager));
                    TextView titleView = (TextView) convertView.findViewById(R.id.title);
                    titleView.setText(activity.loadLabel(packageManager));
                    if (this.mShowDefaultActivity && position == 0 && this.mHighlightDefaultActivity) {
                        convertView.setActivated(true);
                    } else {
                        convertView.setActivated(false);
                    }
                    return convertView;
                case DialogFragment.STYLE_NO_TITLE:
                    if (convertView == null || convertView.getId() != 1) {
                        View convertView2 = LayoutInflater.from(this.this$0.getContext()).inflate(R.layout.abc_activity_chooser_view_list_item, parent, false);
                        convertView2.setId(1);
                        TextView titleView2 = (TextView) convertView2.findViewById(R.id.title);
                        titleView2.setText(this.this$0.getContext().getString(R.string.abc_activity_chooser_view_see_all));
                        return convertView2;
                    }
                    return convertView;
                default:
                    throw new IllegalArgumentException();
            }
        }

        public int measureContentWidth() {
            int oldMaxActivityCount = this.mMaxActivityCount;
            this.mMaxActivityCount = Preference.DEFAULT_ORDER;
            int contentWidth = 0;
            View itemView = null;
            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            int count = getCount();
            for (int i = 0; i < count; i++) {
                itemView = getView(i, itemView, null);
                itemView.measure(widthMeasureSpec, heightMeasureSpec);
                contentWidth = Math.max(contentWidth, itemView.getMeasuredWidth());
            }
            this.mMaxActivityCount = oldMaxActivityCount;
            return contentWidth;
        }

        public void setMaxActivityCount(int maxActivityCount) {
            if (this.mMaxActivityCount != maxActivityCount) {
                this.mMaxActivityCount = maxActivityCount;
                notifyDataSetChanged();
            }
        }

        public ResolveInfo getDefaultActivity() {
            return this.mDataModel.getDefaultActivity();
        }

        public void setShowFooterView(boolean showFooterView) {
            if (this.mShowFooterView != showFooterView) {
                this.mShowFooterView = showFooterView;
                notifyDataSetChanged();
            }
        }

        public int getActivityCount() {
            return this.mDataModel.getActivityCount();
        }

        public ActivityChooserModel getDataModel() {
            return this.mDataModel;
        }

        public void setShowDefaultActivity(boolean showDefaultActivity, boolean highlightDefaultActivity) {
            if (this.mShowDefaultActivity != showDefaultActivity || this.mHighlightDefaultActivity != highlightDefaultActivity) {
                this.mShowDefaultActivity = showDefaultActivity;
                this.mHighlightDefaultActivity = highlightDefaultActivity;
                notifyDataSetChanged();
            }
        }

        public boolean getShowDefaultActivity() {
            return this.mShowDefaultActivity;
        }
    }

    public static class InnerLayout extends LinearLayout {
        private static final int[] TINT_ATTRS = {android.R.attr.background};

        public InnerLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
            TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, TINT_ATTRS);
            setBackgroundDrawable(a.getDrawable(0));
            a.recycle();
        }
    }
}
