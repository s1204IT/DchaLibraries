package com.android.contacts.quickcontact;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Spannable;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.Property;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.dialog.CallSubjectDialog;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExpandingEntryCardView extends CardView {
    private static final Property<View, Integer> VIEW_LAYOUT_HEIGHT_PROPERTY = new Property<View, Integer>(Integer.class, "height") {
        @Override
        public void set(View view, Integer num) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            layoutParams.height = num.intValue();
            view.setLayoutParams(layoutParams);
        }

        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getLayoutParams().height);
        }
    };
    private boolean mAllEntriesInflated;
    private ViewGroup mAnimationViewGroup;
    private int mCollapsedEntriesCount;
    private LinearLayout mContainer;
    private final int mDividerLineHeightPixels;
    private List<List<Entry>> mEntries;
    private LinearLayout mEntriesViewGroup;
    private List<List<View>> mEntryViews;
    private final ImageView mExpandCollapseArrow;
    private View mExpandCollapseButton;
    private final View.OnClickListener mExpandCollapseButtonListener;
    private TextView mExpandCollapseTextView;
    private boolean mIsAlwaysExpanded;
    private boolean mIsExpanded;
    private ExpandingEntryCardViewListener mListener;
    private int mNumEntries;
    private View.OnClickListener mOnClickListener;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener;
    private List<View> mSeparators;
    private int mThemeColor;
    private ColorFilter mThemeColorFilter;
    private TextView mTitleTextView;

    public interface ExpandingEntryCardViewListener {
        void onCollapse(int i);

        void onExpand();

        void onExpandDone();
    }

    public static final class Entry {
        private Spannable mAlternateContentDescription;
        private final Drawable mAlternateIcon;
        private final Intent mAlternateIntent;
        private final EntryContextMenuInfo mEntryContextMenuInfo;
        private final String mHeader;
        private final Drawable mIcon;
        private final int mIconResourceId;
        private final int mId;
        private final Intent mIntent;
        private final boolean mIsEditable;
        private Spannable mPrimaryContentDescription;
        private final boolean mShouldApplyColor;
        private final boolean mShouldApplyThirdIconColor;
        private final Drawable mSimIcon;
        private final String mSimName;
        private final String mSubHeader;
        private final Drawable mSubHeaderIcon;
        private final String mText;
        private final Drawable mTextIcon;
        private final int mThirdAction;
        private final String mThirdContentDescription;
        private final Bundle mThirdExtras;
        private final Drawable mThirdIcon;
        private final Intent mThirdIntent;

        public Entry(int i, Drawable drawable, String str, String str2, Drawable drawable2, String str3, Drawable drawable3, Spannable spannable, Intent intent, Drawable drawable4, Intent intent2, Spannable spannable2, boolean z, boolean z2, EntryContextMenuInfo entryContextMenuInfo, Drawable drawable5, Intent intent3, String str4, int i2, Bundle bundle, boolean z3, int i3) {
            this(i, drawable, str, str2, drawable2, str3, drawable3, null, null, spannable, intent, drawable4, intent2, spannable2, z, z2, entryContextMenuInfo, drawable5, intent3, str4, i2, bundle, z3, i3);
        }

        public Entry(int i, Drawable drawable, String str, String str2, Drawable drawable2, String str3, Drawable drawable3, Drawable drawable4, String str4, Spannable spannable, Intent intent, Drawable drawable5, Intent intent2, Spannable spannable2, boolean z, boolean z2, EntryContextMenuInfo entryContextMenuInfo, Drawable drawable6, Intent intent3, String str5, int i2, Bundle bundle, boolean z3, int i3) {
            this.mId = i;
            this.mIcon = drawable;
            this.mHeader = str;
            this.mSubHeader = str2;
            this.mSubHeaderIcon = drawable2;
            this.mText = str3;
            this.mTextIcon = drawable3;
            this.mSimIcon = drawable4;
            this.mSimName = str4;
            this.mPrimaryContentDescription = spannable;
            this.mIntent = intent;
            this.mAlternateIcon = drawable5;
            this.mAlternateIntent = intent2;
            this.mAlternateContentDescription = spannable2;
            this.mShouldApplyColor = z;
            this.mIsEditable = z2;
            this.mEntryContextMenuInfo = entryContextMenuInfo;
            this.mThirdIcon = drawable6;
            this.mThirdIntent = intent3;
            this.mThirdContentDescription = str5;
            this.mThirdAction = i2;
            this.mThirdExtras = bundle;
            this.mShouldApplyThirdIconColor = z3;
            this.mIconResourceId = i3;
        }

        Drawable getIcon() {
            return this.mIcon;
        }

        String getHeader() {
            return this.mHeader;
        }

        String getSubHeader() {
            return this.mSubHeader;
        }

        Drawable getSubHeaderIcon() {
            return this.mSubHeaderIcon;
        }

        public String getText() {
            return this.mText;
        }

        Drawable getTextIcon() {
            return this.mTextIcon;
        }

        Drawable getSimIcon() {
            return this.mSimIcon;
        }

        String getSimName() {
            return this.mSimName;
        }

        Spannable getPrimaryContentDescription() {
            return this.mPrimaryContentDescription;
        }

        Intent getIntent() {
            return this.mIntent;
        }

        Drawable getAlternateIcon() {
            return this.mAlternateIcon;
        }

        Intent getAlternateIntent() {
            return this.mAlternateIntent;
        }

        Spannable getAlternateContentDescription() {
            return this.mAlternateContentDescription;
        }

        boolean shouldApplyColor() {
            return this.mShouldApplyColor;
        }

        int getId() {
            return this.mId;
        }

        EntryContextMenuInfo getEntryContextMenuInfo() {
            return this.mEntryContextMenuInfo;
        }

        Drawable getThirdIcon() {
            return this.mThirdIcon;
        }

        Intent getThirdIntent() {
            return this.mThirdIntent;
        }

        String getThirdContentDescription() {
            return this.mThirdContentDescription;
        }

        public int getThirdAction() {
            return this.mThirdAction;
        }

        public Bundle getThirdExtras() {
            return this.mThirdExtras;
        }

        boolean shouldApplyThirdIconColor() {
            return this.mShouldApplyThirdIconColor;
        }
    }

    public ExpandingEntryCardView(Context context) {
        this(context, null);
    }

    public ExpandingEntryCardView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsExpanded = false;
        this.mNumEntries = 0;
        this.mAllEntriesInflated = false;
        this.mExpandCollapseButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ExpandingEntryCardView.this.mIsExpanded) {
                    ExpandingEntryCardView.this.collapse();
                } else {
                    ExpandingEntryCardView.this.expand();
                }
            }
        };
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(context);
        View viewInflate = layoutInflaterFrom.inflate(R.layout.expanding_entry_card_view, this);
        this.mEntriesViewGroup = (LinearLayout) viewInflate.findViewById(R.id.content_area_linear_layout);
        this.mTitleTextView = (TextView) viewInflate.findViewById(R.id.title);
        this.mContainer = (LinearLayout) viewInflate.findViewById(R.id.container);
        this.mExpandCollapseButton = layoutInflaterFrom.inflate(R.layout.quickcontact_expanding_entry_card_button, (ViewGroup) this, false);
        this.mExpandCollapseTextView = (TextView) this.mExpandCollapseButton.findViewById(R.id.text);
        this.mExpandCollapseArrow = (ImageView) this.mExpandCollapseButton.findViewById(R.id.arrow);
        this.mExpandCollapseButton.setOnClickListener(this.mExpandCollapseButtonListener);
        this.mDividerLineHeightPixels = getResources().getDimensionPixelSize(R.dimen.divider_line_height);
    }

    public void initialize(List<List<Entry>> list, int i, boolean z, boolean z2, ExpandingEntryCardViewListener expandingEntryCardViewListener, ViewGroup viewGroup) {
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(getContext());
        this.mIsExpanded = z;
        this.mIsAlwaysExpanded = z2;
        this.mIsExpanded |= this.mIsAlwaysExpanded;
        this.mEntryViews = new ArrayList(list.size());
        this.mEntries = list;
        this.mNumEntries = 0;
        this.mAllEntriesInflated = false;
        Iterator<List<Entry>> it = this.mEntries.iterator();
        while (it.hasNext()) {
            this.mNumEntries += it.next().size();
            this.mEntryViews.add(new ArrayList());
        }
        this.mCollapsedEntriesCount = Math.min(i, this.mNumEntries);
        if (list.size() > 1) {
            this.mSeparators = new ArrayList(list.size() - 1);
        }
        this.mListener = expandingEntryCardViewListener;
        this.mAnimationViewGroup = viewGroup;
        if (this.mIsExpanded) {
            updateExpandCollapseButton(getCollapseButtonText(), 0L);
            inflateAllEntries(layoutInflaterFrom);
        } else {
            updateExpandCollapseButton(getExpandButtonText(), 0L);
            inflateInitialEntries(layoutInflaterFrom);
        }
        insertEntriesIntoViewGroup();
        applyColor();
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mOnClickListener = onClickListener;
    }

    @Override
    public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener onCreateContextMenuListener) {
        this.mOnCreateContextMenuListener = onCreateContextMenuListener;
    }

    private List<View> calculateEntriesToRemoveDuringCollapse() {
        List<View> viewsToDisplay = getViewsToDisplay(true);
        viewsToDisplay.removeAll(getViewsToDisplay(false));
        return viewsToDisplay;
    }

    private void insertEntriesIntoViewGroup() {
        this.mEntriesViewGroup.removeAllViews();
        Iterator<View> it = getViewsToDisplay(this.mIsExpanded).iterator();
        while (it.hasNext()) {
            this.mEntriesViewGroup.addView(it.next());
        }
        removeView(this.mExpandCollapseButton);
        if (this.mCollapsedEntriesCount < this.mNumEntries && this.mExpandCollapseButton.getParent() == null && !this.mIsAlwaysExpanded) {
            this.mContainer.addView(this.mExpandCollapseButton, -1);
        }
    }

    private List<View> getViewsToDisplay(boolean z) {
        View viewGenerateSeparator;
        View viewGenerateSeparator2;
        ArrayList arrayList = new ArrayList();
        if (z) {
            for (int i = 0; i < this.mEntryViews.size(); i++) {
                List<View> list = this.mEntryViews.get(i);
                if (i > 0) {
                    int i2 = i - 1;
                    if (this.mSeparators.size() <= i2) {
                        viewGenerateSeparator2 = generateSeparator(list.get(0));
                        this.mSeparators.add(viewGenerateSeparator2);
                    } else {
                        viewGenerateSeparator2 = this.mSeparators.get(i2);
                    }
                    arrayList.add(viewGenerateSeparator2);
                }
                Iterator<View> it = list.iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next());
                }
            }
        } else {
            int size = this.mCollapsedEntriesCount - this.mEntryViews.size();
            int i3 = 0;
            for (int i4 = 0; i4 < this.mEntryViews.size() && i3 < this.mCollapsedEntriesCount; i4++) {
                List<View> list2 = this.mEntryViews.get(i4);
                if (i4 > 0) {
                    int i5 = i4 - 1;
                    if (this.mSeparators.size() <= i5) {
                        viewGenerateSeparator = generateSeparator(list2.get(0));
                        this.mSeparators.add(viewGenerateSeparator);
                    } else {
                        viewGenerateSeparator = this.mSeparators.get(i5);
                    }
                    arrayList.add(viewGenerateSeparator);
                }
                arrayList.add(list2.get(0));
                i3++;
                for (int i6 = 1; i6 < list2.size() && i3 < this.mCollapsedEntriesCount && size > 0; i6++) {
                    arrayList.add(list2.get(i6));
                    i3++;
                    size--;
                }
            }
        }
        formatEntryIfFirst(arrayList);
        return arrayList;
    }

    private void formatEntryIfFirst(List<View> list) {
        if (TextUtils.isEmpty(this.mTitleTextView.getText()) && list.size() > 0) {
            View view = list.get(0);
            view.setPaddingRelative(view.getPaddingStart(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_top) + getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_null_title_top_extra_padding), view.getPaddingEnd(), view.getPaddingBottom());
        }
    }

    private View generateSeparator(View view) {
        View view2 = new View(getContext());
        Resources resources = getResources();
        view2.setBackgroundColor(resources.getColor(R.color.divider_line_color_light));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, this.mDividerLineHeightPixels);
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_start);
        if (((ImageView) view.findViewById(R.id.icon)).getVisibility() == 0) {
            dimensionPixelSize += resources.getDimensionPixelSize(R.dimen.expanding_entry_card_item_icon_width) + resources.getDimensionPixelSize(R.dimen.expanding_entry_card_item_image_spacing);
        }
        layoutParams.setMarginStart(dimensionPixelSize);
        view2.setLayoutParams(layoutParams);
        return view2;
    }

    private CharSequence getExpandButtonText() {
        return getResources().getText(R.string.expanding_entry_card_view_see_more);
    }

    private CharSequence getCollapseButtonText() {
        return getResources().getText(R.string.expanding_entry_card_view_see_less);
    }

    private void inflateInitialEntries(LayoutInflater layoutInflater) {
        if (this.mCollapsedEntriesCount == this.mNumEntries) {
            inflateAllEntries(layoutInflater);
            return;
        }
        int size = this.mCollapsedEntriesCount - this.mEntries.size();
        int i = 0;
        for (int i2 = 0; i2 < this.mEntries.size() && i < this.mCollapsedEntriesCount; i2++) {
            List<Entry> list = this.mEntries.get(i2);
            List<View> list2 = this.mEntryViews.get(i2);
            list2.add(createEntryView(layoutInflater, list.get(0), 0));
            i++;
            for (int i3 = 1; i3 < list.size() && i < this.mCollapsedEntriesCount && size > 0; i3++) {
                list2.add(createEntryView(layoutInflater, list.get(i3), 4));
                i++;
                size--;
            }
        }
    }

    private void inflateAllEntries(LayoutInflater layoutInflater) {
        int i;
        if (this.mAllEntriesInflated) {
            return;
        }
        for (int i2 = 0; i2 < this.mEntries.size(); i2++) {
            List<Entry> list = this.mEntries.get(i2);
            List<View> list2 = this.mEntryViews.get(i2);
            for (int size = list2.size(); size < list.size(); size++) {
                Entry entry = list.get(size);
                if (entry.getIcon() == null) {
                    i = 8;
                } else if (size != 0) {
                    i = 4;
                } else {
                    i = 0;
                }
                list2.add(createEntryView(layoutInflater, entry, i));
            }
        }
        this.mAllEntriesInflated = true;
    }

    public void setColorAndFilter(int i, ColorFilter colorFilter) {
        this.mThemeColor = i;
        this.mThemeColorFilter = colorFilter;
        applyColor();
    }

    public void setEntryHeaderColor(int i) {
        if (this.mEntries != null) {
            Iterator<List<View>> it = this.mEntryViews.iterator();
            while (it.hasNext()) {
                Iterator<View> it2 = it.next().iterator();
                while (it2.hasNext()) {
                    TextView textView = (TextView) it2.next().findViewById(R.id.header);
                    if (textView != null) {
                        textView.setTextColor(i);
                    }
                }
            }
        }
    }

    public void setEntrySubHeaderColor(int i) {
        if (this.mEntries != null) {
            Iterator<List<View>> it = this.mEntryViews.iterator();
            while (it.hasNext()) {
                Iterator<View> it2 = it.next().iterator();
                while (it2.hasNext()) {
                    TextView textView = (TextView) it2.next().findViewById(R.id.sub_header);
                    if (textView != null) {
                        textView.setTextColor(i);
                    }
                }
            }
        }
    }

    public void applyColor() {
        Drawable icon;
        if (this.mThemeColor != 0 && this.mThemeColorFilter != null) {
            if (this.mTitleTextView != null) {
                this.mTitleTextView.setTextColor(this.mThemeColor);
            }
            if (this.mEntries != null) {
                Iterator<List<Entry>> it = this.mEntries.iterator();
                while (it.hasNext()) {
                    for (Entry entry : it.next()) {
                        if (entry.shouldApplyColor() && (icon = entry.getIcon()) != null) {
                            icon.mutate();
                            icon.setColorFilter(this.mThemeColorFilter);
                        }
                        Drawable alternateIcon = entry.getAlternateIcon();
                        if (alternateIcon != null) {
                            alternateIcon.mutate();
                            alternateIcon.setColorFilter(this.mThemeColorFilter);
                        }
                        Drawable thirdIcon = entry.getThirdIcon();
                        if (thirdIcon != null && entry.shouldApplyThirdIconColor()) {
                            thirdIcon.mutate();
                            thirdIcon.setColorFilter(this.mThemeColorFilter);
                        }
                    }
                }
            }
            this.mExpandCollapseTextView.setTextColor(this.mThemeColor);
            this.mExpandCollapseArrow.setColorFilter(this.mThemeColorFilter);
        }
    }

    private View createEntryView(LayoutInflater layoutInflater, final Entry entry, int i) {
        EntryView entryView = (EntryView) layoutInflater.inflate(R.layout.expanding_entry_card_item, (ViewGroup) this, false);
        entryView.setContextMenuInfo(entry.getEntryContextMenuInfo());
        if (!TextUtils.isEmpty(entry.getPrimaryContentDescription())) {
            entryView.setContentDescription(entry.getPrimaryContentDescription());
        }
        ImageView imageView = (ImageView) entryView.findViewById(R.id.icon);
        imageView.setVisibility(i);
        if (entry.getIcon() != null) {
            imageView.setImageDrawable(entry.getIcon());
        }
        TextView textView = (TextView) entryView.findViewById(R.id.header);
        if (!TextUtils.isEmpty(entry.getHeader())) {
            textView.setText(entry.getHeader());
        } else {
            textView.setVisibility(8);
        }
        TextView textView2 = (TextView) entryView.findViewById(R.id.sub_header);
        if (!TextUtils.isEmpty(entry.getSubHeader())) {
            textView2.setText(entry.getSubHeader());
        } else {
            textView2.setVisibility(8);
        }
        ImageView imageView2 = (ImageView) entryView.findViewById(R.id.icon_sub_header);
        if (entry.getSubHeaderIcon() != null) {
            imageView2.setImageDrawable(entry.getSubHeaderIcon());
        } else {
            imageView2.setVisibility(8);
        }
        TextView textView3 = (TextView) entryView.findViewById(R.id.text);
        if (!TextUtils.isEmpty(entry.getText())) {
            textView3.setText(entry.getText());
        } else {
            textView3.setVisibility(8);
        }
        ImageView imageView3 = (ImageView) entryView.findViewById(R.id.icon_text);
        if (entry.getTextIcon() != null) {
            imageView3.setImageDrawable(entry.getTextIcon());
        } else {
            imageView3.setVisibility(8);
        }
        ImageView imageView4 = (ImageView) entryView.findViewById(R.id.icon_sim);
        if (entry.getSimIcon() != null) {
            imageView4.setImageDrawable(entry.getSimIcon());
        } else {
            imageView4.setVisibility(8);
        }
        TextView textView4 = (TextView) entryView.findViewById(R.id.sim_name);
        if (!TextUtils.isEmpty(entry.getSimName())) {
            textView4.setText(entry.getSimName());
        } else {
            textView4.setVisibility(8);
        }
        if (entry.getIntent() != null) {
            entryView.setOnClickListener(this.mOnClickListener);
            entryView.setTag(new EntryTag(entry.getId(), entry.getIntent()));
        }
        if (entry.getIntent() == null && entry.getEntryContextMenuInfo() == null) {
            entryView.setBackground(null);
        }
        if (textView.getVisibility() == 0 && textView2.getVisibility() == 8 && textView3.getVisibility() == 8) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
            layoutParams.topMargin = (int) getResources().getDimension(R.dimen.expanding_entry_card_item_header_only_margin_top);
            layoutParams.bottomMargin += (int) getResources().getDimension(R.dimen.expanding_entry_card_item_header_only_margin_bottom);
            textView.setLayoutParams(layoutParams);
        }
        if (i == 4 && (!TextUtils.isEmpty(entry.getSubHeader()) || !TextUtils.isEmpty(entry.getText()))) {
            entryView.setPaddingRelative(entryView.getPaddingStart(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_no_icon_margin_top), entryView.getPaddingEnd(), entryView.getPaddingBottom());
        } else if (i == 4 && TextUtils.isEmpty(entry.getSubHeader()) && TextUtils.isEmpty(entry.getText())) {
            entryView.setPaddingRelative(entryView.getPaddingStart(), 0, entryView.getPaddingEnd(), entryView.getPaddingBottom());
        }
        ImageView imageView5 = (ImageView) entryView.findViewById(R.id.icon_alternate);
        ImageView imageView6 = (ImageView) entryView.findViewById(R.id.third_icon);
        if (entry.getAlternateIcon() != null && entry.getAlternateIntent() != null) {
            imageView5.setImageDrawable(entry.getAlternateIcon());
            imageView5.setOnClickListener(this.mOnClickListener);
            imageView5.setTag(new EntryTag(entry.getId(), entry.getAlternateIntent()));
            imageView5.setVisibility(0);
            imageView5.setContentDescription(entry.getAlternateContentDescription());
        }
        if (entry.getThirdIcon() != null && entry.getThirdAction() != 1) {
            imageView6.setImageDrawable(entry.getThirdIcon());
            if (entry.getThirdAction() == 2) {
                imageView6.setOnClickListener(this.mOnClickListener);
                imageView6.setTag(new EntryTag(entry.getId(), entry.getThirdIntent()));
            } else if (entry.getThirdAction() == 3) {
                imageView6.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!(view.getTag() instanceof Bundle)) {
                            return;
                        }
                        Context context = ExpandingEntryCardView.this.getContext();
                        if (context instanceof Activity) {
                            CallSubjectDialog.start((Activity) context, entry.getThirdExtras());
                        }
                    }
                });
                imageView6.setTag(entry.getThirdExtras());
            }
            imageView6.setVisibility(0);
            imageView6.setContentDescription(entry.getThirdContentDescription());
        }
        entryView.setOnTouchListener(new EntryTouchListener(entryView, imageView5, imageView6));
        entryView.setOnCreateContextMenuListener(this.mOnCreateContextMenuListener);
        return entryView;
    }

    private void updateExpandCollapseButton(CharSequence charSequence, long j) {
        if (this.mIsExpanded) {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mExpandCollapseArrow, "rotation", 180.0f);
            objectAnimatorOfFloat.setDuration(j);
            objectAnimatorOfFloat.start();
        } else {
            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mExpandCollapseArrow, "rotation", ContactPhotoManager.OFFSET_DEFAULT);
            objectAnimatorOfFloat2.setDuration(j);
            objectAnimatorOfFloat2.start();
        }
        this.mExpandCollapseTextView.setText(charSequence);
    }

    private void expand() {
        ViewGroup viewGroup;
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(300L);
        Fade fade = new Fade(1);
        fade.setDuration(200L);
        fade.setStartDelay(100L);
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(changeBounds);
        transitionSet.addTransition(fade);
        transitionSet.excludeTarget(R.id.text, true);
        if (this.mAnimationViewGroup != null) {
            viewGroup = this.mAnimationViewGroup;
        } else {
            viewGroup = this;
        }
        transitionSet.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                ExpandingEntryCardView.this.mListener.onExpand();
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                ExpandingEntryCardView.this.mListener.onExpandDone();
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
        TransitionManager.beginDelayedTransition(viewGroup, transitionSet);
        this.mIsExpanded = true;
        inflateAllEntries(LayoutInflater.from(getContext()));
        insertEntriesIntoViewGroup();
        updateExpandCollapseButton(getCollapseButtonText(), 300L);
    }

    private void collapse() {
        final List<View> listCalculateEntriesToRemoveDuringCollapse = calculateEntriesToRemoveDuringCollapse();
        AnimatorSet animatorSet = new AnimatorSet();
        ArrayList arrayList = new ArrayList(listCalculateEntriesToRemoveDuringCollapse.size());
        int height = 0;
        for (View view : listCalculateEntriesToRemoveDuringCollapse) {
            ObjectAnimator objectAnimatorOfObject = ObjectAnimator.ofObject(view, (Property<View, V>) VIEW_LAYOUT_HEIGHT_PROPERTY, (TypeEvaluator) null, (Object[]) new Integer[]{Integer.valueOf(view.getHeight()), 0});
            height += view.getHeight();
            objectAnimatorOfObject.setDuration(300L);
            arrayList.add(objectAnimatorOfObject);
            view.animate().alpha(ContactPhotoManager.OFFSET_DEFAULT).setDuration(75L);
        }
        animatorSet.playTogether(arrayList);
        animatorSet.start();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                ExpandingEntryCardView.this.insertEntriesIntoViewGroup();
                for (View view2 : listCalculateEntriesToRemoveDuringCollapse) {
                    if (view2 instanceof EntryView) {
                        ExpandingEntryCardView.VIEW_LAYOUT_HEIGHT_PROPERTY.set(view2, -2);
                    } else {
                        ExpandingEntryCardView.VIEW_LAYOUT_HEIGHT_PROPERTY.set(view2, Integer.valueOf(ExpandingEntryCardView.this.mDividerLineHeightPixels));
                    }
                    view2.animate().cancel();
                    view2.setAlpha(1.0f);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mListener.onCollapse(height);
        this.mIsExpanded = false;
        updateExpandCollapseButton(getExpandButtonText(), 300L);
    }

    public boolean isExpanded() {
        return this.mIsExpanded;
    }

    public void setTitle(String str) {
        if (this.mTitleTextView == null) {
            Log.e("ExpandingEntryCardView", "mTitleTextView is null");
        }
        this.mTitleTextView.setText(str);
        this.mTitleTextView.setVisibility(TextUtils.isEmpty(str) ? 8 : 0);
        findViewById(R.id.title_separator).setVisibility(TextUtils.isEmpty(str) ? 8 : 0);
        if (!TextUtils.isEmpty(str) && this.mEntriesViewGroup.getChildCount() > 0) {
            View childAt = this.mEntriesViewGroup.getChildAt(0);
            childAt.setPadding(childAt.getPaddingLeft(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_top), childAt.getPaddingRight(), childAt.getPaddingBottom());
        } else if (!TextUtils.isEmpty(str) && this.mEntriesViewGroup.getChildCount() > 0) {
            View childAt2 = this.mEntriesViewGroup.getChildAt(0);
            childAt2.setPadding(childAt2.getPaddingLeft(), getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_item_padding_top) + getResources().getDimensionPixelSize(R.dimen.expanding_entry_card_null_title_top_extra_padding), childAt2.getPaddingRight(), childAt2.getPaddingBottom());
        }
    }

    public boolean shouldShow() {
        return this.mEntries != null && this.mEntries.size() > 0;
    }

    public static final class EntryView extends RelativeLayout {
        private EntryContextMenuInfo mEntryContextMenuInfo;

        public EntryView(Context context) {
            super(context);
        }

        public EntryView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public void setContextMenuInfo(EntryContextMenuInfo entryContextMenuInfo) {
            this.mEntryContextMenuInfo = entryContextMenuInfo;
        }

        @Override
        protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
            return this.mEntryContextMenuInfo;
        }
    }

    public static final class EntryContextMenuInfo implements ContextMenu.ContextMenuInfo {
        private final String mCopyLabel;
        private final String mCopyText;
        private final long mId;
        private final boolean mIsSuperPrimary;
        private final String mMimeType;

        public EntryContextMenuInfo(String str, String str2, String str3, long j, boolean z) {
            this.mCopyText = str;
            this.mCopyLabel = str2;
            this.mMimeType = str3;
            this.mId = j;
            this.mIsSuperPrimary = z;
        }

        public String getCopyText() {
            return this.mCopyText;
        }

        public String getCopyLabel() {
            return this.mCopyLabel;
        }

        public String getMimeType() {
            return this.mMimeType;
        }

        public long getId() {
            return this.mId;
        }

        public boolean isSuperPrimary() {
            return this.mIsSuperPrimary;
        }
    }

    public static final class EntryTag {
        private final int mId;
        private final Intent mIntent;

        public EntryTag(int i, Intent intent) {
            this.mId = i;
            this.mIntent = intent;
        }

        public int getId() {
            return this.mId;
        }

        public Intent getIntent() {
            return this.mIntent;
        }
    }

    private static final class EntryTouchListener implements View.OnTouchListener {
        private final ImageView mAlternateIcon;
        private final View mEntry;
        private int mSlop;
        private final ImageView mThirdIcon;
        private View mTouchedView;

        public EntryTouchListener(View view, ImageView imageView, ImageView imageView2) {
            this.mEntry = view;
            this.mAlternateIcon = imageView;
            this.mThirdIcon = imageView2;
            this.mSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            boolean z;
            boolean z2;
            View view2 = this.mTouchedView;
            boolean z3 = true;
            switch (motionEvent.getAction()) {
                case 0:
                    if (hitThirdIcon(motionEvent)) {
                        this.mTouchedView = this.mThirdIcon;
                    } else if (hitAlternateIcon(motionEvent)) {
                        this.mTouchedView = this.mAlternateIcon;
                    } else {
                        this.mTouchedView = this.mEntry;
                        z = false;
                        z2 = z;
                        view2 = this.mTouchedView;
                    }
                    z = true;
                    z2 = z;
                    view2 = this.mTouchedView;
                    break;
                case 1:
                case 2:
                    z2 = (this.mTouchedView == null || this.mTouchedView == this.mEntry) ? false : true;
                    if (z2) {
                        Rect rect = new Rect();
                        view2.getHitRect(rect);
                        rect.inset(-this.mSlop, -this.mSlop);
                        if (!rect.contains((int) motionEvent.getX(), (int) motionEvent.getY())) {
                            z3 = false;
                        }
                    }
                    break;
                case 3:
                    z2 = (this.mTouchedView == null || this.mTouchedView == this.mEntry) ? false : true;
                    this.mTouchedView = null;
                    break;
                default:
                    z2 = false;
                    break;
            }
            if (!z2) {
                return false;
            }
            if (z3) {
                motionEvent.setLocation(view2.getWidth() / 2, view2.getHeight() / 2);
            } else {
                motionEvent.setLocation(-(this.mSlop * 2), -(this.mSlop * 2));
            }
            return view2.dispatchTouchEvent(motionEvent);
        }

        private boolean hitThirdIcon(MotionEvent motionEvent) {
            return this.mEntry.getLayoutDirection() == 1 ? this.mThirdIcon.getVisibility() == 0 && motionEvent.getX() < ((float) this.mThirdIcon.getRight()) : this.mThirdIcon.getVisibility() == 0 && motionEvent.getX() > ((float) this.mThirdIcon.getLeft());
        }

        private boolean hitAlternateIcon(MotionEvent motionEvent) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mAlternateIcon.getLayoutParams();
            return this.mEntry.getLayoutDirection() == 1 ? this.mAlternateIcon.getVisibility() == 0 && motionEvent.getX() < ((float) (this.mAlternateIcon.getRight() + layoutParams.rightMargin)) : this.mAlternateIcon.getVisibility() == 0 && motionEvent.getX() > ((float) (this.mAlternateIcon.getLeft() - layoutParams.leftMargin));
        }
    }
}
