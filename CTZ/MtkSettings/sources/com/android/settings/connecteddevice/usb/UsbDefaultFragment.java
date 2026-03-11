package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreferenceMixin;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UsbDefaultFragment extends RadioButtonPickerFragment {

    @VisibleForTesting
    UsbBackend mUsbBackend;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mUsbBackend = new UsbBackend(context);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        super.onCreatePreferences(bundle, str);
        new FooterPreferenceMixin(this, getLifecycle()).createFooterPreference().setTitle(R.string.usb_default_info);
    }

    @Override
    public int getMetricsCategory() {
        return 1312;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.usb_default_fragment;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        Iterator<Long> it = UsbDetailsFunctionsController.FUNCTIONS_MAP.keySet().iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            final String string = getContext().getString(UsbDetailsFunctionsController.FUNCTIONS_MAP.get(Long.valueOf(jLongValue)).intValue());
            final String strUsbFunctionsToString = UsbBackend.usbFunctionsToString(jLongValue);
            if (this.mUsbBackend.areFunctionsSupported(jLongValue)) {
                arrayListNewArrayList.add(new CandidateInfo(true) {
                    @Override
                    public CharSequence loadLabel() {
                        return string;
                    }

                    @Override
                    public Drawable loadIcon() {
                        return null;
                    }

                    @Override
                    public String getKey() {
                        return strUsbFunctionsToString;
                    }
                });
            }
        }
        return arrayListNewArrayList;
    }

    @Override
    protected String getDefaultKey() {
        return UsbBackend.usbFunctionsToString(this.mUsbBackend.getDefaultUsbFunctions());
    }

    @Override
    protected boolean setDefaultKey(String str) {
        long jUsbFunctionsFromString = UsbBackend.usbFunctionsFromString(str);
        if (!Utils.isMonkeyRunning()) {
            this.mUsbBackend.setDefaultUsbFunctions(jUsbFunctionsFromString);
            return true;
        }
        return true;
    }
}
