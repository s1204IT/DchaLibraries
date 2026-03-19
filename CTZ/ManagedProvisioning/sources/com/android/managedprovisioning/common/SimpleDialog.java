package com.android.managedprovisioning.common;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class SimpleDialog extends DialogFragment {

    public interface SimpleDialogListener {
        void onNegativeButtonClick(DialogFragment dialogFragment);

        void onPositiveButtonClick(DialogFragment dialogFragment);
    }

    @Override
    public AlertDialog onCreateDialog(Bundle bundle) {
        final SimpleDialogListener simpleDialogListener = (SimpleDialogListener) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        if (arguments.containsKey("title")) {
            builder.setTitle(arguments.getInt("title"));
        }
        if (arguments.containsKey("message")) {
            builder.setMessage(arguments.getInt("message"));
        }
        if (arguments.containsKey("negativeButtonMessage")) {
            builder.setNegativeButton(arguments.getInt("negativeButtonMessage"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    simpleDialogListener.onNegativeButtonClick(SimpleDialog.this);
                }
            });
        }
        if (arguments.containsKey("positiveButtonMessage")) {
            builder.setPositiveButton(arguments.getInt("positiveButtonMessage"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    simpleDialogListener.onPositiveButtonClick(SimpleDialog.this);
                }
            });
        }
        return builder.create();
    }

    public static void throwButtonClickHandlerNotImplemented(DialogFragment dialogFragment) {
        throw new IllegalArgumentException("Button click handler not implemented for dialog: " + dialogFragment.getTag());
    }

    public static class Builder implements DialogBuilder {
        private Boolean mCancelable;
        private Integer mMessage;
        private Integer mNegativeButtonMessage;
        private Integer mPositiveButtonMessage;
        private Integer mTitle;

        public Builder setTitle(Integer num) {
            this.mTitle = num;
            return this;
        }

        public Builder setMessage(int i) {
            this.mMessage = Integer.valueOf(i);
            return this;
        }

        public Builder setNegativeButtonMessage(int i) {
            this.mNegativeButtonMessage = Integer.valueOf(i);
            return this;
        }

        public Builder setPositiveButtonMessage(int i) {
            this.mPositiveButtonMessage = Integer.valueOf(i);
            return this;
        }

        public Builder setCancelable(boolean z) {
            this.mCancelable = Boolean.valueOf(z);
            return this;
        }

        @Override
        public SimpleDialog build() {
            SimpleDialog simpleDialog = new SimpleDialog();
            Bundle bundle = new Bundle();
            if (this.mTitle != null) {
                bundle.putInt("title", this.mTitle.intValue());
            }
            if (this.mMessage != null) {
                bundle.putInt("message", this.mMessage.intValue());
            }
            if (this.mNegativeButtonMessage != null) {
                bundle.putInt("negativeButtonMessage", this.mNegativeButtonMessage.intValue());
            }
            if (this.mPositiveButtonMessage != null) {
                bundle.putInt("positiveButtonMessage", this.mPositiveButtonMessage.intValue());
            }
            if (this.mCancelable != null) {
                simpleDialog.setCancelable(this.mCancelable.booleanValue());
            }
            simpleDialog.setArguments(bundle);
            return simpleDialog;
        }
    }
}
