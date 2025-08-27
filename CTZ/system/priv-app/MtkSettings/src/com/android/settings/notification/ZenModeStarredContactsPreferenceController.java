package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.ListFormatter;
import android.provider.ContactsContract;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class ZenModeStarredContactsPreferenceController extends AbstractZenModePreferenceController implements Preference.OnPreferenceClickListener {

    @VisibleForTesting
    Intent mFallbackIntent;
    private final PackageManager mPackageManager;
    private Preference mPreference;
    private final int mPriorityCategory;

    @VisibleForTesting
    Intent mStarredContactsIntent;

    public ZenModeStarredContactsPreferenceController(Context context, Lifecycle lifecycle, int i) {
        super(context, "zen_mode_starred_contacts", lifecycle);
        this.mPriorityCategory = i;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mStarredContactsIntent = new Intent("com.android.contacts.action.LIST_STARRED");
        this.mFallbackIntent = new Intent("android.intent.action.MAIN");
        this.mFallbackIntent.addCategory("android.intent.category.APP_CONTACTS");
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference("zen_mode_starred_contacts");
        this.mPreference.setOnPreferenceClickListener(this);
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_mode_starred_contacts";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (this.mPriorityCategory == 8) {
            return this.mBackend.isPriorityCategoryEnabled(8) && this.mBackend.getPriorityCallSenders() == 2 && isIntentValid();
        }
        if (this.mPriorityCategory == 4) {
            return this.mBackend.isPriorityCategoryEnabled(4) && this.mBackend.getPriorityMessageSenders() == 2 && isIntentValid();
        }
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        List<String> starredContacts = getStarredContacts();
        int size = starredContacts.size();
        ArrayList arrayList = new ArrayList();
        if (size == 0) {
            arrayList.add(this.mContext.getString(R.string.zen_mode_from_none));
        } else {
            for (int i = 0; i < 2 && i < size; i++) {
                arrayList.add(starredContacts.get(i));
            }
            if (size == 3) {
                arrayList.add(starredContacts.get(2));
            } else if (size > 2) {
                int i2 = size - 2;
                arrayList.add(this.mContext.getResources().getQuantityString(R.plurals.zen_mode_starred_contacts_summary_additional_contacts, i2, Integer.valueOf(i2)));
            }
        }
        this.mPreference.setSummary(ListFormatter.getInstance().format(arrayList));
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        if (this.mStarredContactsIntent.resolveActivity(this.mPackageManager) != null) {
            this.mContext.startActivity(this.mStarredContactsIntent);
            return true;
        }
        this.mContext.startActivity(this.mFallbackIntent);
        return true;
    }

    private List<String> getStarredContacts() {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = this.mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[]{"display_name"}, "starred=1", null, "times_contacted");
        if (cursorQuery.moveToFirst()) {
            do {
                arrayList.add(cursorQuery.getString(0));
            } while (cursorQuery.moveToNext());
        }
        return arrayList;
    }

    private boolean isIntentValid() {
        return (this.mStarredContactsIntent.resolveActivity(this.mPackageManager) == null && this.mFallbackIntent.resolveActivity(this.mPackageManager) == null) ? false : true;
    }
}
