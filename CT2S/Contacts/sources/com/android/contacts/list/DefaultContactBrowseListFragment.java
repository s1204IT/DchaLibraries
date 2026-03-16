package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.DefaultContactListAdapter;
import com.android.contacts.common.list.ProfileAndContactsLoader;
import com.android.contacts.common.util.AccountFilterUtil;

public class DefaultContactBrowseListFragment extends ContactBrowseListFragment {
    private static final String TAG = DefaultContactBrowseListFragment.class.getSimpleName();
    private View mAccountFilterHeader;
    private View.OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();
    private View mProfileHeader;
    private FrameLayout mProfileHeaderContainer;
    private Button mProfileMessage;
    private TextView mProfileTitle;
    private View mSearchHeaderView;
    private View mSearchProgress;
    private TextView mSearchProgressText;

    private class FilterHeaderClickListener implements View.OnClickListener {
        private FilterHeaderClickListener() {
        }

        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(DefaultContactBrowseListFragment.this, 1, DefaultContactBrowseListFragment.this.getFilter());
        }
    }

    public DefaultContactBrowseListFragment() {
        setPhotoLoaderEnabled(true);
        setQuickContactEnabled(false);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
    }

    @Override
    public CursorLoader createCursorLoader(Context context) {
        return new ProfileAndContactsLoader(context);
    }

    @Override
    protected void onItemClick(int position, long id) {
        Uri uri = getAdapter().getContactUri(position);
        if (uri != null) {
            viewContact(uri);
        }
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter adapter = new DefaultContactListAdapter(getContext());
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(true);
        adapter.setPhotoPosition(ContactListItemView.getDefaultPhotoPosition(false));
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        this.mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        this.mAccountFilterHeader.setOnClickListener(this.mFilterHeaderClickListener);
        addEmptyUserProfileHeader(inflater);
        showEmptyUserProfile(false);
        FrameLayout headerContainer = new FrameLayout(inflater.getContext());
        this.mSearchHeaderView = inflater.inflate(R.layout.search_header, (ViewGroup) null, false);
        headerContainer.addView(this.mSearchHeaderView);
        getListView().addHeaderView(headerContainer, null, false);
        checkHeaderViewVisibility();
        this.mSearchProgress = getView().findViewById(R.id.search_progress);
        this.mSearchProgressText = (TextView) this.mSearchHeaderView.findViewById(R.id.totalContactsText);
    }

    @Override
    protected void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        checkHeaderViewVisibility();
        if (!flag) {
            showSearchProgress(false);
        }
    }

    private void showSearchProgress(boolean show) {
        if (this.mSearchProgress != null) {
            this.mSearchProgress.setVisibility(show ? 0 : 8);
        }
    }

    private void checkHeaderViewVisibility() {
        updateFilterHeaderView();
        if (this.mSearchHeaderView != null) {
            this.mSearchHeaderView.setVisibility(8);
        }
    }

    @Override
    public void setFilter(ContactListFilter filter) {
        super.setFilter(filter);
        updateFilterHeaderView();
    }

    private void updateFilterHeaderView() {
        if (this.mAccountFilterHeader != null) {
            ContactListFilter filter = getFilter();
            if (filter != null && !isSearchMode()) {
                boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPeople(this.mAccountFilterHeader, filter, false);
                this.mAccountFilterHeader.setVisibility(shouldShowHeader ? 0 : 8);
            } else {
                this.mAccountFilterHeader.setVisibility(8);
            }
        }
    }

    @Override
    protected void setProfileHeader() {
        ContactListAdapter adapter;
        this.mUserProfileExists = getAdapter().hasProfile();
        showEmptyUserProfile((this.mUserProfileExists || isSearchMode()) ? false : true);
        if (isSearchMode() && (adapter = getAdapter()) != null) {
            if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
                this.mSearchHeaderView.setVisibility(8);
                showSearchProgress(false);
            } else {
                this.mSearchHeaderView.setVisibility(0);
                if (adapter.isLoading()) {
                    this.mSearchProgressText.setText(R.string.search_results_searching);
                    showSearchProgress(true);
                } else {
                    this.mSearchProgressText.setText(R.string.listFoundAllContactsZero);
                    this.mSearchProgressText.sendAccessibilityEvent(4);
                    showSearchProgress(false);
                }
            }
            showEmptyUserProfile(false);
        }
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

    private void showEmptyUserProfile(boolean show) {
        this.mProfileHeaderContainer.setVisibility(show ? 0 : 8);
        this.mProfileHeader.setVisibility(show ? 0 : 8);
        this.mProfileTitle.setVisibility(show ? 0 : 8);
        this.mProfileMessage.setVisibility(show ? 0 : 8);
    }

    private void addEmptyUserProfileHeader(LayoutInflater inflater) {
        ListView list = getListView();
        this.mProfileHeader = inflater.inflate(R.layout.user_profile_header, (ViewGroup) null, false);
        this.mProfileTitle = (TextView) this.mProfileHeader.findViewById(R.id.profile_title);
        this.mProfileHeaderContainer = new FrameLayout(inflater.getContext());
        this.mProfileHeaderContainer.addView(this.mProfileHeader);
        list.addHeaderView(this.mProfileHeaderContainer, null, false);
        this.mProfileMessage = (Button) this.mProfileHeader.findViewById(R.id.user_profile_button);
        this.mProfileMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.INSERT", ContactsContract.Contacts.CONTENT_URI);
                intent.putExtra("newLocalProfile", true);
                DefaultContactBrowseListFragment.this.startActivity(intent);
            }
        });
    }
}
