package com.android.settings.inputmethod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.InputDevice;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.inputmethod.KeyboardLayoutDialogFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.utils.ThreadUtils;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public final class PhysicalKeyboardFragment extends SettingsPreferenceFragment implements InputManager.InputDeviceListener, KeyboardLayoutDialogFragment.OnSetupKeyboardLayoutsListener, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.physical_keyboard_settings;
            return Arrays.asList(searchIndexableResource);
        }
    };
    private InputManager mIm;
    private Intent mIntentWaitingForResult;
    private PreferenceCategory mKeyboardAssistanceCategory;
    private InputMethodUtils.InputMethodSettings mSettings;
    private SwitchPreference mShowVirtualKeyboardSwitch;
    private final ArrayList<HardKeyboardDeviceInfo> mLastHardKeyboards = new ArrayList<>();
    private final Preference.OnPreferenceChangeListener mShowVirtualKeyboardSwitchPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object obj) {
            PhysicalKeyboardFragment.this.mSettings.setShowImeWithHardKeyboard(((Boolean) obj).booleanValue());
            return true;
        }
    };
    private final ContentObserver mContentObserver = new ContentObserver(new Handler(true)) {
        @Override
        public void onChange(boolean z) {
            PhysicalKeyboardFragment.this.updateShowVirtualKeyboardSwitch();
        }
    };

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        Activity activity = (Activity) Preconditions.checkNotNull(getActivity());
        addPreferencesFromResource(R.xml.physical_keyboard_settings);
        this.mIm = (InputManager) Preconditions.checkNotNull((InputManager) activity.getSystemService(InputManager.class));
        this.mSettings = new InputMethodUtils.InputMethodSettings(activity.getResources(), getContentResolver(), new HashMap(), new ArrayList(), UserHandle.myUserId(), false);
        this.mKeyboardAssistanceCategory = (PreferenceCategory) Preconditions.checkNotNull((PreferenceCategory) findPreference("keyboard_assistance_category"));
        this.mShowVirtualKeyboardSwitch = (SwitchPreference) Preconditions.checkNotNull((SwitchPreference) this.mKeyboardAssistanceCategory.findPreference("show_virtual_keyboard_switch"));
        findPreference("keyboard_shortcuts_helper").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                PhysicalKeyboardFragment.this.toggleKeyboardShortcutsMenu();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mLastHardKeyboards.clear();
        scheduleUpdateHardKeyboards();
        this.mIm.registerInputDeviceListener(this, null);
        this.mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(this.mShowVirtualKeyboardSwitchPreferenceChangeListener);
        registerShowVirtualKeyboardSettingsObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mLastHardKeyboards.clear();
        this.mIm.unregisterInputDeviceListener(this);
        this.mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(null);
        unregisterShowVirtualKeyboardSettingsObserver();
    }

    @Override
    public void onInputDeviceAdded(int i) {
        scheduleUpdateHardKeyboards();
    }

    @Override
    public void onInputDeviceRemoved(int i) {
        scheduleUpdateHardKeyboards();
    }

    @Override
    public void onInputDeviceChanged(int i) {
        scheduleUpdateHardKeyboards();
    }

    @Override
    public int getMetricsCategory() {
        return 346;
    }

    private void scheduleUpdateHardKeyboards() {
        final Context context = getContext();
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                PhysicalKeyboardFragment.lambda$scheduleUpdateHardKeyboards$1(this.f$0, context);
            }
        });
    }

    public static void lambda$scheduleUpdateHardKeyboards$1(final PhysicalKeyboardFragment physicalKeyboardFragment, Context context) {
        final List<HardKeyboardDeviceInfo> hardKeyboards = getHardKeyboards(context);
        ThreadUtils.postOnMainThread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateHardKeyboards(hardKeyboards);
            }
        });
    }

    public void updateHardKeyboards(List<HardKeyboardDeviceInfo> list) {
        if (Objects.equals(this.mLastHardKeyboards, list)) {
            return;
        }
        this.mLastHardKeyboards.clear();
        this.mLastHardKeyboards.addAll(list);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        PreferenceCategory preferenceCategory = new PreferenceCategory(getPrefContext());
        preferenceCategory.setTitle(R.string.builtin_keyboard_settings_title);
        preferenceCategory.setOrder(0);
        preferenceScreen.addPreference(preferenceCategory);
        for (final HardKeyboardDeviceInfo hardKeyboardDeviceInfo : list) {
            Preference preference = new Preference(getPrefContext());
            preference.setTitle(hardKeyboardDeviceInfo.mDeviceName);
            preference.setSummary(hardKeyboardDeviceInfo.mLayoutLabel);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public final boolean onPreferenceClick(Preference preference2) {
                    return PhysicalKeyboardFragment.lambda$updateHardKeyboards$2(this.f$0, hardKeyboardDeviceInfo, preference2);
                }
            });
            preferenceCategory.addPreference(preference);
        }
        this.mKeyboardAssistanceCategory.setOrder(1);
        preferenceScreen.addPreference(this.mKeyboardAssistanceCategory);
        updateShowVirtualKeyboardSwitch();
    }

    public static boolean lambda$updateHardKeyboards$2(PhysicalKeyboardFragment physicalKeyboardFragment, HardKeyboardDeviceInfo hardKeyboardDeviceInfo, Preference preference) {
        physicalKeyboardFragment.showKeyboardLayoutDialog(hardKeyboardDeviceInfo.mDeviceIdentifier);
        return true;
    }

    private void showKeyboardLayoutDialog(InputDeviceIdentifier inputDeviceIdentifier) {
        KeyboardLayoutDialogFragment keyboardLayoutDialogFragment = new KeyboardLayoutDialogFragment(inputDeviceIdentifier);
        keyboardLayoutDialogFragment.setTargetFragment(this, 0);
        keyboardLayoutDialogFragment.show(getActivity().getFragmentManager(), "keyboardLayout");
    }

    private void registerShowVirtualKeyboardSettingsObserver() {
        unregisterShowVirtualKeyboardSettingsObserver();
        getActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor("show_ime_with_hard_keyboard"), false, this.mContentObserver, UserHandle.myUserId());
        updateShowVirtualKeyboardSwitch();
    }

    private void unregisterShowVirtualKeyboardSettingsObserver() {
        getActivity().getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    public void updateShowVirtualKeyboardSwitch() {
        this.mShowVirtualKeyboardSwitch.setChecked(this.mSettings.isShowImeWithHardKeyboardEnabled());
    }

    public void toggleKeyboardShortcutsMenu() {
        getActivity().requestShowKeyboardShortcuts();
    }

    @Override
    public void onSetupKeyboardLayouts(InputDeviceIdentifier inputDeviceIdentifier) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(getActivity(), Settings.KeyboardLayoutPickerActivity.class);
        intent.putExtra("input_device_identifier", (Parcelable) inputDeviceIdentifier);
        this.mIntentWaitingForResult = intent;
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (this.mIntentWaitingForResult != null) {
            InputDeviceIdentifier inputDeviceIdentifier = (InputDeviceIdentifier) this.mIntentWaitingForResult.getParcelableExtra("input_device_identifier");
            this.mIntentWaitingForResult = null;
            showKeyboardLayoutDialog(inputDeviceIdentifier);
        }
    }

    private static String getLayoutLabel(InputDevice inputDevice, Context context, InputManager inputManager) {
        String currentKeyboardLayoutForInputDevice = inputManager.getCurrentKeyboardLayoutForInputDevice(inputDevice.getIdentifier());
        if (currentKeyboardLayoutForInputDevice == null) {
            return context.getString(R.string.keyboard_layout_default_label);
        }
        KeyboardLayout keyboardLayout = inputManager.getKeyboardLayout(currentKeyboardLayoutForInputDevice);
        if (keyboardLayout == null) {
            return context.getString(R.string.keyboard_layout_default_label);
        }
        return TextUtils.emptyIfNull(keyboardLayout.getLabel());
    }

    static List<HardKeyboardDeviceInfo> getHardKeyboards(Context context) {
        ArrayList arrayList = new ArrayList();
        InputManager inputManager = (InputManager) context.getSystemService(InputManager.class);
        if (inputManager == null) {
            return new ArrayList();
        }
        for (int i : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(i);
            if (device != null && !device.isVirtual() && device.isFullKeyboard()) {
                arrayList.add(new HardKeyboardDeviceInfo(device.getName(), device.getIdentifier(), getLayoutLabel(device, context, inputManager)));
            }
        }
        final Collator collator = Collator.getInstance();
        arrayList.sort(new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return PhysicalKeyboardFragment.lambda$getHardKeyboards$3(collator, (PhysicalKeyboardFragment.HardKeyboardDeviceInfo) obj, (PhysicalKeyboardFragment.HardKeyboardDeviceInfo) obj2);
            }
        });
        return arrayList;
    }

    static int lambda$getHardKeyboards$3(Collator collator, HardKeyboardDeviceInfo hardKeyboardDeviceInfo, HardKeyboardDeviceInfo hardKeyboardDeviceInfo2) {
        int iCompare = collator.compare(hardKeyboardDeviceInfo.mDeviceName, hardKeyboardDeviceInfo2.mDeviceName);
        if (iCompare != 0) {
            return iCompare;
        }
        int iCompareTo = hardKeyboardDeviceInfo.mDeviceIdentifier.getDescriptor().compareTo(hardKeyboardDeviceInfo2.mDeviceIdentifier.getDescriptor());
        if (iCompareTo != 0) {
            return iCompareTo;
        }
        return collator.compare(hardKeyboardDeviceInfo.mLayoutLabel, hardKeyboardDeviceInfo2.mLayoutLabel);
    }

    public static final class HardKeyboardDeviceInfo {
        public final InputDeviceIdentifier mDeviceIdentifier;
        public final String mDeviceName;
        public final String mLayoutLabel;

        public HardKeyboardDeviceInfo(String str, InputDeviceIdentifier inputDeviceIdentifier, String str2) {
            this.mDeviceName = TextUtils.emptyIfNull(str);
            this.mDeviceIdentifier = inputDeviceIdentifier;
            this.mLayoutLabel = str2;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !(obj instanceof HardKeyboardDeviceInfo)) {
                return false;
            }
            HardKeyboardDeviceInfo hardKeyboardDeviceInfo = (HardKeyboardDeviceInfo) obj;
            if (TextUtils.equals(this.mDeviceName, hardKeyboardDeviceInfo.mDeviceName) && Objects.equals(this.mDeviceIdentifier, hardKeyboardDeviceInfo.mDeviceIdentifier) && TextUtils.equals(this.mLayoutLabel, hardKeyboardDeviceInfo.mLayoutLabel)) {
                return true;
            }
            return false;
        }
    }
}
