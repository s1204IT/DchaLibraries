package com.mediatek.contacts.ext;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import com.android.contacts.model.RawContact;
import com.android.contacts.model.RawContactDelta;
import com.google.common.collect.ImmutableList;
import com.mediatek.contacts.ext.IRcsExtension;

public class DefaultRcsExtension implements IRcsExtension {
    private IRcsExtension.ContactListItemRcsView mContactListItemRcsView = new IRcsExtension.ContactListItemRcsView() {
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
    private IRcsExtension.QuickContactRcsScroller mQuickContactScrollerCustom = new IRcsExtension.QuickContactRcsScroller() {
        @Override
        public View createRcsIconView(View view, View view2, Uri uri) {
            return null;
        }

        @Override
        public void updateRcsContact(Uri uri, boolean z) {
        }

        @Override
        public void updateRcsIconView() {
        }
    };

    @Override
    public void getIntentData(Intent intent, Fragment fragment) {
    }

    @Override
    public void setListFilter(StringBuilder sb, Context context) {
    }

    @Override
    public Uri configListUri(Uri uri) {
        return uri;
    }

    @Override
    public void addListMenuOptions(Context context, Menu menu, MenuItem menuItem, Fragment fragment) {
    }

    @Override
    public void getGroupListResult(Fragment fragment, long[] jArr) {
    }

    @Override
    public void addGroupMenuOptions(Menu menu, Context context) {
    }

    @Override
    public void addGroupDetailMenuOptions(Menu menu, long j, Context context, int i) {
    }

    @Override
    public void setTextChangedListener(RawContactDelta rawContactDelta, EditText editText, int i, String str) {
    }

    @Override
    public void closeTextChangedListener(boolean z) {
    }

    @Override
    public void setEditorFragment(Fragment fragment, FragmentManager fragmentManager) {
    }

    @Override
    public void addEditorMenuOptions(Fragment fragment, Menu menu, boolean z) {
    }

    @Override
    public void addPeopleMenuOptions(Menu menu) {
    }

    @Override
    public boolean addRcsProfileEntryListener(Uri uri, boolean z) {
        return false;
    }

    @Override
    public void createPublicAccountEntryView(ListView listView) {
    }

    @Override
    public void createEntryView(ListView listView, Context context) {
    }

    @Override
    public void updateContactPhotoFromRcsServer(Uri uri, ImageView imageView, Context context) {
    }

    @Override
    public ImmutableList<RawContact> rcsConfigureRawContacts(ImmutableList<RawContact> immutableList, boolean z) {
        return immutableList;
    }

    @Override
    public boolean needUpdateContactPhoto(boolean z, boolean z2) {
        return z2;
    }

    @Override
    public void addQuickContactMenuOptions(Menu menu, Uri uri, Context context) {
    }

    @Override
    public IRcsExtension.ContactListItemRcsView getContactListItemRcsView() {
        return this.mContactListItemRcsView;
    }

    @Override
    public IRcsExtension.QuickContactRcsScroller getQuickContactRcsScroller() {
        return this.mQuickContactScrollerCustom;
    }

    @Override
    public boolean isRcsServiceAvailable() {
        return false;
    }
}
