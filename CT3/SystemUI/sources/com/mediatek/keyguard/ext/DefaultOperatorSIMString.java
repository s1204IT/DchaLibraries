package com.mediatek.keyguard.ext;

import android.content.Context;
import com.mediatek.common.PluginImpl;
import com.mediatek.keyguard.ext.IOperatorSIMString;

@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.IOperatorSIMString")
public class DefaultOperatorSIMString implements IOperatorSIMString {
    @Override
    public String getOperatorSIMString(String sourceStr, int slotId, IOperatorSIMString.SIMChangedTag simChangedTag, Context context) {
        return sourceStr;
    }
}
