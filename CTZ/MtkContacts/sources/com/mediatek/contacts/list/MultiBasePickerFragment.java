package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.list.ContactListAdapter;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.FavoritesAndContactsLoader;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.android.contacts.util.AccountFilterUtil;
import com.mediatek.contacts.list.MultiBasePickerAdapter;
import com.mediatek.contacts.util.Log;

public class MultiBasePickerFragment extends AbstractPickerFragment implements MultiBasePickerAdapter.SelectedContactsListener {
    private ContactListFilter mFilter;
    private SharedPreferences mPrefs;
    private boolean mShowFilterHeader = true;
    private View.OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        onLoadFinished((Loader<Cursor>) loader, (Cursor) obj);
    }

    private class FilterHeaderClickListener implements View.OnClickListener {
        private FilterHeaderClickListener() {
        }

        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(MultiBasePickerFragment.this, 1, MultiBasePickerFragment.this.mFilter);
        }
    }

    @Override
    public CursorLoader createCursorLoader(Context context) {
        Log.i("MultiBasePickerFragment", "[createCursorLoader]");
        return new FavoritesAndContactsLoader(context);
    }

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        if (isAccountFilterEnable()) {
            this.mAccountFilterHeader.setOnClickListener(this.mFilterHeaderClickListener);
        } else {
            this.mAccountFilterHeader.setClickable(false);
        }
        updateFilterHeaderView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (isAccountFilterEnable()) {
            restoreFilter();
        }
    }

    private void restoreFilter() {
        this.mFilter = ContactListFilter.restoreDefaultPreferences(this.mPrefs);
    }

    @Override
    protected void configureAdapter() {
        Log.i("MultiBasePickerFragment", "[configureAdapter]");
        super.configureAdapter();
        MultiBasePickerAdapter multiBasePickerAdapter = (MultiBasePickerAdapter) getAdapter();
        multiBasePickerAdapter.setFilter(this.mFilter);
        multiBasePickerAdapter.setSelectedContactsListener((MultiBasePickerAdapter.SelectedContactsListener) this);
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        Log.i("MultiBasePickerFragment", "[createListAdapter] adapter=" + ContactListFilter.createFilterWithType(-2));
        MultiBasePickerAdapter multiBasePickerAdapter = new MultiBasePickerAdapter(getActivity(), getListView());
        multiBasePickerAdapter.setFilter(ContactListFilter.createFilterWithType(-2));
        return multiBasePickerAdapter;
    }

    protected void setListFilter(ContactListFilter contactListFilter) {
        if (isAccountFilterEnable()) {
            Log.e("MultiBasePickerFragment", "[setListFilter]invalid call.");
            throw new RuntimeException("The #setListFilter could not be called if #isAccountFilterEnable is true");
        }
        Log.i("MultiBasePickerFragment", "[configureAdapter]setListFilter:" + contactListFilter.toString());
        this.mFilter = contactListFilter;
        ((MultiSelectEntryContactListAdapter) getAdapter()).setFilter(this.mFilter);
        updateFilterHeaderView();
    }

    public boolean isAccountFilterEnable() {
        return true;
    }

    @Override
    public void restoreSavedState(Bundle bundle) {
        super.restoreSavedState(bundle);
        if (bundle == null) {
            Log.w("MultiBasePickerFragment", "[restoreSavedState]savedState is null,return.");
        } else {
            this.mFilter = (ContactListFilter) bundle.getParcelable("filter");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("filter", this.mFilter);
    }

    private void setFilter(ContactListFilter contactListFilter) {
        if (this.mFilter == null && contactListFilter == null) {
            Log.w("MultiBasePickerFragment", "[setFilter]mFilter and filter is null,return.");
            return;
        }
        if (this.mFilter != null && this.mFilter.equals(contactListFilter)) {
            Log.w("MultiBasePickerFragment", "[setFilter]mFilter equals filter,return.");
            return;
        }
        Log.d("MultiBasePickerFragment", "[setFilter]" + contactListFilter.toString());
        this.mFilter = contactListFilter;
        saveFilter();
        reloadData();
    }

    private void updateFilterHeaderView() {
        if (!this.mShowFilterHeader) {
            if (this.mAccountFilterHeader != null) {
                this.mAccountFilterHeader.setVisibility(8);
            }
        } else if (this.mAccountFilterHeader == null) {
            Log.w("MultiBasePickerFragment", "[updateFilterHeaderView]mAccountFilterHeader is null,return.");
        } else {
            this.mAccountFilterHeader.setVisibility(8);
        }
    }

    private void saveFilter() {
        ContactListFilter.storeToPreferences(this.mPrefs, this.mFilter);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        Log.i("MultiBasePickerFragment", "[onActivityResult]requestCode = " + i + ",resultCode = " + i2);
        if (i == 1) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(ContactListFilterController.getInstance(getActivity()), i2, intent);
                if (i2 == -1) {
                    setFilter(ContactListFilterController.getInstance(getActivity()).getFilter());
                    updateFilterHeaderView();
                    return;
                }
                return;
            }
            Log.e("MultiBasePickerFragment", "[onActivityResult]returns null during onActivityResult()");
        }
    }

    @Override
    public void onOptionAction() {
        long[] checkedItemIds = getCheckedItemIds();
        if (checkedItemIds == null) {
            Log.e("MultiBasePickerFragment", "[onOptionAction]idArray is null,return.");
            return;
        }
        Activity activity = getActivity();
        Intent intent = new Intent();
        intent.putExtra("com.mediatek.contacts.list.pickcontactsresult", checkedItemIds);
        activity.setResult(-1, intent);
        activity.finish();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.i("MultiBasePickerFragment", "[onLoadFinished].");
        updateFilterHeaderView();
        super.onLoadFinished(loader, cursor);
    }

    @Override
    public long getListItemDataId(int i) {
        if (((MultiBasePickerAdapter) getAdapter()) != null) {
            return r0.getContactID(i);
        }
        return -1L;
    }

    @Override
    public void handleCursorItem(Cursor cursor) {
        ((MultiBasePickerAdapter) getAdapter()).cacheDataItem(cursor);
    }

    @Override
    public void onSelectedContactsChangedViaCheckBox() {
        int size = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size();
        Log.i("MultiBasePickerFragment", "[onSelectedContactsChangedViaCheckBox] checkCount : " + size);
        updateSelectedItemsView(size);
    }
}
