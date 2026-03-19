package android.app;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class RemoteInput implements Parcelable {
    public static final Parcelable.Creator<RemoteInput> CREATOR = new Parcelable.Creator<RemoteInput>() {
        @Override
        public RemoteInput createFromParcel(Parcel parcel) {
            return new RemoteInput(parcel);
        }

        @Override
        public RemoteInput[] newArray(int i) {
            return new RemoteInput[i];
        }
    };
    private static final int DEFAULT_FLAGS = 1;
    private static final String EXTRA_DATA_TYPE_RESULTS_DATA = "android.remoteinput.dataTypeResultsData";
    public static final String EXTRA_RESULTS_DATA = "android.remoteinput.resultsData";
    private static final String EXTRA_RESULTS_SOURCE = "android.remoteinput.resultsSource";
    private static final int FLAG_ALLOW_FREE_FORM_INPUT = 1;
    public static final String RESULTS_CLIP_LABEL = "android.remoteinput.results";
    public static final int SOURCE_CHOICE = 1;
    public static final int SOURCE_FREE_FORM_INPUT = 0;
    private final ArraySet<String> mAllowedDataTypes;
    private final CharSequence[] mChoices;
    private final Bundle mExtras;
    private final int mFlags;
    private final CharSequence mLabel;
    private final String mResultKey;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Source {
    }

    private RemoteInput(String str, CharSequence charSequence, CharSequence[] charSequenceArr, int i, Bundle bundle, ArraySet<String> arraySet) {
        this.mResultKey = str;
        this.mLabel = charSequence;
        this.mChoices = charSequenceArr;
        this.mFlags = i;
        this.mExtras = bundle;
        this.mAllowedDataTypes = arraySet;
    }

    public String getResultKey() {
        return this.mResultKey;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public CharSequence[] getChoices() {
        return this.mChoices;
    }

    public Set<String> getAllowedDataTypes() {
        return this.mAllowedDataTypes;
    }

    public boolean isDataOnly() {
        return !getAllowFreeFormInput() && (getChoices() == null || getChoices().length == 0) && !getAllowedDataTypes().isEmpty();
    }

    public boolean getAllowFreeFormInput() {
        return (this.mFlags & 1) != 0;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public static final class Builder {
        private CharSequence[] mChoices;
        private CharSequence mLabel;
        private final String mResultKey;
        private final ArraySet<String> mAllowedDataTypes = new ArraySet<>();
        private final Bundle mExtras = new Bundle();
        private int mFlags = 1;

        public Builder(String str) {
            if (str == null) {
                throw new IllegalArgumentException("Result key can't be null");
            }
            this.mResultKey = str;
        }

        public Builder setLabel(CharSequence charSequence) {
            this.mLabel = Notification.safeCharSequence(charSequence);
            return this;
        }

        public Builder setChoices(CharSequence[] charSequenceArr) {
            if (charSequenceArr == null) {
                this.mChoices = null;
            } else {
                this.mChoices = new CharSequence[charSequenceArr.length];
                for (int i = 0; i < charSequenceArr.length; i++) {
                    this.mChoices[i] = Notification.safeCharSequence(charSequenceArr[i]);
                }
            }
            return this;
        }

        public Builder setAllowDataType(String str, boolean z) {
            if (z) {
                this.mAllowedDataTypes.add(str);
            } else {
                this.mAllowedDataTypes.remove(str);
            }
            return this;
        }

        public Builder setAllowFreeFormInput(boolean z) {
            setFlag(this.mFlags, z);
            return this;
        }

        public Builder addExtras(Bundle bundle) {
            if (bundle != null) {
                this.mExtras.putAll(bundle);
            }
            return this;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        private void setFlag(int i, boolean z) {
            if (z) {
                this.mFlags = i | this.mFlags;
            } else {
                this.mFlags = (~i) & this.mFlags;
            }
        }

        public RemoteInput build() {
            return new RemoteInput(this.mResultKey, this.mLabel, this.mChoices, this.mFlags, this.mExtras, this.mAllowedDataTypes);
        }
    }

    private RemoteInput(Parcel parcel) {
        this.mResultKey = parcel.readString();
        this.mLabel = parcel.readCharSequence();
        this.mChoices = parcel.readCharSequenceArray();
        this.mFlags = parcel.readInt();
        this.mExtras = parcel.readBundle();
        this.mAllowedDataTypes = parcel.readArraySet(null);
    }

    public static Map<String, Uri> getDataResultsFromIntent(Intent intent, String str) {
        String strSubstring;
        String string;
        Intent clipDataIntentFromIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntentFromIntent == null) {
            return null;
        }
        HashMap map = new HashMap();
        for (String str2 : clipDataIntentFromIntent.getExtras().keySet()) {
            if (str2.startsWith(EXTRA_DATA_TYPE_RESULTS_DATA) && (strSubstring = str2.substring(EXTRA_DATA_TYPE_RESULTS_DATA.length())) != null && !strSubstring.isEmpty() && (string = clipDataIntentFromIntent.getBundleExtra(str2).getString(str)) != null && !string.isEmpty()) {
                map.put(strSubstring, Uri.parse(string));
            }
        }
        if (map.isEmpty()) {
            return null;
        }
        return map;
    }

    public static Bundle getResultsFromIntent(Intent intent) {
        Intent clipDataIntentFromIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntentFromIntent == null) {
            return null;
        }
        return (Bundle) clipDataIntentFromIntent.getExtras().getParcelable(EXTRA_RESULTS_DATA);
    }

    public static void addResultsToIntent(RemoteInput[] remoteInputArr, Intent intent, Bundle bundle) {
        Intent clipDataIntentFromIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntentFromIntent == null) {
            clipDataIntentFromIntent = new Intent();
        }
        Bundle bundleExtra = clipDataIntentFromIntent.getBundleExtra(EXTRA_RESULTS_DATA);
        if (bundleExtra == null) {
            bundleExtra = new Bundle();
        }
        for (RemoteInput remoteInput : remoteInputArr) {
            Object obj = bundle.get(remoteInput.getResultKey());
            if (obj instanceof CharSequence) {
                bundleExtra.putCharSequence(remoteInput.getResultKey(), (CharSequence) obj);
            }
        }
        clipDataIntentFromIntent.putExtra(EXTRA_RESULTS_DATA, bundleExtra);
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntentFromIntent));
    }

    public static void addDataResultToIntent(RemoteInput remoteInput, Intent intent, Map<String, Uri> map) {
        Intent clipDataIntentFromIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntentFromIntent == null) {
            clipDataIntentFromIntent = new Intent();
        }
        for (Map.Entry<String, Uri> entry : map.entrySet()) {
            String key = entry.getKey();
            Uri value = entry.getValue();
            if (key != null) {
                Bundle bundleExtra = clipDataIntentFromIntent.getBundleExtra(getExtraResultsKeyForData(key));
                if (bundleExtra == null) {
                    bundleExtra = new Bundle();
                }
                bundleExtra.putString(remoteInput.getResultKey(), value.toString());
                clipDataIntentFromIntent.putExtra(getExtraResultsKeyForData(key), bundleExtra);
            }
        }
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntentFromIntent));
    }

    public static void setResultsSource(Intent intent, int i) {
        Intent clipDataIntentFromIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntentFromIntent == null) {
            clipDataIntentFromIntent = new Intent();
        }
        clipDataIntentFromIntent.putExtra(EXTRA_RESULTS_SOURCE, i);
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntentFromIntent));
    }

    public static int getResultsSource(Intent intent) {
        Intent clipDataIntentFromIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntentFromIntent == null) {
            return 0;
        }
        return clipDataIntentFromIntent.getExtras().getInt(EXTRA_RESULTS_SOURCE, 0);
    }

    private static String getExtraResultsKeyForData(String str) {
        return EXTRA_DATA_TYPE_RESULTS_DATA + str;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mResultKey);
        parcel.writeCharSequence(this.mLabel);
        parcel.writeCharSequenceArray(this.mChoices);
        parcel.writeInt(this.mFlags);
        parcel.writeBundle(this.mExtras);
        parcel.writeArraySet(this.mAllowedDataTypes);
    }

    private static Intent getClipDataIntentFromIntent(Intent intent) {
        ClipData clipData = intent.getClipData();
        if (clipData == null) {
            return null;
        }
        ClipDescription description = clipData.getDescription();
        if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT) || !description.getLabel().equals(RESULTS_CLIP_LABEL)) {
            return null;
        }
        return clipData.getItemAt(0).getIntent();
    }
}
