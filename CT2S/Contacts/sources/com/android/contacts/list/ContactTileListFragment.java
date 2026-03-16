package com.android.contacts.list;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactTileLoaderFactory;
import com.android.contacts.common.list.ContactTileAdapter;
import com.android.contacts.common.list.ContactTileView;
import com.android.contacts.common.util.ContactListViewUtils;

public class ContactTileListFragment extends Fragment {
    private static final String TAG = ContactTileListFragment.class.getSimpleName();
    private ContactTileAdapter mAdapter;
    private ContactTileAdapter.DisplayType mDisplayType;
    private TextView mEmptyView;
    private ListView mListView;
    private Listener mListener;
    private boolean mOptionsMenuHasFrequents;
    private final LoaderManager.LoaderCallbacks<Cursor> mContactTileLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int id, Bundle args) {
            switch (AnonymousClass3.$SwitchMap$com$android$contacts$common$list$ContactTileAdapter$DisplayType[ContactTileListFragment.this.mDisplayType.ordinal()]) {
                case 1:
                    return ContactTileLoaderFactory.createStarredLoader(ContactTileListFragment.this.getActivity());
                case 2:
                    return ContactTileLoaderFactory.createStrequentLoader(ContactTileListFragment.this.getActivity());
                case 3:
                    return ContactTileLoaderFactory.createFrequentLoader(ContactTileListFragment.this.getActivity());
                default:
                    throw new IllegalStateException("Unrecognized DisplayType " + ContactTileListFragment.this.mDisplayType);
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data == null || data.isClosed()) {
                Log.e(ContactTileListFragment.TAG, "Failed to load contacts");
                return;
            }
            ContactTileListFragment.this.mAdapter.setContactCursor(data);
            ContactTileListFragment.this.mEmptyView.setText(ContactTileListFragment.this.getEmptyStateText());
            ContactTileListFragment.this.mListView.setEmptyView(ContactTileListFragment.this.mEmptyView);
            ContactTileListFragment.this.invalidateOptionsMenuIfNeeded();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private ContactTileView.Listener mAdapterListener = new ContactTileView.Listener() {
        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            if (ContactTileListFragment.this.mListener != null) {
                ContactTileListFragment.this.mListener.onContactSelected(contactUri, targetRect);
            }
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            if (ContactTileListFragment.this.mListener != null) {
                ContactTileListFragment.this.mListener.onCallNumberDirectly(phoneNumber);
            }
        }

        @Override
        public int getApproximateTileWidth() {
            return ContactTileListFragment.this.getView().getWidth() / ContactTileListFragment.this.mAdapter.getColumnCount();
        }
    };

    public interface Listener {
        void onCallNumberDirectly(String str);

        void onContactSelected(Uri uri, Rect rect);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Resources res = getResources();
        int columnCount = res.getInteger(R.integer.contact_tile_column_count_in_favorites);
        this.mAdapter = new ContactTileAdapter(activity, this.mAdapterListener, columnCount, this.mDisplayType);
        this.mAdapter.setPhotoLoader(ContactPhotoManager.getInstance(activity));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateAndSetupView(inflater, container, savedInstanceState, R.layout.contact_tile_list);
    }

    protected View inflateAndSetupView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, int layoutResourceId) {
        View listLayout = inflater.inflate(layoutResourceId, container, false);
        this.mEmptyView = (TextView) listLayout.findViewById(R.id.contact_tile_list_empty);
        this.mListView = (ListView) listLayout.findViewById(R.id.contact_tile_list);
        this.mListView.setItemsCanFocus(true);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        ContactListViewUtils.applyCardPaddingToView(getResources(), this.mListView, listLayout);
        return listLayout;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (getActivity() != null && getView() != null && !hidden) {
            ContactListViewUtils.applyCardPaddingToView(getResources(), this.mListView, getView());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ContactTileAdapter.DisplayType displayType = this.mDisplayType;
        ContactTileAdapter.DisplayType[] loaderTypes = ContactTileAdapter.DisplayType.values();
        for (int i = 0; i < loaderTypes.length; i++) {
            if (loaderTypes[i] == this.mDisplayType) {
                getLoaderManager().initLoader(this.mDisplayType.ordinal(), null, this.mContactTileLoaderListener);
            } else {
                getLoaderManager().destroyLoader(loaderTypes[i].ordinal());
            }
        }
    }

    public boolean hasFrequents() {
        this.mOptionsMenuHasFrequents = internalHasFrequents();
        return this.mOptionsMenuHasFrequents;
    }

    private boolean internalHasFrequents() {
        return this.mAdapter.getNumFrequents() > 0;
    }

    public void setDisplayType(ContactTileAdapter.DisplayType displayType) {
        this.mDisplayType = displayType;
        this.mAdapter.setDisplayType(this.mDisplayType);
    }

    private boolean isOptionsMenuChanged() {
        return this.mOptionsMenuHasFrequents != internalHasFrequents();
    }

    private void invalidateOptionsMenuIfNeeded() {
        if (isOptionsMenuChanged()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private String getEmptyStateText() {
        switch (this.mDisplayType) {
            case STARRED_ONLY:
            case STREQUENT:
                String emptyText = getString(R.string.listTotalAllContactsZeroStarred);
                return emptyText;
            case FREQUENT_ONLY:
            case GROUP_MEMBERS:
                String emptyText2 = getString(R.string.noContacts);
                return emptyText2;
            default:
                throw new IllegalArgumentException("Unrecognized DisplayType " + this.mDisplayType);
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }
}
