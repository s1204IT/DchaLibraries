package com.android.managedprovisioning.preprovisioning.terms;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.DisclaimersParam;
import com.android.managedprovisioning.model.ProvisioningParams;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TermsProvider {
    private final Context mContext;
    private final StoreUtils.TextFileReader mTextFileReader;
    private final Utils mUtils;

    public TermsProvider(Context context, StoreUtils.TextFileReader textFileReader, Utils utils) {
        this.mContext = context;
        this.mTextFileReader = textFileReader;
        this.mUtils = utils;
    }

    public List<TermsDocument> getTerms(ProvisioningParams provisioningParams, int i) {
        ArrayList arrayList = new ArrayList();
        int iDetermineProvisioningCase = determineProvisioningCase(provisioningParams);
        if ((i & 1) == 0) {
            arrayList.add(getGeneralDisclaimer(iDetermineProvisioningCase));
        }
        if (iDetermineProvisioningCase == 2) {
            arrayList.addAll(getSystemAppTerms());
        }
        arrayList.addAll(getExtraDisclaimers(provisioningParams));
        return (List) arrayList.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Objects.nonNull((TermsDocument) obj);
            }
        }).collect(Collectors.toList());
    }

    private int determineProvisioningCase(ProvisioningParams provisioningParams) {
        if (this.mUtils.isDeviceOwnerAction(provisioningParams.provisioningAction)) {
            return 2;
        }
        return ((DevicePolicyManager) this.mContext.getSystemService("device_policy")).isDeviceManaged() ? 4 : 1;
    }

    private TermsDocument getGeneralDisclaimer(int i) {
        int i2;
        int i3;
        Context context = this.mContext;
        if (i == 1) {
            i2 = R.string.work_profile_info;
        } else {
            i2 = R.string.managed_device_info;
        }
        String string = context.getString(i2);
        Context context2 = this.mContext;
        if (i == 1) {
            i3 = R.string.admin_has_ability_to_monitor_profile;
        } else {
            i3 = R.string.admin_has_ability_to_monitor_device;
        }
        return TermsDocument.createInstance(string, context2.getString(i3));
    }

    private List<TermsDocument> getSystemAppTerms() {
        ArrayList arrayList = new ArrayList();
        for (ApplicationInfo applicationInfo : this.mContext.getPackageManager().getInstalledApplications(1048704)) {
            String stringMetaData = getStringMetaData(applicationInfo, "android.app.extra.PROVISIONING_DISCLAIMER_HEADER");
            String stringMetaData2 = getStringMetaData(applicationInfo, "android.app.extra.PROVISIONING_DISCLAIMER_CONTENT");
            if (stringMetaData != null && stringMetaData2 != null) {
                arrayList.add(TermsDocument.createInstance(stringMetaData, stringMetaData2));
            }
        }
        return arrayList;
    }

    private List<TermsDocument> getExtraDisclaimers(ProvisioningParams provisioningParams) {
        ArrayList arrayList = new ArrayList();
        DisclaimersParam.Disclaimer[] disclaimerArr = provisioningParams.disclaimersParam == null ? null : provisioningParams.disclaimersParam.mDisclaimers;
        if (disclaimerArr != null) {
            for (DisclaimersParam.Disclaimer disclaimer : disclaimerArr) {
                try {
                    arrayList.add(TermsDocument.createInstance(disclaimer.mHeader, this.mTextFileReader.read(new File(disclaimer.mContentFilePath))));
                } catch (IOException e) {
                    ProvisionLogger.loge("Failed to read disclaimer", e);
                }
            }
        }
        return arrayList;
    }

    private String getStringMetaData(ApplicationInfo applicationInfo, String str) {
        int i;
        if (applicationInfo.metaData != null && (i = applicationInfo.metaData.getInt(str)) != 0) {
            try {
                return this.mContext.getPackageManager().getResourcesForApplication(applicationInfo).getString(i);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                ProvisionLogger.loge("NameNotFoundException", e);
                return null;
            }
        }
        return null;
    }
}
