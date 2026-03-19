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

public interface IRcsExtension {

    public interface ContactListItemRcsView {
        void addCustomView(long j, ViewGroup viewGroup);

        int getWidthWithPadding();

        void onLayout(boolean z, int i, int i2, int i3, int i4);

        void onMeasure(int i, int i2);
    }

    public interface QuickContactRcsScroller {
        View createRcsIconView(View view, View view2, Uri uri);

        void updateRcsContact(Uri uri, boolean z);

        void updateRcsIconView();
    }

    void addEditorMenuOptions(Fragment fragment, Menu menu, boolean z);

    void addGroupDetailMenuOptions(Menu menu, long j, Context context, int i);

    void addGroupMenuOptions(Menu menu, Context context);

    void addListMenuOptions(Context context, Menu menu, MenuItem menuItem, Fragment fragment);

    void addPeopleMenuOptions(Menu menu);

    void addQuickContactMenuOptions(Menu menu, Uri uri, Context context);

    boolean addRcsProfileEntryListener(Uri uri, boolean z);

    void closeTextChangedListener(boolean z);

    Uri configListUri(Uri uri);

    void createEntryView(ListView listView, Context context);

    void createPublicAccountEntryView(ListView listView);

    ContactListItemRcsView getContactListItemRcsView();

    void getGroupListResult(Fragment fragment, long[] jArr);

    void getIntentData(Intent intent, Fragment fragment);

    QuickContactRcsScroller getQuickContactRcsScroller();

    boolean isRcsServiceAvailable();

    boolean needUpdateContactPhoto(boolean z, boolean z2);

    ImmutableList<RawContact> rcsConfigureRawContacts(ImmutableList<RawContact> immutableList, boolean z);

    void setEditorFragment(Fragment fragment, FragmentManager fragmentManager);

    void setListFilter(StringBuilder sb, Context context);

    void setTextChangedListener(RawContactDelta rawContactDelta, EditText editText, int i, String str);

    void updateContactPhotoFromRcsServer(Uri uri, ImageView imageView, Context context);
}
