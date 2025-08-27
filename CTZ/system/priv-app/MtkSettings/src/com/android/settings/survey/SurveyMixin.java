package com.android.settings.survey;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class SurveyMixin implements LifecycleObserver, OnPause, OnResume {
    private Fragment mFragment;
    private String mName;
    private BroadcastReceiver mReceiver;

    public SurveyMixin(Fragment fragment, String str) {
        this.mName = str;
        this.mFragment = fragment;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        SurveyFeatureProvider surveyFeatureProvider;
        Activity activity = this.mFragment.getActivity();
        if (activity != null && (surveyFeatureProvider = FeatureFactory.getFactory(activity).getSurveyFeatureProvider(activity)) != null) {
            String surveyId = surveyFeatureProvider.getSurveyId(activity, this.mName);
            if (surveyFeatureProvider.getSurveyExpirationDate(activity, surveyId) <= -1) {
                this.mReceiver = surveyFeatureProvider.createAndRegisterReceiver(activity);
                surveyFeatureProvider.downloadSurvey(activity, surveyId, null);
            } else {
                surveyFeatureProvider.showSurveyIfAvailable(activity, surveyId);
            }
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        Activity activity = this.mFragment.getActivity();
        if (this.mReceiver != null && activity != null) {
            SurveyFeatureProvider.unregisterReceiver(activity, this.mReceiver);
            this.mReceiver = null;
        }
    }
}
