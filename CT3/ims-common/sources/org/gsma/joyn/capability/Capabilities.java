package org.gsma.joyn.capability;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gsma.joyn.Logger;

public class Capabilities implements Parcelable {
    public static final Parcelable.Creator<Capabilities> CREATOR = new Parcelable.Creator<Capabilities>() {
        @Override
        public Capabilities createFromParcel(Parcel source) {
            return new Capabilities(source);
        }

        @Override
        public Capabilities[] newArray(int size) {
            return new Capabilities[size];
        }
    };
    public static final String TAG = "Capabilities";
    private boolean IR94_DuplexMode;
    private boolean IR94_VideoCall;
    private boolean IR94_VoiceCall;
    private boolean automata;
    private boolean burnAfterRead;
    private boolean csVideoSupported;
    private Set<String> extensions;
    private boolean fileTransfer;
    private boolean fileTransferHttpSupported;
    private boolean geolocPush;
    private boolean imSession;
    private boolean imageSharing;
    private boolean integratedMessagingMode;
    private boolean ipVideoCall;
    private boolean ipVoiceCall;
    private boolean rcsContact;
    private boolean videoSharing;

    public void setImageSharingSupport(boolean imageSharing) {
        Logger.i(TAG, "setImageSharingSupport entry" + imageSharing);
        this.imageSharing = imageSharing;
    }

    public void setVideoSharingSupport(boolean videoSharing) {
        Logger.i(TAG, "setVideoSharingSupport entry" + videoSharing);
        this.videoSharing = videoSharing;
    }

    public void setIntegratedMessagingMode(boolean integratedMessagingMode) {
        Logger.i(TAG, "setIntegratedMessagingMode entry" + integratedMessagingMode);
        this.integratedMessagingMode = integratedMessagingMode;
    }

    public boolean isIntegratedMessagingMode() {
        Logger.i(TAG, "isIntegratedMessagingMode entry" + this.integratedMessagingMode);
        return this.integratedMessagingMode;
    }

    public boolean isCsVideoSupported() {
        return this.csVideoSupported;
    }

    public Capabilities(boolean imageSharing, boolean videoSharing, boolean imSession, boolean fileTransfer, boolean geolocPush, boolean ipVoiceCall, boolean ipVideoCall, Set<String> extensions, boolean automata, boolean fileTransferHttpSupport, boolean rcsContact, boolean integratedMessagingMode, boolean csVideoSupported, boolean isBurnAfterRead) {
        this.imageSharing = false;
        this.videoSharing = false;
        this.imSession = false;
        this.fileTransfer = false;
        this.geolocPush = false;
        this.ipVoiceCall = false;
        this.ipVideoCall = false;
        this.extensions = new HashSet();
        this.automata = false;
        this.fileTransferHttpSupported = false;
        this.rcsContact = false;
        this.burnAfterRead = false;
        this.integratedMessagingMode = false;
        this.IR94_VoiceCall = false;
        this.IR94_VideoCall = false;
        this.IR94_DuplexMode = false;
        this.csVideoSupported = false;
        Logger.i(TAG, "Capabilities entry , values are Imagesharing-" + imageSharing + "videosharing-" + videoSharing + "imSession-" + imSession + "filetransfer-" + fileTransfer + "geolocPush-" + geolocPush + "ipVoicecall-" + ipVoiceCall + "ipVideoCall-" + ipVideoCall + "extensions-" + extensions + "automata-" + automata + "fileTransferHttpSupport-rcsContact-" + rcsContact + "integratedMessagingMode-" + integratedMessagingMode + "csVideoSupported-" + csVideoSupported);
        this.imageSharing = imageSharing;
        this.videoSharing = videoSharing;
        this.imSession = imSession;
        this.fileTransfer = fileTransfer;
        this.geolocPush = geolocPush;
        this.ipVoiceCall = ipVoiceCall;
        this.ipVideoCall = ipVideoCall;
        this.extensions = extensions;
        this.automata = automata;
        this.fileTransferHttpSupported = fileTransferHttpSupport;
        this.rcsContact = rcsContact;
        this.integratedMessagingMode = integratedMessagingMode;
        this.csVideoSupported = csVideoSupported;
        this.burnAfterRead = isBurnAfterRead;
    }

