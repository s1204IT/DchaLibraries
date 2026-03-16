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

        public void fromValues(ContentValues values) {
            this.street = values.getAsString("data4");
            this.pobox = values.getAsString("data5");
            this.neighborhood = values.getAsString("data6");
            this.city = values.getAsString("data7");
            this.region = values.getAsString("data8");
            this.postcode = values.getAsString("data9");
            this.country = values.getAsString("data10");
        }

        public void toValues(ContentValues values) {
            values.put("data4", this.street);
            values.put("data5", this.pobox);
            values.put("data6", this.neighborhood);
            values.put("data7", this.city);
            values.put("data8", this.region);
            values.put("data9", this.postcode);
            values.put("data10", this.country);
        }
    }

    public PostalSplitter(Locale locale) {
        this.mLocale = locale;
    }

    public void split(Postal postal, String formattedAddress) {
        if (!TextUtils.isEmpty(formattedAddress)) {
            postal.street = formattedAddress;
        }
    }

    public String join(Postal postal) {
        String[] values = {postal.street, postal.pobox, postal.neighborhood, postal.city, postal.region, postal.postcode, postal.country};
        return (this.mLocale == null || !JAPANESE_LANGUAGE.equals(this.mLocale.getLanguage()) || arePrintableAsciiOnly(values)) ? joinEnUs(postal) : joinJaJp(postal);
    }

    private String joinJaJp(Postal postal) {
        boolean hasStreet = !TextUtils.isEmpty(postal.street);
        boolean hasPobox = !TextUtils.isEmpty(postal.pobox);
        boolean hasNeighborhood = !TextUtils.isEmpty(postal.neighborhood);
        boolean hasCity = !TextUtils.isEmpty(postal.city);
        boolean hasRegion = !TextUtils.isEmpty(postal.region);
        boolean hasPostcode = !TextUtils.isEmpty(postal.postcode);
        boolean hasCountry = !TextUtils.isEmpty(postal.country);
        StringBuilder builder = new StringBuilder();
        boolean hasFirstBlock = hasCountry || hasPostcode;
        boolean hasSecondBlock = hasRegion || hasCity || hasNeighborhood;
        boolean hasThirdBlock = hasStreet || hasPobox;
        if (hasFirstBlock) {
            if (hasCountry) {
                builder.append(postal.country);
            }
            if (hasPostcode) {
                if (hasCountry) {
                    builder.append(" ");
                }
                builder.append(postal.postcode);
            }
        }
        if (hasSecondBlock) {
            if (hasFirstBlock) {
                builder.append("\n");
            }
            if (hasRegion) {
                builder.append(postal.region);
            }
            if (hasCity) {
                if (hasRegion) {
                    builder.append(" ");
                }
                builder.append(postal.city);
            }
            if (hasNeighborhood) {
                if (hasRegion || hasCity) {
                    builder.append(" ");
                }
                builder.append(postal.neighborhood);
            }
        }
        if (hasThirdBlock) {
            if (hasFirstBlock || hasSecondBlock) {
                builder.append("\n");
            }
            if (hasStreet) {
                builder.append(postal.street);
            }
            if (hasPobox) {
                if (hasStreet) {
                    builder.append(" ");
                }
                builder.append(postal.pobox);
            }
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        return null;
    }

    private String joinEnUs(Postal postal) {
        boolean hasStreet = !TextUtils.isEmpty(postal.street);
        boolean hasPobox = !TextUtils.isEmpty(postal.pobox);
        boolean hasNeighborhood = !TextUtils.isEmpty(postal.neighborhood);
        boolean hasCity = !TextUtils.isEmpty(postal.city);
        boolean hasRegion = !TextUtils.isEmpty(postal.region);
        boolean hasPostcode = !TextUtils.isEmpty(postal.postcode);
        boolean hasCountry = !TextUtils.isEmpty(postal.country);
        StringBuilder builder = new StringBuilder();
        boolean hasFirstBlock = hasStreet || hasPobox || hasNeighborhood;
        boolean hasSecondBlock = hasCity || hasRegion || hasPostcode;
        boolean hasThirdBlock = hasCountry;
        if (hasFirstBlock) {
            if (hasStreet) {
                builder.append(postal.street);
            }
            if (hasPobox) {
                if (hasStreet) {
                    builder.append("\n");
                }
                builder.append(postal.pobox);
            }
            if (hasNeighborhood) {
                if (hasStreet || hasPobox) {
                    builder.append("\n");
                }
                builder.append(postal.neighborhood);
            }
        }
        if (hasSecondBlock) {
            if (hasFirstBlock) {
                builder.append("\n");
            }
            if (hasCity) {
                builder.append(postal.city);
            }
            if (hasRegion) {
                if (hasCity) {
                    builder.append(", ");
                }
                builder.append(postal.region);
            }
            if (hasPostcode) {
                if (hasCity || hasRegion) {
                    builder.append(" ");
                }
                builder.append(postal.postcode);
            }
        }
        if (hasThirdBlock) {
            if (hasFirstBlock || hasSecondBlock) {
                builder.append("\n");
            }
            if (hasCountry) {
                builder.append(postal.country);
            }
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        return null;
    }

    private static boolean arePrintableAsciiOnly(String[] values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !TextUtils.isPrintableAsciiOnly(value)) {
                return false;
            }
        }
        return true;
    }
}
