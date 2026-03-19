package com.mediatek.contacts.ext;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.contacts.ext.IViewCustomExtension;

public class DefaultViewCustomExtension implements IViewCustomExtension {
    private IViewCustomExtension.ContactListItemViewCustom mContactListItemViewCustom = new IViewCustomExtension.ContactListItemViewCustom() {
        @Override
        public void onMeasure(int i, int i2) {
        }

        @Override
        public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        }

        @Override
        public void addCustomView(long j, ViewGroup viewGroup) {
        }

        @Override
        public int getWidthWithPadding() {
            return 0;
        }
    };
    private IViewCustomExtension.QuickContactCardViewCustom mQuickContactCardViewCustom = new IViewCustomExtension.QuickContactCardViewCustom() {
        @Override
        public View createCardView(View view, View view2, Uri uri, Context context) {
            return null;
        }
    };
    private IViewCustomExtension.QuickContactScrollerCustom mQuickContactScrollerCustom = new IViewCustomExtension.QuickContactScrollerCustom() {
        @Override
        public View createJoynIconView(View view, View view2, Uri uri) {
            return null;
        }

        @Override
        public void updateJoynIconView() {
        }
    };

    @Override
    public IViewCustomExtension.ContactListItemViewCustom getContactListItemViewCustom() {
        return this.mContactListItemViewCustom;
    }

    @Override
    public IViewCustomExtension.QuickContactCardViewCustom getQuickContactCardViewCustom() {
        return this.mQuickContactCardViewCustom;
    }

    @Override
    public IViewCustomExtension.QuickContactScrollerCustom getQuickContactScrollerCustom() {
        return this.mQuickContactScrollerCustom;
    }
}
