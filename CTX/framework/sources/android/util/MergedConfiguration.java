package android.util;

import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.PrintWriter;

public class MergedConfiguration implements Parcelable {
    public static final Parcelable.Creator<MergedConfiguration> CREATOR = new Parcelable.Creator<MergedConfiguration>() {
        @Override
        public MergedConfiguration createFromParcel(Parcel parcel) {
            return new MergedConfiguration(parcel);
        }

        @Override
        public MergedConfiguration[] newArray(int i) {
            return new MergedConfiguration[i];
        }
    };
    private Configuration mGlobalConfig;
    private Configuration mMergedConfig;
    private Configuration mOverrideConfig;

    public MergedConfiguration() {
        this.mGlobalConfig = new Configuration();
        this.mOverrideConfig = new Configuration();
        this.mMergedConfig = new Configuration();
    }

    public MergedConfiguration(Configuration configuration, Configuration configuration2) {
        this.mGlobalConfig = new Configuration();
        this.mOverrideConfig = new Configuration();
        this.mMergedConfig = new Configuration();
        setConfiguration(configuration, configuration2);
    }

    public MergedConfiguration(Configuration configuration) {
        this.mGlobalConfig = new Configuration();
        this.mOverrideConfig = new Configuration();
        this.mMergedConfig = new Configuration();
        setGlobalConfiguration(configuration);
    }

    public MergedConfiguration(MergedConfiguration mergedConfiguration) {
        this.mGlobalConfig = new Configuration();
        this.mOverrideConfig = new Configuration();
        this.mMergedConfig = new Configuration();
        setConfiguration(mergedConfiguration.getGlobalConfiguration(), mergedConfiguration.getOverrideConfiguration());
    }

    private MergedConfiguration(Parcel parcel) {
        this.mGlobalConfig = new Configuration();
        this.mOverrideConfig = new Configuration();
        this.mMergedConfig = new Configuration();
        readFromParcel(parcel);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mGlobalConfig, i);
        parcel.writeParcelable(this.mOverrideConfig, i);
        parcel.writeParcelable(this.mMergedConfig, i);
    }

    public void readFromParcel(Parcel parcel) {
        this.mGlobalConfig = (Configuration) parcel.readParcelable(Configuration.class.getClassLoader());
        this.mOverrideConfig = (Configuration) parcel.readParcelable(Configuration.class.getClassLoader());
        this.mMergedConfig = (Configuration) parcel.readParcelable(Configuration.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void setConfiguration(Configuration configuration, Configuration configuration2) {
        this.mGlobalConfig.setTo(configuration);
        this.mOverrideConfig.setTo(configuration2);
        updateMergedConfig();
    }

    public void setGlobalConfiguration(Configuration configuration) {
        this.mGlobalConfig.setTo(configuration);
        updateMergedConfig();
    }

    public void setOverrideConfiguration(Configuration configuration) {
        this.mOverrideConfig.setTo(configuration);
        updateMergedConfig();
    }

    public void setTo(MergedConfiguration mergedConfiguration) {
        setConfiguration(mergedConfiguration.mGlobalConfig, mergedConfiguration.mOverrideConfig);
    }

    public void unset() {
        this.mGlobalConfig.unset();
        this.mOverrideConfig.unset();
        updateMergedConfig();
    }

    public Configuration getGlobalConfiguration() {
        return this.mGlobalConfig;
    }

    public Configuration getOverrideConfiguration() {
        return this.mOverrideConfig;
    }

    public Configuration getMergedConfiguration() {
        return this.mMergedConfig;
    }

    private void updateMergedConfig() {
        this.mMergedConfig.setTo(this.mGlobalConfig);
        this.mMergedConfig.updateFrom(this.mOverrideConfig);
    }

    public String toString() {
        return "{mGlobalConfig=" + this.mGlobalConfig + " mOverrideConfig=" + this.mOverrideConfig + "}";
    }

    public int hashCode() {
        return this.mMergedConfig.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MergedConfiguration)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.mMergedConfig.equals(((MergedConfiguration) obj).mMergedConfig);
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "mGlobalConfig=" + this.mGlobalConfig);
        printWriter.println(str + "mOverrideConfig=" + this.mOverrideConfig);
    }
}
