package com.android.settingslib.inputmethod;

import android.app.ActivityManager;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public class InputMethodSettingValuesWrapper {
    private static final String TAG = InputMethodSettingValuesWrapper.class.getSimpleName();
    private static volatile InputMethodSettingValuesWrapper sInstance;
    private final InputMethodManager mImm;
    private final InputMethodUtils.InputMethodSettings mSettings;
    private final ArrayList<InputMethodInfo> mMethodList = new ArrayList<>();
    private final HashMap<String, InputMethodInfo> mMethodMap = new HashMap<>();
    private final HashSet<InputMethodInfo> mAsciiCapableEnabledImis = new HashSet<>();

    public static InputMethodSettingValuesWrapper getInstance(Context context) {
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
            return ActivityManager.getService().getCurrentUser().id;
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

    public void refreshAllInputMethodAndSubtypes() {
        synchronized (this.mMethodMap) {
            this.mMethodList.clear();
            this.mMethodMap.clear();
            List<InputMethodInfo> inputMethodList = this.mImm.getInputMethodList();
            this.mMethodList.addAll(inputMethodList);
            for (InputMethodInfo inputMethodInfo : inputMethodList) {
                this.mMethodMap.put(inputMethodInfo.getId(), inputMethodInfo);
            }
            updateAsciiCapableEnabledImis();
        }
    }

    private void updateAsciiCapableEnabledImis() {
        synchronized (this.mMethodMap) {
            this.mAsciiCapableEnabledImis.clear();
            for (InputMethodInfo inputMethodInfo : this.mSettings.getEnabledInputMethodListLocked()) {
                int subtypeCount = inputMethodInfo.getSubtypeCount();
                int i = 0;
                while (true) {
                    if (i < subtypeCount) {
                        InputMethodSubtype subtypeAt = inputMethodInfo.getSubtypeAt(i);
                        if (!"keyboard".equalsIgnoreCase(subtypeAt.getMode()) || !subtypeAt.isAsciiCapable()) {
                            i++;
                        } else {
                            this.mAsciiCapableEnabledImis.add(inputMethodInfo);
                            break;
                        }
                    }
                }
            }
        }
    }

    public List<InputMethodInfo> getInputMethodList() {
        ArrayList<InputMethodInfo> arrayList;
        synchronized (this.mMethodMap) {
            arrayList = this.mMethodList;
        }
        return arrayList;
    }

    public boolean isAlwaysCheckedIme(InputMethodInfo inputMethodInfo, Context context) {
        boolean zIsEnabledImi = isEnabledImi(inputMethodInfo);
        synchronized (this.mMethodMap) {
            if (this.mSettings.getEnabledInputMethodListLocked().size() <= 1 && zIsEnabledImi) {
                return true;
            }
            int enabledValidSystemNonAuxAsciiCapableImeCount = getEnabledValidSystemNonAuxAsciiCapableImeCount(context);
            return enabledValidSystemNonAuxAsciiCapableImeCount <= 1 && (enabledValidSystemNonAuxAsciiCapableImeCount != 1 || zIsEnabledImi) && InputMethodUtils.isSystemIme(inputMethodInfo) && isValidSystemNonAuxAsciiCapableIme(inputMethodInfo, context);
        }
    }

    private int getEnabledValidSystemNonAuxAsciiCapableImeCount(Context context) {
        ArrayList enabledInputMethodListLocked;
        synchronized (this.mMethodMap) {
            enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
        }
        Iterator it = enabledInputMethodListLocked.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (isValidSystemNonAuxAsciiCapableIme((InputMethodInfo) it.next(), context)) {
                i++;
            }
        }
        if (i == 0) {
            Log.w(TAG, "No \"enabledValidSystemNonAuxAsciiCapableIme\"s found.");
        }
        return i;
    }

    public boolean isEnabledImi(InputMethodInfo inputMethodInfo) {
        ArrayList enabledInputMethodListLocked;
        synchronized (this.mMethodMap) {
            enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
        }
        Iterator it = enabledInputMethodListLocked.iterator();
        while (it.hasNext()) {
            if (((InputMethodInfo) it.next()).getId().equals(inputMethodInfo.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidSystemNonAuxAsciiCapableIme(InputMethodInfo inputMethodInfo, Context context) {
        if (inputMethodInfo.isAuxiliaryIme()) {
            return false;
        }
        if (InputMethodUtils.isSystemImeThatHasSubtypeOf(inputMethodInfo, context, true, context.getResources().getConfiguration().locale, false, InputMethodUtils.SUBTYPE_MODE_ANY)) {
            return true;
        }
        if (this.mAsciiCapableEnabledImis.isEmpty()) {
            Log.w(TAG, "ascii capable subtype enabled imi not found. Fall back to English Keyboard subtype.");
            return InputMethodUtils.containsSubtypeOf(inputMethodInfo, Locale.ENGLISH, false, "keyboard");
        }
        return this.mAsciiCapableEnabledImis.contains(inputMethodInfo);
    }
}
