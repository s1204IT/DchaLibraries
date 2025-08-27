package com.android.settings.applications.defaultapps;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import com.android.internal.telephony.SmsApplication;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.CandidateInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultSmsPicker extends DefaultAppPickerFragment {
    private DefaultKeyUpdater mDefaultKeyUpdater = new DefaultKeyUpdater();

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 789;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.default_sms_settings;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<DefaultAppInfo> getCandidates() {
        Context context = getContext();
        Collection applicationCollection = SmsApplication.getApplicationCollection(context);
        ArrayList arrayList = new ArrayList(applicationCollection.size());
        Iterator it = applicationCollection.iterator();
        while (it.hasNext()) {
            try {
                arrayList.add(new DefaultAppInfo(context, this.mPm, this.mPm.getApplicationInfoAsUser(((SmsApplication.SmsApplicationData) it.next()).mPackageName, 0, this.mUserId)));
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        return this.mDefaultKeyUpdater.getDefaultApplication(getContext());
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected boolean setDefaultKey(String str) {
        if (!TextUtils.isEmpty(str) && !TextUtils.equals(str, getDefaultKey())) {
            this.mDefaultKeyUpdater.setDefaultApplication(getContext(), str);
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Method merged with bridge method: getConfirmationMessage(Lcom/android/settingslib/widget/CandidateInfo;)Ljava/lang/CharSequence; */
    @Override // com.android.settings.applications.defaultapps.DefaultAppPickerFragment
    protected String getConfirmationMessage(CandidateInfo candidateInfo) {
        if (Utils.isPackageDirectBootAware(getContext(), candidateInfo.getKey())) {
            return null;
        }
        return getContext().getString(R.string.direct_boot_unaware_dialog_message);
    }

    static class DefaultKeyUpdater {
        DefaultKeyUpdater() {
        }

        public String getDefaultApplication(Context context) {
            ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(context, true);
            if (defaultSmsApplication != null) {
                return defaultSmsApplication.getPackageName();
            }
            return null;
        }

        public void setDefaultApplication(Context context, String str) {
            SmsApplication.setDefaultApplication(str, context);
        }
    }
}
