package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class PhysicalKeyboardFragment extends SettingsPreferenceFragment implements InputManager.InputDeviceListener {
    private InputManager mIm;
    private PreferenceCategory mKeyboardAssistanceCategory;
    private InputMethodUtils.InputMethodSettings mSettings;
    private SwitchPreference mShowVirtualKeyboardSwitch;
    private final List<HardKeyboardDeviceInfo> mLastHardKeyboards = new ArrayList();
    private final List<KeyboardInfoPreference> mTempKeyboardInfoList = new ArrayList();
    private final HashSet<Integer> mLoaderIDs = new HashSet<>();
    private int mNextLoaderId = 0;
    private final Preference.OnPreferenceChangeListener mShowVirtualKeyboardSwitchPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            PhysicalKeyboardFragment.this.mSettings.setShowImeWithHardKeyboard(((Boolean) newValue).booleanValue());
            return false;
        }
    };
    private final ContentObserver mContentObserver = new ContentObserver(new Handler(true)) {
        @Override
        public void onChange(boolean selfChange) {
            PhysicalKeyboardFragment.this.updateShowVirtualKeyboardSwitch();
        }
    };

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
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
        clearLoader();
        this.mLastHardKeyboards.clear();
        updateHardKeyboards();
        this.mIm.registerInputDeviceListener(this, null);
        this.mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(this.mShowVirtualKeyboardSwitchPreferenceChangeListener);
        registerShowVirtualKeyboardSettingsObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        clearLoader();
        this.mLastHardKeyboards.clear();
        this.mIm.unregisterInputDeviceListener(this);
        this.mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(null);
        unregisterShowVirtualKeyboardSettingsObserver();
    }

    public void onLoadFinishedInternal(int loaderId, List<Keyboards> keyboardsList) {
        KeyboardInfoPreference keyboardInfoPreference = null;
        if (!this.mLoaderIDs.remove(Integer.valueOf(loaderId))) {
            return;
        }
        Collections.sort(keyboardsList);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        for (final Keyboards keyboards : keyboardsList) {
            PreferenceCategory category = new PreferenceCategory(getPrefContext(), null);
            category.setTitle(keyboards.mDeviceInfo.mDeviceName);
            category.setOrder(0);
            preferenceScreen.addPreference(category);
            for (Keyboards.KeyboardInfo info : keyboards.mKeyboardInfoList) {
                this.mTempKeyboardInfoList.clear();
                final InputMethodInfo imi = info.mImi;
                final InputMethodSubtype imSubtype = info.mImSubtype;
                if (imi != null) {
                    KeyboardInfoPreference pref = new KeyboardInfoPreference(getPrefContext(), info, keyboardInfoPreference);
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            return PhysicalKeyboardFragment.this.m955xf9541f50(keyboards, imi, imSubtype, arg0);
                        }
                    });
                    this.mTempKeyboardInfoList.add(pref);
                    Collections.sort(this.mTempKeyboardInfoList);
                }
                Iterator pref$iterator = this.mTempKeyboardInfoList.iterator();
                while (pref$iterator.hasNext()) {
                    category.addPreference((KeyboardInfoPreference) pref$iterator.next());
                }
            }
        }
        this.mTempKeyboardInfoList.clear();
        this.mKeyboardAssistanceCategory.setOrder(1);
        preferenceScreen.addPreference(this.mKeyboardAssistanceCategory);
        updateShowVirtualKeyboardSwitch();
    }

    boolean m955xf9541f50(Keyboards keyboards, InputMethodInfo imi, InputMethodSubtype imSubtype, Preference preference) {
        showKeyboardLayoutScreen(keyboards.mDeviceInfo.mDeviceIdentifier, imi, imSubtype);
        return true;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateHardKeyboards();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateHardKeyboards();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateHardKeyboards();
    }

    @Override
    protected int getMetricsCategory() {
        return 346;
    }

    private static ArrayList<HardKeyboardDeviceInfo> getHardKeyboards() {
        ArrayList<HardKeyboardDeviceInfo> keyboards = new ArrayList<>();
        int[] devicesIds = InputDevice.getDeviceIds();
        for (int deviceId : devicesIds) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null && !device.isVirtual() && device.isFullKeyboard()) {
                keyboards.add(new HardKeyboardDeviceInfo(device.getName(), device.getIdentifier()));
            }
        }
        return keyboards;
    }

    private void updateHardKeyboards() {
        ArrayList<HardKeyboardDeviceInfo> newHardKeyboards = getHardKeyboards();
        if (Objects.equals(newHardKeyboards, this.mLastHardKeyboards)) {
            return;
        }
        clearLoader();
        this.mLastHardKeyboards.clear();
        this.mLastHardKeyboards.addAll(newHardKeyboards);
        getLoaderManager().initLoader(this.mNextLoaderId, null, new Callbacks(getContext(), this, this.mLastHardKeyboards));
        this.mLoaderIDs.add(Integer.valueOf(this.mNextLoaderId));
        this.mNextLoaderId++;
    }

    private void showKeyboardLayoutScreen(InputDeviceIdentifier inputDeviceIdentifier, InputMethodInfo imi, InputMethodSubtype imSubtype) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(getActivity(), Settings.KeyboardLayoutPickerActivity.class);
        intent.putExtra("input_device_identifier", (Parcelable) inputDeviceIdentifier);
        intent.putExtra("input_method_info", imi);
        intent.putExtra("input_method_subtype", imSubtype);
        startActivity(intent);
    }

    private void clearLoader() {
        Iterator loaderId$iterator = this.mLoaderIDs.iterator();
        while (loaderId$iterator.hasNext()) {
            int loaderId = ((Integer) loaderId$iterator.next()).intValue();
            getLoaderManager().destroyLoader(loaderId);
        }
        this.mLoaderIDs.clear();
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

    private static final class Callbacks implements LoaderManager.LoaderCallbacks<List<Keyboards>> {
        final Context mContext;
        final List<HardKeyboardDeviceInfo> mHardKeyboards;
        final PhysicalKeyboardFragment mPhysicalKeyboardFragment;

        public Callbacks(Context context, PhysicalKeyboardFragment physicalKeyboardFragment, List<HardKeyboardDeviceInfo> hardKeyboards) {
            this.mContext = context;
            this.mPhysicalKeyboardFragment = physicalKeyboardFragment;
            this.mHardKeyboards = hardKeyboards;
        }

        @Override
        public Loader<List<Keyboards>> onCreateLoader(int id, Bundle args) {
            return new KeyboardLayoutLoader(this.mContext, this.mHardKeyboards);
        }

        @Override
        public void onLoadFinished(Loader<List<Keyboards>> loader, List<Keyboards> data) {
            this.mPhysicalKeyboardFragment.onLoadFinishedInternal(loader.getId(), data);
        }

        @Override
        public void onLoaderReset(Loader<List<Keyboards>> loader) {
        }
    }

    private static final class KeyboardLayoutLoader extends AsyncTaskLoader<List<Keyboards>> {
        private final List<HardKeyboardDeviceInfo> mHardKeyboards;

        public KeyboardLayoutLoader(Context context, List<HardKeyboardDeviceInfo> hardKeyboards) {
            super(context);
            this.mHardKeyboards = (List) Preconditions.checkNotNull(hardKeyboards);
        }

        private Keyboards loadInBackground(HardKeyboardDeviceInfo deviceInfo) {
            ArrayList<Keyboards.KeyboardInfo> keyboardInfoList = new ArrayList<>();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(InputMethodManager.class);
            InputManager im = (InputManager) getContext().getSystemService(InputManager.class);
            if (imm != null && im != null) {
                for (InputMethodInfo imi : imm.getEnabledInputMethodList()) {
                    List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
                    if (subtypes.isEmpty()) {
                        KeyboardLayout layout = im.getKeyboardLayoutForInputDevice(deviceInfo.mDeviceIdentifier, imi, null);
                        keyboardInfoList.add(new Keyboards.KeyboardInfo(imi, null, layout));
                    } else {
                        int N = subtypes.size();
                        for (int i = 0; i < N; i++) {
                            InputMethodSubtype subtype = subtypes.get(i);
                            if ("keyboard".equalsIgnoreCase(subtype.getMode())) {
                                KeyboardLayout layout2 = im.getKeyboardLayoutForInputDevice(deviceInfo.mDeviceIdentifier, imi, subtype);
                                keyboardInfoList.add(new Keyboards.KeyboardInfo(imi, subtype, layout2));
                            }
                        }
                    }
                }
            }
            return new Keyboards(deviceInfo, keyboardInfoList);
        }

        @Override
        public List<Keyboards> loadInBackground() {
            List<Keyboards> keyboardsList = new ArrayList<>(this.mHardKeyboards.size());
            for (HardKeyboardDeviceInfo deviceInfo : this.mHardKeyboards) {
                keyboardsList.add(loadInBackground(deviceInfo));
            }
            return keyboardsList;
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            super.onStopLoading();
            cancelLoad();
        }
    }

    public static final class HardKeyboardDeviceInfo {
        public final InputDeviceIdentifier mDeviceIdentifier;
        public final String mDeviceName;

        public HardKeyboardDeviceInfo(String deviceName, InputDeviceIdentifier deviceIdentifier) {
            this.mDeviceName = deviceName == null ? "" : deviceName;
            this.mDeviceIdentifier = deviceIdentifier;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null || !(o instanceof HardKeyboardDeviceInfo)) {
                return false;
            }
            HardKeyboardDeviceInfo that = (HardKeyboardDeviceInfo) o;
            return TextUtils.equals(this.mDeviceName, that.mDeviceName) && this.mDeviceIdentifier.getVendorId() == that.mDeviceIdentifier.getVendorId() && this.mDeviceIdentifier.getProductId() == that.mDeviceIdentifier.getProductId() && TextUtils.equals(this.mDeviceIdentifier.getDescriptor(), that.mDeviceIdentifier.getDescriptor());
        }
    }

    public static final class Keyboards implements Comparable<Keyboards> {
        public final Collator mCollator = Collator.getInstance();
        public final HardKeyboardDeviceInfo mDeviceInfo;
        public final ArrayList<KeyboardInfo> mKeyboardInfoList;

        public Keyboards(HardKeyboardDeviceInfo deviceInfo, ArrayList<KeyboardInfo> keyboardInfoList) {
            this.mDeviceInfo = deviceInfo;
            this.mKeyboardInfoList = keyboardInfoList;
        }

        @Override
        public int compareTo(Keyboards another) {
            return this.mCollator.compare(this.mDeviceInfo.mDeviceName, another.mDeviceInfo.mDeviceName);
        }

        public static final class KeyboardInfo {
            public final InputMethodSubtype mImSubtype;
            public final InputMethodInfo mImi;
            public final KeyboardLayout mLayout;

            public KeyboardInfo(InputMethodInfo imi, InputMethodSubtype imSubtype, KeyboardLayout layout) {
                this.mImi = imi;
                this.mImSubtype = imSubtype;
                this.mLayout = layout;
            }
        }
    }

    static final class KeyboardInfoPreference extends Preference {
        private final Collator collator;
        private final CharSequence mImSubtypeName;
        private final CharSequence mImeName;

        KeyboardInfoPreference(Context context, Keyboards.KeyboardInfo info, KeyboardInfoPreference keyboardInfoPreference) {
            this(context, info);
        }

        private KeyboardInfoPreference(Context context, Keyboards.KeyboardInfo info) {
            super(context);
            this.collator = Collator.getInstance();
            this.mImeName = info.mImi.loadLabel(context.getPackageManager());
            this.mImSubtypeName = getImSubtypeName(context, info.mImi, info.mImSubtype);
            setTitle(formatDisplayName(context, this.mImeName, this.mImSubtypeName));
            if (info.mLayout == null) {
                return;
            }
            setSummary(info.mLayout.getLabel());
        }

        static CharSequence getDisplayName(Context context, InputMethodInfo imi, InputMethodSubtype imSubtype) {
            CharSequence imeName = imi.loadLabel(context.getPackageManager());
            CharSequence imSubtypeName = getImSubtypeName(context, imi, imSubtype);
            return formatDisplayName(context, imeName, imSubtypeName);
        }

        private static CharSequence formatDisplayName(Context context, CharSequence imeName, CharSequence imSubtypeName) {
            if (imSubtypeName == null) {
                return imeName;
            }
            return String.format(context.getString(R.string.physical_device_title), imeName, imSubtypeName);
        }

        private static CharSequence getImSubtypeName(Context context, InputMethodInfo imi, InputMethodSubtype imSubtype) {
            if (imSubtype != null) {
                return InputMethodAndSubtypeUtil.getSubtypeLocaleNameAsSentence(imSubtype, context, imi);
            }
            return null;
        }

        @Override
        public int compareTo(Preference object) {
            if (!(object instanceof KeyboardInfoPreference)) {
                return super.compareTo(object);
            }
            KeyboardInfoPreference another = (KeyboardInfoPreference) object;
            int result = compare(this.mImeName, another.mImeName);
            if (result == 0) {
                return compare(this.mImSubtypeName, another.mImSubtypeName);
            }
            return result;
        }

        private int compare(CharSequence lhs, CharSequence rhs) {
            if (!TextUtils.isEmpty(lhs) && !TextUtils.isEmpty(rhs)) {
                return this.collator.compare(lhs.toString(), rhs.toString());
            }
            if (TextUtils.isEmpty(lhs) && TextUtils.isEmpty(rhs)) {
                return 0;
            }
            if (!TextUtils.isEmpty(lhs)) {
                return -1;
            }
            return 1;
        }
    }
}
