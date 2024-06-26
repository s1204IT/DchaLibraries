package com.android.settings.voice;

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

    /* loaded from: classes.dex */
    public static class BaseInfo implements Comparable {
        public final CharSequence appLabel;
        public final ComponentName componentName;
        public final String key;
        public final CharSequence label;
        public final String labelStr;
        public final ServiceInfo service;
        public final ComponentName settings;

        public BaseInfo(PackageManager pm, ServiceInfo _service, String _settings) {
            this.service = _service;
            this.componentName = new ComponentName(_service.packageName, _service.name);
            this.key = this.componentName.flattenToShortString();
            this.settings = _settings != null ? new ComponentName(_service.packageName, _settings) : null;
            this.label = _service.loadLabel(pm);
            this.labelStr = this.label.toString();
            this.appLabel = _service.applicationInfo.loadLabel(pm);
        }

        @Override // java.lang.Comparable
        public int compareTo(Object another) {
            return this.labelStr.compareTo(((BaseInfo) another).labelStr);
        }
    }

    /* loaded from: classes.dex */
    public static class InteractionInfo extends BaseInfo {
        public final VoiceInteractionServiceInfo serviceInfo;

        public InteractionInfo(PackageManager pm, VoiceInteractionServiceInfo _service) {
            super(pm, _service.getServiceInfo(), _service.getSettingsActivity());
            this.serviceInfo = _service;
        }
    }

    /* loaded from: classes.dex */
    public static class RecognizerInfo extends BaseInfo {
        public RecognizerInfo(PackageManager pm, ServiceInfo _service, String _settings) {
            super(pm, _service, _settings);
        }
    }

    public VoiceInputHelper(Context context) {
        this.mContext = context;
        this.mAvailableVoiceInteractions = this.mContext.getPackageManager().queryIntentServices(new Intent("android.service.voice.VoiceInteractionService"), 128);
        this.mAvailableRecognition = this.mContext.getPackageManager().queryIntentServices(new Intent("android.speech.RecognitionService"), 128);
    }

    public void buildUi() {
        XmlResourceParser parser;
        int type;
        String currentSetting = Settings.Secure.getString(this.mContext.getContentResolver(), "voice_interaction_service");
        if (currentSetting == null || currentSetting.isEmpty()) {
            this.mCurrentVoiceInteraction = null;
        } else {
            this.mCurrentVoiceInteraction = ComponentName.unflattenFromString(currentSetting);
        }
        ArraySet<ComponentName> interactorRecognizers = new ArraySet<>();
        int size = this.mAvailableVoiceInteractions.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = this.mAvailableVoiceInteractions.get(i);
            VoiceInteractionServiceInfo info = new VoiceInteractionServiceInfo(this.mContext.getPackageManager(), resolveInfo.serviceInfo);
            if (info.getParseError() != null) {
                Log.w("VoiceInteractionService", "Error in VoiceInteractionService " + resolveInfo.serviceInfo.packageName + "/" + resolveInfo.serviceInfo.name + ": " + info.getParseError());
            } else {
                this.mAvailableInteractionInfos.add(new InteractionInfo(this.mContext.getPackageManager(), info));
                interactorRecognizers.add(new ComponentName(resolveInfo.serviceInfo.packageName, info.getRecognitionService()));
            }
        }
        Collections.sort(this.mAvailableInteractionInfos);
        String currentSetting2 = Settings.Secure.getString(this.mContext.getContentResolver(), "voice_recognition_service");
        if (currentSetting2 == null || currentSetting2.isEmpty()) {
            this.mCurrentRecognizer = null;
        } else {
            this.mCurrentRecognizer = ComponentName.unflattenFromString(currentSetting2);
        }
        int size2 = this.mAvailableRecognition.size();
        for (int i2 = 0; i2 < size2; i2++) {
            ResolveInfo resolveInfo2 = this.mAvailableRecognition.get(i2);
            ComponentName comp = new ComponentName(resolveInfo2.serviceInfo.packageName, resolveInfo2.serviceInfo.name);
            if (interactorRecognizers.contains(comp)) {
            }
            ServiceInfo si = resolveInfo2.serviceInfo;
            XmlResourceParser xmlResourceParser = null;
            String settingsActivity = null;
            try {
                try {
                    try {
                        try {
                            parser = si.loadXmlMetaData(this.mContext.getPackageManager(), "android.speech");
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e("VoiceInputHelper", "error parsing recognition service meta-data", e);
                            if (0 != 0) {
                                xmlResourceParser.close();
                            }
                        }
                    } catch (IOException e2) {
                        Log.e("VoiceInputHelper", "error parsing recognition service meta-data", e2);
                        if (0 != 0) {
                            xmlResourceParser.close();
                        }
                    }
                } catch (XmlPullParserException e3) {
                    Log.e("VoiceInputHelper", "error parsing recognition service meta-data", e3);
                    if (0 != 0) {
                        xmlResourceParser.close();
                    }
                }
                if (parser == null) {
                    throw new XmlPullParserException("No android.speech meta-data for " + si.packageName);
                }
                Resources res = this.mContext.getPackageManager().getResourcesForApplication(si.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"recognition-service".equals(nodeName)) {
                    throw new XmlPullParserException("Meta-data does not start with recognition-service tag");
                }
                TypedArray array = res.obtainAttributes(attrs, R.styleable.RecognitionService);
                settingsActivity = array.getString(0);
                array.recycle();
                if (parser != null) {
                    parser.close();
                }
                this.mAvailableRecognizerInfos.add(new RecognizerInfo(this.mContext.getPackageManager(), resolveInfo2.serviceInfo, settingsActivity));
            } catch (Throwable th) {
                if (0 != 0) {
                    xmlResourceParser.close();
                }
                throw th;
            }
        }
        Collections.sort(this.mAvailableRecognizerInfos);
    }
}
