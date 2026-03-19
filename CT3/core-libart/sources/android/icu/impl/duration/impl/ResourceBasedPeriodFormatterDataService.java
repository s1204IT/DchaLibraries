package android.icu.impl.duration.impl;

import android.icu.impl.ICUData;
import android.icu.impl.locale.BaseLocale;
import android.icu.util.ICUUncheckedIOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

public class ResourceBasedPeriodFormatterDataService extends PeriodFormatterDataService {
    private static final String PATH = "data/";
    private static final ResourceBasedPeriodFormatterDataService singleton = new ResourceBasedPeriodFormatterDataService();
    private Collection<String> availableLocales;
    private PeriodFormatterData lastData = null;
    private String lastLocale = null;
    private Map<String, PeriodFormatterData> cache = new HashMap();

    public static ResourceBasedPeriodFormatterDataService getInstance() {
        return singleton;
    }

    private ResourceBasedPeriodFormatterDataService() {
        BufferedReader br;
        List<String> localeNames = new ArrayList<>();
        InputStream is = ICUData.getRequiredStream(getClass(), "data/index.txt");
        try {
            try {
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (IOException e) {
                throw new IllegalStateException("IO Error reading data/index.txt: " + e.toString());
            }
        } finally {
            try {
                is.close();
            } catch (IOException e2) {
            }
        }
        while (true) {
            String string = br.readLine();
            if (string == null) {
                break;
            }
            String string2 = string.trim();
            if (!string2.startsWith("#") && string2.length() != 0) {
                localeNames.add(string2);
            }
            is.close();
        }
        br.close();
        this.availableLocales = Collections.unmodifiableList(localeNames);
    }

    @Override
    public PeriodFormatterData get(String localeName) {
        int x = localeName.indexOf(64);
        if (x != -1) {
            localeName = localeName.substring(0, x);
        }
        synchronized (this) {
            if (this.lastLocale != null && this.lastLocale.equals(localeName)) {
                return this.lastData;
            }
            PeriodFormatterData ld = this.cache.get(localeName);
            if (ld == null) {
                String ln = localeName;
                while (true) {
                    if (!this.availableLocales.contains(ln)) {
                        int ix = ln.lastIndexOf(BaseLocale.SEP);
                        if (ix > -1) {
                            ln = ln.substring(0, ix);
                        } else if (!"test".equals(ln)) {
                            ln = "test";
                        } else {
                            ln = null;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (ln != null) {
                    String name = "data/pfd_" + ln + ".xml";
                    try {
                        InputStreamReader reader = new InputStreamReader(ICUData.getRequiredStream(getClass(), name), "UTF-8");
                        DataRecord dr = DataRecord.read(ln, new XMLRecordReader(reader));
                        reader.close();
                        if (dr != null) {
                            ld = new PeriodFormatterData(localeName, dr);
                        }
                        this.cache.put(localeName, ld);
                    } catch (UnsupportedEncodingException e) {
                        throw new MissingResourceException("Unhandled encoding for resource " + name, name, "");
                    } catch (IOException e2) {
                        throw new ICUUncheckedIOException("Failed to close() resource " + name, e2);
                    }
                } else {
                    throw new MissingResourceException("Duration data not found for  " + localeName, PATH, localeName);
                }
            }
            this.lastData = ld;
            this.lastLocale = localeName;
            return ld;
        }
    }

    @Override
    public Collection<String> getAvailableLocales() {
        return this.availableLocales;
    }
}
