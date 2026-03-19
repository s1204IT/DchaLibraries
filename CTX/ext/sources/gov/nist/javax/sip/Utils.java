package gov.nist.javax.sip;

import gov.nist.core.Separators;
import gov.nist.javax.sip.message.SIPResponse;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Random;

public class Utils implements UtilsExt {
    private static int callIDCounter;
    private static MessageDigest digester;
    private static Random rand;
    private static String signature;
    private static long counter = 0;
    private static Utils instance = new Utils();
    private static final char[] toHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    static {
        try {
            digester = MessageDigest.getInstance("MD5");
            rand = new Random();
            signature = toHexString(Integer.toString(Math.abs(rand.nextInt() % 1000)).getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Could not intialize Digester ", e);
        }
    }

    public static Utils getInstance() {
        return instance;
    }

    public static String toHexString(byte[] bArr) {
        char[] cArr = new char[bArr.length * 2];
        int i = 0;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            int i3 = i + 1;
            cArr[i] = toHex[(bArr[i2] >> 4) & 15];
            i = i3 + 1;
            cArr[i3] = toHex[bArr[i2] & 15];
        }
        return new String(cArr);
    }

    public static String getQuotedString(String str) {
        return '\"' + str.replace(Separators.DOUBLE_QUOTE, "\\\"") + '\"';
    }

    protected static String reduceString(String str) {
        String lowerCase = str.toLowerCase();
        int length = lowerCase.length();
        String str2 = "";
        for (int i = 0; i < length; i++) {
            if (lowerCase.charAt(i) != ' ' && lowerCase.charAt(i) != '\t') {
                str2 = str2 + lowerCase.charAt(i);
            }
        }
        return str2;
    }

    @Override
    public synchronized String generateCallIdentifier(String str) {
        long jCurrentTimeMillis;
        int i;
        jCurrentTimeMillis = System.currentTimeMillis();
        i = callIDCounter;
        callIDCounter = i + 1;
        return toHexString(digester.digest(Long.toString(jCurrentTimeMillis + ((long) i) + rand.nextLong()).getBytes())) + Separators.AT + str;
    }

    @Override
    public synchronized String generateTag() {
        return Integer.toHexString(rand.nextInt());
    }

    @Override
    public synchronized String generateBranchId() {
        long jNextLong;
        long j;
        jNextLong = rand.nextLong();
        j = counter;
        counter = 1 + j;
        return SIPConstants.BRANCH_MAGIC_COOKIE + toHexString(digester.digest(Long.toString(jNextLong + j + System.currentTimeMillis()).getBytes())) + signature;
    }

    public boolean responseBelongsToUs(SIPResponse sIPResponse) {
        String branch = sIPResponse.getTopmostVia().getBranch();
        return branch != null && branch.endsWith(signature);
    }

    public static String getSignature() {
        return signature;
    }

    public static void main(String[] strArr) {
        HashSet hashSet = new HashSet();
        for (int i = 0; i < 100000; i++) {
            String strGenerateBranchId = getInstance().generateBranchId();
            if (hashSet.contains(strGenerateBranchId)) {
                throw new RuntimeException("Duplicate Branch ID");
            }
            hashSet.add(strGenerateBranchId);
        }
        System.out.println("Done!!");
    }
}
