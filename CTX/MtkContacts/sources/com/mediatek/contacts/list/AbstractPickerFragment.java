package com.mediatek.contacts.list;

import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public abstract class AbstractPickerFragment extends MultiSelectContactsListFragment<MultiSelectEntryContactListAdapter> implements ContactListMultiChoiceListener {
    private String mSlectedItemsFormater = null;
    private String mSearchString = null;
    protected View mAccountFilterHeader = null;
    private View mToolBar = null;
    private TextView mEmptyView = null;
    private boolean mIsSelectedAll = false;
    private boolean mIsSelectedNone = true;
    private Set<Long> mPushedIds = new HashSet();

    public abstract long getListItemDataId(int i);

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        onLoadFinished((Loader<Cursor>) loader, (Cursor) obj);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Log.i("AbstractPickerFragment", "[onActivityCreated]");
        this.mSlectedItemsFormater = getActivity().getString(R.string.menu_actionbar_selected_items);
        restoreSavedState(bundle);
        showSelectedCount(((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size());
        setOkButtonStatus(false);
        getListView().setChoiceMode(2);
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        Log.i("AbstractPickerFragment", "[inflateView]");
        return layoutInflater.inflate(R.layout.mtk_multichoice_contact_list, (ViewGroup) null);
    }

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        Log.i("AbstractPickerFragment", "[onCreateView]");
        this.mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        this.mEmptyView = (TextView) getView().findViewById(R.id.contact_list_empty);
        if (this.mEmptyView != null) {
            this.mEmptyView.setText(R.string.noContacts);
        }
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        Log.i("AbstractPickerFragment", "[configureAdapter]...");
        ?? adapter = getAdapter();
        if (adapter == 0) {
            Log.w("AbstractPickerFragment", "[configureAdapter]adapter is null.");
            return;
        }
        adapter.setEmptyListEnabled(true);
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        adapter.setQuickContactEnabled(false);
        super.setPhotoLoaderEnabled(true);
        adapter.setQueryString(this.mSearchString);
        adapter.setIncludeFavorites(false);
    }

    @Override
    protected void onItemClick(int i, long j) {
        long j2 = ((Cursor) ((MultiSelectEntryContactListAdapter) getAdapter()).getItem(i)).getLong(0);
        Log.d("AbstractPickerFragment", "[onItemClick] position: " + i + ",contactId:" + j2);
        TreeSet<Long> selectedContactIds = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds();
        int multiChoiceLimitCount = getMultiChoiceLimitCount();
        boolean z = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size() >= multiChoiceLimitCount;
        boolean zContains = selectedContactIds.contains(Long.valueOf(j2));
        if (z && !zContains) {
            Log.i("AbstractPickerFragment", "[onItemClick] Current selected Contact cnt > 3500,cannot select more");
            MtkToast.toast(getActivity().getApplicationContext(), getResources().getString(R.string.multichoice_contacts_limit, Integer.valueOf(multiChoiceLimitCount)));
            return;
        }
        if (selectedContactIds.contains(Long.valueOf(j2))) {
            selectedContactIds.remove(Long.valueOf(j2));
            Log.d("AbstractPickerFragment", "[onItemClick]  SelectedContactIds.remove ");
        } else {
            selectedContactIds.add(Long.valueOf(j2));
            Log.d("AbstractPickerFragment", "[onItemClick] SelectedContactIds.add ");
        }
        ((MultiSelectEntryContactListAdapter) getAdapter()).notifyDataSetChanged();
        updateSelectedItemsView();
    }

    @Override
    protected boolean onItemLongClick(int i, long j) {
        return false;
    }

    public void onClearSelect() {
        Log.i("AbstractPickerFragment", "[onClearSelect]");
        updateCheckBoxState(false);
    }

    public void onSelectAll() {
        Log.i("AbstractPickerFragment", "[onSelectAll]");
        updateCheckBoxState(true);
    }

    @Override
    public void restoreSavedState(Bundle bundle) {
        super.restoreSavedState(bundle);
        if (bundle == null) {
            Log.w("AbstractPickerFragment", "[restoreSavedState]saved state is null");
            return;
        }
        ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().clear();
        long[] longArray = bundle.getLongArray("checkedids");
        Log.i("AbstractPickerFragment", "[restoreSavedState]restore " + longArray.length + " ids");
        for (long j : longArray) {
            ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().add(Long.valueOf(j));
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int[] intArray;
        Log.i("AbstractPickerFragment", "[onLoadFinished]");
        if (!isAdded()) {
            Log.w("AbstractPickerFragment", "[onLoadFinished],This Fragment is not added to the Activity now.data:" + cursor);
            if (cursor != null) {
                cursor.close();
                return;
            }
            return;
        }
        if (cursor != null && (intArray = cursor.getExtras().getIntArray("checked_ids")) != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[onLoadFinished] ids: ");
            for (int i : intArray) {
                sb.append(String.valueOf(i) + ",");
            }
            Iterator<Long> it = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().iterator();
            while (it.hasNext()) {
                if (Arrays.binarySearch(intArray, it.next().intValue()) < 0) {
                    it.remove();
                }
            }
        }
        if (((MultiSelectEntryContactListAdapter) getAdapter()).isSearchMode()) {
            Log.i("AbstractPickerFragment", "[onLoadFinished]SearchMode");
            getListView().setFastScrollEnabled(false);
            getListView().setFastScrollAlwaysVisible(false);
        }
        if (cursor == null || (cursor != null && cursor.getCount() == 0)) {
            Log.w("AbstractPickerFragment", "[onLoadFinished]nothing loaded, data:" + cursor + ",empty view: " + this.mEmptyView);
            if (this.mEmptyView != null) {
                if (!((MultiSelectEntryContactListAdapter) getAdapter()).isSearchMode()) {
                    this.mEmptyView.setText(R.string.noContacts);
                } else {
                    this.mEmptyView.setText(R.string.listFoundAllContactsZero);
                }
                this.mEmptyView.setVisibility(0);
            }
            getListView().setFastScrollEnabled(false);
            getListView().setFastScrollAlwaysVisible(false);
        } else {
            if (this.mEmptyView != null) {
                if (!((MultiSelectEntryContactListAdapter) getAdapter()).isSearchMode()) {
                    this.mEmptyView.setText(R.string.noContacts);
                } else {
                    this.mEmptyView.setText(R.string.listFoundAllContactsZero);
                }
                this.mEmptyView.setVisibility(8);
            }
            if (!((MultiSelectEntryContactListAdapter) getAdapter()).isSearchMode()) {
                getListView().setFastScrollEnabled(true);
                getListView().setFastScrollAlwaysVisible(true);
            }
        }
        getListView().clearChoices();
        HashSet hashSet = new HashSet();
        if (cursor != null) {
            Log.d("AbstractPickerFragment", "[onLoadFinished]query data count: " + cursor.getCount());
            cursor.moveToPosition(-1);
            int i2 = 0;
            while (cursor.moveToNext()) {
                try {
                    long j = cursor.getInt(0);
                    hashSet.add(Long.valueOf(j));
                    if (((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().contains(Long.valueOf(j)) || this.mPushedIds.contains(Long.valueOf(j))) {
                        getListView().setItemChecked(i2, true);
                    }
                    i2++;
                    handleCursorItem(cursor);
                } catch (IllegalStateException e) {
                    Log.e("AbstractPickerFragment", "[onLoadFinished]IllegalStateException", e);
                    if (getActivity() != null) {
                        getActivity().finish();
                        return;
                    }
                    return;
                }
            }
        }
        if (!((MultiSelectEntryContactListAdapter) getAdapter()).isSearchMode()) {
            Iterator<Long> it2 = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().iterator();
            while (it2.hasNext()) {
                if (!hashSet.contains(it2.next())) {
                    it2.remove();
                }
            }
        }
        if (!this.mPushedIds.isEmpty()) {
            ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().addAll(this.mPushedIds);
            this.mPushedIds.clear();
        }
        updateSelectedItemsView(((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size());
        clearListViewLastState();
        super.onLoadFinished(loader, cursor);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        long[] jArr = new long[((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size()];
        Iterator<Long> it = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().iterator();
        int i = 0;
        while (it.hasNext()) {
            jArr[i] = it.next().longValue();
            i++;
        }
        Log.i("AbstractPickerFragment", "[onSaveInstanceState]save " + jArr.length + " ids");
        bundle.putLongArray("checkedids", jArr);
    }

    protected void updateSelectedItemsView(int i) {
        Log.d("AbstractPickerFragment", "[updateSelectedItemsView]checkedItemsCount: " + i);
        if (i == 0) {
            this.mIsSelectedNone = true;
        } else {
            this.mIsSelectedNone = false;
        }
        setOkButtonStatus(true ^ this.mIsSelectedNone);
        if (((MultiSelectEntryContactListAdapter) getAdapter()).isSearchMode()) {
            Log.w("AbstractPickerFragment", "[updateSelectedItemsView]isSearchMonde,don't showSelectedCount:" + i);
            return;
        }
        showSelectedCount(i);
    }

    @Override
    public boolean updateSelectedItemsView() {
        int count = getListView().getAdapter().getCount();
        int size = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size();
        Log.i("AbstractPickerFragment", "[updateSelectedItemsView]count: " + count + ",checkCount:" + size);
        updateSelectedItemsView(size);
        if ((count != 0 && count == ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size()) || size >= getMultiChoiceLimitCount()) {
            this.mIsSelectedAll = true;
        } else {
            this.mIsSelectedAll = false;
        }
        return count >= size;
    }

    public long[] getCheckedItemIds() {
        return convertSetToPrimitive(((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds());
    }

    public void startSearch(String str) {
        if (TextUtils.isEmpty(str)) {
            str = null;
        }
        ?? adapter = getAdapter();
        if (str == null) {
            if (adapter != 0) {
                this.mSearchString = null;
                adapter.setQueryString(str);
                adapter.setSearchMode(false);
                reloadData();
                return;
            }
            return;
        }
        if (!TextUtils.equals(this.mSearchString, str)) {
            this.mSearchString = str;
            if (adapter != 0) {
                adapter.setQueryString(str);
                adapter.setSearchMode(true);
                reloadData();
            }
        }
    }

    public void markItemsAsSelectedForCheckedGroups(long[] jArr) {
        TreeSet treeSet = new TreeSet();
        treeSet.addAll(((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds());
        int multiChoiceLimitCount = getMultiChoiceLimitCount();
        for (long j : jArr) {
            if (treeSet.size() >= multiChoiceLimitCount) {
                MtkToast.toast(getActivity().getApplicationContext(), getResources().getString(R.string.multichoice_contacts_limit, Integer.valueOf(multiChoiceLimitCount)));
                Log.w("AbstractPickerFragment", "[markItemsAsSelectedForCheckedGroups]size:" + ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size() + " >= limit:" + multiChoiceLimitCount);
                return;
            }
            treeSet.add(Long.valueOf(j));
            this.mPushedIds.add(Long.valueOf(j));
        }
    }

    @Override
    public boolean isSelectedAll() {
        return this.mIsSelectedAll;
    }

    public boolean isSelectedNone() {
        return this.mIsSelectedNone;
    }

    public void handleCursorItem(Cursor cursor) {
    }

    private void showSelectedCount(int i) {
        TextView textView;
        Log.d("AbstractPickerFragment", "[showSelectedCount]checkedItemsCount = " + i);
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        if (appCompatActivity == null) {
            Log.w("AbstractPickerFragment", "[showSelectedCount] host Activity is null,return!");
            return;
        }
        View customView = appCompatActivity.getSupportActionBar().getCustomView();
        if (customView != null) {
            textView = (TextView) customView.findViewById(R.id.select_items);
        } else {
            textView = null;
        }
        if (textView == null) {
            Log.e("AbstractPickerFragment", "[showSelectedCount]Load view resource error!");
        } else if (this.mSlectedItemsFormater == null) {
            Log.e("AbstractPickerFragment", "[showSelectedCount]mSlectedItemsFormater is null!");
        } else {
            textView.setText(String.format(this.mSlectedItemsFormater, String.valueOf(i)));
        }
    }

    private void setOkButtonStatus(boolean z) {
        Button button;
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        if (appCompatActivity == null) {
            Log.w("AbstractPickerFragment", "[setOkButtonStatus] host Activity is null,return!");
            return;
        }
        View customView = appCompatActivity.getSupportActionBar().getCustomView();
        if (customView != null) {
            button = (Button) customView.findViewById(R.id.menu_option);
        } else {
            button = null;
        }
        if (button != null) {
            if (z) {
                button.setEnabled(true);
                button.setTextColor(getResources().getColor(R.color.background_primary));
            } else {
                button.setEnabled(false);
                button.setTextColor(-3355444);
            }
        }
    }

    private static long[] convertSetToPrimitive(Set<Long> set) {
        if (set == null) {
            Log.e("AbstractPickerFragment", "[convertSetToPrimitive]set is null,return.");
            return null;
        }
        int size = set.size();
        long[] jArr = new long[size];
        int i = 0;
        for (Long l : set) {
            if (i >= size) {
                break;
            }
            jArr[i] = l.longValue();
            i++;
        }
        return jArr;
    }

    @Override
    public void updateCheckBoxState(boolean z) {
        int count = getListView().getAdapter().getCount();
        int multiChoiceLimitCount = getMultiChoiceLimitCount();
        Log.d("AbstractPickerFragment", "[updateCheckBoxState]count = " + count + ",checked =" + z);
        int i = 0;
        while (true) {
            if (i >= count) {
                break;
            }
            if (z) {
                if (((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size() >= multiChoiceLimitCount) {
                    MtkToast.toast(getActivity().getApplicationContext(), getResources().getString(R.string.multichoice_contacts_limit, Integer.valueOf(multiChoiceLimitCount)));
                    Log.w("AbstractPickerFragment", "[updateCheckBoxState] getAdapter().getSelectedContactIds() size:" + ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size() + " >= limit:" + multiChoiceLimitCount);
                    getListView().setSelection(multiChoiceLimitCount - 1);
                    break;
                }
                long listItemDataId = getListItemDataId(i);
                if (!((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().contains(Long.valueOf(listItemDataId))) {
                    ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().add(Long.valueOf(listItemDataId));
                } else {
                    Log.d("AbstractPickerFragment", "[updateCheckBoxState]has selected,unnecessary to add again.");
                }
                i++;
            } else if (((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size() <= 0) {
                i++;
            } else {
                Log.d("AbstractPickerFragment", "[updateCheckBoxState] clean all contacts done!");
                ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().clear();
                break;
            }
        }
        ((MultiSelectEntryContactListAdapter) getAdapter()).notifyDataSetChanged();
        updateSelectedItemsView(((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size());
    }

    protected int getMultiChoiceLimitCount() {
        return 3500;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (GlobalEnv.isUsingTwoPanes()) {
            int paddingLeft = getListView().getPaddingLeft();
            int dimension = (int) getResources().getDimension(R.dimen.tmp_panding_value);
            Log.i("AbstractPickerFragment", "[onConfigurationChanged] originPadding : " + paddingLeft + " | tmp : " + dimension);
            if (dimension > 0) {
                getListView().setPadding(dimension, getListView().getPaddingTop(), dimension, getListView().getPaddingBottom());
            }
        }
    }
}