    public Capabilities(boolean imageSharing, boolean videoSharing, boolean imSession, boolean fileTransfer, boolean geolocPush, boolean ipVoiceCall, boolean ipVideoCall, Set<String> extensions, boolean automata, boolean fileTransferHttpSupport, boolean rcsContact, boolean integratedMessagingMode, boolean csVideoSupported) {
        this.imageSharing = false;
        this.videoSharing = false;
        this.imSession = false;
        this.fileTransfer = false;
        this.geolocPush = false;
        this.ipVoiceCall = false;
        this.ipVideoCall = false;
        this.extensions = new HashSet();
        this.automata = false;
        this.fileTransferHttpSupported = false;
        this.rcsContact = false;
        this.burnAfterRead = false;
        this.integratedMessagingMode = false;
        this.IR94_VoiceCall = false;
        this.IR94_VideoCall = false;
        this.IR94_DuplexMode = false;
        this.csVideoSupported = false;
        Logger.i(TAG, "Capabilities entry , values are Imagesharing-" + imageSharing + "videosharing-" + videoSharing + "imSession-" + imSession + "filetransfer-" + fileTransfer + "geolocPush-" + geolocPush + "ipVoicecall-" + ipVoiceCall + "ipVideoCall-" + ipVideoCall + "extensions-" + extensions + "automata-" + automata + "fileTransferHttpSupport-rcsContact-" + rcsContact + "integratedMessagingMode-" + integratedMessagingMode + "csVideoSupported-" + csVideoSupported);
        this.imageSharing = imageSharing;
        this.videoSharing = videoSharing;
        this.imSession = imSession;
        this.fileTransfer = fileTransfer;
        this.geolocPush = geolocPush;
        this.ipVoiceCall = ipVoiceCall;
        this.ipVideoCall = ipVideoCall;
        this.extensions = extensions;
        this.automata = automata;
        this.fileTransferHttpSupported = fileTransferHttpSupport;
        this.rcsContact = rcsContact;
        this.integratedMessagingMode = integratedMessagingMode;
        this.csVideoSupported = csVideoSupported;
    }

