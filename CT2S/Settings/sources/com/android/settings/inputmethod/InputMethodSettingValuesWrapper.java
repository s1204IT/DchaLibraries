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
        this.mSettings = new InputMethodUtils.InputMethodSettings(context.getResources(), context.getContentResolver(), this.mMethodMap, this.mMethodList, getDefaultCurrentUserId());
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
        CharSequence imeAndSubtypeDisplayName;
        synchronized (this.mMethodMap) {
            InputMethodInfo imi = this.mMethodMap.get(this.mSettings.getSelectedInputMethod());
            if (imi == null) {
                Log.w(TAG, "Invalid selected imi: " + this.mSettings.getSelectedInputMethod());
                imeAndSubtypeDisplayName = "";
            } else {
                InputMethodSubtype subtype = this.mImm.getCurrentInputMethodSubtype();
                imeAndSubtypeDisplayName = InputMethodUtils.getImeAndSubtypeDisplayName(context, imi, subtype);
            }
        }
        return imeAndSubtypeDisplayName;
    }

    boolean isAlwaysCheckedIme(InputMethodInfo imi, Context context) {
        List<InputMethodInfo> enabledImis;
        boolean zIsValidSystemNonAuxAsciiCapableIme = true;
        boolean isEnabled = isEnabledImi(imi);
        synchronized (this.mMethodMap) {
            enabledImis = this.mSettings.getEnabledInputMethodListLocked();
        }
        if (imi.getPackageName().equals("jp.co.omronsoft.iwnnime.ml")) {
            int mozc_cnt = 0;
            for (InputMethodInfo tempImi : enabledImis) {
                if (tempImi.getPackageName().equals("com.google.android.inputmethod.japanese")) {
                    mozc_cnt++;
                }
            }
            if (mozc_cnt != 0) {
                if (imi.getPackageName().equals("com.google.android.inputmethod.japanese")) {
                    int iWnn_cnt = 0;
                    for (InputMethodInfo tempImi2 : enabledImis) {
                        if (tempImi2.getPackageName().equals("jp.co.omronsoft.iwnnime.ml")) {
                            iWnn_cnt++;
                        }
                    }
                    if (iWnn_cnt != 0) {
                        synchronized (this.mMethodMap) {
                            if (this.mSettings.getEnabledInputMethodListLocked().size() > 1 || !isEnabled) {
                                int enabledValidSystemNonAuxAsciiCapableImeCount = getEnabledValidSystemNonAuxAsciiCapableImeCount(context);
                                if (enabledValidSystemNonAuxAsciiCapableImeCount > 1) {
                                    zIsValidSystemNonAuxAsciiCapableIme = false;
                                } else {
                                    zIsValidSystemNonAuxAsciiCapableIme = ((enabledValidSystemNonAuxAsciiCapableImeCount != 1 || isEnabled) && InputMethodUtils.isSystemIme(imi)) ? isValidSystemNonAuxAsciiCapableIme(imi, context) : false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return zIsValidSystemNonAuxAsciiCapableIme;
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
        if (InputMethodUtils.isValidSystemDefaultIme(true, imi, context)) {
            return true;
        }
        if (this.mAsciiCapableEnabledImis.isEmpty()) {
            Log.w(TAG, "ascii capable subtype enabled imi not found. Fall back to English Keyboard subtype.");
            return InputMethodUtils.containsSubtypeOf(imi, Locale.ENGLISH.getLanguage(), "keyboard");
        }
        return this.mAsciiCapableEnabledImis.contains(imi);
    }
}
