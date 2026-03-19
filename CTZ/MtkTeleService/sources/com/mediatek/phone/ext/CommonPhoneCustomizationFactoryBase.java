package com.mediatek.phone.ext;

public class CommonPhoneCustomizationFactoryBase {
    public IGttInfoExt makeGttInfoExt() {
        return new DefaultGttInfoExt();
    }

    public IRttUtilExt makeRttUtilExt() {
        return new DefaultRttUtilExt();
    }

    public IIncomingCallExt makeIncomingCallExt() {
        return new DefaultIncomingCallExt();
    }
}
