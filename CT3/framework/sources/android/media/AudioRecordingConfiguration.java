package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Objects;

public final class AudioRecordingConfiguration implements Parcelable {
    private final AudioFormat mClientFormat;
    private final int mClientSource;
    private final AudioFormat mDeviceFormat;
    private final int mPatchHandle;
    private final int mSessionId;
    private static final String TAG = new String("AudioRecordingConfiguration");
    public static final Parcelable.Creator<AudioRecordingConfiguration> CREATOR = new Parcelable.Creator<AudioRecordingConfiguration>() {
        @Override
        public AudioRecordingConfiguration createFromParcel(Parcel p) {
            return new AudioRecordingConfiguration(p, null);
        }

        @Override
        public AudioRecordingConfiguration[] newArray(int size) {
            return new AudioRecordingConfiguration[size];
        }
    };

    AudioRecordingConfiguration(Parcel in, AudioRecordingConfiguration audioRecordingConfiguration) {
        this(in);
    }

    public AudioRecordingConfiguration(int session, int source, AudioFormat clientFormat, AudioFormat devFormat, int patchHandle) {
        this.mSessionId = session;
        this.mClientSource = source;
        this.mClientFormat = clientFormat;
        this.mDeviceFormat = devFormat;
        this.mPatchHandle = patchHandle;
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

    public AudioDeviceInfo getAudioDevice() {
        ArrayList<AudioPatch> patches = new ArrayList<>();
        if (AudioManager.listAudioPatches(patches) != 0) {
            Log.e(TAG, "Error retrieving list of audio patches");
            return null;
        }
        int i = 0;
        while (true) {
            if (i >= patches.size()) {
                break;
            }
            AudioPatch patch = patches.get(i);
            if (patch.id() != this.mPatchHandle) {
                i++;
            } else {
                AudioPortConfig[] sources = patch.sources();
                if (sources != null && sources.length > 0) {
                    int devId = sources[0].port().id();
                    AudioDeviceInfo[] devices = AudioManager.getDevicesStatic(1);
                    for (int j = 0; j < devices.length; j++) {
                        if (devices[j].getId() == devId) {
                            return devices[j];
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSessionId);
        dest.writeInt(this.mClientSource);
        this.mClientFormat.writeToParcel(dest, 0);
        this.mDeviceFormat.writeToParcel(dest, 0);
        dest.writeInt(this.mPatchHandle);
    }

    private AudioRecordingConfiguration(Parcel in) {
        this.mSessionId = in.readInt();
        this.mClientSource = in.readInt();
        this.mClientFormat = AudioFormat.CREATOR.createFromParcel(in);
        this.mDeviceFormat = AudioFormat.CREATOR.createFromParcel(in);
        this.mPatchHandle = in.readInt();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof AudioRecordingConfiguration)) {
            return false;
        }
        AudioRecordingConfiguration that = (AudioRecordingConfiguration) o;
        if (this.mSessionId == that.mSessionId && this.mClientSource == that.mClientSource && this.mPatchHandle == that.mPatchHandle && this.mClientFormat.equals(that.mClientFormat)) {
            return this.mDeviceFormat.equals(that.mDeviceFormat);
        }
        return false;
    }
}
