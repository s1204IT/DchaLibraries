package com.android.settings.notification;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.text.BidiFormatter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;

/* loaded from: classes.dex */
public class HeaderPreferenceController extends NotificationPreferenceController implements LifecycleObserver, PreferenceControllerMixin {
    private final PreferenceFragment mFragment;
    private EntityHeaderController mHeaderController;
    private boolean mStarted;

    public HeaderPreferenceController(Context context, PreferenceFragment preferenceFragment) {
        super(context, null);
        this.mStarted = false;
        this.mFragment = preferenceFragment;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "pref_app_header";
    }

    @Override // com.android.settings.notification.NotificationPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mAppRow != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mAppRow != null && this.mFragment != null) {
            Activity activity = null;
            if (this.mStarted) {
                activity = this.mFragment.getActivity();
            }
            this.mHeaderController = EntityHeaderController.newInstance(this.mFragment.getActivity(), this.mFragment, ((LayoutPreference) preference).findViewById(R.id.entity_header));
            this.mHeaderController.setIcon(this.mAppRow.icon).setLabel(getLabel()).setSummary(getSummary()).setPackageName(this.mAppRow.pkg).setUid(this.mAppRow.uid).setButtonActions(1, 0).setHasAppInfoLink(true).done(activity, this.mContext).findViewById(R.id.entity_header).setVisibility(0);
        }
    }

    CharSequence getLabel() {
        if (this.mChannel != null && !isDefaultChannel()) {
            return this.mChannel.getName();
        }
        if (this.mChannelGroup != null) {
            return this.mChannelGroup.getName();
        }
        return this.mAppRow.label;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        if (this.mChannel != null && !isDefaultChannel()) {
            if (this.mChannelGroup != null && !TextUtils.isEmpty(this.mChannelGroup.getName())) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                spannableStringBuilder.append((CharSequence) bidiFormatter.unicodeWrap(this.mAppRow.label.toString()));
                spannableStringBuilder.append(bidiFormatter.unicodeWrap(this.mContext.getText(R.string.notification_header_divider_symbol_with_spaces)));
                spannableStringBuilder.append((CharSequence) bidiFormatter.unicodeWrap(this.mChannelGroup.getName().toString()));
                return spannableStringBuilder.toString();
            }
            return this.mAppRow.label.toString();
        }
        if (this.mChannelGroup != null) {
            return this.mAppRow.label.toString();
        }
        return "";
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        this.mStarted = true;
        if (this.mHeaderController != null) {
            this.mHeaderController.styleActionBar(this.mFragment.getActivity());
        }
    }
}
