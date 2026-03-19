package com.android.commands.monkey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class MonkeyUtils {
    private static final Date DATE = new Date();
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ");
    private static PackageFilter sFilter;

    private MonkeyUtils() {
    }

    public static synchronized String toCalendarTime(long j) {
        DATE.setTime(j);
        return DATE_FORMATTER.format(DATE);
    }

    public static PackageFilter getPackageFilter() {
        if (sFilter == null) {
            sFilter = new PackageFilter();
        }
        return sFilter;
    }

    public static class PackageFilter {
        private Set<String> mInvalidPackages;
        private Set<String> mValidPackages;

        private PackageFilter() {
            this.mValidPackages = new HashSet();
            this.mInvalidPackages = new HashSet();
        }

        public void addValidPackages(Set<String> set) {
            this.mValidPackages.addAll(set);
        }

        public void addInvalidPackages(Set<String> set) {
            this.mInvalidPackages.addAll(set);
        }

        public boolean hasValidPackages() {
            return this.mValidPackages.size() > 0;
        }

        public boolean isPackageValid(String str) {
            return this.mValidPackages.contains(str);
        }

        public boolean isPackageInvalid(String str) {
            return this.mInvalidPackages.contains(str);
        }

        public boolean checkEnteringPackage(String str) {
            return this.mInvalidPackages.size() > 0 ? !this.mInvalidPackages.contains(str) : this.mValidPackages.size() <= 0 || this.mValidPackages.contains(str);
        }

        public void dump() {
            if (this.mValidPackages.size() > 0) {
                Iterator<String> it = this.mValidPackages.iterator();
                while (it.hasNext()) {
                    Logger.out.println(":AllowPackage: " + it.next());
                }
            }
            if (this.mInvalidPackages.size() > 0) {
                Iterator<String> it2 = this.mInvalidPackages.iterator();
                while (it2.hasNext()) {
                    Logger.out.println(":DisallowPackage: " + it2.next());
                }
            }
        }
    }
}
