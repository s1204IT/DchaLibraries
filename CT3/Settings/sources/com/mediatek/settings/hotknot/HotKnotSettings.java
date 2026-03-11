package com.mediatek.settings.hotknot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.List;

public class HotKnotSettings extends SettingsPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            HotKnotAdapter adapter = HotKnotAdapter.getDefaultAdapter(context);
            if (adapter != null) {
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.hotknot_settings_title);
                data.screenTitle = res.getString(R.string.hotknot_settings_title);
                data.keywords = res.getString(R.string.hotknot_settings_title);
                result.add(data);
            }
            return result;
        }
    };
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    private HotKnotAdapter mAdapter;
    private HotKnotEnabler mHotKnotEnabler;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getAction();
        }
    };
    private SwitchBar mSwitchBar;

    @Override
    protected int getMetricsCategory() {
        return 100002;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mAdapter = HotKnotAdapter.getDefaultAdapter(activity);
        if (this.mAdapter == null) {
            Log.d("@M_HotKnotSettings", "Hotknot adapter is null, finish Hotknot settings");
            getActivity().finish();
        }
        this.mIntentFilter = new IntentFilter("com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED");
    }

    @Override
    public void onStart() {
        super.onStart();
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        Log.d("@M_HotKnotSettings", "onCreate, mSwitchBar = " + this.mSwitchBar);
        this.mHotKnotEnabler = new HotKnotEnabler(activity, this.mSwitchBar);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.hotknot_settings, container, false);
        TextView textView = (TextView) view.findViewById(R.id.hotknot_warning_msg);
        if (textView != null) {
            textView.setText(getString(R.string.hotknot_charging_warning, new Object[]{getString(R.string.hotknot_settings_title)}));
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mHotKnotEnabler == null) {
            return;
        }
        this.mHotKnotEnabler.teardownSwitchBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mHotKnotEnabler != null) {
            this.mHotKnotEnabler.resume();
        }
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
        if (this.mHotKnotEnabler == null) {
            return;
        }
        this.mHotKnotEnabler.pause();
    }

    private static class SummaryProvider extends BroadcastReceiver implements SummaryLoader.SummaryProvider {
        private HotKnotAdapter mAdapter;
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
            this.mAdapter = HotKnotAdapter.getDefaultAdapter(context);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                if (this.mAdapter == null) {
                    return;
                }
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED");
                this.mContext.registerReceiver(this, intentFilter);
                int initState = this.mAdapter.isEnabled() ? 2 : 1;
                this.mSummaryLoader.setSummary(this, getSummary(initState));
                return;
            }
            if (this.mAdapter == null) {
                return;
            }
            this.mContext.unregisterReceiver(this);
        }

        private String getSummary(int state) {
            switch (state) {
                case DefaultWfcSettingsExt.PAUSE:
                    String summary = this.mContext.getResources().getString(R.string.switch_off_text);
                    return summary;
                case DefaultWfcSettingsExt.CREATE:
                    String summary2 = this.mContext.getResources().getString(R.string.switch_on_text);
                    return summary2;
                default:
                    return null;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("com.mediatek.hotknot.extra.ADAPTER_STATE", -1);
            Log.d("HotKnotSettings", "HotKnot state changed to " + state);
            this.mSummaryLoader.setSummary(this, getSummary(state));
        }
    }
}
