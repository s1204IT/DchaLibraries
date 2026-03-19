package com.android.contacts.list;

import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.ShortcutIntentBuilder;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.PhoneNumberListAdapter;
import com.mediatek.contacts.util.Log;

public class PhoneNumberPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> implements ShortcutIntentBuilder.OnShortcutIntentCreatedListener, PhoneNumberListAdapter.Listener {
    private ContactListFilter mFilter;
    private OnPhoneNumberPickerActionListener mListener;
    private boolean mLoaderStarted;
    private ContactListItemView.PhotoPosition mPhotoPosition = ContactListItemView.getDefaultPhotoPosition(false);
    private String mShortcutAction;
    private boolean mUseCallableUri;

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        onLoadFinished((Loader<Cursor>) loader, (Cursor) obj);
    }

    @Override
    public void onVideoCallIconClicked(int i) {
        callNumber(i, true, new long[0]);
    }

    public PhoneNumberPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(0);
        setHasOptionsMenu(true);
    }

    public void setOnPhoneNumberPickerActionListener(OnPhoneNumberPickerActionListener onPhoneNumberPickerActionListener) {
        this.mListener = onPhoneNumberPickerActionListener;
    }

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        setVisibleScrollbarEnabled(getVisibleScrollbarEnabled());
    }

    protected boolean getVisibleScrollbarEnabled() {
        return true;
    }

    @Override
    public void restoreSavedState(Bundle bundle) {
        super.restoreSavedState(bundle);
        if (bundle == null) {
            return;
        }
        this.mFilter = (ContactListFilter) bundle.getParcelable("filter");
        this.mShortcutAction = bundle.getString("shortcutAction");
        this.mUseCallableUri = bundle.getBoolean("isCallableUri");
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("filter", this.mFilter);
        bundle.putString("shortcutAction", this.mShortcutAction);
        bundle.putBoolean("isCallableUri", this.mUseCallableUri);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            if (this.mListener != null) {
                this.mListener.onHomeInActionBarSelected();
                return true;
            }
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void setShortcutAction(String str) {
        this.mShortcutAction = str;
    }

    @Override
    protected void onItemClick(int i, long j) {
        callNumber(i, false, j);
    }

    private void callNumber(int i, boolean z, long... jArr) {
        Uri phoneUri = getPhoneUri(i);
        if (phoneUri != null && (jArr.length == 0 || jArr[0] >= 0)) {
            pickPhoneNumber(phoneUri, z);
            return;
        }
        String phoneNumber = getPhoneNumber(i);
        if (!TextUtils.isEmpty(phoneNumber)) {
            cacheContactInfo(i);
            this.mListener.onPickPhoneNumber(phoneNumber, z, getCallInitiationType(true));
            return;
        }
        Log.w("PhoneNumberPicker", "Item at " + i + " was clicked before adapter is ready. Ignoring");
    }

    protected void cacheContactInfo(int i) {
    }

    protected String getPhoneNumber(int i) {
        return ((PhoneNumberListAdapter) getAdapter()).getPhoneNumber(i);
    }

    protected Uri getPhoneUri(int i) {
        return ((PhoneNumberListAdapter) getAdapter()).getDataUri(i);
    }

    @Override
    protected void startLoading() {
        this.mLoaderStarted = true;
        super.startLoading();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        super.onLoadFinished(loader, cursor);
        setVisibleScrollbarEnabled((cursor == null || cursor.isClosed() || cursor.getCount() <= 0) ? false : true);
    }

    public void setUseCallableUri(boolean z) {
        this.mUseCallableUri = z;
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        PhoneNumberListAdapter phoneNumberListAdapter = new PhoneNumberListAdapter(getActivity());
        phoneNumberListAdapter.setDisplayPhotos(true);
        phoneNumberListAdapter.setUseCallableUri(this.mUseCallableUri);
        return phoneNumberListAdapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (!isSearchMode() && this.mFilter != null) {
            adapter.setFilter(this.mFilter);
        }
        setPhotoPosition(adapter);
    }

    protected void setPhotoPosition(ContactEntryListAdapter contactEntryListAdapter) {
        ((PhoneNumberListAdapter) contactEntryListAdapter).setPhotoPosition(this.mPhotoPosition);
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    public void pickPhoneNumber(Uri uri, boolean z) {
        if (this.mShortcutAction == null) {
            this.mListener.onPickDataUri(uri, z, getCallInitiationType(false));
        } else {
            startPhoneNumberShortcutIntent(uri, z);
        }
    }

    protected void startPhoneNumberShortcutIntent(Uri uri, boolean z) {
        new ShortcutIntentBuilder(getActivity(), this).createPhoneNumberShortcutIntent(uri, this.mShortcutAction);
    }

    @Override
    public void onShortcutIntentCreated(Uri uri, Intent intent) {
        this.mListener.onShortcutIntentCreated(intent);
    }

    @Override
    public void onPickerResult(Intent intent) {
        this.mListener.onPickDataUri(intent.getData(), false, getCallInitiationType(false));
    }

    protected int getCallInitiationType(boolean z) {
        return 0;
    }
}