    public Capabilities(Parcel source) {
        this.imageSharing = false;
        this.videoSharing = false;
        this.imSession = false;
        this.fileTransfer = false;
        this.geolocPush = false;
        this.ipVoiceCall = false;
        this.ipVideoCall = false;
        this.extensions = new HashSet();
        this.automata = false;
        this.fileTransferHttpSupported = false;
        this.rcsContact = false;
        this.burnAfterRead = false;
        this.integratedMessagingMode = false;
        this.IR94_VoiceCall = false;
        this.IR94_VideoCall = false;
        this.IR94_DuplexMode = false;
        this.csVideoSupported = false;
        this.imageSharing = source.readInt() != 0;
        this.videoSharing = source.readInt() != 0;
        this.imSession = source.readInt() != 0;
        this.fileTransfer = source.readInt() != 0;
        List<String> exts = new ArrayList<>();
        source.readStringList(exts);
        this.extensions = new HashSet(exts);
        this.geolocPush = source.readInt() != 0;
        this.ipVoiceCall = source.readInt() != 0;
        this.ipVideoCall = source.readInt() != 0;
        this.automata = source.readInt() != 0;
        this.fileTransferHttpSupported = source.readInt() != 0;
        this.rcsContact = source.readInt() != 0;
        this.integratedMessagingMode = source.readInt() != 0;
        this.csVideoSupported = source.readInt() != 0;
        this.burnAfterRead = source.readInt() != 0;
        this.IR94_VoiceCall = source.readInt() != 0;
        this.IR94_VideoCall = source.readInt() != 0;
        this.IR94_DuplexMode = source.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.imageSharing ? 1 : 0);
        dest.writeInt(this.videoSharing ? 1 : 0);
        dest.writeInt(this.imSession ? 1 : 0);
        dest.writeInt(this.fileTransfer ? 1 : 0);
        if (this.extensions != null) {
            List<String> exts = new ArrayList<>();
            exts.addAll(this.extensions);
            dest.writeStringList(exts);
        }
        dest.writeInt(this.geolocPush ? 1 : 0);
        dest.writeInt(this.ipVoiceCall ? 1 : 0);
        dest.writeInt(this.ipVideoCall ? 1 : 0);
        dest.writeInt(this.automata ? 1 : 0);
        dest.writeInt(this.fileTransferHttpSupported ? 1 : 0);
        dest.writeInt(this.rcsContact ? 1 : 0);
        dest.writeInt(this.integratedMessagingMode ? 1 : 0);
        dest.writeInt(this.csVideoSupported ? 1 : 0);
        dest.writeInt(this.burnAfterRead ? 1 : 0);
        dest.writeInt(this.IR94_VoiceCall ? 1 : 0);
        dest.writeInt(this.IR94_VideoCall ? 1 : 0);
        dest.writeInt(this.IR94_DuplexMode ? 1 : 0);
    }

    public boolean isImageSharingSupported() {
        Logger.i(TAG, "isImageSharingSupported value " + this.imageSharing);
        return this.imageSharing;
    }

    public boolean isVideoSharingSupported() {
        Logger.i(TAG, "isVideoSharingSupported value " + this.videoSharing);
        return this.videoSharing;
    }

    public boolean isImSessionSupported() {
        Logger.i(TAG, "isImSessionSupported value " + this.imSession);
        return this.imSession;
    }

    public boolean isFileTransferSupported() {
        Logger.i(TAG, "isFileTransferSupported value " + this.fileTransfer);
        return this.fileTransfer;
    }

    public boolean isGeolocPushSupported() {
        Logger.i(TAG, "isGeolocPushSupported value " + this.geolocPush);
        return this.geolocPush;
    }

    public boolean isIPVoiceCallSupported() {
        Logger.i(TAG, "isIPVoiceCallSupported value " + this.ipVoiceCall);
        return this.ipVoiceCall;
    }

    public boolean isIPVideoCallSupported() {
        Logger.i(TAG, "isIPVideoCallSupported value " + this.ipVideoCall);
        return this.ipVideoCall;
    }

    public boolean isExtensionSupported(String tag) {
        Logger.i(TAG, "isExtensionSupported value " + this.extensions.contains(tag));
        return this.extensions.contains(tag);
    }

    public Set<String> getSupportedExtensions() {
        Logger.i(TAG, "getSupportedExtensions value " + this.extensions);
        return this.extensions;
    }

    public boolean isAutomata() {
        Logger.i(TAG, "isAutomata value" + this.automata);
        return this.automata;
    }

    public boolean isFileTransferHttpSupported() {
        Logger.i(TAG, "isFileTransferHttpSupported value" + this.fileTransferHttpSupported);
        return this.fileTransferHttpSupported;
    }

    public boolean isSupportedRcseContact() {
        Logger.i(TAG, "isSupportedRcseContact value" + this.rcsContact);
        return this.rcsContact;
    }

    public boolean isBurnAfterRead() {
        return this.burnAfterRead;
    }

    public void setIR94_VoiceCall(boolean supported) {
        this.IR94_VoiceCall = supported;
    }

    public boolean isIR94_VoiceCallSupported() {
        return this.IR94_VoiceCall;
    }

    public void setIR94_VideoCall(boolean supported) {
        this.IR94_VideoCall = supported;
    }

    public boolean isIR94_VideoCallSupported() {
        return this.IR94_VideoCall;
    }

    public void setIR94_DuplexMode(boolean supported) {
        this.IR94_DuplexMode = supported;
    }

    public boolean isIR94_DuplexModeSupported() {
        return this.IR94_DuplexMode;
    }
}
