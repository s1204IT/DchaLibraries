package com.android.contacts.interactions;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.vcard.ExportVCardActivity;
import com.android.contacts.vcard.ShareVCardActivity;

public class ExportDialogFragment extends DialogFragment {
    private static int mExportMode = -1;
    private final String[] LOOKUP_PROJECTION = {"lookup"};

    public static void show(FragmentManager fragmentManager, Class cls, int i) {
        ExportDialogFragment exportDialogFragment = new ExportDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("CALLING_ACTIVITY", cls.getName());
        exportDialogFragment.setArguments(bundle);
        exportDialogFragment.show(fragmentManager, "ExportDialogFragment");
        mExportMode = i;
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Resources resources = getActivity().getResources();
        final LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService("layout_inflater");
        final String string = getArguments().getString("CALLING_ACTIVITY");
        final ArrayAdapter<AdapterEntry> arrayAdapter = new ArrayAdapter<AdapterEntry>(getActivity(), R.layout.select_dialog_item) {
            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.select_dialog_item, viewGroup, false);
                }
                TextView textView = (TextView) view.findViewById(R.id.primary_text);
                view.findViewById(R.id.secondary_text).setVisibility(8);
                textView.setText(getItem(i).mLabel);
                return view;
            }
        };
        if (resources.getBoolean(R.bool.config_allow_export)) {
            arrayAdapter.add(new AdapterEntry(getString(R.string.export_to_vcf_file), R.string.export_to_vcf_file));
        }
        if (resources.getBoolean(R.bool.config_allow_share_contacts)) {
            if (mExportMode == 0) {
                arrayAdapter.add(new AdapterEntry(getString(R.string.share_favorite_contacts), R.string.share_contacts));
            } else {
                arrayAdapter.add(new AdapterEntry(getString(R.string.share_contacts), R.string.share_contacts));
            }
        }
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int i2 = ((AdapterEntry) arrayAdapter.getItem(i)).mChoiceResourceId;
                if (i2 == R.string.export_to_vcf_file) {
                    Intent intent = new Intent(ExportDialogFragment.this.getActivity(), (Class<?>) ExportVCardActivity.class);
                    intent.putExtra("CALLING_ACTIVITY", string);
                    ExportDialogFragment.this.getActivity().startActivity(intent);
                } else if (i2 == R.string.share_contacts) {
                    if (ExportDialogFragment.mExportMode == 0) {
                        ExportDialogFragment.this.doShareFavoriteContacts();
                    } else {
                        Intent intent2 = new Intent(ExportDialogFragment.this.getActivity(), (Class<?>) ShareVCardActivity.class);
                        intent2.putExtra("CALLING_ACTIVITY", string);
                        ExportDialogFragment.this.getActivity().startActivity(intent2);
                    }
                } else {
                    Log.e("ExportDialogFragment", "Unexpected resource: " + ExportDialogFragment.this.getActivity().getResources().getResourceEntryName(i2));
                }
                dialogInterface.dismiss();
            }
        };
        TextView textView = (TextView) View.inflate(getActivity(), R.layout.dialog_title, null);
        textView.setText(R.string.dialog_export);
        return new AlertDialog.Builder(getActivity()).setCustomTitle(textView).setSingleChoiceItems(arrayAdapter, -1, onClickListener).create();
    }

    private void doShareFavoriteContacts() {
        try {
            Cursor cursorQuery = getActivity().getContentResolver().query(ContactsContract.Contacts.CONTENT_STREQUENT_URI, this.LOOKUP_PROJECTION, null, null, "display_name COLLATE NOCASE ASC");
            if (cursorQuery != null) {
                try {
                    if (!cursorQuery.moveToFirst()) {
                        Toast.makeText(getActivity(), R.string.no_contact_to_share, 0).show();
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    int i = 0;
                    do {
                        if (i != 0) {
                            sb.append(':');
                        }
                        sb.append(cursorQuery.getString(0));
                        i++;
                    } while (cursorQuery.moveToNext());
                    Uri uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(sb.toString()));
                    Intent intent = new Intent("android.intent.action.SEND");
                    intent.setType("text/x-vcard");
                    intent.putExtra("android.intent.extra.STREAM", uriWithAppendedPath);
                    ImplicitIntentsUtil.startActivityOutsideApp(getActivity(), intent);
                    cursorQuery.close();
                } finally {
                    cursorQuery.close();
                }
            }
        } catch (Exception e) {
            Log.e("ExportDialogFragment", "Sharing contacts failed", e);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ExportDialogFragment.this.getContext(), R.string.share_contacts_failure, 0).show();
                }
            });
        }
    }

    private static class AdapterEntry {
        public final int mChoiceResourceId;
        public final CharSequence mLabel;
        public final int mSubscriptionId;

        public AdapterEntry(CharSequence charSequence, int i, int i2) {
            this.mLabel = charSequence;
            this.mChoiceResourceId = i;
            this.mSubscriptionId = i2;
        }

        public AdapterEntry(String str, int i) {
            this(str, i, -1);
        }
    }
}
