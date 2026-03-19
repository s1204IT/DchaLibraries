package com.android.managedprovisioning.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import com.android.internal.annotations.Immutable;
import com.android.managedprovisioning.common.PersistableBundlable;
import java.io.File;

@Immutable
public class DisclaimersParam extends PersistableBundlable {
    public static final Parcelable.Creator<DisclaimersParam> CREATOR = new Parcelable.Creator<DisclaimersParam>() {
        @Override
        public DisclaimersParam createFromParcel(Parcel parcel) {
            return new DisclaimersParam(parcel);
        }

        @Override
        public DisclaimersParam[] newArray(int i) {
            return new DisclaimersParam[i];
        }
    };
    public final Disclaimer[] mDisclaimers;

    private DisclaimersParam(Builder builder) {
        this.mDisclaimers = builder.mDisclaimers;
    }

    private DisclaimersParam(Parcel parcel) {
        this(createBuilderFromPersistableBundle(PersistableBundlable.getPersistableBundleFromParcel(parcel)));
    }

    public static DisclaimersParam fromPersistableBundle(PersistableBundle persistableBundle) {
        return createBuilderFromPersistableBundle(persistableBundle).build();
    }

    private static Builder createBuilderFromPersistableBundle(PersistableBundle persistableBundle) {
        String[] stringArray = persistableBundle.getStringArray("HEADER_KEY");
        String[] stringArray2 = persistableBundle.getStringArray("CONTENT_PATH_KEY");
        Builder builder = new Builder();
        if (stringArray != null) {
            Disclaimer[] disclaimerArr = new Disclaimer[stringArray.length];
            for (int i = 0; i < stringArray.length; i++) {
                disclaimerArr[i] = new Disclaimer(stringArray[i], stringArray2[i]);
            }
            builder.setDisclaimers(disclaimerArr);
        }
        return builder;
    }

    public void cleanUp() {
        if (this.mDisclaimers != null) {
            for (Disclaimer disclaimer : this.mDisclaimers) {
                new File(disclaimer.mContentFilePath).delete();
            }
        }
    }

    @Override
    public PersistableBundle toPersistableBundle() {
        PersistableBundle persistableBundle = new PersistableBundle();
        if (this.mDisclaimers != null) {
            String[] strArr = new String[this.mDisclaimers.length];
            String[] strArr2 = new String[this.mDisclaimers.length];
            for (int i = 0; i < this.mDisclaimers.length; i++) {
                strArr[i] = this.mDisclaimers[i].mHeader;
                strArr2[i] = this.mDisclaimers[i].mContentFilePath;
            }
            persistableBundle.putStringArray("HEADER_KEY", strArr);
            persistableBundle.putStringArray("CONTENT_PATH_KEY", strArr2);
        }
        return persistableBundle;
    }

    @Immutable
    public static class Disclaimer {
        public final String mContentFilePath;
        public final String mHeader;

        public Disclaimer(String str, String str2) {
            this.mHeader = str;
            this.mContentFilePath = str2;
        }
    }

    public static final class Builder {
        private Disclaimer[] mDisclaimers;

        public Builder setDisclaimers(Disclaimer[] disclaimerArr) {
            this.mDisclaimers = disclaimerArr;
            return this;
        }

        public DisclaimersParam build() {
            return new DisclaimersParam(this);
        }
    }
}
