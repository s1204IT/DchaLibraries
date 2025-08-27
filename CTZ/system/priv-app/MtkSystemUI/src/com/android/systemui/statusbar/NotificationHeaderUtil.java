package com.android.systemui.statusbar;

import android.R;
import android.app.Notification;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.NotificationHeaderView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/* loaded from: classes.dex */
public class NotificationHeaderUtil {
    private static final TextViewComparator sTextViewComparator;
    private static final VisibilityApplicator sVisibilityApplicator;
    private final ArrayList<HeaderProcessor> mComparators = new ArrayList<>();
    private final HashSet<Integer> mDividers = new HashSet<>();
    private final ExpandableNotificationRow mRow;
    private static final DataExtractor sIconExtractor = new DataExtractor() { // from class: com.android.systemui.statusbar.NotificationHeaderUtil.1
        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.DataExtractor
        public Object extractData(ExpandableNotificationRow expandableNotificationRow) {
            return expandableNotificationRow.getStatusBarNotification().getNotification();
        }
    };
    private static final IconComparator sIconVisibilityComparator = new IconComparator() { // from class: com.android.systemui.statusbar.NotificationHeaderUtil.2
        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.IconComparator, com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
        public boolean compare(View view, View view2, Object obj, Object obj2) {
            return hasSameIcon(obj, obj2) && hasSameColor(obj, obj2);
        }
    };
    private static final IconComparator sGreyComparator = new IconComparator() { // from class: com.android.systemui.statusbar.NotificationHeaderUtil.3
        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.IconComparator, com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
        public boolean compare(View view, View view2, Object obj, Object obj2) {
            return !hasSameIcon(obj, obj2) || hasSameColor(obj, obj2);
        }
    };
    private static final ResultApplicator mGreyApplicator = new ResultApplicator() { // from class: com.android.systemui.statusbar.NotificationHeaderUtil.4
        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ResultApplicator
        public void apply(View view, boolean z) {
            NotificationHeaderView notificationHeaderView = (NotificationHeaderView) view;
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            ImageView imageView2 = (ImageView) view.findViewById(R.id.by_org_unit);
            applyToChild(imageView, z, notificationHeaderView.getOriginalIconColor());
            applyToChild(imageView2, z, notificationHeaderView.getOriginalNotificationColor());
        }

        private void applyToChild(View view, boolean z, int i) {
            if (i != 1) {
                ImageView imageView = (ImageView) view;
                imageView.getDrawable().mutate();
                if (z) {
                    imageView.getDrawable().setColorFilter(view.getContext().getColor(R.color.accessibility_magnification_thumbnail_background_color), PorterDuff.Mode.SRC_ATOP);
                } else {
                    imageView.getDrawable().setColorFilter(i, PorterDuff.Mode.SRC_ATOP);
                }
            }
        }
    };

    private interface DataExtractor {
        Object extractData(ExpandableNotificationRow expandableNotificationRow);
    }

    private interface ResultApplicator {
        void apply(View view, boolean z);
    }

    private interface ViewComparator {
        boolean compare(View view, View view2, Object obj, Object obj2);

        boolean isEmpty(View view);
    }

    static {
        sTextViewComparator = new TextViewComparator();
        sVisibilityApplicator = new VisibilityApplicator();
    }

