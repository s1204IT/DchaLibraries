package com.android.contacts.interactions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.android.contacts.R;

public abstract class GroupNameDialogFragment extends DialogFragment {
    protected abstract int getTitleResourceId();

    protected abstract void initializeGroupLabelEditText(EditText editText);

    protected abstract void onCompleted(String str);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = LayoutInflater.from(builder.getContext());
        View view = layoutInflater.inflate(R.layout.group_name_dialog, (ViewGroup) null);
        final EditText editText = (EditText) view.findViewById(R.id.group_label);
        initializeGroupLabelEditText(editText);
        builder.setTitle(getTitleResourceId());
        builder.setView(view);
        editText.requestFocus();
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int whichButton) {
                GroupNameDialogFragment.this.onCompleted(editText.getText().toString().trim());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                GroupNameDialogFragment.this.updateOkButtonState(dialog, editText);
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                GroupNameDialogFragment.this.updateOkButtonState(dialog, editText);
            }
        });
        dialog.getWindow().setSoftInputMode(5);
        return dialog;
    }

    void updateOkButtonState(AlertDialog dialog, EditText editText) {
        Button okButton = dialog.getButton(-1);
        okButton.setEnabled(!TextUtils.isEmpty(editText.getText().toString().trim()));
    }
}
