package com.android.internal.inputmethod;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Printer;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.inputmethod.InputMethodUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

public class InputMethodSubtypeSwitchingController {
    private static final boolean DEBUG = false;
    private static final int NOT_A_SUBTYPE_ID = -1;
    private static final String TAG = InputMethodSubtypeSwitchingController.class.getSimpleName();
    private ControllerImpl mController;
    private final InputMethodUtils.InputMethodSettings mSettings;
    private InputMethodAndSubtypeList mSubtypeList;

    public static class ImeSubtypeListItem implements Comparable<ImeSubtypeListItem> {
        public final CharSequence mImeName;
        public final InputMethodInfo mImi;
        public final boolean mIsSystemLanguage;
        public final boolean mIsSystemLocale;
        public final int mSubtypeId;
        public final CharSequence mSubtypeName;

        public ImeSubtypeListItem(CharSequence charSequence, CharSequence charSequence2, InputMethodInfo inputMethodInfo, int i, String str, String str2) {
            this.mImeName = charSequence;
            this.mSubtypeName = charSequence2;
            this.mImi = inputMethodInfo;
            this.mSubtypeId = i;
            boolean z = false;
            if (TextUtils.isEmpty(str)) {
                this.mIsSystemLocale = false;
                this.mIsSystemLanguage = false;
                return;
            }
            this.mIsSystemLocale = str.equals(str2);
            if (this.mIsSystemLocale) {
                this.mIsSystemLanguage = true;
                return;
            }
            String languageFromLocaleString = parseLanguageFromLocaleString(str2);
            String languageFromLocaleString2 = parseLanguageFromLocaleString(str);
            if (languageFromLocaleString.length() >= 2 && languageFromLocaleString.equals(languageFromLocaleString2)) {
                z = true;
            }
            this.mIsSystemLanguage = z;
        }

        private static String parseLanguageFromLocaleString(String str) {
            int iIndexOf = str.indexOf(95);
            if (iIndexOf < 0) {
                return str;
            }
            return str.substring(0, iIndexOf);
        }

        private static int compareNullableCharSequences(CharSequence charSequence, CharSequence charSequence2) {
            boolean zIsEmpty = TextUtils.isEmpty(charSequence);
            boolean zIsEmpty2 = TextUtils.isEmpty(charSequence2);
            if (zIsEmpty || zIsEmpty2) {
                return (zIsEmpty ? 1 : 0) - (zIsEmpty2 ? 1 : 0);
            }
            return charSequence.toString().compareTo(charSequence2.toString());
        }

        @Override
        public int compareTo(ImeSubtypeListItem imeSubtypeListItem) {
            int iCompareNullableCharSequences = compareNullableCharSequences(this.mImeName, imeSubtypeListItem.mImeName);
            if (iCompareNullableCharSequences != 0) {
                return iCompareNullableCharSequences;
            }
            int i = (this.mIsSystemLocale ? -1 : 0) - (imeSubtypeListItem.mIsSystemLocale ? -1 : 0);
            if (i != 0) {
                return i;
            }
            int i2 = (this.mIsSystemLanguage ? -1 : 0) - (imeSubtypeListItem.mIsSystemLanguage ? -1 : 0);
            if (i2 != 0) {
                return i2;
            }
            return compareNullableCharSequences(this.mSubtypeName, imeSubtypeListItem.mSubtypeName);
        }

