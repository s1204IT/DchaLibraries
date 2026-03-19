package com.android.settings.dream;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DreamSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.dream_fragment_overview;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return DreamSettings.buildPreferenceControllers(context);
        }
    };

    static int getSettingFromPrefKey(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != -1592701525) {
            if (iHashCode != -294641318) {
                if (iHashCode != 104712844) {
                    b = (iHashCode == 1019349036 && str.equals("while_charging_only")) ? (byte) 0 : (byte) -1;
                } else if (str.equals("never")) {
                    b = 3;
                }
            } else if (str.equals("either_charging_or_docked")) {
                b = 2;
            }
        } else if (str.equals("while_docked_only")) {
            b = 1;
        }
        switch (b) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 3;
        }
    }

    static String getKeyFromSetting(int i) {
        switch (i) {
            case 0:
                return "while_charging_only";
            case 1:
                return "while_docked_only";
            case 2:
                return "either_charging_or_docked";
            default:
                return "never";
        }
    }

    static int getDreamSettingDescriptionResId(int i) {
        switch (i) {
            case 0:
                return R.string.screensaver_settings_summary_sleep;
            case 1:
                return R.string.screensaver_settings_summary_dock;
            case 2:
                return R.string.screensaver_settings_summary_either_long;
            default:
                return R.string.screensaver_settings_summary_never;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 47;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dream_fragment_overview;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_screen_saver;
    }

    @Override
    protected String getLogTag() {
        return "DreamSettings";
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    public static CharSequence getSummaryTextWithDreamName(Context context) {
        return getSummaryTextFromBackend(DreamBackend.getInstance(context), context);
    }

    static CharSequence getSummaryTextFromBackend(DreamBackend dreamBackend, Context context) {
        if (!dreamBackend.isEnabled()) {
            return context.getString(R.string.screensaver_settings_summary_off);
        }
        return dreamBackend.getActiveDreamName();
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new CurrentDreamPreferenceController(context));
        arrayList.add(new WhenToDreamPreferenceController(context));
        arrayList.add(new StartNowPreferenceController(context));
        return arrayList;
    }
}
