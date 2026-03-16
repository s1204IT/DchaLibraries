package com.android.contacts.common.list;

import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.ShortcutIntentBuilder;
import com.android.contacts.common.util.AccountFilterUtil;

public class PhoneNumberPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> implements ShortcutIntentBuilder.OnShortcutIntentCreatedListener {
    private static final String TAG = PhoneNumberPickerFragment.class.getSimpleName();
    private View mAccountFilterHeader;
    private ContactListFilter mFilter;
    private OnPhoneNumberPickerActionListener mListener;
    private boolean mLoaderStarted;
    private View mPaddingView;
    private String mShortcutAction;
    private boolean mUseCallableUri;
    private ContactListItemView.PhotoPosition mPhotoPosition = ContactListItemView.getDefaultPhotoPosition(false);
    private View.OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    private class FilterHeaderClickListener implements View.OnClickListener {
        private FilterHeaderClickListener() {
        }

        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(PhoneNumberPickerFragment.this, 1, PhoneNumberPickerFragment.this.mFilter);
        }
    }

    public PhoneNumberPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(0);
        setHasOptionsMenu(true);
    }

    public void setOnPhoneNumberPickerActionListener(OnPhoneNumberPickerActionListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        View paddingView = inflater.inflate(R.layout.contact_detail_list_padding, (ViewGroup) null, false);
        this.mPaddingView = paddingView.findViewById(R.id.contact_detail_list_padding);
        getListView().addHeaderView(paddingView);
        this.mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        this.mAccountFilterHeader.setOnClickListener(this.mFilterHeaderClickListener);
        updateFilterHeaderView();
        setVisibleScrollbarEnabled(getVisibleScrollbarEnabled());
    }

    protected boolean getVisibleScrollbarEnabled() {
        return true;
    }

    @Override
    protected void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        updateFilterHeaderView();
    }

    private void updateFilterHeaderView() {
        ContactListFilter filter = getFilter();
        if (this.mAccountFilterHeader != null && filter != null) {
            boolean shouldShowHeader = !isSearchMode() && AccountFilterUtil.updateAccountFilterTitleForPhone(this.mAccountFilterHeader, filter, false);
            if (shouldShowHeader) {
                this.mPaddingView.setVisibility(8);
                this.mAccountFilterHeader.setVisibility(0);
            } else {
                this.mPaddingView.setVisibility(0);
                this.mAccountFilterHeader.setVisibility(8);
            }
        }
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);
        if (savedState != null) {
            this.mFilter = (ContactListFilter) savedState.getParcelable("filter");
            this.mShortcutAction = savedState.getString("shortcutAction");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("filter", this.mFilter);
        outState.putString("shortcutAction", this.mShortcutAction);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        if (this.mListener != null) {
            this.mListener.onHomeInActionBarSelected();
        }
        return true;
    }

    public void setShortcutAction(String shortcutAction) {
        this.mShortcutAction = shortcutAction;
    }

    @Override
    protected void onItemClick(int position, long id) {
        Uri phoneUri = getPhoneUri(position);
        if (phoneUri != null) {
            pickPhoneNumber(phoneUri);
            return;
        }
        String number = getPhoneNumber(position);
        if (!TextUtils.isEmpty(number)) {
            cacheContactInfo(position);
            this.mListener.onCallNumberDirectly(number);
        } else {
            Log.w(TAG, "Item at " + position + " was clicked before adapter is ready. Ignoring");
        }
    }

    protected void cacheContactInfo(int position) {
    }

    protected String getPhoneNumber(int position) {
        PhoneNumberListAdapter adapter = (PhoneNumberListAdapter) getAdapter();
        return adapter.getPhoneNumber(position);
    }

    protected Uri getPhoneUri(int position) {
        PhoneNumberListAdapter adapter = (PhoneNumberListAdapter) getAdapter();
        return adapter.getDataUri(position);
    }

    @Override
    protected void startLoading() {
        this.mLoaderStarted = true;
        super.startLoading();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        setVisibleScrollbarEnabled(data != null && data.getCount() > 0);
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        PhoneNumberListAdapter adapter = new PhoneNumberListAdapter(getActivity());
        adapter.setDisplayPhotos(true);
        adapter.setUseCallableUri(this.mUseCallableUri);
        return adapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        ContactEntryListAdapter adapter = getAdapter();
        if (adapter != null) {
            if (!isSearchMode() && this.mFilter != null) {
                adapter.setFilter(this.mFilter);
            }
            setPhotoPosition(adapter);
        }
    }

    protected void setPhotoPosition(ContactEntryListAdapter adapter) {
        ((PhoneNumberListAdapter) adapter).setPhotoPosition(this.mPhotoPosition);
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    public void pickPhoneNumber(Uri uri) {
        if (this.mShortcutAction == null) {
            this.mListener.onPickPhoneNumberAction(uri);
        } else {
            startPhoneNumberShortcutIntent(uri);
        }
    }

    protected void startPhoneNumberShortcutIntent(Uri uri) {
        ShortcutIntentBuilder builder = new ShortcutIntentBuilder(getActivity(), this);
        builder.createPhoneNumberShortcutIntent(uri, this.mShortcutAction);
    }

    @Override
    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
        this.mListener.onShortcutIntentCreated(shortcutIntent);
    }

    @Override
    public void onPickerResult(Intent data) {
        this.mListener.onPickPhoneNumberAction(data.getData());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(ContactListFilterController.getInstance(getActivity()), resultCode, data);
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    public ContactListFilter getFilter() {
        return this.mFilter;
    }
}
