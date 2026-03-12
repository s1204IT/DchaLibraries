package com.android.settings.intelligence.suggestions.model;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.intelligence.suggestions.eligibility.AccountEligibilityChecker;
import com.android.settings.intelligence.suggestions.eligibility.AutomotiveEligibilityChecker;
import com.android.settings.intelligence.suggestions.eligibility.ConnectivityEligibilityChecker;
import com.android.settings.intelligence.suggestions.eligibility.DismissedChecker;
import com.android.settings.intelligence.suggestions.eligibility.FeatureEligibilityChecker;
import com.android.settings.intelligence.suggestions.eligibility.ProviderEligibilityChecker;
import java.util.List;

public class CandidateSuggestion {
    public static final String META_DATA_PREFERENCE_CUSTOM_VIEW = "com.android.settings.custom_view";
    public static final String META_DATA_PREFERENCE_ICON = "com.android.settings.icon";
    public static final String META_DATA_PREFERENCE_SUMMARY = "com.android.settings.summary";
    public static final String META_DATA_PREFERENCE_SUMMARY_URI = "com.android.settings.summary_uri";
    public static final String META_DATA_PREFERENCE_TITLE = "com.android.settings.title";
    private final ComponentName mComponent;
    private final Context mContext;
    private final boolean mIgnoreAppearRule;
    private final Intent mIntent;
    private final ResolveInfo mResolveInfo;
    private final String mId = generateId();
    private final boolean mIsEligible = initIsEligible();

    public CandidateSuggestion(Context context, ResolveInfo resolveInfo, boolean z) {
        this.mContext = context;
        this.mIgnoreAppearRule = z;
        this.mResolveInfo = resolveInfo;
        this.mIntent = new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        this.mComponent = this.mIntent.getComponent();
    }

