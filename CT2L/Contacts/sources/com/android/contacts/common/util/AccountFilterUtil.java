package com.android.contacts.common.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.list.AccountFilterActivity;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;

public class AccountFilterUtil {
    private static final String TAG = AccountFilterUtil.class.getSimpleName();

    public static boolean updateAccountFilterTitleForPeople(View filterContainer, ContactListFilter filter, boolean showTitleForAllAccounts) {
        return updateAccountFilterTitle(filterContainer, filter, showTitleForAllAccounts, false);
    }

    public static boolean updateAccountFilterTitleForPhone(View filterContainer, ContactListFilter filter, boolean showTitleForAllAccounts) {
        return updateAccountFilterTitle(filterContainer, filter, showTitleForAllAccounts, true);
    }

    private static boolean updateAccountFilterTitle(View filterContainer, ContactListFilter filter, boolean showTitleForAllAccounts, boolean forPhone) {
        Context context = filterContainer.getContext();
        TextView headerTextView = (TextView) filterContainer.findViewById(R.id.account_filter_header);
        if (filter != null) {
            if (forPhone) {
                if (filter.filterType == -2) {
                    if (!showTitleForAllAccounts) {
                        return false;
                    }
                    headerTextView.setText(R.string.list_filter_phones);
                    return true;
                }
                if (filter.filterType == 0) {
                    headerTextView.setText(context.getString(R.string.listAllContactsInAccount, filter.accountName));
                    return true;
                }
                if (filter.filterType == -3) {
                    headerTextView.setText(R.string.listCustomView);
                    return true;
                }
                Log.w(TAG, "Filter type \"" + filter.filterType + "\" isn't expected.");
                return false;
            }
            if (filter.filterType == -2) {
                if (!showTitleForAllAccounts) {
                    return false;
                }
                headerTextView.setText(R.string.list_filter_all_accounts);
                return true;
            }
            if (filter.filterType == 0) {
                headerTextView.setText(context.getString(R.string.listAllContactsInAccount, filter.accountName));
                return true;
            }
            if (filter.filterType == -3) {
                headerTextView.setText(R.string.listCustomView);
                return true;
            }
            if (filter.filterType == -6) {
                headerTextView.setText(R.string.listSingleContact);
                return true;
            }
            Log.w(TAG, "Filter type \"" + filter.filterType + "\" isn't expected.");
            return false;
        }
        Log.w(TAG, "Filter is null.");
        return false;
    }

    public static void startAccountFilterActivityForResult(Activity activity, int requestCode, ContactListFilter currentFilter) {
        Intent intent = new Intent(activity, (Class<?>) AccountFilterActivity.class);
        intent.putExtra("currentFilter", currentFilter);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startAccountFilterActivityForResult(Fragment fragment, int requestCode, ContactListFilter currentFilter) {
        Activity activity = fragment.getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity, (Class<?>) AccountFilterActivity.class);
            intent.putExtra("currentFilter", currentFilter);
            fragment.startActivityForResult(intent, requestCode);
            return;
        }
        Log.w(TAG, "getActivity() returned null. Ignored");
    }

    public static void handleAccountFilterResult(ContactListFilterController filterController, int resultCode, Intent data) {
        ContactListFilter filter;
        if (resultCode == -1 && (filter = (ContactListFilter) data.getParcelableExtra("contactListFilter")) != null) {
            if (filter.filterType == -3) {
                filterController.selectCustomFilter();
            } else {
                filterController.setContactListFilter(filter, true);
            }
        }
    }
}
