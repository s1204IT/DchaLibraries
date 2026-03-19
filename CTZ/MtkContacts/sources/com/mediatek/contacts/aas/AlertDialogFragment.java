package com.mediatek.contacts.aas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.android.contacts.R;
import com.mediatek.contacts.util.Log;

public class AlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    protected DialogInterface.OnClickListener mDoneListener = null;
    protected DialogInterface.OnDismissListener mDismissListener = null;

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putAll(getArguments());
        super.onSaveInstanceState(bundle);
    }

    public Bundle createBundle(int i, int i2, int i3, int i4, boolean z, int i5, int i6) {
        Bundle bundle = new Bundle();
        bundle.putInt("title", i);
        bundle.putBoolean("cancelable", z);
        bundle.putInt("icon", i3);
        bundle.putInt("message", i4);
        bundle.putInt("layout", i2);
        bundle.putInt("negativeTitle", i5);
        bundle.putInt("positiveTitle", i6);
        return bundle;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Log.d("AlertDialogFragment", "[onCreateDialog]");
        return alertDialogBuild(bundle).create();
    }

    protected AlertDialog.Builder alertDialogBuild(Bundle bundle) {
        if (bundle == null) {
            bundle = getArguments();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (bundle != null) {
            int i = bundle.getInt("title", -1);
            if (i != -1) {
                builder.setTitle(i);
            }
            int i2 = bundle.getInt("icon", -1);
            if (i2 != -1) {
                builder.setIcon(i2);
            }
            int i3 = bundle.getInt("message", -1);
            int i4 = bundle.getInt("layout", -1);
            if (i4 != -1) {
                builder.setView(getActivity().getLayoutInflater().inflate(i4, (ViewGroup) null));
            } else if (i3 != -1) {
                builder.setMessage(i3);
            }
            int i5 = bundle.getInt("negativeTitle", -1);
            if (i5 != -1) {
                builder.setNegativeButton(i5, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i6) {
                        dialogInterface.cancel();
                    }
                });
            }
            int i6 = bundle.getInt("positiveTitle", -1);
            if (i6 != -1) {
                builder.setPositiveButton(i6, this);
            }
            builder.setCancelable(bundle.getBoolean("cancelable", true));
        }
        return builder;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mDoneListener != null) {
            this.mDoneListener.onClick(dialogInterface, i);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if (this.mDismissListener != null) {
            this.mDismissListener.onDismiss(dialogInterface);
        }
        super.onDismiss(dialogInterface);
    }

    public static class EditTextDialogFragment extends AlertDialogFragment {
        private EditText mEditText;

        public interface EditTextDoneListener {
            void onEditTextDone(String str);
        }

        public static EditTextDialogFragment newInstance(int i, int i2, int i3, String str) {
            EditTextDialogFragment editTextDialogFragment = new EditTextDialogFragment();
            Bundle bundleCreateBundle = editTextDialogFragment.createBundle(i, -1, -1, -1, true, i2, i3);
            bundleCreateBundle.putString("defaultString", str);
            editTextDialogFragment.setArguments(bundleCreateBundle);
            return editTextDialogFragment;
        }

        @Override
        public void onSaveInstanceState(Bundle bundle) {
            Log.d("EditTextDialogFragment", "[onSaveInstanceState] mEditText=" + Log.anonymize(this.mEditText.getText().toString()));
            getArguments().putString("defaultString", this.mEditText.getText().toString());
            super.onSaveInstanceState(bundle);
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Log.d("EditTextDialogFragment", "[onCreateDialog] dialog tag = " + getTag());
            AlertDialog.Builder builderAlertDialogBuild = alertDialogBuild(bundle);
            if (bundle == null) {
                bundle = getArguments();
            } else {
                Log.sensitive("EditTextDialogFragment", "[onCreateDialog] savedInstanceState=" + bundle.getString("defaultString", ""));
            }
            if (bundle != null) {
                String string = bundle.getString("defaultString", "");
                View viewInflate = getActivity().getLayoutInflater().inflate(R.layout.dialog_edit_text, (ViewGroup) null);
                builderAlertDialogBuild.setView(viewInflate);
                this.mEditText = (EditText) viewInflate.findViewById(R.id.edit_text);
                this.mEditText.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
                if (this.mEditText != null) {
                    this.mEditText.setText(string);
                    this.mEditText.setSelection(string.length());
                }
            }
            return builderAlertDialogBuild.create();
        }

        @Override
        public void onResume() {
            Button button;
            super.onResume();
            if (this.mEditText != null && this.mEditText.getText().length() == 0 && (button = ((AlertDialog) getDialog()).getButton(-1)) != null) {
                button.setEnabled(false);
            }
            getDialog().getWindow().setSoftInputMode(5);
            setTextChangedCallback(this.mEditText, (AlertDialog) getDialog());
        }

        protected void setTextChangedCallback(EditText editText, final AlertDialog alertDialog) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    if (charSequence.toString().length() <= 0) {
                        Button button = alertDialog.getButton(-1);
                        if (button != null) {
                            button.setEnabled(false);
                            return;
                        }
                        return;
                    }
                    Button button2 = alertDialog.getButton(-1);
                    if (button2 != null) {
                        button2.setEnabled(true);
                    }
                }
            });
        }

        public String getText() {
            if (this.mEditText != null) {
                return this.mEditText.getText().toString().trim();
            }
            return null;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (!(getActivity() instanceof AasTagActivity)) {
                Log.w("EditTextDialogFragment", "[onClick] not AasTagActivity, do nothing");
                return;
            }
            EditTextDoneListener editTextDoneListener = ((AasTagActivity) getActivity()).getEditTextDoneListener(getTag());
            if (editTextDoneListener != null) {
                editTextDoneListener.onEditTextDone(getText().trim());
            }
        }
    }
}
