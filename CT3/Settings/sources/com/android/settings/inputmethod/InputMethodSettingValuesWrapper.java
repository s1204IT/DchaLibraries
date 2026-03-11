package com.android.settings.inputmethod;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.inputmethod.InputMethodUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

class InputMethodSettingValuesWrapper {
    private static final String TAG = InputMethodSettingValuesWrapper.class.getSimpleName();
    private static volatile InputMethodSettingValuesWrapper sInstance;
    private final InputMethodManager mImm;
    private final InputMethodUtils.InputMethodSettings mSettings;
    private final ArrayList<InputMethodInfo> mMethodList = new ArrayList<>();
    private final HashMap<String, InputMethodInfo> mMethodMap = new HashMap<>();
    private final HashSet<InputMethodInfo> mAsciiCapableEnabledImis = new HashSet<>();

    static InputMethodSettingValuesWrapper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (TAG) {
                if (sInstance == null) {
                    sInstance = new InputMethodSettingValuesWrapper(context);
                }
            }
        }
        return sInstance;
    }

    private static int getDefaultCurrentUserId() {
        try {
            return ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
            return 0;
        }
    }

    private InputMethodSettingValuesWrapper(Context context) {
        this.mSettings = new InputMethodUtils.InputMethodSettings(context.getResources(), context.getContentResolver(), this.mMethodMap, this.mMethodList, getDefaultCurrentUserId(), false);
        this.mImm = (InputMethodManager) context.getSystemService("input_method");
        refreshAllInputMethodAndSubtypes();
    }

    void refreshAllInputMethodAndSubtypes() {
        synchronized (this.mMethodMap) {
            this.mMethodList.clear();
            this.mMethodMap.clear();
            List<InputMethodInfo> imms = this.mImm.getInputMethodList();
            this.mMethodList.addAll(imms);
            for (InputMethodInfo imi : imms) {
                this.mMethodMap.put(imi.getId(), imi);
            }
            updateAsciiCapableEnabledImis();
        }
    }

    private void updateAsciiCapableEnabledImis() {
        synchronized (this.mMethodMap) {
            this.mAsciiCapableEnabledImis.clear();
            List<InputMethodInfo> enabledImis = this.mSettings.getEnabledInputMethodListLocked();
            for (InputMethodInfo imi : enabledImis) {
                int subtypeCount = imi.getSubtypeCount();
                int i = 0;
                while (true) {
                    if (i < subtypeCount) {
                        InputMethodSubtype subtype = imi.getSubtypeAt(i);
                        if (!"keyboard".equalsIgnoreCase(subtype.getMode()) || !subtype.isAsciiCapable()) {
                            i++;
                        } else {
                            this.mAsciiCapableEnabledImis.add(imi);
                            break;
                        }
                    }
                }
            }
        }
    }

    List<InputMethodInfo> getInputMethodList() {
        ArrayList<InputMethodInfo> arrayList;
        synchronized (this.mMethodMap) {
            arrayList = this.mMethodList;
        }
        return arrayList;
    }

    CharSequence getCurrentInputMethodName(Context context) {
        synchronized (this.mMethodMap) {
            InputMethodInfo imi = this.mMethodMap.get(this.mSettings.getSelectedInputMethod());
            if (imi == null) {
                Log.w(TAG, "Invalid selected imi: " + this.mSettings.getSelectedInputMethod());
                return "";
            }
            InputMethodSubtype subtype = this.mImm.getCurrentInputMethodSubtype();
            return InputMethodUtils.getImeAndSubtypeDisplayName(context, imi, subtype);
        }
    }

    boolean isAlwaysCheckedIme(InputMethodInfo imi, Context context) {
        boolean isEnabled = isEnabledImi(imi);
        synchronized (this.mMethodMap) {
            if (this.mSettings.getEnabledInputMethodListLocked().size() <= 1 && isEnabled) {
                return true;
            }
            int enabledValidSystemNonAuxAsciiCapableImeCount = getEnabledValidSystemNonAuxAsciiCapableImeCount(context);
            if (enabledValidSystemNonAuxAsciiCapableImeCount > 1) {
                return false;
            }
            if ((enabledValidSystemNonAuxAsciiCapableImeCount != 1 || isEnabled) && InputMethodUtils.isSystemIme(imi)) {
                return isValidSystemNonAuxAsciiCapableIme(imi, context);
            }
            return false;
        }
    }

    private int getEnabledValidSystemNonAuxAsciiCapableImeCount(Context context) {
        List<InputMethodInfo> enabledImis;
        int count = 0;
        synchronized (this.mMethodMap) {
            enabledImis = this.mSettings.getEnabledInputMethodListLocked();
        }
        for (InputMethodInfo imi : enabledImis) {
            if (isValidSystemNonAuxAsciiCapableIme(imi, context)) {
                count++;
            }
        }
        if (count == 0) {
            Log.w(TAG, "No \"enabledValidSystemNonAuxAsciiCapableIme\"s found.");
        }
        return count;
    }

    boolean isEnabledImi(InputMethodInfo imi) {
        List<InputMethodInfo> enabledImis;
        synchronized (this.mMethodMap) {
            enabledImis = this.mSettings.getEnabledInputMethodListLocked();
        }
        for (InputMethodInfo tempImi : enabledImis) {
            if (tempImi.getId().equals(imi.getId())) {
                return true;
            }
        }
        return false;
    }

    boolean isValidSystemNonAuxAsciiCapableIme(InputMethodInfo imi, Context context) {
        if (imi.isAuxiliaryIme()) {
            return false;
        }
        Locale systemLocale = context.getResources().getConfiguration().locale;
        if (InputMethodUtils.isSystemImeThatHasSubtypeOf(imi, context, true, systemLocale, false, InputMethodUtils.SUBTYPE_MODE_ANY)) {
            return true;
        }
        if (this.mAsciiCapableEnabledImis.isEmpty()) {
            Log.w(TAG, "ascii capable subtype enabled imi not found. Fall back to English Keyboard subtype.");
            return InputMethodUtils.containsSubtypeOf(imi, Locale.ENGLISH, false, "keyboard");
        }
        return this.mAsciiCapableEnabledImis.contains(imi);
    }
}
