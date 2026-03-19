package com.android.internal.app;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.ListFragment;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.R;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LocalePicker extends ListFragment {
    private static final boolean DEBUG = false;
    private static final String TAG = "LocalePicker";
    private static final String[] pseudoLocales = {"en-XA", "ar-XB"};
    LocaleSelectionListener mListener;

    public interface LocaleSelectionListener {
        void onLocaleSelected(Locale locale);
    }

    public static class LocaleInfo implements Comparable<LocaleInfo> {
        static final Collator sCollator = Collator.getInstance();
        String label;
        final Locale locale;

        public LocaleInfo(String str, Locale locale) {
            this.label = str;
            this.locale = locale;
        }

        public String getLabel() {
            return this.label;
        }

        public Locale getLocale() {
            return this.locale;
        }

        public String toString() {
            return this.label;
        }

        @Override
        public int compareTo(LocaleInfo localeInfo) {
            return sCollator.compare(this.label, localeInfo.label);
        }
    }

    public static String[] getSystemAssetLocales() {
        return Resources.getSystem().getAssets().getLocales();
    }

    public static String[] getSupportedLocales(Context context) {
        return context.getResources().getStringArray(R.array.supported_locales);
    }

    public static List<LocaleInfo> getAllAssetLocales(Context context, boolean z) {
        Resources resources = context.getResources();
        String[] systemAssetLocales = getSystemAssetLocales();
        ArrayList arrayList = new ArrayList(systemAssetLocales.length);
        Collections.addAll(arrayList, systemAssetLocales);
        Collections.sort(arrayList);
        String[] stringArray = resources.getStringArray(R.array.special_locale_codes);
        String[] stringArray2 = resources.getStringArray(R.array.special_locale_names);
        ArrayList arrayList2 = new ArrayList(arrayList.size());
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            Locale localeForLanguageTag = Locale.forLanguageTag(((String) it.next()).replace('_', '-'));
            if (localeForLanguageTag != null && !"und".equals(localeForLanguageTag.getLanguage()) && !localeForLanguageTag.getLanguage().isEmpty() && !localeForLanguageTag.getCountry().isEmpty() && (z || !LocaleList.isPseudoLocale(localeForLanguageTag))) {
                if (arrayList2.isEmpty()) {
                    arrayList2.add(new LocaleInfo(toTitleCase(localeForLanguageTag.getDisplayLanguage(localeForLanguageTag)), localeForLanguageTag));
                } else {
                    LocaleInfo localeInfo = (LocaleInfo) arrayList2.get(arrayList2.size() - 1);
                    if (localeInfo.locale.getLanguage().equals(localeForLanguageTag.getLanguage()) && !localeInfo.locale.getLanguage().equals("zz")) {
                        localeInfo.label = toTitleCase(getDisplayName(localeInfo.locale, stringArray, stringArray2));
                        arrayList2.add(new LocaleInfo(toTitleCase(getDisplayName(localeForLanguageTag, stringArray, stringArray2)), localeForLanguageTag));
                    } else {
                        arrayList2.add(new LocaleInfo(toTitleCase(localeForLanguageTag.getDisplayLanguage(localeForLanguageTag)), localeForLanguageTag));
                    }
                }
            }
        }
        Collections.sort(arrayList2);
        return arrayList2;
    }

    public static ArrayAdapter<LocaleInfo> constructAdapter(Context context) {
        return constructAdapter(context, R.layout.locale_picker_item, R.id.locale);
    }

    public static ArrayAdapter<LocaleInfo> constructAdapter(Context context, final int i, final int i2) {
        List<LocaleInfo> allAssetLocales = getAllAssetLocales(context, Settings.Global.getInt(context.getContentResolver(), "development_settings_enabled", 0) != 0);
        final LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new ArrayAdapter<LocaleInfo>(context, i, i2, allAssetLocales) {
            @Override
            public View getView(int i3, View view, ViewGroup viewGroup) {
                TextView textView;
                if (view == null) {
                    view = layoutInflater.inflate(i, viewGroup, false);
                    textView = (TextView) view.findViewById(i2);
                    view.setTag(textView);
                } else {
                    textView = (TextView) view.getTag();
                }
                LocaleInfo item = getItem(i3);
                textView.setText(item.toString());
                textView.setTextLocale(item.getLocale());
                return view;
            }
        };
    }

    private static String toTitleCase(String str) {
        if (str.length() == 0) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private static String getDisplayName(Locale locale, String[] strArr, String[] strArr2) {
        String string = locale.toString();
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].equals(string)) {
                return strArr2[i];
            }
        }
        return locale.getDisplayName(locale);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setListAdapter(constructAdapter(getActivity()));
    }

    public void setLocaleSelectionListener(LocaleSelectionListener localeSelectionListener) {
        this.mListener = localeSelectionListener;
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().requestFocus();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int i, long j) {
        if (this.mListener != null) {
            this.mListener.onLocaleSelected(((LocaleInfo) getListAdapter().getItem(i)).locale);
        }
    }

    public static void updateLocale(Locale locale) {
        updateLocales(new LocaleList(locale));
    }

    public static void updateLocales(LocaleList localeList) {
        try {
            IActivityManager service = ActivityManager.getService();
            Configuration configuration = service.getConfiguration();
            configuration.setLocales(localeList);
            configuration.userSetLocale = true;
            service.updatePersistentConfiguration(configuration);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (RemoteException e) {
        }
    }

    public static LocaleList getLocales() {
        try {
            return ActivityManager.getService().getConfiguration().getLocales();
        } catch (RemoteException e) {
            return LocaleList.getDefault();
        }
    }
}
