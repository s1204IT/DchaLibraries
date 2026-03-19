package android.view;

import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Objects;

public class DisplayAdjustments {
    public static final DisplayAdjustments DEFAULT_DISPLAY_ADJUSTMENTS = new DisplayAdjustments();
    private volatile CompatibilityInfo mCompatInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
    private Configuration mConfiguration;

    public DisplayAdjustments() {
    }

    public DisplayAdjustments(Configuration configuration) {
        this.mConfiguration = new Configuration(configuration == null ? Configuration.EMPTY : configuration);
    }

    public DisplayAdjustments(DisplayAdjustments displayAdjustments) {
        setCompatibilityInfo(displayAdjustments.mCompatInfo);
        this.mConfiguration = new Configuration(displayAdjustments.mConfiguration != null ? displayAdjustments.mConfiguration : Configuration.EMPTY);
    }

    public void setCompatibilityInfo(CompatibilityInfo compatibilityInfo) {
        if (this == DEFAULT_DISPLAY_ADJUSTMENTS) {
            throw new IllegalArgumentException("setCompatbilityInfo: Cannot modify DEFAULT_DISPLAY_ADJUSTMENTS");
        }
        if (compatibilityInfo != null && (compatibilityInfo.isScalingRequired() || !compatibilityInfo.supportsScreen())) {
            this.mCompatInfo = compatibilityInfo;
        } else {
            this.mCompatInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
        }
    }

    public CompatibilityInfo getCompatibilityInfo() {
        return this.mCompatInfo;
    }

    public void setConfiguration(Configuration configuration) {
        if (this == DEFAULT_DISPLAY_ADJUSTMENTS) {
            throw new IllegalArgumentException("setConfiguration: Cannot modify DEFAULT_DISPLAY_ADJUSTMENTS");
        }
        Configuration configuration2 = this.mConfiguration;
        if (configuration == null) {
            configuration = Configuration.EMPTY;
        }
        configuration2.setTo(configuration);
    }

    public Configuration getConfiguration() {
        return this.mConfiguration;
    }

    public int hashCode() {
        return ((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + Objects.hashCode(this.mCompatInfo)) * 31) + Objects.hashCode(this.mConfiguration);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DisplayAdjustments)) {
            return false;
        }
        DisplayAdjustments displayAdjustments = (DisplayAdjustments) obj;
        return Objects.equals(displayAdjustments.mCompatInfo, this.mCompatInfo) && Objects.equals(displayAdjustments.mConfiguration, this.mConfiguration);
    }
}
