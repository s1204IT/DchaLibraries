package com.android.settings.intelligence.search.query;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.icu.text.ListFormatter;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.search.ResultPayload;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.indexing.DatabaseIndexingUtils;
import com.android.settings.intelligence.search.query.SearchQueryTask;
import com.android.settings.intelligence.search.sitemap.SiteMapManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class InputDeviceResultTask extends SearchQueryTask.QueryWorker {
    static final String PHYSICAL_KEYBOARD_FRAGMENT = "com.android.settings.inputmethod.PhysicalKeyboardFragment";
    static final String VIRTUAL_KEYBOARD_FRAGMENT = "com.android.settings.inputmethod.AvailableVirtualKeyboardFragment";
    private final InputMethodManager mImm;
    private final PackageManager mPackageManager;
    private List<String> mPhysicalKeyboardBreadcrumb;
    private List<String> mVirtualKeyboardBreadcrumb;

    public static SearchQueryTask newTask(Context context, SiteMapManager siteMapManager, String str) {
        return new SearchQueryTask(new InputDeviceResultTask(context, siteMapManager, str));
    }

    public InputDeviceResultTask(Context context, SiteMapManager siteMapManager, String str) {
        super(context, siteMapManager, str);
        this.mImm = (InputMethodManager) context.getSystemService("input_method");
        this.mPackageManager = context.getPackageManager();
    }

    @Override
    protected int getQueryWorkerId() {
        return 15;
    }

    @Override
    protected List<? extends SearchResult> query() {
        System.currentTimeMillis();
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(buildPhysicalKeyboardSearchResults());
        arrayList.addAll(buildVirtualKeyboardSearchResults());
        Collections.sort(arrayList);
        return arrayList;
    }

    private Set<SearchResult> buildPhysicalKeyboardSearchResults() {
        HashSet hashSet = new HashSet();
        String string = this.mContext.getString(R.string.physical_keyboard_title);
        Iterator<InputDevice> it = getPhysicalFullKeyboards().iterator();
        while (it.hasNext()) {
            String name = it.next().getName();
            int wordDifference = SearchQueryUtils.getWordDifference(name, this.mQuery);
            if (wordDifference != -1) {
                hashSet.add(new SearchResult.Builder().setTitle(name).setPayload(new ResultPayload(DatabaseIndexingUtils.buildSearchTrampolineIntent(this.mContext, PHYSICAL_KEYBOARD_FRAGMENT, name, string))).setDataKey(name).setRank(wordDifference).addBreadcrumbs(getPhysicalKeyboardBreadCrumb()).build());
            }
        }
        return hashSet;
    }

    private Set<SearchResult> buildVirtualKeyboardSearchResults() {
        HashSet hashSet = new HashSet();
        String string = this.mContext.getString(R.string.add_virtual_keyboard);
        for (InputMethodInfo inputMethodInfo : this.mImm.getInputMethodList()) {
            String string2 = inputMethodInfo.loadLabel(this.mPackageManager).toString();
            String subtypeLocaleNameListAsSentence = getSubtypeLocaleNameListAsSentence(getAllSubtypesOf(inputMethodInfo), this.mContext, inputMethodInfo);
            int wordDifference = SearchQueryUtils.getWordDifference(string2, this.mQuery);
            if (wordDifference == -1) {
                wordDifference = SearchQueryUtils.getWordDifference(subtypeLocaleNameListAsSentence, this.mQuery);
            }
            if (wordDifference != -1) {
                ServiceInfo serviceInfo = inputMethodInfo.getServiceInfo();
                String strFlattenToString = new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString();
                hashSet.add(new SearchResult.Builder().setTitle(string2).setSummary(subtypeLocaleNameListAsSentence).setRank(wordDifference).setDataKey(strFlattenToString).addBreadcrumbs(getVirtualKeyboardBreadCrumb()).setPayload(new ResultPayload(DatabaseIndexingUtils.buildSearchTrampolineIntent(this.mContext, VIRTUAL_KEYBOARD_FRAGMENT, strFlattenToString, string))).build());
            }
        }
        return hashSet;
    }

    private List<String> getPhysicalKeyboardBreadCrumb() {
        if (this.mPhysicalKeyboardBreadcrumb == null || this.mPhysicalKeyboardBreadcrumb.isEmpty()) {
            this.mPhysicalKeyboardBreadcrumb = this.mSiteMapManager.buildBreadCrumb(this.mContext, PHYSICAL_KEYBOARD_FRAGMENT, this.mContext.getString(R.string.physical_keyboard_title));
        }
        return this.mPhysicalKeyboardBreadcrumb;
    }

    private List<String> getVirtualKeyboardBreadCrumb() {
        if (this.mVirtualKeyboardBreadcrumb == null || this.mVirtualKeyboardBreadcrumb.isEmpty()) {
            Context context = this.mContext;
            this.mVirtualKeyboardBreadcrumb = this.mSiteMapManager.buildBreadCrumb(context, VIRTUAL_KEYBOARD_FRAGMENT, context.getString(R.string.add_virtual_keyboard));
        }
        return this.mVirtualKeyboardBreadcrumb;
    }

    private List<InputDevice> getPhysicalFullKeyboards() {
        ArrayList arrayList = new ArrayList();
        int[] deviceIds = InputDevice.getDeviceIds();
        if (deviceIds != null) {
            for (int i : deviceIds) {
                InputDevice device = InputDevice.getDevice(i);
                if (isFullPhysicalKeyboard(device)) {
                    arrayList.add(device);
                }
            }
        }
        return arrayList;
    }

    private static String getSubtypeLocaleNameListAsSentence(List<InputMethodSubtype> list, Context context, InputMethodInfo inputMethodInfo) {
        if (list.isEmpty()) {
            return "";
        }
        Locale locale = Locale.getDefault();
        int size = list.size();
        CharSequence[] charSequenceArr = new CharSequence[size];
        for (int i = 0; i < size; i++) {
            charSequenceArr[i] = list.get(i).getDisplayName(context, inputMethodInfo.getPackageName(), inputMethodInfo.getServiceInfo().applicationInfo);
        }
        return toSentenceCase(ListFormatter.getInstance(locale).format(charSequenceArr), locale);
    }

    private static String toSentenceCase(String str, Locale locale) {
        if (str.isEmpty()) {
            return str;
        }
        int iOffsetByCodePoints = str.offsetByCodePoints(0, 1);
        return str.substring(0, iOffsetByCodePoints).toUpperCase(locale) + str.substring(iOffsetByCodePoints);
    }

    private static boolean isFullPhysicalKeyboard(InputDevice inputDevice) {
        return inputDevice != null && !inputDevice.isVirtual() && (inputDevice.getSources() & 257) == 257 && inputDevice.getKeyboardType() == 2;
    }

    private static List<InputMethodSubtype> getAllSubtypesOf(InputMethodInfo inputMethodInfo) {
        int subtypeCount = inputMethodInfo.getSubtypeCount();
        ArrayList arrayList = new ArrayList(subtypeCount);
        for (int i = 0; i < subtypeCount; i++) {
            arrayList.add(inputMethodInfo.getSubtypeAt(i));
        }
        return arrayList;
    }
}
