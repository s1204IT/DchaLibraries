package com.android.settings.network;

import android.content.Context;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/* loaded from: classes.dex */
public class NetworkScorerPickerPreferenceController extends BasePreferenceController {
    private final NetworkScoreManager mNetworkScoreManager;

    public NetworkScorerPickerPreferenceController(Context context, String str) {
        super(context, str);
        this.mNetworkScoreManager = (NetworkScoreManager) this.mContext.getSystemService("network_score");
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        boolean z = !this.mNetworkScoreManager.getAllValidScorers().isEmpty();
        preference.setEnabled(z);
        if (!z) {
            preference.setSummary((CharSequence) null);
            return;
        }
        NetworkScorerAppData activeScorer = this.mNetworkScoreManager.getActiveScorer();
        if (activeScorer == null) {
            preference.setSummary(this.mContext.getString(R.string.network_scorer_picker_none_preference));
        } else {
            preference.setSummary(activeScorer.getRecommendationServiceLabel());
        }
    }
}