    public String getId() {
        return this.mId;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    public boolean isEligible() {
        return this.mIsEligible;
    }

    public Suggestion toSuggestion() {
        if (!this.mIsEligible) {
            return null;
        }
        Suggestion.Builder builder = new Suggestion.Builder(this.mId);
        updateBuilder(builder);
        return builder.build();
    }

    private boolean initIsEligible() {
        return ProviderEligibilityChecker.isEligible(this.mContext, this.mId, this.mResolveInfo) && ConnectivityEligibilityChecker.isEligible(this.mContext, this.mId, this.mResolveInfo) && FeatureEligibilityChecker.isEligible(this.mContext, this.mId, this.mResolveInfo) && AccountEligibilityChecker.isEligible(this.mContext, this.mId, this.mResolveInfo) && DismissedChecker.isEligible(this.mContext, this.mId, this.mResolveInfo, this.mIgnoreAppearRule) && AutomotiveEligibilityChecker.isEligible(this.mContext, this.mId, this.mResolveInfo);
    }

    private void updateBuilder(Suggestion.Builder builder) {
        CharSequence charSequence;
        Icon iconCreateWithResource;
        int i;
        int i2;
        CharSequence string;
        String string2;
        PackageManager packageManager = this.mContext.getPackageManager();
        CharSequence charSequenceLoadLabel = null;
        try {
            Resources resourcesForApplication = packageManager.getResourcesForApplication(this.mComponent.getPackageName());
            Bundle bundle = this.mResolveInfo.activityInfo.metaData;
            if (resourcesForApplication != null && bundle != null) {
                Bundle overrideData = getOverrideData(bundle);
                if (bundle.containsKey(META_DATA_PREFERENCE_ICON)) {
                    i2 = bundle.getInt(META_DATA_PREFERENCE_ICON);
                } else {
                    i2 = this.mResolveInfo.activityInfo.icon;
                }
                if (i2 != 0) {
                    iconCreateWithResource = Icon.createWithResource(this.mResolveInfo.activityInfo.packageName, i2);
                } else {
                    iconCreateWithResource = null;
                }
                try {
                    CharSequence stringFromBundle = getStringFromBundle(overrideData, META_DATA_PREFERENCE_TITLE);
                    try {
                        if (TextUtils.isEmpty(stringFromBundle) && bundle.containsKey(META_DATA_PREFERENCE_TITLE)) {
                            if (bundle.get(META_DATA_PREFERENCE_TITLE) instanceof Integer) {
                                string = resourcesForApplication.getString(bundle.getInt(META_DATA_PREFERENCE_TITLE));
                            } else {
                                string = bundle.getString(META_DATA_PREFERENCE_TITLE);
                            }
                        } else {
                            string = stringFromBundle;
                        }
                        try {
                            CharSequence stringFromBundle2 = getStringFromBundle(overrideData, META_DATA_PREFERENCE_SUMMARY);
                            try {
                                if (TextUtils.isEmpty(stringFromBundle2) && bundle.containsKey(META_DATA_PREFERENCE_SUMMARY)) {
                                    if (bundle.get(META_DATA_PREFERENCE_SUMMARY) instanceof Integer) {
                                        string2 = resourcesForApplication.getString(bundle.getInt(META_DATA_PREFERENCE_SUMMARY));
                                    } else {
                                        string2 = bundle.getString(META_DATA_PREFERENCE_SUMMARY);
                                    }
                                    charSequenceLoadLabel = string2;
                                } else {
                                    charSequenceLoadLabel = stringFromBundle2;
                                }
                                if (bundle.containsKey(META_DATA_PREFERENCE_CUSTOM_VIEW)) {
                                    i = 1;
                                } else {
                                    i = 0;
                                }
                                charSequence = charSequenceLoadLabel;
                                charSequenceLoadLabel = string;
                            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                                e = e;
                                charSequence = stringFromBundle2;
                                charSequenceLoadLabel = string;
                                Log.w("CandidateSuggestion", "Couldn't find info", e);
                                i = 0;
                            }
                        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e2) {
                            e = e2;
                            charSequence = charSequenceLoadLabel;
                        }
                    } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e3) {
                        e = e3;
                        charSequence = null;
                        charSequenceLoadLabel = stringFromBundle;
                    }
                } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e4) {
                    e = e4;
                    charSequence = null;
                }
            } else {
                charSequence = null;
                iconCreateWithResource = null;
                i = 0;
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e5) {
            e = e5;
            charSequence = null;
            iconCreateWithResource = null;
        }
        if (TextUtils.isEmpty(charSequenceLoadLabel)) {
            charSequenceLoadLabel = this.mResolveInfo.activityInfo.loadLabel(packageManager);
        }
        builder.setTitle(charSequenceLoadLabel).setSummary(charSequence).setFlags(i).setIcon(iconCreateWithResource).setPendingIntent(PendingIntent.getActivity(this.mContext, 0, this.mIntent, 0));
    }

    private CharSequence getStringFromBundle(Bundle bundle, String str) {
        if (bundle == null || TextUtils.isEmpty(str)) {
            return null;
        }
        return bundle.getString(str);
    }

    private Bundle getOverrideData(Bundle bundle) {
        if (bundle == null || !bundle.containsKey(META_DATA_PREFERENCE_SUMMARY_URI)) {
            Log.d("CandidateSuggestion", "Metadata null or has no info about summary_uri");
            return null;
        }
        return getBundleFromUri(bundle.getString(META_DATA_PREFERENCE_SUMMARY_URI));
    }

    private Bundle getBundleFromUri(String str) {
        Uri uri = Uri.parse(str);
        String methodFromUri = getMethodFromUri(uri);
        if (TextUtils.isEmpty(methodFromUri)) {
            return null;
        }
        try {
            return this.mContext.getContentResolver().call(uri, methodFromUri, (String) null, (Bundle) null);
        } catch (IllegalArgumentException e) {
            Log.d("CandidateSuggestion", "Unknown summary_uri", e);
            return null;
        }
    }

    private String getMethodFromUri(Uri uri) {
        List<String> pathSegments;
        if (uri == null || (pathSegments = uri.getPathSegments()) == null || pathSegments.isEmpty()) {
            return null;
        }
        return pathSegments.get(0);
    }

    private String generateId() {
        return this.mComponent.flattenToString();
    }
}
