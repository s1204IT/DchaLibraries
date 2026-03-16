package android.hardware.soundtrigger;

import android.media.AudioFormat;
import android.net.ProxyInfo;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class SoundTrigger {
    public static final int RECOGNITION_MODE_USER_AUTHENTICATION = 4;
    public static final int RECOGNITION_MODE_USER_IDENTIFICATION = 2;
    public static final int RECOGNITION_MODE_VOICE_TRIGGER = 1;
    public static final int RECOGNITION_STATUS_ABORT = 1;
    public static final int RECOGNITION_STATUS_FAILURE = 2;
    public static final int RECOGNITION_STATUS_SUCCESS = 0;
    public static final int SERVICE_STATE_DISABLED = 1;
    public static final int SERVICE_STATE_ENABLED = 0;
    public static final int SOUNDMODEL_STATUS_UPDATED = 0;
    public static final int STATUS_BAD_VALUE = -22;
    public static final int STATUS_DEAD_OBJECT = -32;
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_INVALID_OPERATION = -38;
    public static final int STATUS_NO_INIT = -19;
    public static final int STATUS_OK = 0;
    public static final int STATUS_PERMISSION_DENIED = -1;

    public interface StatusListener {
        void onRecognition(RecognitionEvent recognitionEvent);

        void onServiceDied();

        void onServiceStateChange(int i);

        void onSoundModelUpdate(SoundModelEvent soundModelEvent);
    }

    public static native int listModules(ArrayList<ModuleProperties> arrayList);

    public static class ModuleProperties implements Parcelable {
        public static final Parcelable.Creator<ModuleProperties> CREATOR = new Parcelable.Creator<ModuleProperties>() {
            @Override
            public ModuleProperties createFromParcel(Parcel in) {
                return ModuleProperties.fromParcel(in);
            }

            @Override
            public ModuleProperties[] newArray(int size) {
                return new ModuleProperties[size];
            }
        };
        public final String description;
        public final int id;
        public final String implementor;
        public final int maxBufferMs;
        public final int maxKeyphrases;
        public final int maxSoundModels;
        public final int maxUsers;
        public final int powerConsumptionMw;
        public final int recognitionModes;
        public final boolean returnsTriggerInEvent;
        public final boolean supportsCaptureTransition;
        public final boolean supportsConcurrentCapture;
        public final UUID uuid;
        public final int version;

        ModuleProperties(int id, String implementor, String description, String uuid, int version, int maxSoundModels, int maxKeyphrases, int maxUsers, int recognitionModes, boolean supportsCaptureTransition, int maxBufferMs, boolean supportsConcurrentCapture, int powerConsumptionMw, boolean returnsTriggerInEvent) {
            this.id = id;
            this.implementor = implementor;
            this.description = description;
            this.uuid = UUID.fromString(uuid);
            this.version = version;
            this.maxSoundModels = maxSoundModels;
            this.maxKeyphrases = maxKeyphrases;
            this.maxUsers = maxUsers;
            this.recognitionModes = recognitionModes;
            this.supportsCaptureTransition = supportsCaptureTransition;
            this.maxBufferMs = maxBufferMs;
            this.supportsConcurrentCapture = supportsConcurrentCapture;
            this.powerConsumptionMw = powerConsumptionMw;
            this.returnsTriggerInEvent = returnsTriggerInEvent;
        }

        private static ModuleProperties fromParcel(Parcel in) {
            int id = in.readInt();
            String implementor = in.readString();
            String description = in.readString();
            String uuid = in.readString();
            int version = in.readInt();
            int maxSoundModels = in.readInt();
            int maxKeyphrases = in.readInt();
            int maxUsers = in.readInt();
            int recognitionModes = in.readInt();
            boolean supportsCaptureTransition = in.readByte() == 1;
            int maxBufferMs = in.readInt();
            boolean supportsConcurrentCapture = in.readByte() == 1;
            int powerConsumptionMw = in.readInt();
            boolean returnsTriggerInEvent = in.readByte() == 1;
            return new ModuleProperties(id, implementor, description, uuid, version, maxSoundModels, maxKeyphrases, maxUsers, recognitionModes, supportsCaptureTransition, maxBufferMs, supportsConcurrentCapture, powerConsumptionMw, returnsTriggerInEvent);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.id);
            dest.writeString(this.implementor);
            dest.writeString(this.description);
            dest.writeString(this.uuid.toString());
            dest.writeInt(this.version);
            dest.writeInt(this.maxSoundModels);
            dest.writeInt(this.maxKeyphrases);
            dest.writeInt(this.maxUsers);
            dest.writeInt(this.recognitionModes);
            dest.writeByte((byte) (this.supportsCaptureTransition ? 1 : 0));
            dest.writeInt(this.maxBufferMs);
            dest.writeByte((byte) (this.supportsConcurrentCapture ? 1 : 0));
            dest.writeInt(this.powerConsumptionMw);
            dest.writeByte((byte) (this.returnsTriggerInEvent ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "ModuleProperties [id=" + this.id + ", implementor=" + this.implementor + ", description=" + this.description + ", uuid=" + this.uuid + ", version=" + this.version + ", maxSoundModels=" + this.maxSoundModels + ", maxKeyphrases=" + this.maxKeyphrases + ", maxUsers=" + this.maxUsers + ", recognitionModes=" + this.recognitionModes + ", supportsCaptureTransition=" + this.supportsCaptureTransition + ", maxBufferMs=" + this.maxBufferMs + ", supportsConcurrentCapture=" + this.supportsConcurrentCapture + ", powerConsumptionMw=" + this.powerConsumptionMw + ", returnsTriggerInEvent=" + this.returnsTriggerInEvent + "]";
        }
    }

    public static class SoundModel {
        public static final int TYPE_KEYPHRASE = 0;
        public static final int TYPE_UNKNOWN = -1;
        public final byte[] data;
        public final int type;
        public final UUID uuid;
        public final UUID vendorUuid;

        public SoundModel(UUID uuid, UUID vendorUuid, int type, byte[] data) {
            this.uuid = uuid;
            this.vendorUuid = vendorUuid;
            this.type = type;
            this.data = data;
        }

        public int hashCode() {
            int result = Arrays.hashCode(this.data) + 31;
            return (((((result * 31) + this.type) * 31) + (this.uuid == null ? 0 : this.uuid.hashCode())) * 31) + (this.vendorUuid != null ? this.vendorUuid.hashCode() : 0);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && (obj instanceof SoundModel)) {
                SoundModel other = (SoundModel) obj;
                if (Arrays.equals(this.data, other.data) && this.type == other.type) {
                    if (this.uuid == null) {
                        if (other.uuid != null) {
                            return false;
                        }
                    } else if (!this.uuid.equals(other.uuid)) {
                        return false;
                    }
                    return this.vendorUuid == null ? other.vendorUuid == null : this.vendorUuid.equals(other.vendorUuid);
                }
                return false;
            }
            return false;
        }
    }

    public static class Keyphrase implements Parcelable {
        public static final Parcelable.Creator<Keyphrase> CREATOR = new Parcelable.Creator<Keyphrase>() {
            @Override
            public Keyphrase createFromParcel(Parcel in) {
                return Keyphrase.fromParcel(in);
            }

            @Override
            public Keyphrase[] newArray(int size) {
                return new Keyphrase[size];
            }
        };
        public final int id;
        public final String locale;
        public final int recognitionModes;
        public final String text;
        public final int[] users;

        public Keyphrase(int id, int recognitionModes, String locale, String text, int[] users) {
            this.id = id;
            this.recognitionModes = recognitionModes;
            this.locale = locale;
            this.text = text;
            this.users = users;
        }

        private static Keyphrase fromParcel(Parcel in) {
            int id = in.readInt();
            int recognitionModes = in.readInt();
            String locale = in.readString();
            String text = in.readString();
            int[] users = null;
            int numUsers = in.readInt();
            if (numUsers >= 0) {
                users = new int[numUsers];
                in.readIntArray(users);
            }
            return new Keyphrase(id, recognitionModes, locale, text, users);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.id);
            dest.writeInt(this.recognitionModes);
            dest.writeString(this.locale);
            dest.writeString(this.text);
            if (this.users != null) {
                dest.writeInt(this.users.length);
                dest.writeIntArray(this.users);
            } else {
                dest.writeInt(-1);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            int result = (this.text == null ? 0 : this.text.hashCode()) + 31;
            return (((((((result * 31) + this.id) * 31) + (this.locale != null ? this.locale.hashCode() : 0)) * 31) + this.recognitionModes) * 31) + Arrays.hashCode(this.users);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                Keyphrase other = (Keyphrase) obj;
                if (this.text == null) {
                    if (other.text != null) {
                        return false;
                    }
                } else if (!this.text.equals(other.text)) {
                    return false;
                }
                if (this.id != other.id) {
                    return false;
                }
                if (this.locale == null) {
                    if (other.locale != null) {
                        return false;
                    }
                } else if (!this.locale.equals(other.locale)) {
                    return false;
                }
                return this.recognitionModes == other.recognitionModes && Arrays.equals(this.users, other.users);
            }
            return false;
        }

        public String toString() {
            return "Keyphrase [id=" + this.id + ", recognitionModes=" + this.recognitionModes + ", locale=" + this.locale + ", text=" + this.text + ", users=" + Arrays.toString(this.users) + "]";
        }
    }

    public static class KeyphraseSoundModel extends SoundModel implements Parcelable {
        public static final Parcelable.Creator<KeyphraseSoundModel> CREATOR = new Parcelable.Creator<KeyphraseSoundModel>() {
            @Override
            public KeyphraseSoundModel createFromParcel(Parcel in) {
                return KeyphraseSoundModel.fromParcel(in);
            }

            @Override
            public KeyphraseSoundModel[] newArray(int size) {
                return new KeyphraseSoundModel[size];
            }
        };
        public final Keyphrase[] keyphrases;

        public KeyphraseSoundModel(UUID uuid, UUID vendorUuid, byte[] data, Keyphrase[] keyphrases) {
            super(uuid, vendorUuid, 0, data);
            this.keyphrases = keyphrases;
        }

        private static KeyphraseSoundModel fromParcel(Parcel in) {
            UUID uuid = UUID.fromString(in.readString());
            UUID vendorUuid = null;
            int length = in.readInt();
            if (length >= 0) {
                vendorUuid = UUID.fromString(in.readString());
            }
            byte[] data = in.readBlob();
            Keyphrase[] keyphrases = (Keyphrase[]) in.createTypedArray(Keyphrase.CREATOR);
            return new KeyphraseSoundModel(uuid, vendorUuid, data, keyphrases);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.uuid.toString());
            if (this.vendorUuid == null) {
                dest.writeInt(-1);
            } else {
                dest.writeInt(this.vendorUuid.toString().length());
                dest.writeString(this.vendorUuid.toString());
            }
            dest.writeBlob(this.data);
            dest.writeTypedArray(this.keyphrases, flags);
        }

        public String toString() {
            return "KeyphraseSoundModel [keyphrases=" + Arrays.toString(this.keyphrases) + ", uuid=" + this.uuid + ", vendorUuid=" + this.vendorUuid + ", type=" + this.type + ", data=" + (this.data == null ? 0 : this.data.length) + "]";
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return (result * 31) + Arrays.hashCode(this.keyphrases);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (super.equals(obj) && (obj instanceof KeyphraseSoundModel)) {
                KeyphraseSoundModel other = (KeyphraseSoundModel) obj;
                return Arrays.equals(this.keyphrases, other.keyphrases);
            }
            return false;
        }
    }

    public static class RecognitionEvent implements Parcelable {
        public static final Parcelable.Creator<RecognitionEvent> CREATOR = new Parcelable.Creator<RecognitionEvent>() {
            @Override
            public RecognitionEvent createFromParcel(Parcel in) {
                return RecognitionEvent.fromParcel(in);
            }

            @Override
            public RecognitionEvent[] newArray(int size) {
                return new RecognitionEvent[size];
            }
        };
        public final boolean captureAvailable;
        public final int captureDelayMs;
        public AudioFormat captureFormat;
        public final int capturePreambleMs;
        public final int captureSession;
        public final byte[] data;
        public final int soundModelHandle;
        public final int status;
        public final boolean triggerInData;

        public RecognitionEvent(int status, int soundModelHandle, boolean captureAvailable, int captureSession, int captureDelayMs, int capturePreambleMs, boolean triggerInData, AudioFormat captureFormat, byte[] data) {
            this.status = status;
            this.soundModelHandle = soundModelHandle;
            this.captureAvailable = captureAvailable;
            this.captureSession = captureSession;
            this.captureDelayMs = captureDelayMs;
            this.capturePreambleMs = capturePreambleMs;
            this.triggerInData = triggerInData;
            this.captureFormat = captureFormat;
            this.data = data;
        }

        private static RecognitionEvent fromParcel(Parcel in) {
            int status = in.readInt();
            int soundModelHandle = in.readInt();
            boolean captureAvailable = in.readByte() == 1;
            int captureSession = in.readInt();
            int captureDelayMs = in.readInt();
            int capturePreambleMs = in.readInt();
            boolean triggerInData = in.readByte() == 1;
            AudioFormat captureFormat = null;
            if (in.readByte() == 1) {
                int sampleRate = in.readInt();
                int encoding = in.readInt();
                int channelMask = in.readInt();
                captureFormat = new AudioFormat.Builder().setChannelMask(channelMask).setEncoding(encoding).setSampleRate(sampleRate).build();
            }
            byte[] data = in.readBlob();
            return new RecognitionEvent(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, data);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.status);
            dest.writeInt(this.soundModelHandle);
            dest.writeByte((byte) (this.captureAvailable ? 1 : 0));
            dest.writeInt(this.captureSession);
            dest.writeInt(this.captureDelayMs);
            dest.writeInt(this.capturePreambleMs);
            dest.writeByte((byte) (this.triggerInData ? 1 : 0));
            if (this.captureFormat != null) {
                dest.writeByte((byte) 1);
                dest.writeInt(this.captureFormat.getSampleRate());
                dest.writeInt(this.captureFormat.getEncoding());
                dest.writeInt(this.captureFormat.getChannelMask());
            } else {
                dest.writeByte((byte) 0);
            }
            dest.writeBlob(this.data);
        }

        public int hashCode() {
            int result = (((((((((this.captureAvailable ? 1231 : 1237) + 31) * 31) + this.captureDelayMs) * 31) + this.capturePreambleMs) * 31) + this.captureSession) * 31) + (this.triggerInData ? 1231 : 1237);
            if (this.captureFormat != null) {
                result = (((((result * 31) + this.captureFormat.getSampleRate()) * 31) + this.captureFormat.getEncoding()) * 31) + this.captureFormat.getChannelMask();
            }
            return (((((result * 31) + Arrays.hashCode(this.data)) * 31) + this.soundModelHandle) * 31) + this.status;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                RecognitionEvent other = (RecognitionEvent) obj;
                return this.captureAvailable == other.captureAvailable && this.captureDelayMs == other.captureDelayMs && this.capturePreambleMs == other.capturePreambleMs && this.captureSession == other.captureSession && Arrays.equals(this.data, other.data) && this.soundModelHandle == other.soundModelHandle && this.status == other.status && this.triggerInData == other.triggerInData && this.captureFormat.getSampleRate() == other.captureFormat.getSampleRate() && this.captureFormat.getEncoding() == other.captureFormat.getEncoding() && this.captureFormat.getChannelMask() == other.captureFormat.getChannelMask();
            }
            return false;
        }

        public String toString() {
            return "RecognitionEvent [status=" + this.status + ", soundModelHandle=" + this.soundModelHandle + ", captureAvailable=" + this.captureAvailable + ", captureSession=" + this.captureSession + ", captureDelayMs=" + this.captureDelayMs + ", capturePreambleMs=" + this.capturePreambleMs + ", triggerInData=" + this.triggerInData + (this.captureFormat == null ? ProxyInfo.LOCAL_EXCL_LIST : ", sampleRate=" + this.captureFormat.getSampleRate()) + (this.captureFormat == null ? ProxyInfo.LOCAL_EXCL_LIST : ", encoding=" + this.captureFormat.getEncoding()) + (this.captureFormat == null ? ProxyInfo.LOCAL_EXCL_LIST : ", channelMask=" + this.captureFormat.getChannelMask()) + ", data=" + (this.data == null ? 0 : this.data.length) + "]";
        }
    }

    public static class RecognitionConfig implements Parcelable {
        public static final Parcelable.Creator<RecognitionConfig> CREATOR = new Parcelable.Creator<RecognitionConfig>() {
            @Override
            public RecognitionConfig createFromParcel(Parcel in) {
                return RecognitionConfig.fromParcel(in);
            }

            @Override
            public RecognitionConfig[] newArray(int size) {
                return new RecognitionConfig[size];
            }
        };
        public final boolean allowMultipleTriggers;
        public final boolean captureRequested;
        public final byte[] data;
        public final KeyphraseRecognitionExtra[] keyphrases;

        public RecognitionConfig(boolean captureRequested, boolean allowMultipleTriggers, KeyphraseRecognitionExtra[] keyphrases, byte[] data) {
            this.captureRequested = captureRequested;
            this.allowMultipleTriggers = allowMultipleTriggers;
            this.keyphrases = keyphrases;
            this.data = data;
        }

        private static RecognitionConfig fromParcel(Parcel in) {
            boolean captureRequested = in.readByte() == 1;
            boolean allowMultipleTriggers = in.readByte() == 1;
            KeyphraseRecognitionExtra[] keyphrases = (KeyphraseRecognitionExtra[]) in.createTypedArray(KeyphraseRecognitionExtra.CREATOR);
            byte[] data = in.readBlob();
            return new RecognitionConfig(captureRequested, allowMultipleTriggers, keyphrases, data);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (this.captureRequested ? 1 : 0));
            dest.writeByte((byte) (this.allowMultipleTriggers ? 1 : 0));
            dest.writeTypedArray(this.keyphrases, flags);
            dest.writeBlob(this.data);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "RecognitionConfig [captureRequested=" + this.captureRequested + ", allowMultipleTriggers=" + this.allowMultipleTriggers + ", keyphrases=" + Arrays.toString(this.keyphrases) + ", data=" + Arrays.toString(this.data) + "]";
        }
    }

    public static class ConfidenceLevel implements Parcelable {
        public static final Parcelable.Creator<ConfidenceLevel> CREATOR = new Parcelable.Creator<ConfidenceLevel>() {
            @Override
            public ConfidenceLevel createFromParcel(Parcel in) {
                return ConfidenceLevel.fromParcel(in);
            }

            @Override
            public ConfidenceLevel[] newArray(int size) {
                return new ConfidenceLevel[size];
            }
        };
        public final int confidenceLevel;
        public final int userId;

        public ConfidenceLevel(int userId, int confidenceLevel) {
            this.userId = userId;
            this.confidenceLevel = confidenceLevel;
        }

        private static ConfidenceLevel fromParcel(Parcel in) {
            int userId = in.readInt();
            int confidenceLevel = in.readInt();
            return new ConfidenceLevel(userId, confidenceLevel);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.userId);
            dest.writeInt(this.confidenceLevel);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            int result = this.confidenceLevel + 31;
            return (result * 31) + this.userId;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                ConfidenceLevel other = (ConfidenceLevel) obj;
                return this.confidenceLevel == other.confidenceLevel && this.userId == other.userId;
            }
            return false;
        }

        public String toString() {
            return "ConfidenceLevel [userId=" + this.userId + ", confidenceLevel=" + this.confidenceLevel + "]";
        }
    }

    public static class KeyphraseRecognitionExtra implements Parcelable {
        public static final Parcelable.Creator<KeyphraseRecognitionExtra> CREATOR = new Parcelable.Creator<KeyphraseRecognitionExtra>() {
            @Override
            public KeyphraseRecognitionExtra createFromParcel(Parcel in) {
                return KeyphraseRecognitionExtra.fromParcel(in);
            }

            @Override
            public KeyphraseRecognitionExtra[] newArray(int size) {
                return new KeyphraseRecognitionExtra[size];
            }
        };
        public final int coarseConfidenceLevel;
        public final ConfidenceLevel[] confidenceLevels;
        public final int id;
        public final int recognitionModes;

        public KeyphraseRecognitionExtra(int id, int recognitionModes, int coarseConfidenceLevel, ConfidenceLevel[] confidenceLevels) {
            this.id = id;
            this.recognitionModes = recognitionModes;
            this.coarseConfidenceLevel = coarseConfidenceLevel;
            this.confidenceLevels = confidenceLevels;
        }

        private static KeyphraseRecognitionExtra fromParcel(Parcel in) {
            int id = in.readInt();
            int recognitionModes = in.readInt();
            int coarseConfidenceLevel = in.readInt();
            ConfidenceLevel[] confidenceLevels = (ConfidenceLevel[]) in.createTypedArray(ConfidenceLevel.CREATOR);
            return new KeyphraseRecognitionExtra(id, recognitionModes, coarseConfidenceLevel, confidenceLevels);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.id);
            dest.writeInt(this.recognitionModes);
            dest.writeInt(this.coarseConfidenceLevel);
            dest.writeTypedArray(this.confidenceLevels, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            int result = Arrays.hashCode(this.confidenceLevels) + 31;
            return (((((result * 31) + this.id) * 31) + this.recognitionModes) * 31) + this.coarseConfidenceLevel;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                KeyphraseRecognitionExtra other = (KeyphraseRecognitionExtra) obj;
                return Arrays.equals(this.confidenceLevels, other.confidenceLevels) && this.id == other.id && this.recognitionModes == other.recognitionModes && this.coarseConfidenceLevel == other.coarseConfidenceLevel;
            }
            return false;
        }

        public String toString() {
            return "KeyphraseRecognitionExtra [id=" + this.id + ", recognitionModes=" + this.recognitionModes + ", coarseConfidenceLevel=" + this.coarseConfidenceLevel + ", confidenceLevels=" + Arrays.toString(this.confidenceLevels) + "]";
        }
    }

    public static class KeyphraseRecognitionEvent extends RecognitionEvent {
        public static final Parcelable.Creator<KeyphraseRecognitionEvent> CREATOR = new Parcelable.Creator<KeyphraseRecognitionEvent>() {
            @Override
            public KeyphraseRecognitionEvent createFromParcel(Parcel in) {
                return KeyphraseRecognitionEvent.fromParcel(in);
            }

            @Override
            public KeyphraseRecognitionEvent[] newArray(int size) {
                return new KeyphraseRecognitionEvent[size];
            }
        };
        public final KeyphraseRecognitionExtra[] keyphraseExtras;

        public KeyphraseRecognitionEvent(int status, int soundModelHandle, boolean captureAvailable, int captureSession, int captureDelayMs, int capturePreambleMs, boolean triggerInData, AudioFormat captureFormat, byte[] data, KeyphraseRecognitionExtra[] keyphraseExtras) {
            super(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, data);
            this.keyphraseExtras = keyphraseExtras;
        }

        private static KeyphraseRecognitionEvent fromParcel(Parcel in) {
            int status = in.readInt();
            int soundModelHandle = in.readInt();
            boolean captureAvailable = in.readByte() == 1;
            int captureSession = in.readInt();
            int captureDelayMs = in.readInt();
            int capturePreambleMs = in.readInt();
            boolean triggerInData = in.readByte() == 1;
            AudioFormat captureFormat = null;
            if (in.readByte() == 1) {
                int sampleRate = in.readInt();
                int encoding = in.readInt();
                int channelMask = in.readInt();
                captureFormat = new AudioFormat.Builder().setChannelMask(channelMask).setEncoding(encoding).setSampleRate(sampleRate).build();
            }
            byte[] data = in.readBlob();
            KeyphraseRecognitionExtra[] keyphraseExtras = (KeyphraseRecognitionExtra[]) in.createTypedArray(KeyphraseRecognitionExtra.CREATOR);
            return new KeyphraseRecognitionEvent(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, data, keyphraseExtras);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.status);
            dest.writeInt(this.soundModelHandle);
            dest.writeByte((byte) (this.captureAvailable ? 1 : 0));
            dest.writeInt(this.captureSession);
            dest.writeInt(this.captureDelayMs);
            dest.writeInt(this.capturePreambleMs);
            dest.writeByte((byte) (this.triggerInData ? 1 : 0));
            if (this.captureFormat != null) {
                dest.writeByte((byte) 1);
                dest.writeInt(this.captureFormat.getSampleRate());
                dest.writeInt(this.captureFormat.getEncoding());
                dest.writeInt(this.captureFormat.getChannelMask());
            } else {
                dest.writeByte((byte) 0);
            }
            dest.writeBlob(this.data);
            dest.writeTypedArray(this.keyphraseExtras, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return (result * 31) + Arrays.hashCode(this.keyphraseExtras);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (super.equals(obj) && getClass() == obj.getClass()) {
                KeyphraseRecognitionEvent other = (KeyphraseRecognitionEvent) obj;
                return Arrays.equals(this.keyphraseExtras, other.keyphraseExtras);
            }
            return false;
        }

        @Override
        public String toString() {
            return "KeyphraseRecognitionEvent [keyphraseExtras=" + Arrays.toString(this.keyphraseExtras) + ", status=" + this.status + ", soundModelHandle=" + this.soundModelHandle + ", captureAvailable=" + this.captureAvailable + ", captureSession=" + this.captureSession + ", captureDelayMs=" + this.captureDelayMs + ", capturePreambleMs=" + this.capturePreambleMs + ", triggerInData=" + this.triggerInData + (this.captureFormat == null ? ProxyInfo.LOCAL_EXCL_LIST : ", sampleRate=" + this.captureFormat.getSampleRate()) + (this.captureFormat == null ? ProxyInfo.LOCAL_EXCL_LIST : ", encoding=" + this.captureFormat.getEncoding()) + (this.captureFormat == null ? ProxyInfo.LOCAL_EXCL_LIST : ", channelMask=" + this.captureFormat.getChannelMask()) + ", data=" + (this.data == null ? 0 : this.data.length) + "]";
        }
    }

    public static class SoundModelEvent implements Parcelable {
        public static final Parcelable.Creator<SoundModelEvent> CREATOR = new Parcelable.Creator<SoundModelEvent>() {
            @Override
            public SoundModelEvent createFromParcel(Parcel in) {
                return SoundModelEvent.fromParcel(in);
            }

            @Override
            public SoundModelEvent[] newArray(int size) {
                return new SoundModelEvent[size];
            }
        };
        public final byte[] data;
        public final int soundModelHandle;
        public final int status;

        SoundModelEvent(int status, int soundModelHandle, byte[] data) {
            this.status = status;
            this.soundModelHandle = soundModelHandle;
            this.data = data;
        }

        private static SoundModelEvent fromParcel(Parcel in) {
            int status = in.readInt();
            int soundModelHandle = in.readInt();
            byte[] data = in.readBlob();
            return new SoundModelEvent(status, soundModelHandle, data);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.status);
            dest.writeInt(this.soundModelHandle);
            dest.writeBlob(this.data);
        }

        public int hashCode() {
            int result = Arrays.hashCode(this.data) + 31;
            return (((result * 31) + this.soundModelHandle) * 31) + this.status;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                SoundModelEvent other = (SoundModelEvent) obj;
                return Arrays.equals(this.data, other.data) && this.soundModelHandle == other.soundModelHandle && this.status == other.status;
            }
            return false;
        }

        public String toString() {
            return "SoundModelEvent [status=" + this.status + ", soundModelHandle=" + this.soundModelHandle + ", data=" + (this.data == null ? 0 : this.data.length) + "]";
        }
    }

    public static SoundTriggerModule attachModule(int moduleId, StatusListener listener, Handler handler) {
        if (listener == null) {
            return null;
        }
        return new SoundTriggerModule(moduleId, listener, handler);
    }
}
