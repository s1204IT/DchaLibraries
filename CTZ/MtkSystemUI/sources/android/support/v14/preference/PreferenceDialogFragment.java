package android.support.v14.preference;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

public abstract class PreferenceDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private BitmapDrawable mDialogIcon;
    private int mDialogLayoutRes;
    private CharSequence mDialogMessage;
    private CharSequence mDialogTitle;
    private CharSequence mNegativeButtonText;
    private CharSequence mPositiveButtonText;
    private DialogPreference mPreference;
    private int mWhichButtonClicked;

    public abstract void onDialogClosed(boolean z);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentCallbacks2 targetFragment = getTargetFragment();
        if (!(targetFragment instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException("Target fragment must implement TargetFragment interface");
        }
        DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) targetFragment;
        String key = getArguments().getString("key");
        if (savedInstanceState == null) {
            this.mPreference = (DialogPreference) fragment.findPreference(key);
            this.mDialogTitle = this.mPreference.getDialogTitle();
            this.mPositiveButtonText = this.mPreference.getPositiveButtonText();
            this.mNegativeButtonText = this.mPreference.getNegativeButtonText();
            this.mDialogMessage = this.mPreference.getDialogMessage();
            this.mDialogLayoutRes = this.mPreference.getDialogLayoutResource();
            Drawable icon = this.mPreference.getDialogIcon();
            if (icon == null || (icon instanceof BitmapDrawable)) {
                this.mDialogIcon = (BitmapDrawable) icon;
                return;
            }
            Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            icon.draw(canvas);
            this.mDialogIcon = new BitmapDrawable(getResources(), bitmap);
            return;
        }
        this.mDialogTitle = savedInstanceState.getCharSequence("PreferenceDialogFragment.title");
        this.mPositiveButtonText = savedInstanceState.getCharSequence("PreferenceDialogFragment.positiveText");
        this.mNegativeButtonText = savedInstanceState.getCharSequence("PreferenceDialogFragment.negativeText");
        this.mDialogMessage = savedInstanceState.getCharSequence("PreferenceDialogFragment.message");
        this.mDialogLayoutRes = savedInstanceState.getInt("PreferenceDialogFragment.layout", 0);
        Bitmap bitmap2 = (Bitmap) savedInstanceState.getParcelable("PreferenceDialogFragment.icon");
        if (bitmap2 != null) {
            this.mDialogIcon = new BitmapDrawable(getResources(), bitmap2);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("PreferenceDialogFragment.title", this.mDialogTitle);
        outState.putCharSequence("PreferenceDialogFragment.positiveText", this.mPositiveButtonText);
        outState.putCharSequence("PreferenceDialogFragment.negativeText", this.mNegativeButtonText);
        outState.putCharSequence("PreferenceDialogFragment.message", this.mDialogMessage);
        outState.putInt("PreferenceDialogFragment.layout", this.mDialogLayoutRes);
        if (this.mDialogIcon != null) {
            outState.putParcelable("PreferenceDialogFragment.icon", this.mDialogIcon.getBitmap());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        this.mWhichButtonClicked = -2;
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(this.mDialogTitle).setIcon(this.mDialogIcon).setPositiveButton(this.mPositiveButtonText, this).setNegativeButton(this.mNegativeButtonText, this);
        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(this.mDialogMessage);
        }
        onPrepareDialogBuilder(builder);
        Dialog dialog = builder.create();
        if (needInputMethod()) {
            requestInputMethod(dialog);
        }
        return dialog;
    }

    public DialogPreference getPreference() {
        if (this.mPreference == null) {
            String key = getArguments().getString("key");
            DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) getTargetFragment();
            this.mPreference = (DialogPreference) fragment.findPreference(key);
        }
        return this.mPreference;
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    }

    protected boolean needInputMethod() {
        return false;
    }

    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(5);
    }

    protected View onCreateDialogView(Context context) {
        int resId = this.mDialogLayoutRes;
        if (resId == 0) {
            return null;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(resId, (ViewGroup) null);
    }

    protected void onBindDialogView(View view) {
        View dialogMessageView = view.findViewById(R.id.message);
        if (dialogMessageView != null) {
            CharSequence message = this.mDialogMessage;
            int newVisibility = 8;
            if (!TextUtils.isEmpty(message)) {
                if (dialogMessageView instanceof TextView) {
                    ((TextView) dialogMessageView).setText(message);
                }
                newVisibility = 0;
            }
            if (dialogMessageView.getVisibility() != newVisibility) {
                dialogMessageView.setVisibility(newVisibility);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        this.mWhichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onDialogClosed(this.mWhichButtonClicked == -1);
    }
}
