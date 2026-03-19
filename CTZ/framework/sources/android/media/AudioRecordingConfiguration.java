package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Objects;

public final class AudioRecordingConfiguration implements Parcelable {
    private final AudioFormat mClientFormat;
    private final String mClientPackageName;
    private final int mClientSource;
    private final int mClientUid;
    private final AudioFormat mDeviceFormat;
    private final int mPatchHandle;
    private final int mSessionId;
    private static final String TAG = new String("AudioRecordingConfiguration");
    public static final Parcelable.Creator<AudioRecordingConfiguration> CREATOR = new Parcelable.Creator<AudioRecordingConfiguration>() {
        @Override
        public AudioRecordingConfiguration createFromParcel(Parcel parcel) {
            return new AudioRecordingConfiguration(parcel);
        }

        @Override
        public AudioRecordingConfiguration[] newArray(int i) {
            return new AudioRecordingConfiguration[i];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioSource {
    }

    public AudioRecordingConfiguration(int i, int i2, int i3, AudioFormat audioFormat, AudioFormat audioFormat2, int i4, String str) {
        this.mClientUid = i;
        this.mSessionId = i2;
        this.mClientSource = i3;
        this.mClientFormat = audioFormat;
        this.mDeviceFormat = audioFormat2;
        this.mPatchHandle = i4;
        this.mClientPackageName = str;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("  " + toLogFriendlyString(this));
    }

    public static String toLogFriendlyString(AudioRecordingConfiguration audioRecordingConfiguration) {
        return new String("session:" + audioRecordingConfiguration.mSessionId + " -- source:" + MediaRecorder.toLogFriendlyAudioSource(audioRecordingConfiguration.mClientSource) + " -- uid:" + audioRecordingConfiguration.mClientUid + " -- patch:" + audioRecordingConfiguration.mPatchHandle + " -- pack:" + audioRecordingConfiguration.mClientPackageName + " -- format client=" + audioRecordingConfiguration.mClientFormat.toLogFriendlyString() + ", dev=" + audioRecordingConfiguration.mDeviceFormat.toLogFriendlyString());
    }

    public static AudioRecordingConfiguration anonymizedCopy(AudioRecordingConfiguration audioRecordingConfiguration) {
        return new AudioRecordingConfiguration(-1, audioRecordingConfiguration.mSessionId, audioRecordingConfiguration.mClientSource, audioRecordingConfiguration.mClientFormat, audioRecordingConfiguration.mDeviceFormat, audioRecordingConfiguration.mPatchHandle, "");
    }

    public int getClientAudioSource() {
        return this.mClientSource;
    }

    public int getClientAudioSessionId() {
        return this.mSessionId;
    }

    public AudioFormat getFormat() {
        return this.mDeviceFormat;
    }

    public AudioFormat getClientFormat() {
        return this.mClientFormat;
    }

    public String getClientPackageName() {
        return this.mClientPackageName;
    }

    public int getClientUid() {
        return this.mClientUid;
    }

    public AudioDeviceInfo getAudioDevice() {
        ArrayList arrayList = new ArrayList();
        if (AudioManager.listAudioPatches(arrayList) != 0) {
            Log.e(TAG, "Error retrieving list of audio patches");
            return null;
        }
        int i = 0;
        while (true) {
            if (i >= arrayList.size()) {
                break;
            }
            AudioPatch audioPatch = (AudioPatch) arrayList.get(i);
            if (audioPatch.id() != this.mPatchHandle) {
                i++;
            } else {
                AudioPortConfig[] audioPortConfigArrSources = audioPatch.sources();
                if (audioPortConfigArrSources != null && audioPortConfigArrSources.length > 0) {
                    int iId = audioPortConfigArrSources[0].port().id();
                    AudioDeviceInfo[] devicesStatic = AudioManager.getDevicesStatic(1);
                    for (int i2 = 0; i2 < devicesStatic.length; i2++) {
                        if (devicesStatic[i2].getId() == iId) {
                            return devicesStatic[i2];
                        }
                    }
                }
            }
        }
        Log.e(TAG, "Couldn't find device for recording, did recording end already?");
        return null;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSessionId), Integer.valueOf(this.mClientSource));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSessionId);
        parcel.writeInt(this.mClientSource);
        this.mClientFormat.writeToParcel(parcel, 0);
        this.mDeviceFormat.writeToParcel(parcel, 0);
        parcel.writeInt(this.mPatchHandle);
        parcel.writeString(this.mClientPackageName);
        parcel.writeInt(this.mClientUid);
    }

    private AudioRecordingConfiguration(Parcel parcel) {
        this.mSessionId = parcel.readInt();
        this.mClientSource = parcel.readInt();
        this.mClientFormat = AudioFormat.CREATOR.createFromParcel(parcel);
        this.mDeviceFormat = AudioFormat.CREATOR.createFromParcel(parcel);
        this.mPatchHandle = parcel.readInt();
        this.mClientPackageName = parcel.readString();
        this.mClientUid = parcel.readInt();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AudioRecordingConfiguration)) {
            return false;
        }
        AudioRecordingConfiguration audioRecordingConfiguration = (AudioRecordingConfiguration) obj;
        if (this.mClientUid == audioRecordingConfiguration.mClientUid && this.mSessionId == audioRecordingConfiguration.mSessionId && this.mClientSource == audioRecordingConfiguration.mClientSource && this.mPatchHandle == audioRecordingConfiguration.mPatchHandle && this.mClientFormat.equals(audioRecordingConfiguration.mClientFormat) && this.mDeviceFormat.equals(audioRecordingConfiguration.mDeviceFormat) && this.mClientPackageName.equals(audioRecordingConfiguration.mClientPackageName)) {
            return true;
        }
        return false;
    }
}
