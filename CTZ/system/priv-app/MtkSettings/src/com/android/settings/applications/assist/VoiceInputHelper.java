package com.android.settings.applications.assist;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.provider.Settings;
import android.service.voice.VoiceInteractionServiceInfo;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public final class VoiceInputHelper {
    final List<ResolveInfo> mAvailableRecognition;
    final List<ResolveInfo> mAvailableVoiceInteractions;
    final Context mContext;
    ComponentName mCurrentRecognizer;
    ComponentName mCurrentVoiceInteraction;
    final ArrayList<InteractionInfo> mAvailableInteractionInfos = new ArrayList<>();
    final ArrayList<RecognizerInfo> mAvailableRecognizerInfos = new ArrayList<>();

    public static class BaseInfo implements Comparable {
        public final CharSequence appLabel;
        public final ComponentName componentName;
        public final String key;
        public final CharSequence label;
        public final String labelStr;
        public final ServiceInfo service;
        public final ComponentName settings;

        public BaseInfo(PackageManager packageManager, ServiceInfo serviceInfo, String str) {
            this.service = serviceInfo;
            this.componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            this.key = this.componentName.flattenToShortString();
            this.settings = str != null ? new ComponentName(serviceInfo.packageName, str) : null;
            this.label = serviceInfo.loadLabel(packageManager);
            this.labelStr = this.label.toString();
            this.appLabel = serviceInfo.applicationInfo.loadLabel(packageManager);
        }

        @Override // java.lang.Comparable
        public int compareTo(Object obj) {
            return this.labelStr.compareTo(((BaseInfo) obj).labelStr);
        }
    }

    public static class InteractionInfo extends BaseInfo {
        public final VoiceInteractionServiceInfo serviceInfo;

        public InteractionInfo(PackageManager packageManager, VoiceInteractionServiceInfo voiceInteractionServiceInfo) {
            super(packageManager, voiceInteractionServiceInfo.getServiceInfo(), voiceInteractionServiceInfo.getSettingsActivity());
            this.serviceInfo = voiceInteractionServiceInfo;
        }
    }

    public static class RecognizerInfo extends BaseInfo {
        public RecognizerInfo(PackageManager packageManager, ServiceInfo serviceInfo, String str) {
            super(packageManager, serviceInfo, str);
        }
    }

    public VoiceInputHelper(Context context) {
        this.mContext = context;
        this.mAvailableVoiceInteractions = this.mContext.getPackageManager().queryIntentServices(new Intent("android.service.voice.VoiceInteractionService"), 128);
        this.mAvailableRecognition = this.mContext.getPackageManager().queryIntentServices(new Intent("android.speech.RecognitionService"), 128);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [199=6] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x0129 A[PHI: r7 r8
  0x0129: PHI (r7v11 android.content.res.XmlResourceParser) = 
  (r7v7 android.content.res.XmlResourceParser)
  (r7v8 android.content.res.XmlResourceParser)
  (r7v9 android.content.res.XmlResourceParser)
  (r7v14 android.content.res.XmlResourceParser)
 binds: [B:69:0x0185, B:65:0x017a, B:60:0x016d, B:36:0x0127] A[DONT_GENERATE, DONT_INLINE]
  0x0129: PHI (r8v9 java.lang.String) = (r8v5 java.lang.String), (r8v6 java.lang.String), (r8v7 java.lang.String), (r8v19 java.lang.String) binds: [B:69:0x0185, B:65:0x017a, B:60:0x016d, B:36:0x0127] A[DONT_GENERATE, DONT_INLINE]] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void buildUi() throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        String string;
        Resources resourcesForApplication;
        AttributeSet attributeSetAsAttributeSet;
        int next;
        String string2 = Settings.Secure.getString(this.mContext.getContentResolver(), "voice_interaction_service");
        if (string2 == null || string2.isEmpty()) {
            this.mCurrentVoiceInteraction = null;
        } else {
            this.mCurrentVoiceInteraction = ComponentName.unflattenFromString(string2);
        }
        ArraySet arraySet = new ArraySet();
        int size = this.mAvailableVoiceInteractions.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = this.mAvailableVoiceInteractions.get(i);
            VoiceInteractionServiceInfo voiceInteractionServiceInfo = new VoiceInteractionServiceInfo(this.mContext.getPackageManager(), resolveInfo.serviceInfo);
            if (voiceInteractionServiceInfo.getParseError() != null) {
                Log.w("VoiceInteractionService", "Error in VoiceInteractionService " + resolveInfo.serviceInfo.packageName + "/" + resolveInfo.serviceInfo.name + ": " + voiceInteractionServiceInfo.getParseError());
            } else {
                this.mAvailableInteractionInfos.add(new InteractionInfo(this.mContext.getPackageManager(), voiceInteractionServiceInfo));
                arraySet.add(new ComponentName(resolveInfo.serviceInfo.packageName, voiceInteractionServiceInfo.getRecognitionService()));
            }
        }
        Collections.sort(this.mAvailableInteractionInfos);
        String string3 = Settings.Secure.getString(this.mContext.getContentResolver(), "voice_recognition_service");
        if (string3 == null || string3.isEmpty()) {
            this.mCurrentRecognizer = null;
        } else {
            this.mCurrentRecognizer = ComponentName.unflattenFromString(string3);
        }
        int size2 = this.mAvailableRecognition.size();
        for (int i2 = 0; i2 < size2; i2++) {
            ResolveInfo resolveInfo2 = this.mAvailableRecognition.get(i2);
            arraySet.contains(new ComponentName(resolveInfo2.serviceInfo.packageName, resolveInfo2.serviceInfo.name));
            ServiceInfo serviceInfo = resolveInfo2.serviceInfo;
            try {
                xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(this.mContext.getPackageManager(), "android.speech");
            } catch (PackageManager.NameNotFoundException e) {
                e = e;
                xmlResourceParserLoadXmlMetaData = null;
                string = null;
            } catch (IOException e2) {
                e = e2;
                xmlResourceParserLoadXmlMetaData = null;
                string = null;
            } catch (XmlPullParserException e3) {
                e = e3;
                xmlResourceParserLoadXmlMetaData = null;
                string = null;
            } catch (Throwable th) {
                th = th;
                xmlResourceParserLoadXmlMetaData = null;
            }
            if (xmlResourceParserLoadXmlMetaData == null) {
                throw new XmlPullParserException("No android.speech meta-data for " + serviceInfo.packageName);
            }
            try {
                try {
                    resourcesForApplication = this.mContext.getPackageManager().getResourcesForApplication(serviceInfo.applicationInfo);
                    attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
                    do {
                        next = xmlResourceParserLoadXmlMetaData.next();
                        if (next == 1) {
                            break;
                        }
                    } while (next != 2);
                } catch (PackageManager.NameNotFoundException e4) {
                    e = e4;
                    string = null;
                } catch (IOException e5) {
                    e = e5;
                    string = null;
                } catch (XmlPullParserException e6) {
                    e = e6;
                    string = null;
                }
                if (!"recognition-service".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                    throw new XmlPullParserException("Meta-data does not start with recognition-service tag");
                }
                TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.RecognitionService);
                string = typedArrayObtainAttributes.getString(0);
                try {
                    typedArrayObtainAttributes.recycle();
                } catch (PackageManager.NameNotFoundException e7) {
                    e = e7;
                    Log.e("VoiceInputHelper", "error parsing recognition service meta-data", e);
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                        this.mAvailableRecognizerInfos.add(new RecognizerInfo(this.mContext.getPackageManager(), resolveInfo2.serviceInfo, string));
                    } else {
                        this.mAvailableRecognizerInfos.add(new RecognizerInfo(this.mContext.getPackageManager(), resolveInfo2.serviceInfo, string));
                    }
                } catch (IOException e8) {
                    e = e8;
                    Log.e("VoiceInputHelper", "error parsing recognition service meta-data", e);
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                        this.mAvailableRecognizerInfos.add(new RecognizerInfo(this.mContext.getPackageManager(), resolveInfo2.serviceInfo, string));
                    } else {
                        this.mAvailableRecognizerInfos.add(new RecognizerInfo(this.mContext.getPackageManager(), resolveInfo2.serviceInfo, string));
                    }
                } catch (XmlPullParserException e9) {
                    e = e9;
                    Log.e("VoiceInputHelper", "error parsing recognition service meta-data", e);
                    if (xmlResourceParserLoadXmlMetaData != null) {
                    }
                    this.mAvailableRecognizerInfos.add(new RecognizerInfo(this.mContext.getPackageManager(), resolveInfo2.serviceInfo, string));
                }
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                this.mAvailableRecognizerInfos.add(new RecognizerInfo(this.mContext.getPackageManager(), resolveInfo2.serviceInfo, string));
            } catch (Throwable th2) {
                th = th2;
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                throw th;
            }
        }
        Collections.sort(this.mAvailableRecognizerInfos);
    }
}
