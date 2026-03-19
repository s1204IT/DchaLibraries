package com.android.contacts.list;

import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.activities.ActionBarAdapter;
import com.android.contacts.group.GroupMembersFragment;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.android.contacts.logging.Logger;
import com.android.contacts.logging.SearchState;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.GoogleAccountType;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

public abstract class MultiSelectContactsListFragment<T extends MultiSelectEntryContactListAdapter> extends ContactEntryListFragment<T> implements MultiSelectEntryContactListAdapter.SelectedContactsListener {
    protected boolean mAnimateOnLoad;
    private OnCheckBoxListActionListener mCheckBoxListListener;
    private boolean mIsSelectedAll = false;
    protected Menu mOptionMenu = null;

    public interface OnCheckBoxListActionListener {
        void onSelectedContactIdsChanged();

        void onStartDisplayingCheckBoxes();

        void onStopDisplayingCheckBoxes();
    }

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        onLoadFinished((Loader<Cursor>) loader, (Cursor) obj);
    }

    public void setCheckBoxListListener(OnCheckBoxListActionListener onCheckBoxListActionListener) {
        this.mCheckBoxListListener = onCheckBoxListActionListener;
    }

    public void setAnimateOnLoad(boolean z) {
        this.mAnimateOnLoad = z;
    }

    @Override
    public void onSelectedContactsChanged() {
        if (this.mCheckBoxListListener != null) {
            this.mCheckBoxListListener.onSelectedContactIdsChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        if (bundle == null && this.mAnimateOnLoad) {
            setLayoutAnimation(getListView(), R.anim.slide_and_fade_in_layout_animation);
        }
        return getView();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        Log.d("MultiContactsList", "[onActivityCreated] savedState=" + bundle);
        super.onActivityCreated(bundle);
        if (bundle != null) {
            getAdapter().setSelectedContactIds((TreeSet) bundle.getSerializable("selected_contacts"));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mCheckBoxListListener != null) {
            this.mCheckBoxListListener.onSelectedContactIdsChanged();
        }
    }

    public TreeSet<Long> getSelectedContactIds() {
        return getAdapter().getSelectedContactIds();
    }

    public long[] getSelectedContactIdsArray() {
        return getAdapter().getSelectedContactIdsArray();
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        getAdapter().setSelectedContactsListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putSerializable("selected_contacts", getSelectedContactIds());
    }

    public void displayCheckBoxes(boolean z) {
        if (getAdapter() != null) {
            getAdapter().setDisplayCheckBoxes(z);
            if (!z) {
                clearCheckBoxes();
            }
        }
    }

    public void clearCheckBoxes() {
        getAdapter().setSelectedContactIds(new TreeSet<>());
    }

    @Override
    protected boolean onItemLongClick(int i, long j) {
        Log.d("MultiContactsList", "[onItemLongClick]position = " + i + ",id = " + j);
        int size = getAdapter().getSelectedContactIds().size();
        long contactId = getContactId(i);
        int partitionForPosition = getAdapter().getPartitionForPosition(i);
        if (contactId >= 0 && partitionForPosition == 0 && isAllowMultiSelect(i)) {
            if (this.mCheckBoxListListener != null) {
                this.mCheckBoxListListener.onStartDisplayingCheckBoxes();
            }
            getAdapter().toggleSelectionOfContactId(contactId);
            Logger.logListEvent(3, getListType(), getAdapter().getCount(), i, 1);
            int headerViewsCount = (i + getListView().getHeaderViewsCount()) - getListView().getFirstVisiblePosition();
            if (headerViewsCount >= 0 && headerViewsCount < getListView().getChildCount()) {
                getListView().getChildAt(headerViewsCount).sendAccessibilityEvent(1);
            }
        }
        int size2 = getAdapter().getSelectedContactIds().size();
        if (this.mCheckBoxListListener != null && size != 0 && size2 == 0) {
            Log.d("MultiContactsList", "[onItemLongClick]onStopDisplayingCheckBoxes");
            this.mCheckBoxListListener.onStopDisplayingCheckBoxes();
        }
        return true;
    }

    @Override
    protected void onItemClick(int i, long j) {
        Log.d("MultiContactsList", "[onItemClick]position = " + i + ",id = " + j);
        if (getActionBarAdapter() != null && getActionBarAdapter().isSelectMenuShown()) {
            Log.e("MultiContactsList", "[onItemClick]ignore due to select menu has shown");
            return;
        }
        long contactId = getContactId(i);
        if (contactId < 0) {
            Log.w("MultiContactsList", "[onItemClick]contactId < 0,return.");
            return;
        }
        if (getAdapter().isDisplayingCheckBoxes() && isAllowMultiSelect(i)) {
            getAdapter().toggleSelectionOfContactId(contactId);
        }
        if (this.mCheckBoxListListener != null && getAdapter().getSelectedContactIds().size() == 0) {
            this.mCheckBoxListListener.onStopDisplayingCheckBoxes();
        }
    }

    private boolean isAllowMultiSelect(int i) {
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            return true ^ getAdapter().isSdnNumber(i);
        }
        return true;
    }

    private long getContactId(int i) {
        int contactColumnIdIndex = getAdapter().getContactColumnIdIndex();
        Cursor cursor = (Cursor) getAdapter().getItem(i);
        if (cursor != null && cursor.getColumnCount() > contactColumnIdIndex) {
            return cursor.getLong(contactColumnIdIndex);
        }
        Log.w("MultiContactsList", "Failed to get contact ID from cursor column " + contactColumnIdIndex);
        return -1L;
    }

    public SearchState createSearchState() {
        return createSearchState(-1);
    }

    public SearchState createSearchStateForSearchResultClick(int i) {
        return createSearchState(i);
    }

    private SearchState createSearchState(int i) {
        T adapter = getAdapter();
        if (adapter == null) {
            return null;
        }
        SearchState searchState = new SearchState();
        searchState.queryLength = adapter.getQueryString() == null ? 0 : adapter.getQueryString().length();
        searchState.numPartitions = adapter.getPartitionCount();
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < adapter.getPartitionCount(); i2++) {
            Cursor cursor = adapter.getCursor(i2);
            if (cursor == null || cursor.isClosed()) {
                arrayList.clear();
                break;
            }
            arrayList.add(Integer.valueOf(cursor.getCount()));
        }
        if (!arrayList.isEmpty()) {
            int iIntValue = 0;
            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                iIntValue += ((Integer) arrayList.get(i3)).intValue();
            }
            searchState.numResults = iIntValue;
        }
        if (i >= 0) {
            searchState.selectedPartition = adapter.getPartitionForPosition(i);
            searchState.selectedIndexInPartition = adapter.getOffsetInPartition(i);
            Cursor cursor2 = adapter.getCursor(searchState.selectedPartition);
            searchState.numResultsInSelectedPartition = (cursor2 == null || cursor2.isClosed()) ? -1 : cursor2.getCount();
            if (!arrayList.isEmpty()) {
                int iIntValue2 = 0;
                for (int i4 = 0; i4 < searchState.selectedPartition; i4++) {
                    iIntValue2 += ((Integer) arrayList.get(i4)).intValue();
                }
                searchState.selectedIndex = iIntValue2 + searchState.selectedIndexInPartition;
            }
        }
        return searchState;
    }

    protected void setLayoutAnimation(final ViewGroup viewGroup, int i) {
        if (viewGroup == null) {
            return;
        }
        viewGroup.setLayoutAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewGroup.setLayoutAnimation(null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        viewGroup.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getActivity(), i));
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        View viewFindViewById = getView().findViewById(R.id.account_filter_header_container);
        if (viewFindViewById == null) {
            return;
        }
        if (absListView != null && absListView.getChildAt(0) != null && absListView.getChildAt(0).getTop() < 0) {
            i++;
        }
        if (i == 0) {
            ViewCompat.setElevation(viewFindViewById, ContactPhotoManager.OFFSET_DEFAULT);
        } else {
            ViewCompat.setElevation(viewFindViewById, getResources().getDimension(R.dimen.contact_list_header_elevation));
        }
    }

    protected void bindListHeaderCustom(View view, View view2) {
        bindListHeaderCommon(view, view2);
        TextView textView = (TextView) view2.findViewById(R.id.account_filter_header);
        textView.setText(R.string.listCustomView);
        textView.setAllCaps(false);
        ((ImageView) view2.findViewById(R.id.account_filter_icon)).setVisibility(8);
    }

    protected void bindListHeader(Context context, View view, View view2, AccountWithDataSet accountWithDataSet, int i) {
        String quantityString;
        if (i < 0) {
            hideHeaderAndAddPadding(context, view, view2);
            return;
        }
        bindListHeaderCommon(view, view2);
        AccountType accountType = AccountTypeManager.getInstance(context).getAccountType(accountWithDataSet.type, accountWithDataSet.dataSet);
        TextView textView = (TextView) view2.findViewById(R.id.account_filter_header);
        if (shouldShowAccountName(accountType)) {
            quantityString = String.format(context.getResources().getQuantityString(R.plurals.contacts_count_with_account, i), Integer.valueOf(i), accountWithDataSet.name);
        } else {
            quantityString = context.getResources().getQuantityString(R.plurals.contacts_count, i, Integer.valueOf(i));
        }
        textView.setText(quantityString);
        textView.setAllCaps(false);
        Drawable displayIcon = accountType != null ? accountType.getDisplayIcon(context) : null;
        ImageView imageView = (ImageView) view2.findViewById(R.id.account_filter_icon);
        if (accountType instanceof GoogleAccountType) {
            imageView.getLayoutParams().height = getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_size);
            imageView.getLayoutParams().width = getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_size);
            setMargins(imageView, getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_left_margin), getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_right_margin));
        } else {
            imageView.getLayoutParams().height = getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_size_alt);
            imageView.getLayoutParams().width = getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_size_alt);
            setMargins(imageView, getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_left_margin_alt), getResources().getDimensionPixelOffset(R.dimen.contact_browser_list_header_icon_right_margin_alt));
        }
        imageView.requestLayout();
        imageView.setVisibility(0);
        imageView.setImageDrawable(displayIcon);
    }

    private boolean shouldShowAccountName(AccountType accountType) {
        return (accountType.isGroupMembershipEditable() && (this instanceof GroupMembersFragment)) || GoogleAccountType.ACCOUNT_TYPE.equals(accountType.accountType);
    }

    private void setMargins(View view, int i, int i2) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            marginLayoutParams.setMarginStart(i);
            marginLayoutParams.setMarginEnd(i2);
            view.setLayoutParams(marginLayoutParams);
            view.requestLayout();
        }
    }

    private void bindListHeaderCommon(View view, View view2) {
        view2.setVisibility(0);
        setListViewPaddingTop(view, 0);
    }

    protected void hideHeaderAndAddPadding(Context context, View view, View view2) {
        view2.setVisibility(8);
        setListViewPaddingTop(view, context.getResources().getDimensionPixelSize(R.dimen.contact_browser_list_item_padding_top_or_bottom));
    }

    private void setListViewPaddingTop(View view, int i) {
        view.setPadding(view.getPaddingLeft(), i, view.getPaddingRight(), view.getPaddingBottom());
    }

    public ActionBarAdapter getActionBarAdapter() {
        return null;
    }

    @Override
    public void onDestroy() {
        closeMenusIfOpen(true, true);
        super.onDestroy();
    }

    public boolean isSelectedAll() {
        return this.mIsSelectedAll;
    }

    public boolean updateSelectedItemsView() {
        int count = getAdapter().getCount();
        Log.d("MultiContactsList", "[updateSelectedItemsView]count:" + count + ", isSearchMode:" + isSearchMode() + ", sdnNumber:" + Log.anonymize(Integer.valueOf(getAdapter().getSdnNumber())) + ", favorite:" + Log.anonymize(Integer.valueOf(getAdapter().getNumberOfFavorites())) + ", shouldIncludeFavorite:" + getAdapter().shouldIncludeFavorites());
        if (getAdapter().getSdnNumber() > 0) {
            count -= getAdapter().getSdnNumber();
        }
        if (getAdapter().getNumberOfFavorites() > 0 && getAdapter().shouldIncludeFavorites()) {
            count -= getAdapter().getNumberOfFavorites();
        }
        int size = getAdapter().getSelectedContactIds().size();
        Log.d("MultiContactsList", "[updateSelectedItemsView]count=" + count + ", checkcount=" + size);
        if (count != 0 && count == size) {
            this.mIsSelectedAll = true;
        } else {
            this.mIsSelectedAll = false;
        }
        return count >= size;
    }

    public void updateCheckBoxState(boolean z) {
        int count = getAdapter().getCount();
        StringBuilder sb = new StringBuilder();
        sb.append("[updateCheckBoxState]checked = ");
        sb.append(z);
        sb.append(",count = ");
        sb.append(count);
        sb.append(",postion: ");
        sb.append(0);
        sb.append(",isSearchMode: ");
        sb.append(isSearchMode());
        Log.d("MultiContactsList", sb.toString());
        if (z) {
            for (int i = 0; i < count; i++) {
                if ((!ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT || !getAdapter().isSdnNumber(i)) && !getListView().isItemChecked(i)) {
                    getListView().setItemChecked(i, z);
                    long contactId = getContactId(i);
                    if (contactId >= 0) {
                        getSelectedContactIds().add(Long.valueOf(contactId));
                    } else {
                        Log.e("MultiContactsList", "[updateCheckBoxState]invalid contactId at pos: " + i);
                    }
                }
            }
            return;
        }
        getSelectedContactIds().clear();
    }

    public void closeMenusIfOpen(boolean z, boolean z2) {
        if (z && this.mOptionMenu != null) {
            this.mOptionMenu.close();
        }
        if (z2 && getActionBarAdapter() != null) {
            getActionBarAdapter().closeSelectMenu();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        super.onLoadFinished(loader, cursor);
        if (cursor != null && (cursor instanceof CursorWrapper)) {
            CursorWrapper cursorWrapper = (CursorWrapper) cursor;
            Cursor wrappedCursor = cursorWrapper.getWrappedCursor();
            Log.d("MultiContactsList", "[onLoadFinished]Conver cursor:" + cursorWrapper + " to " + wrappedCursor);
            cursor = wrappedCursor;
        }
        HashSet hashSet = new HashSet();
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                try {
                    long j = cursor.getLong(0);
                    if (j != -1) {
                        hashSet.add(Long.valueOf(j));
                    }
                } catch (IllegalStateException e) {
                    Log.e("MultiContactsList", "[onLoadFinished]IllegalStateException", e);
                    if (this.mCheckBoxListListener != null) {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                MultiSelectContactsListFragment.this.closeMenusIfOpen(true, true);
                                MultiSelectContactsListFragment.this.mCheckBoxListListener.onStopDisplayingCheckBoxes();
                            }
                        });
                        return;
                    }
                    return;
                }
            }
        }
        int size = getAdapter().getSelectedContactIds().size();
        int id = loader.getId();
        Log.d("MultiContactsList", "[onLoadFinished]sizeBefore = " + size + ",loader =" + loader + ",loaderId = " + id + ",newDataSet: " + hashSet.toString() + ",currentSelected: " + getAdapter().getSelectedContactIds().toString());
        Iterator<Long> it = getAdapter().getSelectedContactIds().iterator();
        while (it.hasNext()) {
            Long next = it.next();
            if (id == 0 && !hashSet.contains(next)) {
                Log.d("MultiContactsList", "[onLoadFinished] selected removeId = " + next);
                it.remove();
            }
        }
        int size2 = getAdapter().getSelectedContactIds().size();
        Log.d("MultiContactsList", "[onLoadFinished]sizeAfter = " + size2);
        if (size2 > 0) {
            if (size2 != size) {
                getAdapter().notifySelectedContactsChanged();
                updateSelectedItemsView();
                return;
            }
            return;
        }
        if (size > 0 && this.mCheckBoxListListener != null) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    MultiSelectContactsListFragment.this.closeMenusIfOpen(true, true);
                    MultiSelectContactsListFragment.this.mCheckBoxListListener.onStopDisplayingCheckBoxes();
                }
            });
        }
    }
}
