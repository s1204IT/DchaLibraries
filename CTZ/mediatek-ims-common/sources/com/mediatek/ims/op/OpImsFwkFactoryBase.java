package com.mediatek.ims.op;

import android.telephony.Rlog;

public class OpImsFwkFactoryBase implements OpImsFwkFactory {
    private static OpImsFwkFactory sInstance;

    public static OpImsFwkFactory getInstance() {
        if (sInstance == null) {
            try {
                sInstance = (OpImsFwkFactory) Class.forName("com.mediatek.op.net.ims.OpImsFwkFactoryImpl").newInstance();
                Rlog.d("OpImsFactory[NET IMS]", "OP packages loaded sucessfully");
            } catch (Exception e) {
                sInstance = new OpImsFwkFactoryBase();
                Rlog.d("OpImsFactory[NET IMS]", "OP packages are not found");
            }
        }
        return sInstance;
    }

    @Override
    public OpImsCall makeOpImsCall() {
        return new OpImsCallBase();
    }
}
