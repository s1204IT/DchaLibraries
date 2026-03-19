package java.util.logging;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class Level implements Serializable {
    private static final long serialVersionUID = -8176160795706313070L;
    private transient Locale cachedLocale;
    private transient String localizedLevelName;
    private final String name;
    private final String resourceBundleName;
    private final int value;
    private static final String defaultBundle = "sun.util.logging.resources.logging";
    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE, defaultBundle);
    public static final Level SEVERE = new Level("SEVERE", 1000, defaultBundle);
    public static final Level WARNING = new Level("WARNING", 900, defaultBundle);
    public static final Level INFO = new Level("INFO", 800, defaultBundle);
    public static final Level CONFIG = new Level("CONFIG", 700, defaultBundle);
    public static final Level FINE = new Level("FINE", 500, defaultBundle);
    public static final Level FINER = new Level("FINER", HttpURLConnection.HTTP_BAD_REQUEST, defaultBundle);
    public static final Level FINEST = new Level("FINEST", HttpURLConnection.HTTP_MULT_CHOICE, defaultBundle);
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE, defaultBundle);

    protected Level(String str, int i) {
        this(str, i, null);
    }

    protected Level(String str, int i, String str2) {
        this(str, i, str2, true);
    }

    private Level(String str, int i, String str2, boolean z) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.name = str;
        this.value = i;
        this.resourceBundleName = str2;
        this.localizedLevelName = str2 != null ? null : str;
        this.cachedLocale = null;
        if (z) {
            KnownLevel.add(this);
        }
    }

    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    public String getName() {
        return this.name;
    }

    public String getLocalizedName() {
        return getLocalizedLevelName();
    }

    final String getLevelName() {
        return this.name;
    }

    private String computeLocalizedLevelName(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(this.resourceBundleName, locale, Thread.currentThread().getContextClassLoader());
        String string = bundle.getString(this.name);
        if (!defaultBundle.equals(this.resourceBundleName)) {
            return string;
        }
        Locale locale2 = bundle.getLocale();
        if (Locale.ROOT.equals(locale2) || this.name.equals(string.toUpperCase(Locale.ROOT))) {
            locale2 = Locale.ROOT;
        }
        return Locale.ROOT.equals(locale2) ? this.name : string.toUpperCase(locale2);
    }

    final String getCachedLocalizedLevelName() {
        if (this.localizedLevelName != null && this.cachedLocale != null && this.cachedLocale.equals(Locale.getDefault())) {
            return this.localizedLevelName;
        }
        if (this.resourceBundleName == null) {
            return this.name;
        }
        return null;
    }

    final synchronized String getLocalizedLevelName() {
        String cachedLocalizedLevelName = getCachedLocalizedLevelName();
        if (cachedLocalizedLevelName != null) {
            return cachedLocalizedLevelName;
        }
        Locale locale = Locale.getDefault();
        try {
            this.localizedLevelName = computeLocalizedLevelName(locale);
        } catch (Exception e) {
            this.localizedLevelName = this.name;
        }
        this.cachedLocale = locale;
        return this.localizedLevelName;
    }

    static Level findLevel(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        KnownLevel knownLevelFindByName = KnownLevel.findByName(str);
        if (knownLevelFindByName != null) {
            return knownLevelFindByName.mirroredLevel;
        }
        try {
            int i = Integer.parseInt(str);
            KnownLevel knownLevelFindByValue = KnownLevel.findByValue(i);
            if (knownLevelFindByValue == null) {
                new Level(str, i);
                knownLevelFindByValue = KnownLevel.findByValue(i);
            }
            return knownLevelFindByValue.mirroredLevel;
        } catch (NumberFormatException e) {
            KnownLevel knownLevelFindByLocalizedLevelName = KnownLevel.findByLocalizedLevelName(str);
            if (knownLevelFindByLocalizedLevelName != null) {
                return knownLevelFindByLocalizedLevelName.mirroredLevel;
            }
            return null;
        }
    }

    public final String toString() {
        return this.name;
    }

    public final int intValue() {
        return this.value;
    }

    private Object readResolve() {
        KnownLevel knownLevelMatches = KnownLevel.matches(this);
        if (knownLevelMatches != null) {
            return knownLevelMatches.levelObject;
        }
        return new Level(this.name, this.value, this.resourceBundleName);
    }

    public static synchronized Level parse(String str) throws IllegalArgumentException {
        str.length();
        KnownLevel knownLevelFindByName = KnownLevel.findByName(str);
        if (knownLevelFindByName != null) {
            return knownLevelFindByName.levelObject;
        }
        try {
            int i = Integer.parseInt(str);
            KnownLevel knownLevelFindByValue = KnownLevel.findByValue(i);
            if (knownLevelFindByValue == null) {
                new Level(str, i);
                knownLevelFindByValue = KnownLevel.findByValue(i);
            }
            return knownLevelFindByValue.levelObject;
        } catch (NumberFormatException e) {
            KnownLevel knownLevelFindByLocalizedLevelName = KnownLevel.findByLocalizedLevelName(str);
            if (knownLevelFindByLocalizedLevelName != null) {
                return knownLevelFindByLocalizedLevelName.levelObject;
            }
            throw new IllegalArgumentException("Bad level \"" + str + "\"");
        }
    }

    public boolean equals(Object obj) {
        try {
            return ((Level) obj).value == this.value;
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode() {
        return this.value;
    }

    static final class KnownLevel {
        final Level levelObject;
        final Level mirroredLevel;
        private static Map<String, List<KnownLevel>> nameToLevels = new HashMap();
        private static Map<Integer, List<KnownLevel>> intToLevels = new HashMap();

        KnownLevel(Level level) {
            this.levelObject = level;
            if (level.getClass() == Level.class) {
                this.mirroredLevel = level;
            } else {
                this.mirroredLevel = new Level(level.name, level.value, level.resourceBundleName, false);
            }
        }

        static synchronized void add(Level level) {
            KnownLevel knownLevel = new KnownLevel(level);
            List<KnownLevel> arrayList = nameToLevels.get(level.name);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                nameToLevels.put(level.name, arrayList);
            }
            arrayList.add(knownLevel);
            List<KnownLevel> arrayList2 = intToLevels.get(Integer.valueOf(level.value));
            if (arrayList2 == null) {
                arrayList2 = new ArrayList<>();
                intToLevels.put(Integer.valueOf(level.value), arrayList2);
            }
            arrayList2.add(knownLevel);
        }

        static synchronized KnownLevel findByName(String str) {
            List<KnownLevel> list = nameToLevels.get(str);
            if (list != null) {
                return list.get(0);
            }
            return null;
        }

        static synchronized KnownLevel findByValue(int i) {
            List<KnownLevel> list = intToLevels.get(Integer.valueOf(i));
            if (list != null) {
                return list.get(0);
            }
            return null;
        }

        static synchronized KnownLevel findByLocalizedLevelName(String str) {
            Iterator<List<KnownLevel>> it = nameToLevels.values().iterator();
            while (it.hasNext()) {
                for (KnownLevel knownLevel : it.next()) {
                    if (str.equals(knownLevel.levelObject.getLocalizedLevelName())) {
                        return knownLevel;
                    }
                }
            }
            return null;
        }

        static synchronized KnownLevel matches(Level level) {
            List<KnownLevel> list = nameToLevels.get(level.name);
            if (list != null) {
                for (KnownLevel knownLevel : list) {
                    Level level2 = knownLevel.mirroredLevel;
                    Class<?> cls = knownLevel.levelObject.getClass();
                    if (level.value == level2.value && (level.resourceBundleName == level2.resourceBundleName || (level.resourceBundleName != null && level.resourceBundleName.equals(level2.resourceBundleName)))) {
                        if (cls == level.getClass()) {
                            return knownLevel;
                        }
                    }
                }
            }
            return null;
        }
    }
}