        public String toString() {
            return "ImeSubtypeListItem{mImeName=" + ((Object) this.mImeName) + " mSubtypeName=" + ((Object) this.mSubtypeName) + " mSubtypeId=" + this.mSubtypeId + " mIsSystemLocale=" + this.mIsSystemLocale + " mIsSystemLanguage=" + this.mIsSystemLanguage + "}";
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ImeSubtypeListItem)) {
                return false;
            }
            ImeSubtypeListItem imeSubtypeListItem = (ImeSubtypeListItem) obj;
            return Objects.equals(this.mImi, imeSubtypeListItem.mImi) && this.mSubtypeId == imeSubtypeListItem.mSubtypeId;
        }
    }

    private static class InputMethodAndSubtypeList {
        private final Context mContext;
        private final PackageManager mPm;
        private final InputMethodUtils.InputMethodSettings mSettings;
        private final TreeMap<InputMethodInfo, List<InputMethodSubtype>> mSortedImmis = new TreeMap<>(new Comparator<InputMethodInfo>() {
            @Override
            public int compare(InputMethodInfo inputMethodInfo, InputMethodInfo inputMethodInfo2) {
                if (inputMethodInfo2 == null) {
                    return 0;
                }
                if (inputMethodInfo != null) {
                    if (InputMethodAndSubtypeList.this.mPm == null) {
                        return inputMethodInfo.getId().compareTo(inputMethodInfo2.getId());
                    }
                    return (((Object) inputMethodInfo.loadLabel(InputMethodAndSubtypeList.this.mPm)) + "/" + inputMethodInfo.getId()).toString().compareTo((((Object) inputMethodInfo2.loadLabel(InputMethodAndSubtypeList.this.mPm)) + "/" + inputMethodInfo2.getId()).toString());
                }
                return 1;
            }
        });
        private final String mSystemLocaleStr;

        public InputMethodAndSubtypeList(Context context, InputMethodUtils.InputMethodSettings inputMethodSettings) {
            this.mContext = context;
            this.mSettings = inputMethodSettings;
            this.mPm = context.getPackageManager();
            Locale locale = context.getResources().getConfiguration().locale;
            this.mSystemLocaleStr = locale != null ? locale.toString() : "";
        }

        public List<ImeSubtypeListItem> getSortedInputMethodAndSubtypeList(boolean z, boolean z2) {
            HashMap<InputMethodInfo, List<InputMethodSubtype>> map;
            HashMap<InputMethodInfo, List<InputMethodSubtype>> map2;
            int i;
            int i2;
            ArrayList arrayList = new ArrayList();
            HashMap<InputMethodInfo, List<InputMethodSubtype>> explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked = this.mSettings.getExplicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked(this.mContext);
            if (explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked == null || explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked.size() == 0) {
                return Collections.emptyList();
            }
            boolean z3 = (z2 && z) ? false : z;
            this.mSortedImmis.clear();
            this.mSortedImmis.putAll(explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked);
            for (InputMethodInfo inputMethodInfo : this.mSortedImmis.keySet()) {
                if (inputMethodInfo != null) {
                    List<InputMethodSubtype> list = explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked.get(inputMethodInfo);
                    HashSet hashSet = new HashSet();
                    Iterator<InputMethodSubtype> it = list.iterator();
                    while (it.hasNext()) {
                        hashSet.add(String.valueOf(it.next().hashCode()));
                    }
                    CharSequence charSequenceLoadLabel = inputMethodInfo.loadLabel(this.mPm);
                    if (hashSet.size() > 0) {
                        int subtypeCount = inputMethodInfo.getSubtypeCount();
                        int i3 = 0;
                        while (i3 < subtypeCount) {
                            InputMethodSubtype subtypeAt = inputMethodInfo.getSubtypeAt(i3);
                            String strValueOf = String.valueOf(subtypeAt.hashCode());
                            if (!hashSet.contains(strValueOf) || (!z3 && subtypeAt.isAuxiliary())) {
                                map2 = explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked;
                                i = i3;
                                i2 = subtypeCount;
                            } else {
                                map2 = explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked;
                                i = i3;
                                i2 = subtypeCount;
                                arrayList.add(new ImeSubtypeListItem(charSequenceLoadLabel, subtypeAt.overridesImplicitlyEnabledSubtype() ? null : subtypeAt.getDisplayName(this.mContext, inputMethodInfo.getPackageName(), inputMethodInfo.getServiceInfo().applicationInfo), inputMethodInfo, i3, subtypeAt.getLocale(), this.mSystemLocaleStr));
                                hashSet.remove(strValueOf);
                            }
                            i3 = i + 1;
                            subtypeCount = i2;
                            explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked = map2;
                        }
                        map = explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked;
                    } else {
                        map = explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked;
                        arrayList.add(new ImeSubtypeListItem(charSequenceLoadLabel, null, inputMethodInfo, -1, null, this.mSystemLocaleStr));
                    }
                    explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked = map;
                }
            }
            Collections.sort(arrayList);
            return arrayList;
        }
    }

    private static int calculateSubtypeId(InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype) {
        if (inputMethodSubtype != null) {
            return InputMethodUtils.getSubtypeIdFromHashCode(inputMethodInfo, inputMethodSubtype.hashCode());
        }
        return -1;
    }

    private static class StaticRotationList {
        private final List<ImeSubtypeListItem> mImeSubtypeList;

        public StaticRotationList(List<ImeSubtypeListItem> list) {
            this.mImeSubtypeList = list;
        }

        private int getIndex(InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype) {
            int iCalculateSubtypeId = InputMethodSubtypeSwitchingController.calculateSubtypeId(inputMethodInfo, inputMethodSubtype);
            int size = this.mImeSubtypeList.size();
            for (int i = 0; i < size; i++) {
                ImeSubtypeListItem imeSubtypeListItem = this.mImeSubtypeList.get(i);
                if (inputMethodInfo.equals(imeSubtypeListItem.mImi) && imeSubtypeListItem.mSubtypeId == iCalculateSubtypeId) {
                    return i;
                }
            }
            return -1;
        }

        public ImeSubtypeListItem getNextInputMethodLocked(boolean z, InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype, boolean z2) {
            int index;
            int i;
            if (inputMethodInfo == null) {
                return null;
            }
            if (this.mImeSubtypeList.size() <= 1 || (index = getIndex(inputMethodInfo, inputMethodSubtype)) < 0) {
                return null;
            }
            int size = this.mImeSubtypeList.size();
            for (int i2 = 1; i2 < size; i2++) {
                if (!z2) {
                    i = size - i2;
                } else {
                    i = i2;
                }
                ImeSubtypeListItem imeSubtypeListItem = this.mImeSubtypeList.get((i + index) % size);
                if (!z || inputMethodInfo.equals(imeSubtypeListItem.mImi)) {
                    return imeSubtypeListItem;
                }
            }
            return null;
        }

        protected void dump(Printer printer, String str) {
            int size = this.mImeSubtypeList.size();
            for (int i = 0; i < size; i++) {
                printer.println(str + "rank=" + i + " item=" + this.mImeSubtypeList.get(i));
            }
        }
    }

    private static class DynamicRotationList {
        private static final String TAG = DynamicRotationList.class.getSimpleName();
        private final List<ImeSubtypeListItem> mImeSubtypeList;
        private final int[] mUsageHistoryOfSubtypeListItemIndex;

        private DynamicRotationList(List<ImeSubtypeListItem> list) {
            this.mImeSubtypeList = list;
            this.mUsageHistoryOfSubtypeListItemIndex = new int[this.mImeSubtypeList.size()];
            int size = this.mImeSubtypeList.size();
            for (int i = 0; i < size; i++) {
                this.mUsageHistoryOfSubtypeListItemIndex[i] = i;
            }
        }

        private int getUsageRank(InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype) {
            int iCalculateSubtypeId = InputMethodSubtypeSwitchingController.calculateSubtypeId(inputMethodInfo, inputMethodSubtype);
            int length = this.mUsageHistoryOfSubtypeListItemIndex.length;
            for (int i = 0; i < length; i++) {
                ImeSubtypeListItem imeSubtypeListItem = this.mImeSubtypeList.get(this.mUsageHistoryOfSubtypeListItemIndex[i]);
                if (imeSubtypeListItem.mImi.equals(inputMethodInfo) && imeSubtypeListItem.mSubtypeId == iCalculateSubtypeId) {
                    return i;
                }
            }
            return -1;
        }

        public void onUserAction(InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype) {
            int usageRank = getUsageRank(inputMethodInfo, inputMethodSubtype);
            if (usageRank <= 0) {
                return;
            }
            int i = this.mUsageHistoryOfSubtypeListItemIndex[usageRank];
            System.arraycopy(this.mUsageHistoryOfSubtypeListItemIndex, 0, this.mUsageHistoryOfSubtypeListItemIndex, 1, usageRank);
            this.mUsageHistoryOfSubtypeListItemIndex[0] = i;
        }

        public ImeSubtypeListItem getNextInputMethodLocked(boolean z, InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype, boolean z2) {
            int i;
            int usageRank = getUsageRank(inputMethodInfo, inputMethodSubtype);
            if (usageRank < 0) {
                return null;
            }
            int length = this.mUsageHistoryOfSubtypeListItemIndex.length;
            for (int i2 = 1; i2 < length; i2++) {
                if (!z2) {
                    i = length - i2;
                } else {
                    i = i2;
                }
                ImeSubtypeListItem imeSubtypeListItem = this.mImeSubtypeList.get(this.mUsageHistoryOfSubtypeListItemIndex[(i + usageRank) % length]);
                if (!z || inputMethodInfo.equals(imeSubtypeListItem.mImi)) {
                    return imeSubtypeListItem;
                }
            }
            return null;
        }

        protected void dump(Printer printer, String str) {
            for (int i = 0; i < this.mUsageHistoryOfSubtypeListItemIndex.length; i++) {
                printer.println(str + "rank=" + this.mUsageHistoryOfSubtypeListItemIndex[i] + " item=" + this.mImeSubtypeList.get(i));
            }
        }
    }

    @VisibleForTesting
    public static class ControllerImpl {
        private final DynamicRotationList mSwitchingAwareRotationList;
        private final StaticRotationList mSwitchingUnawareRotationList;

        public static ControllerImpl createFrom(ControllerImpl controllerImpl, List<ImeSubtypeListItem> list) {
            DynamicRotationList dynamicRotationList;
            List<ImeSubtypeListItem> listFilterImeSubtypeList = filterImeSubtypeList(list, true);
            StaticRotationList staticRotationList = 0;
            staticRotationList = 0;
            staticRotationList = 0;
            if (controllerImpl != null && controllerImpl.mSwitchingAwareRotationList != null && Objects.equals(controllerImpl.mSwitchingAwareRotationList.mImeSubtypeList, listFilterImeSubtypeList)) {
                dynamicRotationList = controllerImpl.mSwitchingAwareRotationList;
            } else {
                dynamicRotationList = null;
            }
            if (dynamicRotationList == null) {
                dynamicRotationList = new DynamicRotationList(listFilterImeSubtypeList);
            }
            List<ImeSubtypeListItem> listFilterImeSubtypeList2 = filterImeSubtypeList(list, false);
            if (controllerImpl != null && controllerImpl.mSwitchingUnawareRotationList != null && Objects.equals(controllerImpl.mSwitchingUnawareRotationList.mImeSubtypeList, listFilterImeSubtypeList2)) {
                staticRotationList = controllerImpl.mSwitchingUnawareRotationList;
            }
            if (staticRotationList == 0) {
                staticRotationList = new StaticRotationList(listFilterImeSubtypeList2);
            }
            return new ControllerImpl(dynamicRotationList, staticRotationList);
        }

        private ControllerImpl(DynamicRotationList dynamicRotationList, StaticRotationList staticRotationList) {
            this.mSwitchingAwareRotationList = dynamicRotationList;
            this.mSwitchingUnawareRotationList = staticRotationList;
        }

        public ImeSubtypeListItem getNextInputMethod(boolean z, InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype, boolean z2) {
            if (inputMethodInfo == null) {
                return null;
            }
            if (inputMethodInfo.supportsSwitchingToNextInputMethod()) {
                return this.mSwitchingAwareRotationList.getNextInputMethodLocked(z, inputMethodInfo, inputMethodSubtype, z2);
            }
            return this.mSwitchingUnawareRotationList.getNextInputMethodLocked(z, inputMethodInfo, inputMethodSubtype, z2);
        }

        public void onUserActionLocked(InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype) {
            if (inputMethodInfo != null && inputMethodInfo.supportsSwitchingToNextInputMethod()) {
                this.mSwitchingAwareRotationList.onUserAction(inputMethodInfo, inputMethodSubtype);
            }
        }

        private static List<ImeSubtypeListItem> filterImeSubtypeList(List<ImeSubtypeListItem> list, boolean z) {
            ArrayList arrayList = new ArrayList();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                ImeSubtypeListItem imeSubtypeListItem = list.get(i);
                if (imeSubtypeListItem.mImi.supportsSwitchingToNextInputMethod() == z) {
                    arrayList.add(imeSubtypeListItem);
                }
            }
            return arrayList;
        }

        protected void dump(Printer printer) {
            printer.println("    mSwitchingAwareRotationList:");
            this.mSwitchingAwareRotationList.dump(printer, "      ");
            printer.println("    mSwitchingUnawareRotationList:");
            this.mSwitchingUnawareRotationList.dump(printer, "      ");
        }
    }

    private InputMethodSubtypeSwitchingController(InputMethodUtils.InputMethodSettings inputMethodSettings, Context context) {
        this.mSettings = inputMethodSettings;
        resetCircularListLocked(context);
    }

    public static InputMethodSubtypeSwitchingController createInstanceLocked(InputMethodUtils.InputMethodSettings inputMethodSettings, Context context) {
        return new InputMethodSubtypeSwitchingController(inputMethodSettings, context);
    }

    public void onUserActionLocked(InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype) {
        if (this.mController == null) {
            return;
        }
        this.mController.onUserActionLocked(inputMethodInfo, inputMethodSubtype);
    }

    public void resetCircularListLocked(Context context) {
        this.mSubtypeList = new InputMethodAndSubtypeList(context, this.mSettings);
        this.mController = ControllerImpl.createFrom(this.mController, this.mSubtypeList.getSortedInputMethodAndSubtypeList(false, false));
    }

    public ImeSubtypeListItem getNextInputMethodLocked(boolean z, InputMethodInfo inputMethodInfo, InputMethodSubtype inputMethodSubtype, boolean z2) {
        if (this.mController == null) {
            return null;
        }
        return this.mController.getNextInputMethod(z, inputMethodInfo, inputMethodSubtype, z2);
    }

    public List<ImeSubtypeListItem> getSortedInputMethodAndSubtypeListLocked(boolean z, boolean z2) {
        return this.mSubtypeList.getSortedInputMethodAndSubtypeList(z, z2);
    }

    public void dump(Printer printer) {
        if (this.mController != null) {
            this.mController.dump(printer);
        } else {
            printer.println("    mController=null");
        }
    }
}
