package com.android.providers.contacts;

import android.content.ContentValues;
import android.text.TextUtils;
import java.util.Locale;

public class PostalSplitter {
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();
    private final Locale mLocale;

    public static class Postal {
        public String city;
        public String country;
        public String neighborhood;
        public String pobox;
        public String postcode;
        public String region;
        public String street;

        public void fromValues(ContentValues contentValues) {
            this.street = contentValues.getAsString("data4");
            this.pobox = contentValues.getAsString("data5");
            this.neighborhood = contentValues.getAsString("data6");
            this.city = contentValues.getAsString("data7");
            this.region = contentValues.getAsString("data8");
            this.postcode = contentValues.getAsString("data9");
            this.country = contentValues.getAsString("data10");
        }

        public void toValues(ContentValues contentValues) {
            contentValues.put("data4", this.street);
            contentValues.put("data5", this.pobox);
            contentValues.put("data6", this.neighborhood);
            contentValues.put("data7", this.city);
            contentValues.put("data8", this.region);
            contentValues.put("data9", this.postcode);
            contentValues.put("data10", this.country);
        }
    }

    public PostalSplitter(Locale locale) {
        this.mLocale = locale;
    }

    public void split(Postal postal, String str) {
        if (!TextUtils.isEmpty(str)) {
            postal.street = str;
        }
    }

    public String join(Postal postal) {
        String[] strArr = {postal.street, postal.pobox, postal.neighborhood, postal.city, postal.region, postal.postcode, postal.country};
        if (this.mLocale != null && JAPANESE_LANGUAGE.equals(this.mLocale.getLanguage()) && !arePrintableAsciiOnly(strArr)) {
            return joinJaJp(postal);
        }
        return joinEnUs(postal);
    }

    private String joinJaJp(Postal postal) {
        boolean z = true;
        boolean z2 = !TextUtils.isEmpty(postal.street);
        boolean z3 = !TextUtils.isEmpty(postal.pobox);
        boolean z4 = !TextUtils.isEmpty(postal.neighborhood);
        boolean z5 = !TextUtils.isEmpty(postal.city);
        boolean z6 = !TextUtils.isEmpty(postal.region);
        boolean z7 = !TextUtils.isEmpty(postal.postcode);
        boolean z8 = !TextUtils.isEmpty(postal.country);
        StringBuilder sb = new StringBuilder();
        boolean z9 = z8 || z7;
        boolean z10 = z6 || z5 || z4;
        if (!z2 && !z3) {
            z = false;
        }
        if (z9) {
            if (z8) {
                sb.append(postal.country);
            }
            if (z7) {
                if (z8) {
                    sb.append(" ");
                }
                sb.append(postal.postcode);
            }
        }
        if (z10) {
            if (z9) {
                sb.append("\n");
            }
            if (z6) {
                sb.append(postal.region);
            }
            if (z5) {
                if (z6) {
                    sb.append(" ");
                }
                sb.append(postal.city);
            }
            if (z4) {
                if (z6 || z5) {
                    sb.append(" ");
                }
                sb.append(postal.neighborhood);
            }
        }
        if (z) {
            if (z9 || z10) {
                sb.append("\n");
            }
            if (z2) {
                sb.append(postal.street);
            }
            if (z3) {
                if (z2) {
                    sb.append(" ");
                }
                sb.append(postal.pobox);
            }
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

    private String joinEnUs(Postal postal) {
        boolean z = true;
        boolean z2 = !TextUtils.isEmpty(postal.street);
        boolean z3 = !TextUtils.isEmpty(postal.pobox);
        boolean z4 = !TextUtils.isEmpty(postal.neighborhood);
        boolean z5 = !TextUtils.isEmpty(postal.city);
        boolean z6 = !TextUtils.isEmpty(postal.region);
        boolean z7 = !TextUtils.isEmpty(postal.postcode);
        boolean z8 = !TextUtils.isEmpty(postal.country);
        StringBuilder sb = new StringBuilder();
        boolean z9 = z2 || z3 || z4;
        if (!z5 && !z6 && !z7) {
            z = false;
        }
        if (z9) {
            if (z2) {
                sb.append(postal.street);
            }
            if (z3) {
                if (z2) {
                    sb.append("\n");
                }
                sb.append(postal.pobox);
            }
            if (z4) {
                if (z2 || z3) {
                    sb.append("\n");
                }
                sb.append(postal.neighborhood);
            }
        }
        if (z) {
            if (z9) {
                sb.append("\n");
            }
            if (z5) {
                sb.append(postal.city);
            }
            if (z6) {
                if (z5) {
                    sb.append(", ");
                }
                sb.append(postal.region);
            }
            if (z7) {
                if (z5 || z6) {
                    sb.append(" ");
                }
                sb.append(postal.postcode);
            }
        }
        if (z8) {
            if (z9 || z) {
                sb.append("\n");
            }
            if (z8) {
                sb.append(postal.country);
            }
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

    private static boolean arePrintableAsciiOnly(String[] strArr) {
        if (strArr == null) {
            return true;
        }
        for (String str : strArr) {
            if (!TextUtils.isEmpty(str) && !TextUtils.isPrintableAsciiOnly(str)) {
                return false;
            }
        }
        return true;
    }
}
