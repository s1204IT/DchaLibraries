package com.android.contacts.group;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.model.account.AccountWithDataSet;
import com.google.common.base.Strings;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class GroupNameEditDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private AccountWithDataSet mAccount;
    private long mGroupId;
    private String mGroupName;
    private EditText mGroupNameEditText;
    private TextInputLayout mGroupNameTextLayout;
    private boolean mIsInsert;
    private Listener mListener;
    private Set<String> mExistingGroups = Collections.emptySet();
    private int mSubId = SubInfoUtils.getInvalidSubId();

    public interface Listener {
        public static final Listener None = new Listener() {
            @Override
            public void onGroupNameEditCancelled() {
            }

            @Override
            public void onGroupNameEditCompleted(String str) {
            }
        };

        void onGroupNameEditCancelled();

        void onGroupNameEditCompleted(String str);
    }

    public int getSubId() {
        return this.mSubId;
    }

    public static GroupNameEditDialogFragment newInstanceForCreation(AccountWithDataSet accountWithDataSet, String str) {
        return newInstance(accountWithDataSet, str, -1L, null);
    }

    public static GroupNameEditDialogFragment newInstanceForUpdate(AccountWithDataSet accountWithDataSet, String str, long j, String str2) {
        return newInstance(accountWithDataSet, str, j, str2);
    }

    private static GroupNameEditDialogFragment newInstance(AccountWithDataSet accountWithDataSet, String str, long j, String str2) {
        if (accountWithDataSet == null || accountWithDataSet.name == null || accountWithDataSet.type == null) {
            throw new IllegalArgumentException("Invalid account");
        }
        boolean z = j == -1;
        Bundle bundle = new Bundle();
        bundle.putBoolean("isInsert", z);
        bundle.putLong(ContactSaveService.EXTRA_GROUP_ID, j);
        bundle.putString("groupName", str2);
        bundle.putParcelable("account", accountWithDataSet);
        bundle.putString("callbackAction", str);
        GroupNameEditDialogFragment groupNameEditDialogFragment = new GroupNameEditDialogFragment();
        groupNameEditDialogFragment.setArguments(bundle);
        return groupNameEditDialogFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setStyle(0, R.style.ContactsAlertDialogThemeAppCompat);
        Bundle arguments = getArguments();
        if (bundle == null) {
            this.mGroupName = arguments.getString("groupName");
        } else {
            this.mGroupName = bundle.getString("groupName");
        }
        this.mGroupId = arguments.getLong(ContactSaveService.EXTRA_GROUP_ID, -1L);
        this.mIsInsert = arguments.getBoolean("isInsert", true);
        this.mAccount = (AccountWithDataSet) getArguments().getParcelable("account");
        this.mSubId = AccountTypeUtils.getSubIdBySimAccountName(getActivity(), this.mAccount.name);
        Log.d("GroupNameEditDialogFragment", "[onCreate] mSubId = " + this.mSubId);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        int i;
        TextView textView = (TextView) View.inflate(getActivity(), R.layout.dialog_title, null);
        if (this.mIsInsert) {
            i = R.string.group_name_dialog_insert_title;
        } else {
            i = R.string.group_name_dialog_update_title;
        }
        textView.setText(i);
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity(), getTheme()).setCustomTitle(textView).setView(R.layout.group_name_edit_dialog).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                GroupNameEditDialogFragment.this.hideInputMethod();
                GroupNameEditDialogFragment.this.getListener().onGroupNameEditCancelled();
                GroupNameEditDialogFragment.this.dismiss();
            }
        }).setPositiveButton(android.R.string.ok, null).create();
        alertDialogCreate.getWindow().setSoftInputMode(36);
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (!GroupNameEditDialogFragment.this.isAdded()) {
                    Log.e("GroupNameEditDialogFragment", "[onShow] Fragment not added yet !!!");
                    try {
                        GroupNameEditDialogFragment.this.dismiss();
                        return;
                    } catch (Exception e) {
                        Log.e("GroupNameEditDialogFragment", "Error dismissing group name edit dialog", e);
                        return;
                    }
                }
                GroupNameEditDialogFragment.this.mGroupNameEditText = (EditText) alertDialogCreate.findViewById(android.R.id.text1);
                GroupNameEditDialogFragment.this.mGroupNameTextLayout = (TextInputLayout) alertDialogCreate.findViewById(R.id.text_input_layout);
                GroupNameEditDialogFragment.this.mGroupNameTextLayout.setError(null);
                if (!TextUtils.isEmpty(GroupNameEditDialogFragment.this.mGroupName)) {
                    GroupNameEditDialogFragment.this.mGroupNameEditText.setText(GroupNameEditDialogFragment.this.mGroupName);
                    int integer = GroupNameEditDialogFragment.this.getResources().getInteger(R.integer.group_name_max_length);
                    EditText editText = GroupNameEditDialogFragment.this.mGroupNameEditText;
                    if (GroupNameEditDialogFragment.this.mGroupName.length() <= integer) {
                        integer = GroupNameEditDialogFragment.this.mGroupName.length();
                    }
                    editText.setSelection(integer);
                }
                GroupNameEditDialogFragment.this.mGroupNameEditText.requestFocus();
                GroupNameEditDialogFragment.this.showInputMethod(GroupNameEditDialogFragment.this.mGroupNameEditText);
                final Button button = alertDialogCreate.getButton(-1);
                button.setEnabled(!TextUtils.isEmpty(GroupNameEditDialogFragment.this.getGroupName()));
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        GroupNameEditDialogFragment.this.maybePersistCurrentGroupName(view);
                    }
                });
                GroupNameEditDialogFragment.this.mGroupNameEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        GroupNameEditDialogFragment.this.mGroupNameTextLayout.setError(null);
                        button.setEnabled(!TextUtils.isEmpty(editable));
                    }
                });
            }
        });
        return alertDialogCreate;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    private boolean hasNameChanged() {
        String strNullToEmpty = Strings.nullToEmpty(getGroupName());
        return (this.mIsInsert && !strNullToEmpty.isEmpty()) || !strNullToEmpty.equals(getArguments().getString("groupName"));
    }

    private void maybePersistCurrentGroupName(View view) {
        Intent intentCreateGroupRenameIntent;
        Log.d("GroupNameEditDialogFragment", "[maybePersistCurrentGroupName] mIsInsert is " + this.mIsInsert);
        if (!SimGroupUtils.checkServiceState(true, this.mSubId, getActivity())) {
            return;
        }
        if (!hasNameChanged()) {
            dismiss();
            return;
        }
        String groupName = getGroupName();
        if (!TextUtils.isEmpty(groupName)) {
            groupName = groupName.trim();
        }
        String str = groupName;
        if (this.mExistingGroups.contains(str)) {
            this.mGroupNameTextLayout.setError(getString(R.string.groupExistsErrorMessage));
            view.setEnabled(false);
            return;
        }
        String string = getArguments().getString("callbackAction");
        Log.d("GroupNameEditDialogFragment", "[maybePersistCurrentGroupName]name=" + Log.anonymize(str) + ", mSubId=" + this.mSubId);
        if (this.mSubId > 0) {
            if (this.mIsInsert) {
                intentCreateGroupRenameIntent = SimGroupUtils.createNewGroupIntentForIcc(getActivity(), this.mAccount, str, null, getActivity().getClass(), string, null, this.mSubId);
            } else {
                intentCreateGroupRenameIntent = SimGroupUtils.createGroupRenameIntentForIcc(getActivity(), this.mGroupId, str, getActivity().getClass(), string, getArguments().getString("groupName"), this.mSubId);
            }
        } else if (this.mIsInsert) {
            intentCreateGroupRenameIntent = ContactSaveService.createNewGroupIntent(getActivity(), this.mAccount, str, null, getActivity().getClass(), string);
        } else {
            intentCreateGroupRenameIntent = ContactSaveService.createGroupRenameIntent(getActivity(), this.mGroupId, str, getActivity().getClass(), string);
        }
        ContactSaveService.startService(getActivity(), intentCreateGroupRenameIntent);
        getListener().onGroupNameEditCompleted(this.mGroupName);
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        super.onCancel(dialogInterface);
        getListener().onGroupNameEditCancelled();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("groupName", getGroupName());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), ContactsContract.Groups.CONTENT_SUMMARY_URI, new String[]{"title", "system_id", "account_type", "summ_count", "group_is_read_only"}, getSelection(), getSelectionArgs(), null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.mExistingGroups = new HashSet();
        GroupUtil.GroupsProjection groupsProjection = new GroupUtil.GroupsProjection(cursor);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String title = groupsProjection.getTitle(cursor);
            if (!TextUtils.isEmpty(title)) {
                title = title.trim();
            }
            if (!groupsProjection.isEmptyFFCGroup(cursor)) {
                this.mExistingGroups.add(title);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void showInputMethod(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, 0);
        }
    }

    private void hideInputMethod() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService("input_method");
        if (inputMethodManager != null && this.mGroupNameEditText != null) {
            inputMethodManager.hideSoftInputFromWindow(this.mGroupNameEditText.getWindowToken(), 0);
        }
    }

    private Listener getListener() {
        if (this.mListener != null) {
            return this.mListener;
        }
        if (getActivity() instanceof Listener) {
            return (Listener) getActivity();
        }
        return Listener.None;
    }

    private String getGroupName() {
        if (this.mGroupNameEditText == null || this.mGroupNameEditText.getText() == null) {
            return null;
        }
        return this.mGroupNameEditText.getText().toString();
    }

    private String getSelection() {
        StringBuilder sb = new StringBuilder();
        sb.append("account_name");
        sb.append("=? AND ");
        sb.append("account_type");
        sb.append("=? AND ");
        sb.append("deleted");
        sb.append("=?");
        if (this.mAccount.dataSet != null) {
            sb.append(" AND ");
            sb.append("data_set");
            sb.append("=?");
        }
        return sb.toString();
    }

    private String[] getSelectionArgs() {
        int i;
        if (this.mAccount.dataSet != null) {
            i = 4;
        } else {
            i = 3;
        }
        String[] strArr = new String[i];
        strArr[0] = this.mAccount.name;
        strArr[1] = this.mAccount.type;
        strArr[2] = "0";
        if (this.mAccount.dataSet != null) {
            strArr[3] = this.mAccount.dataSet;
        }
        return strArr;
    }
}
