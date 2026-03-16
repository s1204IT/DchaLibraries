package com.android.managedprovisioning;

import android.content.IntentFilter;
import android.content.pm.PackageManager;

public class CrossProfileIntentFiltersHelper {
    public static void setFilters(PackageManager pm, int parentUserId, int managedProfileUserId) {
        ProvisionLogger.logd("Setting cross-profile intent filters");
        IntentFilter mimeTypeTelephony = new IntentFilter();
        mimeTypeTelephony.addAction("android.intent.action.DIAL");
        mimeTypeTelephony.addAction("android.intent.action.VIEW");
        mimeTypeTelephony.addAction("android.intent.action.CALL_EMERGENCY");
        mimeTypeTelephony.addAction("android.intent.action.CALL_PRIVILEGED");
        mimeTypeTelephony.addCategory("android.intent.category.DEFAULT");
        mimeTypeTelephony.addCategory("android.intent.category.BROWSABLE");
        try {
            mimeTypeTelephony.addDataType("vnd.android.cursor.item/phone");
            mimeTypeTelephony.addDataType("vnd.android.cursor.item/phone_v2");
            mimeTypeTelephony.addDataType("vnd.android.cursor.item/person");
            mimeTypeTelephony.addDataType("vnd.android.cursor.dir/calls");
            mimeTypeTelephony.addDataType("vnd.android.cursor.item/calls");
        } catch (IntentFilter.MalformedMimeTypeException e) {
        }
        pm.addCrossProfileIntentFilter(mimeTypeTelephony, managedProfileUserId, parentUserId, 2);
        IntentFilter callEmergency = new IntentFilter();
        callEmergency.addAction("android.intent.action.CALL_EMERGENCY");
        callEmergency.addAction("android.intent.action.CALL_PRIVILEGED");
        callEmergency.addCategory("android.intent.category.DEFAULT");
        callEmergency.addCategory("android.intent.category.BROWSABLE");
        callEmergency.addDataScheme("tel");
        callEmergency.addDataScheme("sip");
        callEmergency.addDataScheme("voicemail");
        pm.addCrossProfileIntentFilter(callEmergency, managedProfileUserId, parentUserId, 2);
        IntentFilter callVoicemail = new IntentFilter();
        callVoicemail.addAction("android.intent.action.DIAL");
        callVoicemail.addAction("android.intent.action.CALL");
        callVoicemail.addAction("android.intent.action.VIEW");
        callVoicemail.addCategory("android.intent.category.DEFAULT");
        callVoicemail.addCategory("android.intent.category.BROWSABLE");
        callVoicemail.addDataScheme("voicemail");
        pm.addCrossProfileIntentFilter(callVoicemail, managedProfileUserId, parentUserId, 2);
        IntentFilter callDial = new IntentFilter();
        callDial.addAction("android.intent.action.DIAL");
        callDial.addAction("android.intent.action.CALL");
        callDial.addAction("android.intent.action.VIEW");
        callDial.addCategory("android.intent.category.DEFAULT");
        callDial.addCategory("android.intent.category.BROWSABLE");
        callDial.addDataScheme("tel");
        callDial.addDataScheme("sip");
        pm.addCrossProfileIntentFilter(callDial, managedProfileUserId, parentUserId, 0);
        IntentFilter callButton = new IntentFilter();
        callButton.addAction("android.intent.action.CALL_BUTTON");
        callButton.addCategory("android.intent.category.DEFAULT");
        pm.addCrossProfileIntentFilter(callButton, managedProfileUserId, parentUserId, 0);
        IntentFilter callDialNoData = new IntentFilter();
        callDialNoData.addAction("android.intent.action.DIAL");
        callDialNoData.addAction("android.intent.action.CALL");
        callDialNoData.addCategory("android.intent.category.DEFAULT");
        callDialNoData.addCategory("android.intent.category.BROWSABLE");
        pm.addCrossProfileIntentFilter(callDialNoData, managedProfileUserId, parentUserId, 2);
        IntentFilter smsMms = new IntentFilter();
        smsMms.addAction("android.intent.action.VIEW");
        smsMms.addAction("android.intent.action.SENDTO");
        smsMms.addCategory("android.intent.category.DEFAULT");
        smsMms.addCategory("android.intent.category.BROWSABLE");
        smsMms.addDataScheme("sms");
        smsMms.addDataScheme("smsto");
        smsMms.addDataScheme("mms");
        smsMms.addDataScheme("mmsto");
        pm.addCrossProfileIntentFilter(smsMms, managedProfileUserId, parentUserId, 2);
        IntentFilter mobileNetworkSettings = new IntentFilter();
        mobileNetworkSettings.addAction("android.settings.DATA_ROAMING_SETTINGS");
        mobileNetworkSettings.addAction("android.settings.NETWORK_OPERATOR_SETTINGS");
        mobileNetworkSettings.addCategory("android.intent.category.DEFAULT");
        pm.addCrossProfileIntentFilter(mobileNetworkSettings, managedProfileUserId, parentUserId, 2);
        IntentFilter home = new IntentFilter();
        home.addAction("android.intent.action.MAIN");
        home.addCategory("android.intent.category.DEFAULT");
        home.addCategory("android.intent.category.HOME");
        pm.addCrossProfileIntentFilter(home, managedProfileUserId, parentUserId, 2);
        IntentFilter send = new IntentFilter();
        send.addAction("android.intent.action.SEND");
        send.addAction("android.intent.action.SEND_MULTIPLE");
        send.addCategory("android.intent.category.DEFAULT");
        try {
            send.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e2) {
        }
        pm.addCrossProfileIntentFilter(send, parentUserId, managedProfileUserId, 0);
        IntentFilter getContent = new IntentFilter();
        getContent.addAction("android.intent.action.GET_CONTENT");
        getContent.addCategory("android.intent.category.DEFAULT");
        getContent.addCategory("android.intent.category.OPENABLE");
        try {
            getContent.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e3) {
        }
        pm.addCrossProfileIntentFilter(getContent, managedProfileUserId, parentUserId, 0);
        IntentFilter openDocument = new IntentFilter();
        openDocument.addAction("android.intent.action.OPEN_DOCUMENT");
        openDocument.addCategory("android.intent.category.DEFAULT");
        openDocument.addCategory("android.intent.category.OPENABLE");
        try {
            openDocument.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e4) {
        }
        pm.addCrossProfileIntentFilter(openDocument, managedProfileUserId, parentUserId, 0);
        IntentFilter pick = new IntentFilter();
        pick.addAction("android.intent.action.PICK");
        pick.addCategory("android.intent.category.DEFAULT");
        try {
            pick.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e5) {
        }
        pm.addCrossProfileIntentFilter(pick, managedProfileUserId, parentUserId, 0);
        IntentFilter pickNoData = new IntentFilter();
        pickNoData.addAction("android.intent.action.PICK");
        pickNoData.addCategory("android.intent.category.DEFAULT");
        pm.addCrossProfileIntentFilter(pickNoData, managedProfileUserId, parentUserId, 0);
        IntentFilter recognizeSpeech = new IntentFilter();
        recognizeSpeech.addAction("android.speech.action.RECOGNIZE_SPEECH");
        recognizeSpeech.addCategory("android.intent.category.DEFAULT");
        pm.addCrossProfileIntentFilter(recognizeSpeech, managedProfileUserId, parentUserId, 0);
        IntentFilter capture = new IntentFilter();
        capture.addAction("android.media.action.IMAGE_CAPTURE");
        capture.addAction("android.media.action.IMAGE_CAPTURE_SECURE");
        capture.addAction("android.media.action.VIDEO_CAPTURE");
        capture.addAction("android.media.action.STILL_IMAGE_CAMERA");
        capture.addAction("android.media.action.STILL_IMAGE_CAMERA_SECURE");
        capture.addAction("android.media.action.VIDEO_CAMERA");
        capture.addCategory("android.intent.category.DEFAULT");
        pm.addCrossProfileIntentFilter(capture, managedProfileUserId, parentUserId, 0);
    }
}
