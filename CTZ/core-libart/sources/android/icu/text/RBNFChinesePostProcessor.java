package android.icu.text;

final class RBNFChinesePostProcessor implements RBNFPostProcessor {
    private static final String[] rulesetNames = {"%traditional", "%simplified", "%accounting", "%time"};
    private int format;
    private boolean longForm;

    RBNFChinesePostProcessor() {
    }

    @Override
    public void init(RuleBasedNumberFormat ruleBasedNumberFormat, String str) {
    }

    @Override
    public void process(StringBuilder sb, NFRuleSet nFRuleSet) {
        int i;
        String name = nFRuleSet.getName();
        int i2 = 0;
        while (true) {
            if (i2 >= rulesetNames.length) {
                break;
            }
            if (!rulesetNames[i2].equals(name)) {
                i2++;
            } else {
                this.format = i2;
                this.longForm = i2 == 1 || i2 == 3;
            }
        }
        if (this.longForm) {
            int iIndexOf = sb.indexOf("*");
            while (iIndexOf != -1) {
                sb.delete(iIndexOf, iIndexOf + 1);
                iIndexOf = sb.indexOf("*", iIndexOf);
            }
            return;
        }
        String[][] strArr = {new String[]{"萬", "億", "兆", "〇"}, new String[]{"万", "亿", "兆", "〇"}, new String[]{"萬", "億", "兆", "零"}};
        String[] strArr2 = strArr[this.format];
        for (int i3 = 0; i3 < strArr2.length - 1; i3++) {
            int iIndexOf2 = sb.indexOf(strArr2[i3]);
            if (iIndexOf2 != -1) {
                sb.insert(iIndexOf2 + strArr2[i3].length(), '|');
            }
        }
        int iIndexOf3 = sb.indexOf("點");
        if (iIndexOf3 == -1) {
            iIndexOf3 = sb.length();
        }
        String str = strArr[this.format][3];
        int i4 = 0;
        int i5 = -1;
        while (iIndexOf3 >= 0) {
            int iLastIndexOf = sb.lastIndexOf("|", iIndexOf3);
            int iLastIndexOf2 = sb.lastIndexOf(str, iIndexOf3);
            if (iLastIndexOf2 <= iLastIndexOf) {
                i = 0;
            } else {
                i = (iLastIndexOf2 <= 0 || sb.charAt(iLastIndexOf2 + (-1)) == '*') ? 1 : 2;
            }
            int i6 = iLastIndexOf - 1;
            switch ((i4 * 3) + i) {
                case 0:
                case 2:
                case 3:
                case 6:
                case 8:
                    i5 = -1;
                    i4 = i;
                    iIndexOf3 = i6;
                    break;
                case 1:
                    i5 = iLastIndexOf2;
                    i4 = i;
                    iIndexOf3 = i6;
                    break;
                case 4:
                    sb.delete(iLastIndexOf2 - 1, iLastIndexOf2 + str.length());
                    i4 = 0;
                    i5 = -1;
                    iIndexOf3 = i6;
                    break;
                case 5:
                    sb.delete(i5 - 1, i5 + str.length());
                    i5 = -1;
                    i4 = i;
                    iIndexOf3 = i6;
                    break;
                case 7:
                    sb.delete(iLastIndexOf2 - 1, iLastIndexOf2 + str.length());
                    i4 = 0;
                    i5 = -1;
                    iIndexOf3 = i6;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        int length = sb.length();
        while (true) {
            length--;
            if (length >= 0) {
                char cCharAt = sb.charAt(length);
                if (cCharAt == '*' || cCharAt == '|') {
                    sb.delete(length, length + 1);
                }
            } else {
                return;
            }
        }
    }
}
