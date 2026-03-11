package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.EntityHeaderController;

public class UsbDetailsHeaderController extends UsbDetailsController {
    private EntityHeaderController mHeaderController;

    public UsbDetailsHeaderController(Context context, UsbDetailsFragment usbDetailsFragment, UsbBackend usbBackend) {
        super(context, usbDetailsFragment, usbBackend);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mHeaderController = EntityHeaderController.newInstance(this.mFragment.getActivity(), this.mFragment, ((LayoutPreference) preferenceScreen.findPreference("usb_device_header")).findViewById(R.id.entity_header));
    }

    @Override
    protected void refresh(boolean z, long j, int i, int i2) {
        this.mHeaderController.setLabel(this.mContext.getString(R.string.usb_pref));
        this.mHeaderController.setIcon(this.mContext.getDrawable(R.drawable.ic_usb));
        this.mHeaderController.done(this.mFragment.getActivity(), true);
    }

    @Override
    public String getPreferenceKey() {
        return "usb_device_header";
    }
}
