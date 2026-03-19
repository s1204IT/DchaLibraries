package com.android.managedprovisioning.preprovisioning.terms;

import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.AccessibilityContextMenuMaker;
import com.android.managedprovisioning.common.HtmlToSpannedParser;
import java.util.List;

class TermsListAdapter extends BaseExpandableListAdapter {
    private final AccessibilityContextMenuMaker mContextMenuMaker;
    private final GroupExpandedInfo mGroupExpandedInfo;
    private final HtmlToSpannedParser mHtmlToSpannedParser;
    private final LayoutInflater mInflater;
    private final List<TermsDocument> mTermsDocuments;

    interface GroupExpandedInfo {
        boolean isGroupExpanded(int i);
    }

    TermsListAdapter(List<TermsDocument> list, LayoutInflater layoutInflater, AccessibilityContextMenuMaker accessibilityContextMenuMaker, HtmlToSpannedParser htmlToSpannedParser, GroupExpandedInfo groupExpandedInfo) {
        this.mTermsDocuments = (List) Preconditions.checkNotNull(list);
        this.mInflater = (LayoutInflater) Preconditions.checkNotNull(layoutInflater);
        this.mHtmlToSpannedParser = (HtmlToSpannedParser) Preconditions.checkNotNull(htmlToSpannedParser);
        this.mGroupExpandedInfo = (GroupExpandedInfo) Preconditions.checkNotNull(groupExpandedInfo);
        this.mContextMenuMaker = (AccessibilityContextMenuMaker) Preconditions.checkNotNull(accessibilityContextMenuMaker);
    }

    @Override
    public int getGroupCount() {
        return this.mTermsDocuments.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return 1;
    }

    @Override
    public TermsDocument getGroup(int i) {
        return getDisclaimer(i);
    }

    @Override
    public TermsDocument getChild(int i, int i2) {
        return getDisclaimer(i);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i2) {
        return i2;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, final boolean z, View view, final ViewGroup viewGroup) {
        String heading = getDisclaimer(i).getHeading();
        if (view == null) {
            view = this.mInflater.inflate(R.layout.terms_disclaimer_header, viewGroup, false);
        }
        view.setContentDescription(viewGroup.getResources().getString(R.string.section_heading, heading));
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view2, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view2, accessibilityNodeInfo);
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.getId(), viewGroup.getResources().getString(z ? R.string.collapse : R.string.expand)));
            }
        });
        ((TextView) view.findViewById(R.id.header_text)).setText(heading);
        ((ImageView) view.findViewById(R.id.chevron)).setRotation(z ? 90.0f : -90.0f);
        view.findViewById(R.id.divider).setVisibility(shouldShowGroupDivider(i) ? 0 : 4);
        return view;
    }

    private boolean shouldShowGroupDivider(int i) {
        return this.mGroupExpandedInfo.isGroupExpanded(i) && (i == 0 || !this.mGroupExpandedInfo.isGroupExpanded(i - 1));
    }

    @Override
    public View getChildView(int i, int i2, boolean z, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(R.layout.terms_disclaimer_content, viewGroup, false);
        }
        TermsDocument disclaimer = getDisclaimer(i);
        TextView textView = (TextView) view.findViewById(R.id.disclaimer_content);
        Spanned html = this.mHtmlToSpannedParser.parseHtml(disclaimer.getContent());
        textView.setText(html);
        textView.setContentDescription(viewGroup.getResources().getString(R.string.section_content, disclaimer.getHeading(), html));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        this.mContextMenuMaker.registerWithActivity(textView);
        return view;
    }

    private TermsDocument getDisclaimer(int i) {
        return this.mTermsDocuments.get(i);
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return false;
    }
}
