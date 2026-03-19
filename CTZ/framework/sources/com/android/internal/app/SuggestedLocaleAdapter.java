package com.android.internal.app;

import android.content.Context;
import android.content.res.Configuration;
import android.net.wifi.WifiEnterpriseConfig;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class SuggestedLocaleAdapter extends BaseAdapter implements Filterable {
    private static final int MIN_REGIONS_FOR_SUGGESTIONS = 6;
    private static final int TYPE_HEADER_ALL_OTHERS = 1;
    private static final int TYPE_HEADER_SUGGESTED = 0;
    private static final int TYPE_LOCALE = 2;
    private final boolean mCountryMode;
    private LayoutInflater mInflater;
    private ArrayList<LocaleStore.LocaleInfo> mLocaleOptions;
    private ArrayList<LocaleStore.LocaleInfo> mOriginalLocaleOptions;
    private int mSuggestionCount;
    private Locale mDisplayLocale = null;
    private Context mContextOverride = null;

    static int access$208(SuggestedLocaleAdapter suggestedLocaleAdapter) {
        int i = suggestedLocaleAdapter.mSuggestionCount;
        suggestedLocaleAdapter.mSuggestionCount = i + 1;
        return i;
    }

    public SuggestedLocaleAdapter(Set<LocaleStore.LocaleInfo> set, boolean z) {
        this.mCountryMode = z;
        this.mLocaleOptions = new ArrayList<>(set.size());
        for (LocaleStore.LocaleInfo localeInfo : set) {
            if (localeInfo.isSuggested()) {
                this.mSuggestionCount++;
            }
            this.mLocaleOptions.add(localeInfo);
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return getItemViewType(i) == 2;
    }

    @Override
    public int getItemViewType(int i) {
        if (!showHeaders()) {
            return 2;
        }
        if (i == 0) {
            return 0;
        }
        return i == this.mSuggestionCount + 1 ? 1 : 2;
    }

    @Override
    public int getViewTypeCount() {
        if (showHeaders()) {
            return 3;
        }
        return 1;
    }

    @Override
    public int getCount() {
        if (showHeaders()) {
            return this.mLocaleOptions.size() + 2;
        }
        return this.mLocaleOptions.size();
    }

    @Override
    public Object getItem(int i) {
        int i2;
        if (showHeaders()) {
            i2 = i > this.mSuggestionCount ? -2 : -1;
        } else {
            i2 = 0;
        }
        return this.mLocaleOptions.get(i + i2);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setDisplayLocale(Context context, Locale locale) {
        if (locale == null) {
            this.mDisplayLocale = null;
            this.mContextOverride = null;
        } else if (!locale.equals(this.mDisplayLocale)) {
            this.mDisplayLocale = locale;
            Configuration configuration = new Configuration();
            configuration.setLocale(locale);
            this.mContextOverride = context.createConfigurationContext(configuration);
        }
    }

    private void setTextTo(TextView textView, int i) {
        if (this.mContextOverride == null) {
            textView.setText(i);
        } else {
            textView.setText(this.mContextOverride.getText(i));
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        int i2;
        if (view == null && this.mInflater == null) {
            this.mInflater = LayoutInflater.from(viewGroup.getContext());
        }
        int itemViewType = getItemViewType(i);
        switch (itemViewType) {
            case 0:
            case 1:
                if (!(view instanceof TextView)) {
                    view = this.mInflater.inflate(R.layout.language_picker_section_header, viewGroup, false);
                }
                TextView textView = (TextView) view;
                if (itemViewType == 0) {
                    setTextTo(textView, R.string.language_picker_section_suggested);
                } else if (this.mCountryMode) {
                    setTextTo(textView, R.string.region_picker_section_all);
                } else {
                    setTextTo(textView, R.string.language_picker_section_all);
                }
                textView.setTextLocale(this.mDisplayLocale != null ? this.mDisplayLocale : Locale.getDefault());
                return view;
            default:
                if (!(view instanceof ViewGroup)) {
                    view = this.mInflater.inflate(R.layout.language_picker_item, viewGroup, false);
                }
                TextView textView2 = (TextView) view.findViewById(R.id.locale);
                LocaleStore.LocaleInfo localeInfo = (LocaleStore.LocaleInfo) getItem(i);
                textView2.setText(localeInfo.getLabel(this.mCountryMode));
                textView2.setTextLocale(localeInfo.getLocale());
                textView2.setContentDescription(localeInfo.getContentDescription(this.mCountryMode));
                if (this.mCountryMode) {
                    int layoutDirectionFromLocale = TextUtils.getLayoutDirectionFromLocale(localeInfo.getParent());
                    view.setLayoutDirection(layoutDirectionFromLocale);
                    if (layoutDirectionFromLocale == 1) {
                        i2 = 4;
                    } else {
                        i2 = 3;
                    }
                    textView2.setTextDirection(i2);
                }
                return view;
        }
    }

    private boolean showHeaders() {
        return ((this.mCountryMode && this.mLocaleOptions.size() < 6) || this.mSuggestionCount == 0 || this.mSuggestionCount == this.mLocaleOptions.size()) ? false : true;
    }

    public void sort(LocaleHelper.LocaleInfoComparator localeInfoComparator) {
        Collections.sort(this.mLocaleOptions, localeInfoComparator);
    }

    class FilterByNativeAndUiNames extends Filter {
        FilterByNativeAndUiNames() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (SuggestedLocaleAdapter.this.mOriginalLocaleOptions == null) {
                SuggestedLocaleAdapter.this.mOriginalLocaleOptions = new ArrayList(SuggestedLocaleAdapter.this.mLocaleOptions);
            }
            ArrayList arrayList = new ArrayList(SuggestedLocaleAdapter.this.mOriginalLocaleOptions);
            if (charSequence == null || charSequence.length() == 0) {
                filterResults.values = arrayList;
                filterResults.count = arrayList.size();
            } else {
                Locale locale = Locale.getDefault();
                String strNormalizeForSearch = LocaleHelper.normalizeForSearch(charSequence.toString(), locale);
                int size = arrayList.size();
                ArrayList arrayList2 = new ArrayList();
                for (int i = 0; i < size; i++) {
                    LocaleStore.LocaleInfo localeInfo = (LocaleStore.LocaleInfo) arrayList.get(i);
                    String strNormalizeForSearch2 = LocaleHelper.normalizeForSearch(localeInfo.getFullNameInUiLanguage(), locale);
                    if (wordMatches(LocaleHelper.normalizeForSearch(localeInfo.getFullNameNative(), locale), strNormalizeForSearch) || wordMatches(strNormalizeForSearch2, strNormalizeForSearch)) {
                        arrayList2.add(localeInfo);
                    }
                }
                filterResults.values = arrayList2;
                filterResults.count = arrayList2.size();
            }
            return filterResults;
        }

        boolean wordMatches(String str, String str2) {
            if (str.startsWith(str2)) {
                return true;
            }
            for (String str3 : str.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER)) {
                if (str3.startsWith(str2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            SuggestedLocaleAdapter.this.mLocaleOptions = (ArrayList) filterResults.values;
            SuggestedLocaleAdapter.this.mSuggestionCount = 0;
            Iterator it = SuggestedLocaleAdapter.this.mLocaleOptions.iterator();
            while (it.hasNext()) {
                if (((LocaleStore.LocaleInfo) it.next()).isSuggested()) {
                    SuggestedLocaleAdapter.access$208(SuggestedLocaleAdapter.this);
                }
            }
            if (filterResults.count > 0) {
                SuggestedLocaleAdapter.this.notifyDataSetChanged();
            } else {
                SuggestedLocaleAdapter.this.notifyDataSetInvalidated();
            }
        }
    }

    @Override
    public Filter getFilter() {
        return new FilterByNativeAndUiNames();
    }
}