    public NotificationHeaderUtil(ExpandableNotificationRow expandableNotificationRow) {
        this.mRow = expandableNotificationRow;
        this.mComparators.add(new HeaderProcessor(this.mRow, R.id.icon, sIconExtractor, sIconVisibilityComparator, sVisibilityApplicator));
        this.mComparators.add(new HeaderProcessor(this.mRow, R.id.icon_menu, sIconExtractor, sGreyComparator, mGreyApplicator));
        this.mComparators.add(new HeaderProcessor(this.mRow, R.id.leftPanel, null, new ViewComparator() { // from class: com.android.systemui.statusbar.NotificationHeaderUtil.5
            @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
            public boolean compare(View view, View view2, Object obj, Object obj2) {
                return view.getVisibility() != 8;
            }

            @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
            public boolean isEmpty(View view) {
                return (view instanceof ImageView) && ((ImageView) view).getDrawable() == null;
            }
        }, sVisibilityApplicator));
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, R.id.accountPreferences));
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, R.id.conversation_icon));
        this.mDividers.add(Integer.valueOf(R.id.conversation_icon_badge));
        this.mDividers.add(Integer.valueOf(R.id.conversation_icon_badge_ring));
        this.mDividers.add(Integer.valueOf(R.id.pressed));
    }

    public void updateChildrenHeaderAppearance() {
        List<ExpandableNotificationRow> notificationChildren = this.mRow.getNotificationChildren();
        if (notificationChildren == null) {
            return;
        }
        for (int i = 0; i < this.mComparators.size(); i++) {
            this.mComparators.get(i).init();
        }
        for (int i2 = 0; i2 < notificationChildren.size(); i2++) {
            ExpandableNotificationRow expandableNotificationRow = notificationChildren.get(i2);
            for (int i3 = 0; i3 < this.mComparators.size(); i3++) {
                this.mComparators.get(i3).compareToHeader(expandableNotificationRow);
            }
        }
        for (int i4 = 0; i4 < notificationChildren.size(); i4++) {
            ExpandableNotificationRow expandableNotificationRow2 = notificationChildren.get(i4);
            for (int i5 = 0; i5 < this.mComparators.size(); i5++) {
                this.mComparators.get(i5).apply(expandableNotificationRow2);
            }
            sanitizeHeaderViews(expandableNotificationRow2);
        }
    }

    private void sanitizeHeaderViews(ExpandableNotificationRow expandableNotificationRow) {
        if (expandableNotificationRow.isSummaryWithChildren()) {
            sanitizeHeader(expandableNotificationRow.getNotificationHeader());
            return;
        }
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        sanitizeChild(privateLayout.getContractedChild());
        sanitizeChild(privateLayout.getHeadsUpChild());
        sanitizeChild(privateLayout.getExpandedChild());
    }

    private void sanitizeChild(View view) {
        if (view != null) {
            sanitizeHeader((NotificationHeaderView) view.findViewById(R.id.icon_menu));
        }
    }

    private void sanitizeHeader(NotificationHeaderView notificationHeaderView) {
        int i;
        boolean z;
        View childAt;
        boolean z2;
        if (notificationHeaderView == null) {
            return;
        }
        int childCount = notificationHeaderView.getChildCount();
        View viewFindViewById = notificationHeaderView.findViewById(R.id.prefs);
        int i2 = 1;
        while (true) {
            i = childCount - 1;
            if (i2 < i) {
                View childAt2 = notificationHeaderView.getChildAt(i2);
                if (!(childAt2 instanceof TextView) || childAt2.getVisibility() == 8 || this.mDividers.contains(Integer.valueOf(childAt2.getId())) || childAt2 == viewFindViewById) {
                    i2++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        viewFindViewById.setVisibility((!z || this.mRow.getStatusBarNotification().getNotification().showsTime()) ? 0 : 8);
        View view = null;
        int i3 = 1;
        while (i3 < i) {
            View childAt3 = notificationHeaderView.getChildAt(i3);
            if (this.mDividers.contains(Integer.valueOf(childAt3.getId()))) {
                while (true) {
                    i3++;
                    if (i3 >= i) {
                        break;
                    }
                    childAt = notificationHeaderView.getChildAt(i3);
                    if (this.mDividers.contains(Integer.valueOf(childAt.getId()))) {
                        i3--;
                        break;
                    } else if (childAt.getVisibility() != 8 && (childAt instanceof TextView)) {
                        z2 = view != null;
                    }
                }
                childAt = view;
                z2 = false;
                childAt3.setVisibility(z2 ? 0 : 8);
                view = childAt;
            } else if (childAt3.getVisibility() != 8 && (childAt3 instanceof TextView)) {
                view = childAt3;
            }
            i3++;
        }
    }

    public void restoreNotificationHeader(ExpandableNotificationRow expandableNotificationRow) {
        for (int i = 0; i < this.mComparators.size(); i++) {
            this.mComparators.get(i).apply(expandableNotificationRow, true);
        }
        sanitizeHeaderViews(expandableNotificationRow);
    }

    private static class HeaderProcessor {
        private final ResultApplicator mApplicator;
        private boolean mApply;
        private ViewComparator mComparator;
        private final DataExtractor mExtractor;
        private final int mId;
        private Object mParentData;
        private final ExpandableNotificationRow mParentRow;
        private View mParentView;

        public static HeaderProcessor forTextView(ExpandableNotificationRow expandableNotificationRow, int i) {
            return new HeaderProcessor(expandableNotificationRow, i, null, NotificationHeaderUtil.sTextViewComparator, NotificationHeaderUtil.sVisibilityApplicator);
        }

        HeaderProcessor(ExpandableNotificationRow expandableNotificationRow, int i, DataExtractor dataExtractor, ViewComparator viewComparator, ResultApplicator resultApplicator) {
            this.mId = i;
            this.mExtractor = dataExtractor;
            this.mApplicator = resultApplicator;
            this.mComparator = viewComparator;
            this.mParentRow = expandableNotificationRow;
        }

        public void init() {
            this.mParentView = this.mParentRow.getNotificationHeader().findViewById(this.mId);
            this.mParentData = this.mExtractor == null ? null : this.mExtractor.extractData(this.mParentRow);
            this.mApply = !this.mComparator.isEmpty(this.mParentView);
        }

        public void compareToHeader(ExpandableNotificationRow expandableNotificationRow) {
            NotificationHeaderView contractedNotificationHeader;
            if (!this.mApply || (contractedNotificationHeader = expandableNotificationRow.getContractedNotificationHeader()) == null) {
                return;
            }
            this.mApply = this.mComparator.compare(this.mParentView, contractedNotificationHeader.findViewById(this.mId), this.mParentData, this.mExtractor == null ? null : this.mExtractor.extractData(expandableNotificationRow));
        }

        public void apply(ExpandableNotificationRow expandableNotificationRow) {
            apply(expandableNotificationRow, false);
        }

        public void apply(ExpandableNotificationRow expandableNotificationRow, boolean z) {
            boolean z2 = this.mApply && !z;
            if (expandableNotificationRow.isSummaryWithChildren()) {
                applyToView(z2, expandableNotificationRow.getNotificationHeader());
                return;
            }
            applyToView(z2, expandableNotificationRow.getPrivateLayout().getContractedChild());
            applyToView(z2, expandableNotificationRow.getPrivateLayout().getHeadsUpChild());
            applyToView(z2, expandableNotificationRow.getPrivateLayout().getExpandedChild());
        }

        private void applyToView(boolean z, View view) {
            View viewFindViewById;
            if (view != null && (viewFindViewById = view.findViewById(this.mId)) != null && !this.mComparator.isEmpty(viewFindViewById)) {
                this.mApplicator.apply(viewFindViewById, z);
            }
        }
    }

    private static class TextViewComparator implements ViewComparator {
        private TextViewComparator() {
        }

        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
        public boolean compare(View view, View view2, Object obj, Object obj2) {
            return ((TextView) view).getText().equals(((TextView) view2).getText());
        }

        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
        public boolean isEmpty(View view) {
            return TextUtils.isEmpty(((TextView) view).getText());
        }
    }

    private static abstract class IconComparator implements ViewComparator {
        private IconComparator() {
        }

        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
        public boolean compare(View view, View view2, Object obj, Object obj2) {
            return false;
        }

        protected boolean hasSameIcon(Object obj, Object obj2) {
            return ((Notification) obj).getSmallIcon().sameAs(((Notification) obj2).getSmallIcon());
        }

        protected boolean hasSameColor(Object obj, Object obj2) {
            return ((Notification) obj).color == ((Notification) obj2).color;
        }

        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ViewComparator
        public boolean isEmpty(View view) {
            return false;
        }
    }

    private static class VisibilityApplicator implements ResultApplicator {
        private VisibilityApplicator() {
        }

        @Override // com.android.systemui.statusbar.NotificationHeaderUtil.ResultApplicator
        public void apply(View view, boolean z) {
            view.setVisibility(z ? 8 : 0);
        }
    }
}
