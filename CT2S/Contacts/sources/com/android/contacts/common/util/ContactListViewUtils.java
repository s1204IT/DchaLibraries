package com.android.contacts.common.util;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;
import com.android.contacts.R;

public class ContactListViewUtils {
    private static void addPaddingToView(ListView listView, int parentWidth, int listSpaceWeight, int listViewWeight) {
        if (listSpaceWeight > 0 && listViewWeight > 0) {
            double paddingPercent = ((double) listSpaceWeight) / ((double) ((listSpaceWeight * 2) + listViewWeight));
            listView.setPadding((int) (((double) parentWidth) * paddingPercent * 1.1d), listView.getPaddingTop(), (int) (((double) parentWidth) * paddingPercent * 1.1d), listView.getPaddingBottom());
            listView.setClipToPadding(false);
            listView.setScrollBarStyle(33554432);
        }
    }

    public static void applyCardPaddingToView(Resources resources, final ListView listView, final View rootLayout) {
        final int listSpaceWeight = resources.getInteger(R.integer.contact_list_space_layout_weight);
        final int listViewWeight = resources.getInteger(R.integer.contact_list_card_layout_weight);
        if (listSpaceWeight > 0 && listViewWeight > 0) {
            rootLayout.setBackgroundResource(0);
            View mCardView = rootLayout.findViewById(R.id.list_card);
            if (mCardView == null) {
                throw new RuntimeException("Your content must have a list card view who can be turned visible whenever it is necessary.");
            }
            mCardView.setVisibility(0);
            SchedulingUtils.doOnPreDraw(listView, false, new Runnable() {
                @Override
                public void run() {
                    ContactListViewUtils.addPaddingToView(listView, rootLayout.getWidth(), listSpaceWeight, listViewWeight);
                }
            });
        }
    }
}
