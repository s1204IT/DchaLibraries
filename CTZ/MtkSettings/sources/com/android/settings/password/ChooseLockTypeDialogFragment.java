package com.android.settings.password;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.setupwizardlib.util.WizardManagerHelper;
import java.util.List;

public class ChooseLockTypeDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    private ScreenLockAdapter mAdapter;
    private ChooseLockGenericController mController;

    public static ChooseLockTypeDialogFragment newInstance(int i) {
        Bundle bundle = new Bundle();
        bundle.putInt("userId", i);
        ChooseLockTypeDialogFragment chooseLockTypeDialogFragment = new ChooseLockTypeDialogFragment();
        chooseLockTypeDialogFragment.setArguments(bundle);
        return chooseLockTypeDialogFragment;
    }

    public interface OnLockTypeSelectedListener {
        void onLockTypeSelected(ScreenLockType screenLockType);

        default void startChooseLockActivity(ScreenLockType screenLockType, Activity activity) {
            Intent intent = activity.getIntent();
            Intent intent2 = new Intent(activity, (Class<?>) SetupChooseLockGeneric.class);
            intent2.addFlags(33554432);
            ChooseLockTypeDialogFragment.copyBooleanExtra(intent, intent2, "has_challenge", false);
            ChooseLockTypeDialogFragment.copyBooleanExtra(intent, intent2, "show_options_button", false);
            if (intent.hasExtra("choose_lock_generic_extras")) {
                intent2.putExtras(intent.getBundleExtra("choose_lock_generic_extras"));
            }
            intent2.putExtra("lockscreen.password_type", screenLockType.defaultQuality);
            intent2.putExtra("challenge", intent.getLongExtra("challenge", 0L));
            WizardManagerHelper.copyWizardManagerExtras(intent, intent2);
            activity.startActivity(intent2);
            activity.finish();
        }
    }

    private static void copyBooleanExtra(Intent intent, Intent intent2, String str, boolean z) {
        intent2.putExtra(str, intent.getBooleanExtra(str, z));
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mController = new ChooseLockGenericController(getContext(), getArguments().getInt("userId"));
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        OnLockTypeSelectedListener onLockTypeSelectedListener;
        ComponentCallbacks2 parentFragment = getParentFragment();
        if (parentFragment instanceof OnLockTypeSelectedListener) {
            onLockTypeSelectedListener = (OnLockTypeSelectedListener) parentFragment;
        } else {
            Object context = getContext();
            if (context instanceof OnLockTypeSelectedListener) {
                onLockTypeSelectedListener = (OnLockTypeSelectedListener) context;
            } else {
                onLockTypeSelectedListener = null;
            }
        }
        if (onLockTypeSelectedListener != null) {
            onLockTypeSelectedListener.onLockTypeSelected(this.mAdapter.getItem(i));
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        this.mAdapter = new ScreenLockAdapter(context, this.mController.getVisibleScreenLockTypes(65536, false), this.mController);
        builder.setAdapter(this.mAdapter, this);
        builder.setTitle(R.string.setup_lock_settings_options_dialog_title);
        return builder.create();
    }

    @Override
    public int getMetricsCategory() {
        return 990;
    }

    private static class ScreenLockAdapter extends ArrayAdapter<ScreenLockType> {
        private final ChooseLockGenericController mController;

        ScreenLockAdapter(Context context, List<ScreenLockType> list, ChooseLockGenericController chooseLockGenericController) {
            super(context, R.layout.choose_lock_dialog_item, list);
            this.mController = chooseLockGenericController;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Context context = viewGroup.getContext();
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.choose_lock_dialog_item, viewGroup, false);
            }
            ScreenLockType item = getItem(i);
            TextView textView = (TextView) view;
            textView.setText(this.mController.getTitle(item));
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(getIconForScreenLock(context, item), (Drawable) null, (Drawable) null, (Drawable) null);
            return view;
        }

        private static Drawable getIconForScreenLock(Context context, ScreenLockType screenLockType) {
            switch (screenLockType) {
                case PATTERN:
                    return context.getDrawable(R.drawable.ic_pattern);
                case PIN:
                    return context.getDrawable(R.drawable.ic_pin);
                case PASSWORD:
                    return context.getDrawable(R.drawable.ic_password);
                default:
                    return null;
            }
        }
    }
}
