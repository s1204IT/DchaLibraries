package com.android.contacts.common.interactions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.common.editor.SelectAccountDialogFragment;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountSelectionUtil;
import com.android.contacts.common.util.AccountsListAdapter;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import com.android.contacts.common.vcard.ExportVCardActivity;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import java.util.List;

public class ImportExportDialogFragment extends DialogFragment implements SelectAccountDialogFragment.Listener {
    private final String[] LOOKUP_PROJECTION = {"lookup"};
    private DialogFragment mChildFragment;
    private SubscriptionManager mSubscriptionManager;

    public static void show(FragmentManager fragmentManager, boolean contactsAreAvailable, Class callingActivity) {
        ImportExportDialogFragment fragment = new ImportExportDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("CONTACTS_ARE_AVAILABLE", contactsAreAvailable);
        args.putString("CALLING_ACTIVITY", callingActivity.getName());
        fragment.setArguments(args);
        fragment.show(fragmentManager, "ImportExportDialogFragment");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        AnalyticsUtil.sendScreenView(this);
    }

    @Override
    public void onDestroy() {
        if (this.mChildFragment != null) {
            this.mChildFragment.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<SubscriptionInfo> subInfoRecords;
        List<SubscriptionInfo> subInfoRecords2;
        Resources res = getActivity().getResources();
        final LayoutInflater dialogInflater = (LayoutInflater) getActivity().getSystemService("layout_inflater");
        boolean contactsAreAvailable = getArguments().getBoolean("CONTACTS_ARE_AVAILABLE");
        final String callingActivity = getArguments().getString("CALLING_ACTIVITY");
        final ArrayAdapter<AdapterEntry> adapter = new ArrayAdapter<AdapterEntry>(getActivity(), R.layout.select_dialog_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView result = (TextView) (convertView != null ? convertView : dialogInflater.inflate(R.layout.select_dialog_item, parent, false));
                result.setText(getItem(position).mLabel);
                return result;
            }
        };
        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService("phone");
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        if (res.getBoolean(R.bool.config_allow_import_from_sdcard)) {
            adapter.add(new AdapterEntry(getString(R.string.import_from_sdcard), R.string.import_from_sdcard));
        }
        if (manager != null && res.getBoolean(R.bool.config_allow_sim_import) && (subInfoRecords2 = this.mSubscriptionManager.getActiveSubscriptionInfoList()) != null) {
            if (subInfoRecords2.size() == 1) {
                adapter.add(new AdapterEntry(getString(R.string.import_from_sim), R.string.import_from_sim, subInfoRecords2.get(0).getSubscriptionId()));
            } else {
                for (SubscriptionInfo record : subInfoRecords2) {
                    if (SimPhoneBookCommonUtil.isSimContactsLoaded(record.getSimSlotIndex())) {
                        adapter.add(new AdapterEntry(getSubDescription(record), R.string.import_from_sim, record.getSubscriptionId()));
                    }
                }
            }
        }
        if (res.getBoolean(R.bool.config_allow_export_to_sdcard) && contactsAreAvailable) {
            adapter.add(new AdapterEntry(getString(R.string.export_to_sdcard), R.string.export_to_sdcard));
        }
        if (manager != null && res.getBoolean(R.bool.config_allow_export_to_sim) && (subInfoRecords = this.mSubscriptionManager.getActiveSubscriptionInfoList()) != null) {
            if (subInfoRecords.size() == 1) {
                adapter.add(new AdapterEntry(getString(R.string.export_to_sim), R.string.export_to_sim, subInfoRecords.get(0).getSubscriptionId()));
            } else {
                for (SubscriptionInfo record2 : subInfoRecords) {
                    adapter.add(new AdapterEntry(getSubDescriptionExport(record2), R.string.export_to_sim, record2.getSubscriptionId()));
                }
            }
        }
        if (res.getBoolean(R.bool.config_allow_share_visible_contacts) && contactsAreAvailable) {
            adapter.add(new AdapterEntry(getString(R.string.share_visible_contacts), R.string.share_visible_contacts));
        }
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean dismissDialog;
                int resId = ((AdapterEntry) adapter.getItem(which)).mChoiceResourceId;
                int subId = ((AdapterEntry) adapter.getItem(which)).mSubscriptionId;
                switch (resId) {
                    case R.string.import_from_sim:
                    case R.string.import_from_sdcard:
                        dismissDialog = ImportExportDialogFragment.this.handleImportRequest(resId, ((AdapterEntry) adapter.getItem(which)).mSubscriptionId);
                        break;
                    case R.string.export_to_sim:
                        Intent exportIntent = new Intent("android.intent.action.VIEW");
                        exportIntent.setType("vnd.android.cursor.item/sim-contact");
                        dismissDialog = true;
                        exportIntent.putExtra("mode", 1);
                        exportIntent.setClassName("com.android.contacts", "com.android.contacts.activities.SimContactsSelectActivity");
                        exportIntent.putExtra("slot", SubscriptionManager.getSlotId(subId));
                        ImportExportDialogFragment.this.getActivity().startActivity(exportIntent);
                        break;
                    case R.string.export_to_sdcard:
                        dismissDialog = true;
                        Intent exportIntent2 = new Intent(ImportExportDialogFragment.this.getActivity(), (Class<?>) ExportVCardActivity.class);
                        exportIntent2.putExtra("CALLING_ACTIVITY", callingActivity);
                        ImportExportDialogFragment.this.getActivity().startActivity(exportIntent2);
                        break;
                    case R.string.share_visible_contacts:
                        dismissDialog = true;
                        ImportExportDialogFragment.this.doShareVisibleContacts();
                        break;
                    default:
                        dismissDialog = true;
                        Log.e("ImportExportDialogFragment", "Unexpected resource: " + ImportExportDialogFragment.this.getActivity().getResources().getResourceEntryName(resId));
                        break;
                }
                if (dismissDialog) {
                    dialog.dismiss();
                }
            }
        };
        return new AlertDialog.Builder(getActivity()).setTitle(contactsAreAvailable ? R.string.dialog_import_export : R.string.dialog_import).setSingleChoiceItems(adapter, -1, clickListener).create();
    }

    private void doShareVisibleContacts() {
        Cursor cursor = getActivity().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, this.LOOKUP_PROJECTION, "in_visible_group!=0", null, null);
        if (cursor != null) {
            try {
                if (!cursor.moveToFirst()) {
                    Toast.makeText(getActivity(), R.string.share_error, 0).show();
                    return;
                }
                StringBuilder uriListBuilder = new StringBuilder();
                int index = 0;
                do {
                    if (index != 0) {
                        uriListBuilder.append(':');
                    }
                    uriListBuilder.append(cursor.getString(0));
                    index++;
                } while (cursor.moveToNext());
                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(uriListBuilder.toString()));
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("text/x-vcard");
                intent.putExtra("android.intent.extra.STREAM", uri);
                getActivity().startActivity(intent);
            } finally {
                cursor.close();
            }
        }
    }

    private boolean handleImportRequest(int resId, int subscriptionId) {
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(getActivity());
        List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
        int size = accountList.size();
        if (size > 1) {
            Bundle args = new Bundle();
            args.putInt("resourceId", resId);
            args.putInt("subscriptionId", subscriptionId);
            this.mChildFragment = SelectAccountDialogFragment.show(getFragmentManager(), this, R.string.dialog_new_contact_account, AccountsListAdapter.AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_WITHOUT_SIM, args);
            return false;
        }
        AccountSelectionUtil.doImport(getActivity(), resId, size == 1 ? accountList.get(0) : null, subscriptionId);
        return true;
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        Context context = getActivity();
        if (context != null) {
            AccountSelectionUtil.doImport(getActivity(), extraArgs.getInt("resourceId"), account, extraArgs.getInt("subscriptionId"));
        }
        dismiss();
    }

    @Override
    public void onAccountSelectorCancelled() {
        dismiss();
    }

    private CharSequence getSubDescription(SubscriptionInfo record) {
        CharSequence name = record.getDisplayName();
        return TextUtils.isEmpty(record.getNumber()) ? getString(R.string.import_from_sim_summary_no_number, new Object[]{name}) : TextUtils.expandTemplate(getString(R.string.import_from_sim_summary), name, PhoneNumberUtils.ttsSpanAsPhoneNumber(record.getNumber()));
    }

    private CharSequence getSubDescriptionExport(SubscriptionInfo record) {
        CharSequence name = record.getDisplayName();
        return TextUtils.isEmpty(record.getNumber()) ? getString(R.string.export_to_sim_summary_no_number, new Object[]{name}) : TextUtils.expandTemplate(getString(R.string.export_to_sim_summary), name, PhoneNumberUtils.ttsSpanAsPhoneNumber(record.getNumber()));
    }

    private static class AdapterEntry {
        public final int mChoiceResourceId;
        public final CharSequence mLabel;
        public final int mSubscriptionId;

        public AdapterEntry(CharSequence label, int resId, int subId) {
            this.mLabel = label;
            this.mChoiceResourceId = resId;
            this.mSubscriptionId = subId;
        }

        public AdapterEntry(String label, int resId) {
            this(label, resId, -1);
        }
    }
}
