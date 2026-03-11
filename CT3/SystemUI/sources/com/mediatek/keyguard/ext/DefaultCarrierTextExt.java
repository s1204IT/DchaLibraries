package com.mediatek.keyguard.ext;

import android.content.Context;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.ICarrierTextExt")
public class DefaultCarrierTextExt implements ICarrierTextExt {
    @Override
    public CharSequence customizeCarrierTextCapital(CharSequence carrierText) {
        if (carrierText != null) {
            return carrierText.toString().toUpperCase();
        }
        return null;
    }

    @Override
    public CharSequence customizeCarrierText(CharSequence carrierText, CharSequence simMessage, int simId) {
        return carrierText;
    }

    @Override
    public boolean showCarrierTextWhenSimMissing(boolean isSimMissing, int simId) {
        return isSimMissing;
    }

    @Override
    public CharSequence customizeCarrierTextWhenCardTypeLocked(CharSequence carrierText, Context context, int phoneId, boolean isCardLocked) {
        return carrierText;
    }

    @Override
    public CharSequence customizeCarrierTextWhenSimMissing(CharSequence carrierText) {
        return carrierText;
    }

    @Override
    public String customizeCarrierTextDivider(String divider) {
        return divider;
    }
}
