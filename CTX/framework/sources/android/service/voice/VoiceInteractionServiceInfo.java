package android.service.voice;

import android.Manifest;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public class VoiceInteractionServiceInfo {
    static final String TAG = "VoiceInteractionServiceInfo";
    private String mParseError;
    private String mRecognitionService;
    private ServiceInfo mServiceInfo;
    private String mSessionService;
    private String mSettingsActivity;
    private boolean mSupportsAssist;
    private boolean mSupportsLaunchFromKeyguard;
    private boolean mSupportsLocalInteraction;

    public VoiceInteractionServiceInfo(PackageManager packageManager, ComponentName componentName) throws PackageManager.NameNotFoundException {
        this(packageManager, packageManager.getServiceInfo(componentName, 128));
    }

    public VoiceInteractionServiceInfo(PackageManager packageManager, ComponentName componentName, int i) throws PackageManager.NameNotFoundException {
        this(packageManager, getServiceInfoOrThrow(componentName, i));
    }

    static ServiceInfo getServiceInfoOrThrow(ComponentName componentName, int i) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo serviceInfo = AppGlobals.getPackageManager().getServiceInfo(componentName, 269222016, i);
            if (serviceInfo != null) {
                return serviceInfo;
            }
        } catch (RemoteException e) {
        }
        throw new PackageManager.NameNotFoundException(componentName.toString());
    }

    public VoiceInteractionServiceInfo(PackageManager packageManager, ServiceInfo serviceInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        int next;
        if (serviceInfo == null) {
            this.mParseError = "Service not available";
            return;
        }
        if (!Manifest.permission.BIND_VOICE_INTERACTION.equals(serviceInfo.permission)) {
            this.mParseError = "Service does not require permission android.permission.BIND_VOICE_INTERACTION";
            return;
        }
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(packageManager, VoiceInteractionService.SERVICE_META_DATA);
            } catch (Throwable th) {
                th = th;
                xmlResourceParserLoadXmlMetaData = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e = e;
        } catch (IOException e2) {
            e = e2;
        } catch (XmlPullParserException e3) {
            e = e3;
        }
        try {
            if (xmlResourceParserLoadXmlMetaData == null) {
                this.mParseError = "No android.voice_interaction meta-data for " + serviceInfo.packageName;
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                    return;
                }
                return;
            }
            Resources resourcesForApplication = packageManager.getResourcesForApplication(serviceInfo.applicationInfo);
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
            do {
                next = xmlResourceParserLoadXmlMetaData.next();
                if (next == 1) {
                    break;
                }
            } while (next != 2);
            if (!"voice-interaction-service".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                this.mParseError = "Meta-data does not start with voice-interaction-service tag";
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                    return;
                }
                return;
            }
            TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.VoiceInteractionService);
            this.mSessionService = typedArrayObtainAttributes.getString(1);
            this.mRecognitionService = typedArrayObtainAttributes.getString(2);
            this.mSettingsActivity = typedArrayObtainAttributes.getString(0);
            this.mSupportsAssist = typedArrayObtainAttributes.getBoolean(3, false);
            this.mSupportsLaunchFromKeyguard = typedArrayObtainAttributes.getBoolean(4, false);
            this.mSupportsLocalInteraction = typedArrayObtainAttributes.getBoolean(5, false);
            typedArrayObtainAttributes.recycle();
            if (this.mSessionService == null) {
                this.mParseError = "No sessionService specified";
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                    return;
                }
                return;
            }
            if (this.mRecognitionService != null) {
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                this.mServiceInfo = serviceInfo;
            } else {
                this.mParseError = "No recognitionService specified";
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
            }
        } catch (PackageManager.NameNotFoundException e4) {
            e = e4;
            xmlResourceParser = xmlResourceParserLoadXmlMetaData;
            this.mParseError = "Error parsing voice interation service meta-data: " + e;
            Log.w(TAG, "error parsing voice interaction service meta-data", e);
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        } catch (IOException e5) {
            e = e5;
            xmlResourceParser = xmlResourceParserLoadXmlMetaData;
            this.mParseError = "Error parsing voice interation service meta-data: " + e;
            Log.w(TAG, "error parsing voice interaction service meta-data", e);
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        } catch (XmlPullParserException e6) {
            e = e6;
            xmlResourceParser = xmlResourceParserLoadXmlMetaData;
            this.mParseError = "Error parsing voice interation service meta-data: " + e;
            Log.w(TAG, "error parsing voice interaction service meta-data", e);
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        } catch (Throwable th2) {
            th = th2;
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            throw th;
        }
    }

    public String getParseError() {
        return this.mParseError;
    }

    public ServiceInfo getServiceInfo() {
        return this.mServiceInfo;
    }

    public String getSessionService() {
        return this.mSessionService;
    }

    public String getRecognitionService() {
        return this.mRecognitionService;
    }

    public String getSettingsActivity() {
        return this.mSettingsActivity;
    }

    public boolean getSupportsAssist() {
        return this.mSupportsAssist;
    }

    public boolean getSupportsLaunchFromKeyguard() {
        return this.mSupportsLaunchFromKeyguard;
    }

    public boolean getSupportsLocalInteraction() {
        return this.mSupportsLocalInteraction;
    }
}
