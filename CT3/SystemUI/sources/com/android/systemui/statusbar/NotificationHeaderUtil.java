package com.android.systemui.statusbar;

import android.R;
import android.app.Notification;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.view.NotificationHeaderView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NotificationHeaderUtil {
    private final ArrayList<HeaderProcessor> mComparators = new ArrayList<>();
    private final HashSet<Integer> mDividers = new HashSet<>();
    private final ExpandableNotificationRow mRow;
    private static final TextViewComparator sTextViewComparator = new TextViewComparator(null);
    private static final VisibilityApplicator sVisibilityApplicator = new VisibilityApplicator(0 == true ? 1 : 0);
    private static final DataExtractor sIconExtractor = new DataExtractor() {
        @Override
        public Object extractData(ExpandableNotificationRow row) {
            return row.getStatusBarNotification().getNotification();
        }
    };
    private static final IconComparator sIconVisibilityComparator = new IconComparator() {
        @Override
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            if (hasSameIcon(parentData, childData)) {
                return hasSameColor(parentData, childData);
            }
            return false;
        }
    };
    private static final IconComparator sGreyComparator = new IconComparator() {
        @Override
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            if (hasSameIcon(parentData, childData)) {
                return hasSameColor(parentData, childData);
            }
            return true;
        }
    };
    private static final ResultApplicator mGreyApplicator = new ResultApplicator() {
        @Override
        public void apply(View view, boolean apply) {
            NotificationHeaderView header = (NotificationHeaderView) view;
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            ImageView expand = (ImageView) view.findViewById(R.id.label_hour);
            applyToChild(icon, apply, header.getOriginalIconColor());
            applyToChild(expand, apply, header.getOriginalNotificationColor());
        }

        private void applyToChild(View view, boolean shouldApply, int originalColor) {
            if (originalColor == -1) {
                return;
            }
            ImageView imageView = (ImageView) view;
            imageView.getDrawable().mutate();
            if (shouldApply) {
                int grey = view.getContext().getColor(R.color.system_accent3_0);
                imageView.getDrawable().setColorFilter(grey, PorterDuff.Mode.SRC_ATOP);
            } else {
                imageView.getDrawable().setColorFilter(originalColor, PorterDuff.Mode.SRC_ATOP);
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

    public NotificationHeaderUtil(ExpandableNotificationRow row) {
        this.mRow = row;
        this.mComparators.add(new HeaderProcessor(this.mRow, R.id.icon, sIconExtractor, sIconVisibilityComparator, sVisibilityApplicator));
        this.mComparators.add(new HeaderProcessor(this.mRow, R.id.keyguard, sIconExtractor, sGreyComparator, mGreyApplicator));
        this.mComparators.add(new HeaderProcessor(this.mRow, R.id.label_minute, null, new ViewComparator() {
            @Override
            public boolean compare(View parent, View child, Object parentData, Object childData) {
                return parent.getVisibility() != 8;
            }

            @Override
            public boolean isEmpty(View view) {
                return (view instanceof ImageView) && ((ImageView) view).getDrawable() == null;
            }
        }, sVisibilityApplicator));
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, R.id.keyguard_click_area));
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, R.id.knownSigner));
        this.mDividers.add(Integer.valueOf(R.id.keyguard_message_area));
        this.mDividers.add(Integer.valueOf(R.id.label));
    }

    public void updateChildrenHeaderAppearance() {
        List<ExpandableNotificationRow> notificationChildren = this.mRow.getNotificationChildren();
        if (notificationChildren == null) {
            return;
        }
        for (int compI = 0; compI < this.mComparators.size(); compI++) {
            this.mComparators.get(compI).init();
        }
        for (int i = 0; i < notificationChildren.size(); i++) {
            ExpandableNotificationRow row = notificationChildren.get(i);
            for (int compI2 = 0; compI2 < this.mComparators.size(); compI2++) {
                this.mComparators.get(compI2).compareToHeader(row);
            }
        }
        for (int i2 = 0; i2 < notificationChildren.size(); i2++) {
            ExpandableNotificationRow row2 = notificationChildren.get(i2);
            for (int compI3 = 0; compI3 < this.mComparators.size(); compI3++) {
                this.mComparators.get(compI3).apply(row2);
            }
            sanitizeHeaderViews(row2);
        }
    }

    private void sanitizeHeaderViews(ExpandableNotificationRow row) {
        if (row.isSummaryWithChildren()) {
            sanitizeHeader(row.getNotificationHeader());
            return;
        }
        NotificationContentView layout = row.getPrivateLayout();
        sanitizeChild(layout.getContractedChild());
        sanitizeChild(layout.getHeadsUpChild());
        sanitizeChild(layout.getExpandedChild());
    }

    private void sanitizeChild(View child) {
        if (child == null) {
            return;
        }
        NotificationHeaderView header = (NotificationHeaderView) child.findViewById(R.id.keyguard);
        sanitizeHeader(header);
    }

    private void sanitizeHeader(NotificationHeaderView rowHeader) {
        if (rowHeader == null) {
            return;
        }
        int childCount = rowHeader.getChildCount();
        View time = rowHeader.findViewById(R.id.KEYCODE_BUTTON_L1);
        boolean hasVisibleText = false;
        int i = 1;
        while (true) {
            if (i >= childCount - 1) {
                break;
            }
            View child = rowHeader.getChildAt(i);
            if (!(child instanceof TextView) || child.getVisibility() == 8 || this.mDividers.contains(Integer.valueOf(child.getId())) || child == time) {
                i++;
            } else {
                hasVisibleText = true;
                break;
            }
        }
        int timeVisibility = (!hasVisibleText || this.mRow.getStatusBarNotification().getNotification().showsTime()) ? 0 : 8;
        time.setVisibility(timeVisibility);
        View left = null;
        int i2 = 1;
        while (i2 < childCount - 1) {
            View child2 = rowHeader.getChildAt(i2);
            if (this.mDividers.contains(Integer.valueOf(child2.getId()))) {
                boolean visible = false;
                while (true) {
                    i2++;
                    if (i2 >= childCount - 1) {
                        break;
                    }
                    View right = rowHeader.getChildAt(i2);
                    if (this.mDividers.contains(Integer.valueOf(right.getId()))) {
                        i2--;
                        break;
                    } else if (right.getVisibility() != 8 && (right instanceof TextView)) {
                        visible = left != null;
                        left = right;
                    }
                }
                child2.setVisibility(visible ? 0 : 8);
            } else if (child2.getVisibility() != 8 && (child2 instanceof TextView)) {
                left = child2;
            }
            i2++;
        }
    }

    public void restoreNotificationHeader(ExpandableNotificationRow row) {
        for (int compI = 0; compI < this.mComparators.size(); compI++) {
            this.mComparators.get(compI).apply(row, true);
        }
        sanitizeHeaderViews(row);
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

        public static HeaderProcessor forTextView(ExpandableNotificationRow row, int id) {
            return new HeaderProcessor(row, id, null, NotificationHeaderUtil.sTextViewComparator, NotificationHeaderUtil.sVisibilityApplicator);
        }

        HeaderProcessor(ExpandableNotificationRow row, int id, DataExtractor extractor, ViewComparator comparator, ResultApplicator applicator) {
            this.mId = id;
            this.mExtractor = extractor;
            this.mApplicator = applicator;
            this.mComparator = comparator;
            this.mParentRow = row;
        }

        public void init() {
            this.mParentView = this.mParentRow.getNotificationHeader().findViewById(this.mId);
            this.mParentData = this.mExtractor != null ? this.mExtractor.extractData(this.mParentRow) : null;
            this.mApply = !this.mComparator.isEmpty(this.mParentView);
        }

        public void compareToHeader(ExpandableNotificationRow row) {
            if (!this.mApply) {
                return;
            }
            NotificationHeaderView header = row.getNotificationHeader();
            if (header == null) {
                this.mApply = false;
            } else {
                this.mApply = this.mComparator.compare(this.mParentView, header.findViewById(this.mId), this.mParentData, this.mExtractor == null ? null : this.mExtractor.extractData(row));
            }
        }

        public void apply(ExpandableNotificationRow row) {
            apply(row, false);
        }

        public void apply(ExpandableNotificationRow row, boolean reset) {
            boolean apply = this.mApply && !reset;
            if (row.isSummaryWithChildren()) {
                applyToView(apply, row.getNotificationHeader());
                return;
            }
            applyToView(apply, row.getPrivateLayout().getContractedChild());
            applyToView(apply, row.getPrivateLayout().getHeadsUpChild());
            applyToView(apply, row.getPrivateLayout().getExpandedChild());
        }

        private void applyToView(boolean apply, View parent) {
            View view;
            if (parent == null || (view = parent.findViewById(this.mId)) == null || this.mComparator.isEmpty(view)) {
                return;
            }
            this.mApplicator.apply(view, apply);
        }
    }

    private static class TextViewComparator implements ViewComparator {
        TextViewComparator(TextViewComparator textViewComparator) {
            this();
        }

        private TextViewComparator() {
        }

        @Override
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            TextView parentView = (TextView) parent;
            TextView childView = (TextView) child;
            return parentView.getText().equals(childView.getText());
        }

        @Override
        public boolean isEmpty(View view) {
            return TextUtils.isEmpty(((TextView) view).getText());
        }
    }

    private static abstract class IconComparator implements ViewComparator {
        IconComparator(IconComparator iconComparator) {
            this();
        }

        private IconComparator() {
        }

        @Override
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            return false;
        }

        protected boolean hasSameIcon(Object parentData, Object childData) {
            Icon parentIcon = ((Notification) parentData).getSmallIcon();
            Icon childIcon = ((Notification) childData).getSmallIcon();
            return parentIcon.sameAs(childIcon);
        }

        protected boolean hasSameColor(Object parentData, Object childData) {
            int parentColor = ((Notification) parentData).color;
            int childColor = ((Notification) childData).color;
            return parentColor == childColor;
        }

        @Override
        public boolean isEmpty(View view) {
            return false;
        }
    }

    private static class VisibilityApplicator implements ResultApplicator {
        VisibilityApplicator(VisibilityApplicator visibilityApplicator) {
            this();
        }

        private VisibilityApplicator() {
        }

        @Override
        public void apply(View view, boolean apply) {
            view.setVisibility(apply ? 8 : 0);
        }
    }
}
