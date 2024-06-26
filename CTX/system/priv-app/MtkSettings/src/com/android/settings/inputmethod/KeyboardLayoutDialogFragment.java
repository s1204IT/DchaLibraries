package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import java.util.ArrayList;
import java.util.Collections;
/* loaded from: classes.dex */
public class KeyboardLayoutDialogFragment extends InstrumentedDialogFragment implements LoaderManager.LoaderCallbacks<Keyboards>, InputManager.InputDeviceListener {
    private KeyboardLayoutAdapter mAdapter;
    private InputManager mIm;
    private int mInputDeviceId = -1;
    private InputDeviceIdentifier mInputDeviceIdentifier;

    /* loaded from: classes.dex */
    public static final class Keyboards {
        public final ArrayList<KeyboardLayout> keyboardLayouts = new ArrayList<>();
        public int current = -1;
    }

    /* loaded from: classes.dex */
    public interface OnSetupKeyboardLayoutsListener {
        void onSetupKeyboardLayouts(InputDeviceIdentifier inputDeviceIdentifier);
    }

    public KeyboardLayoutDialogFragment() {
    }

    public KeyboardLayoutDialogFragment(InputDeviceIdentifier inputDeviceIdentifier) {
        this.mInputDeviceIdentifier = inputDeviceIdentifier;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 541;
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Context baseContext = activity.getBaseContext();
        this.mIm = (InputManager) baseContext.getSystemService("input");
        this.mAdapter = new KeyboardLayoutAdapter(baseContext);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mInputDeviceIdentifier = bundle.getParcelable("inputDeviceIdentifier");
        }
        getLoaderManager().initLoader(0, null, this);
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("inputDeviceIdentifier", this.mInputDeviceIdentifier);
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        Activity activity = getActivity();
        AlertDialog.Builder view = new AlertDialog.Builder(activity).setTitle(R.string.keyboard_layout_dialog_title).setPositiveButton(R.string.keyboard_layout_dialog_setup_button, new DialogInterface.OnClickListener() { // from class: com.android.settings.inputmethod.KeyboardLayoutDialogFragment.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                KeyboardLayoutDialogFragment.this.onSetupLayoutsButtonClicked();
            }
        }).setSingleChoiceItems(this.mAdapter, -1, new DialogInterface.OnClickListener() { // from class: com.android.settings.inputmethod.KeyboardLayoutDialogFragment.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                KeyboardLayoutDialogFragment.this.onKeyboardLayoutClicked(i);
            }
        }).setView(LayoutInflater.from(activity).inflate(R.layout.keyboard_layout_dialog_switch_hint, (ViewGroup) null));
        updateSwitchHintVisibility();
        return view.create();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mIm.registerInputDeviceListener(this, null);
        InputDevice inputDeviceByDescriptor = this.mIm.getInputDeviceByDescriptor(this.mInputDeviceIdentifier.getDescriptor());
        if (inputDeviceByDescriptor == null) {
            dismiss();
        } else {
            this.mInputDeviceId = inputDeviceByDescriptor.getId();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
    public void onPause() {
        this.mIm.unregisterInputDeviceListener(this);
        this.mInputDeviceId = -1;
        super.onPause();
    }

    @Override // android.app.DialogFragment, android.content.DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialogInterface) {
        super.onCancel(dialogInterface);
        dismiss();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSetupLayoutsButtonClicked() {
        ((OnSetupKeyboardLayoutsListener) getTargetFragment()).onSetupKeyboardLayouts(this.mInputDeviceIdentifier);
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        show(getActivity().getFragmentManager(), "layout");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onKeyboardLayoutClicked(int i) {
        if (i >= 0 && i < this.mAdapter.getCount()) {
            KeyboardLayout item = this.mAdapter.getItem(i);
            if (item != null) {
                this.mIm.setCurrentKeyboardLayoutForInputDevice(this.mInputDeviceIdentifier, item.getDescriptor());
            }
            dismiss();
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<Keyboards> onCreateLoader(int i, Bundle bundle) {
        return new KeyboardLayoutLoader(getActivity().getBaseContext(), this.mInputDeviceIdentifier);
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<Keyboards> loader, Keyboards keyboards) {
        this.mAdapter.clear();
        this.mAdapter.addAll(keyboards.keyboardLayouts);
        this.mAdapter.setCheckedItem(keyboards.current);
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null) {
            alertDialog.getListView().setItemChecked(keyboards.current, true);
        }
        updateSwitchHintVisibility();
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<Keyboards> loader) {
        this.mAdapter.clear();
        updateSwitchHintVisibility();
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceAdded(int i) {
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceChanged(int i) {
        if (this.mInputDeviceId >= 0 && i == this.mInputDeviceId) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceRemoved(int i) {
        if (this.mInputDeviceId >= 0 && i == this.mInputDeviceId) {
            dismiss();
        }
    }

    private void updateSwitchHintVisibility() {
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null) {
            alertDialog.findViewById(16908820).setVisibility(this.mAdapter.getCount() > 1 ? 0 : 8);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class KeyboardLayoutAdapter extends ArrayAdapter<KeyboardLayout> {
        private int mCheckedItem;
        private final LayoutInflater mInflater;

        public KeyboardLayoutAdapter(Context context) {
            super(context, 17367282);
            this.mCheckedItem = -1;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        public void setCheckedItem(int i) {
            this.mCheckedItem = i;
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            String string;
            String str;
            KeyboardLayout item = getItem(i);
            if (item != null) {
                string = item.getLabel();
                str = item.getCollection();
            } else {
                string = getContext().getString(R.string.keyboard_layout_default_label);
                str = "";
            }
            String str2 = str;
            String str3 = string;
            boolean z = i == this.mCheckedItem;
            if (str2.isEmpty()) {
                return inflateOneLine(view, viewGroup, str3, z);
            }
            return inflateTwoLine(view, viewGroup, str3, str2, z);
        }

        private View inflateOneLine(View view, ViewGroup viewGroup, String str, boolean z) {
            if (view == null || isTwoLine(view)) {
                view = this.mInflater.inflate(17367055, viewGroup, false);
                setTwoLine(view, false);
            }
            CheckedTextView checkedTextView = (CheckedTextView) view.findViewById(16908308);
            checkedTextView.setText(str);
            checkedTextView.setChecked(z);
            return view;
        }

        private View inflateTwoLine(View view, ViewGroup viewGroup, String str, String str2, boolean z) {
            if (view == null || !isTwoLine(view)) {
                view = this.mInflater.inflate(17367282, viewGroup, false);
                setTwoLine(view, true);
            }
            ((TextView) view.findViewById(16908308)).setText(str);
            ((TextView) view.findViewById(16908309)).setText(str2);
            ((RadioButton) view.findViewById(16909214)).setChecked(z);
            return view;
        }

        private static boolean isTwoLine(View view) {
            return view.getTag() == Boolean.TRUE;
        }

        private static void setTwoLine(View view, boolean z) {
            view.setTag(Boolean.valueOf(z));
        }
    }

    /* loaded from: classes.dex */
    private static final class KeyboardLayoutLoader extends AsyncTaskLoader<Keyboards> {
        private final InputDeviceIdentifier mInputDeviceIdentifier;

        public KeyboardLayoutLoader(Context context, InputDeviceIdentifier inputDeviceIdentifier) {
            super(context);
            this.mInputDeviceIdentifier = inputDeviceIdentifier;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.content.AsyncTaskLoader
        public Keyboards loadInBackground() {
            Keyboards keyboards = new Keyboards();
            InputManager inputManager = (InputManager) getContext().getSystemService("input");
            for (String str : inputManager.getEnabledKeyboardLayoutsForInputDevice(this.mInputDeviceIdentifier)) {
                KeyboardLayout keyboardLayout = inputManager.getKeyboardLayout(str);
                if (keyboardLayout != null) {
                    keyboards.keyboardLayouts.add(keyboardLayout);
                }
            }
            Collections.sort(keyboards.keyboardLayouts);
            String currentKeyboardLayoutForInputDevice = inputManager.getCurrentKeyboardLayoutForInputDevice(this.mInputDeviceIdentifier);
            if (currentKeyboardLayoutForInputDevice != null) {
                int size = keyboards.keyboardLayouts.size();
                int i = 0;
                while (true) {
                    if (i < size) {
                        if (!keyboards.keyboardLayouts.get(i).getDescriptor().equals(currentKeyboardLayoutForInputDevice)) {
                            i++;
                        } else {
                            keyboards.current = i;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (keyboards.keyboardLayouts.isEmpty()) {
                keyboards.keyboardLayouts.add(null);
                keyboards.current = 0;
            }
            return keyboards;
        }

        @Override // android.content.Loader
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }

        @Override // android.content.Loader
        protected void onStopLoading() {
            super.onStopLoading();
            cancelLoad();
        }
    }
}
