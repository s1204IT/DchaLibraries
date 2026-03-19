package com.mediatek.phone.ext;

public class OpPhoneCustomizationFactoryBase {
    public IAccessibilitySettingsExt makeAccessibilitySettingsExt() {
        return new DefaultAccessibilitySettingsExt();
    }

    public ITtyModeListPreferenceExt makeTtyModeListPreferenceExt() {
        return new DefaultTtyModeListPreferenceExt();
    }

    public ICallFeaturesSettingExt makeCallFeaturesSettingExt() {
        return new DefaultCallFeaturesSettingExt();
    }

    public ICallForwardExt makeCallForwardExt() {
        return new DefaultCallForwardExt();
    }

    public IPhoneMiscExt makePhoneMiscExt() {
        return new DefaultPhoneMiscExt();
    }

    public IMobileNetworkSettingsExt makeMobileNetworkSettingsExt() {
        return new DefaultMobileNetworkSettingsExt();
    }

    public INetworkSettingExt makeNetworkSettingExt() {
        return new DefaultNetworkSettingExt();
    }

    public IEmergencyDialerExt makeEmergencyDialerExt() {
        return new DefaultEmergencyDialerExt();
    }

    public IMmiCodeExt makeMmiCodeExt() {
        return new DefaultMmiCodeExt();
    }

    public ISsRoamingServiceExt makeSsRoamingServiceExt() {
        return new DefaultSsRoamingServiceExt();
    }

    public IDisconnectCauseExt makeDisconnectCauseExt() {
        return new DefaultDisconnectCauseExt();
    }

    public IIncomingCallExt makeIncomingCallExt() {
        return new DefaultIncomingCallExt();
    }

    public ITelephonyConnectionServiceExt makeTelephonyConnectionServiceExt() {
        return new DefaultTelephonyConnectionServiceExt();
    }

    public IDigitsUtilExt makeDigitsUtilExt() {
        return new DefaultDigitsUtilExt();
    }

    public ISimDialogExt makeSimDialogExt() {
        return new DefaultSimDialogExt();
    }

    public IPhoneGlobalsExt makePhoneGlobalsExt() {
        return new DefaultPhoneGlobalsExt();
    }
}
