package android.content.pm;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.util.Printer;

public class ComponentInfo extends PackageItemInfo {
    public ApplicationInfo applicationInfo;
    public int descriptionRes;
    public boolean enabled;
    public boolean exported;
    public String processName;

    public ComponentInfo() {
        this.enabled = true;
        this.exported = false;
    }

    public ComponentInfo(ComponentInfo orig) {
        super(orig);
        this.enabled = true;
        this.exported = false;
        this.applicationInfo = orig.applicationInfo;
        this.processName = orig.processName;
        this.descriptionRes = orig.descriptionRes;
        this.enabled = orig.enabled;
        this.exported = orig.exported;
    }

    @Override
    public CharSequence loadLabel(PackageManager pm) {
        CharSequence label;
        CharSequence label2;
        if (this.nonLocalizedLabel != null) {
            return this.nonLocalizedLabel;
        }
        ApplicationInfo ai = this.applicationInfo;
        if (this.labelRes == 0 || (label2 = pm.getText(this.packageName, this.labelRes, ai)) == null) {
            if (ai.nonLocalizedLabel != null) {
                return ai.nonLocalizedLabel;
            }
            return (ai.labelRes == 0 || (label = pm.getText(this.packageName, ai.labelRes, ai)) == null) ? this.name : label;
        }
        return label2;
    }

    public boolean isEnabled() {
        return this.enabled && this.applicationInfo.enabled;
    }

    public final int getIconResource() {
        return this.icon != 0 ? this.icon : this.applicationInfo.icon;
    }

    public final int getLogoResource() {
        return this.logo != 0 ? this.logo : this.applicationInfo.logo;
    }

    public final int getBannerResource() {
        return this.banner != 0 ? this.banner : this.applicationInfo.banner;
    }

    @Override
    protected void dumpFront(Printer pw, String prefix) {
        super.dumpFront(pw, prefix);
        pw.println(prefix + "enabled=" + this.enabled + " exported=" + this.exported + " processName=" + this.processName);
        if (this.descriptionRes != 0) {
            pw.println(prefix + "description=" + this.descriptionRes);
        }
    }

    @Override
    protected void dumpBack(Printer pw, String prefix) {
        if (this.applicationInfo != null) {
            pw.println(prefix + "ApplicationInfo:");
            this.applicationInfo.dump(pw, prefix + "  ");
        } else {
            pw.println(prefix + "ApplicationInfo: null");
        }
        super.dumpBack(pw, prefix);
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        this.applicationInfo.writeToParcel(dest, parcelableFlags);
        dest.writeString(this.processName);
        dest.writeInt(this.descriptionRes);
        dest.writeInt(this.enabled ? 1 : 0);
        dest.writeInt(this.exported ? 1 : 0);
    }

    protected ComponentInfo(Parcel source) {
        super(source);
        this.enabled = true;
        this.exported = false;
        this.applicationInfo = ApplicationInfo.CREATOR.createFromParcel(source);
        this.processName = source.readString();
        this.descriptionRes = source.readInt();
        this.enabled = source.readInt() != 0;
        this.exported = source.readInt() != 0;
    }

    @Override
    public Drawable loadDefaultIcon(PackageManager pm) {
        return this.applicationInfo.loadIcon(pm);
    }

    @Override
    protected Drawable loadDefaultBanner(PackageManager pm) {
        return this.applicationInfo.loadBanner(pm);
    }

    @Override
    protected Drawable loadDefaultLogo(PackageManager pm) {
        return this.applicationInfo.loadLogo(pm);
    }

    @Override
    protected ApplicationInfo getApplicationInfo() {
        return this.applicationInfo;
    }
}
