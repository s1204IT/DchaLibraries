package com.mediatek.contacts.ext;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

public interface IViewCustomExtension {

    public interface ContactListItemViewCustom {
        void addCustomView(long j, ViewGroup viewGroup);

        int getWidthWithPadding();

        void onLayout(boolean z, int i, int i2, int i3, int i4);

        void onMeasure(int i, int i2);
    }

    public interface QuickContactCardViewCustom {
        View createCardView(View view, View view2, Uri uri, Context context);
    }

    public interface QuickContactScrollerCustom {
        View createJoynIconView(View view, View view2, Uri uri);

        void updateJoynIconView();
    }

    ContactListItemViewCustom getContactListItemViewCustom();

    QuickContactCardViewCustom getQuickContactCardViewCustom();

    QuickContactScrollerCustom getQuickContactScrollerCustom();
}
