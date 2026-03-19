package android.provider;

import android.content.ComponentName;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import com.android.internal.util.ArrayUtils;
import java.util.Locale;

public class SettingsValidators {
    public static final Validator BOOLEAN_VALIDATOR = new DiscreteValueValidator(new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE});
    public static final Validator ANY_STRING_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            return true;
        }
    };
    public static final Validator NON_NEGATIVE_INTEGER_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            try {
                return Integer.parseInt(str) >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };
    public static final Validator ANY_INTEGER_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            try {
                Integer.parseInt(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };
    public static final Validator URI_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            try {
                Uri.decode(str);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    };
    public static final Validator COMPONENT_NAME_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            return (str == null || ComponentName.unflattenFromString(str) == null) ? false : true;
        }
    };
    public static final Validator NULLABLE_COMPONENT_NAME_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            return str == null || SettingsValidators.COMPONENT_NAME_VALIDATOR.validate(str);
        }
    };
    public static final Validator PACKAGE_NAME_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            return str != null && isStringPackageName(str);
        }

        private boolean isStringPackageName(String str) {
            if (str == null) {
                return false;
            }
            boolean zIsSubpartValidForPackageName = true;
            for (String str2 : str.split("\\.")) {
                zIsSubpartValidForPackageName &= isSubpartValidForPackageName(str2);
                if (!zIsSubpartValidForPackageName) {
                    break;
                }
            }
            return zIsSubpartValidForPackageName;
        }

        private boolean isSubpartValidForPackageName(String str) {
            if (str.length() == 0) {
                return false;
            }
            boolean zIsLetter = Character.isLetter(str.charAt(0));
            for (int i = 1; i < str.length(); i++) {
                zIsLetter &= Character.isLetterOrDigit(str.charAt(i)) || str.charAt(i) == '_';
                if (!zIsLetter) {
                    break;
                }
            }
            return zIsLetter;
        }
    };
    public static final Validator LENIENT_IP_ADDRESS_VALIDATOR = new Validator() {
        private static final int MAX_IPV6_LENGTH = 45;

        @Override
        public boolean validate(String str) {
            return str != null && str.length() <= 45;
        }
    };
    public static final Validator LOCALE_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String str) {
            if (str == null) {
                return false;
            }
            for (Locale locale : Locale.getAvailableLocales()) {
                if (str.equals(locale.toString())) {
                    return true;
                }
            }
            return false;
        }
    };

    public interface Validator {
        boolean validate(String str);
    }

    public static final class DiscreteValueValidator implements Validator {
        private final String[] mValues;

        public DiscreteValueValidator(String[] strArr) {
            this.mValues = strArr;
        }

        @Override
        public boolean validate(String str) {
            return ArrayUtils.contains(this.mValues, str);
        }
    }

    public static final class InclusiveIntegerRangeValidator implements Validator {
        private final int mMax;
        private final int mMin;

        public InclusiveIntegerRangeValidator(int i, int i2) {
            this.mMin = i;
            this.mMax = i2;
        }

        @Override
        public boolean validate(String str) {
            try {
                int i = Integer.parseInt(str);
                if (i >= this.mMin) {
                    return i <= this.mMax;
                }
                return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    public static final class InclusiveFloatRangeValidator implements Validator {
        private final float mMax;
        private final float mMin;

        public InclusiveFloatRangeValidator(float f, float f2) {
            this.mMin = f;
            this.mMax = f2;
        }

        @Override
        public boolean validate(String str) {
            try {
                float f = Float.parseFloat(str);
                if (f >= this.mMin) {
                    return f <= this.mMax;
                }
                return false;
            } catch (NullPointerException | NumberFormatException e) {
                return false;
            }
        }
    }

    public static final class ComponentNameListValidator implements Validator {
        private final String mSeparator;

        public ComponentNameListValidator(String str) {
            this.mSeparator = str;
        }

        @Override
        public boolean validate(String str) {
            if (str == null) {
                return false;
            }
            for (String str2 : str.split(this.mSeparator)) {
                if (!SettingsValidators.COMPONENT_NAME_VALIDATOR.validate(str2)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static final class PackageNameListValidator implements Validator {
        private final String mSeparator;

        public PackageNameListValidator(String str) {
            this.mSeparator = str;
        }

        @Override
        public boolean validate(String str) {
            if (str == null) {
                return false;
            }
            for (String str2 : str.split(this.mSeparator)) {
                if (!SettingsValidators.PACKAGE_NAME_VALIDATOR.validate(str2)) {
                    return false;
                }
            }
            return true;
        }
    }
}
