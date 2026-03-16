package gov.nist.javax.sip;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.Via;
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
        } catch (Exception ex) {
            throw new RuntimeException("Could not intialize Digester ", ex);
        }
    }

    public static Utils getInstance() {
        return instance;
    }

    public static String toHexString(byte[] b) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            int pos2 = pos + 1;
            c[pos] = toHex[(b[i] >> 4) & 15];
            pos = pos2 + 1;
            c[pos2] = toHex[b[i] & 15];
        }
        return new String(c);
    }

    public static String getQuotedString(String str) {
        return '\"' + str.replace(Separators.DOUBLE_QUOTE, "\\\"") + '\"';
    }

    protected static String reduceString(String input) {
        String newString = input.toLowerCase();
        int len = newString.length();
        String retval = "";
        for (int i = 0; i < len; i++) {
            if (newString.charAt(i) != ' ' && newString.charAt(i) != '\t') {
                retval = retval + newString.charAt(i);
            }
        }
        return retval;
    }

    @Override
    public synchronized String generateCallIdentifier(String address) {
        String cidString;
        long jCurrentTimeMillis = System.currentTimeMillis();
        int i = callIDCounter;
        callIDCounter = i + 1;
        String date = Long.toString(jCurrentTimeMillis + ((long) i) + rand.nextLong());
        byte[] cid = digester.digest(date.getBytes());
        cidString = toHexString(cid);
        return cidString + Separators.AT + address;
    }

    @Override
    public synchronized String generateTag() {
        return Integer.toHexString(rand.nextInt());
    }

    @Override
    public synchronized String generateBranchId() {
        byte[] bid;
        long jNextLong = rand.nextLong();
        long j = counter;
        counter = 1 + j;
        long num = jNextLong + j + System.currentTimeMillis();
        bid = digester.digest(Long.toString(num).getBytes());
        return SIPConstants.BRANCH_MAGIC_COOKIE + toHexString(bid) + signature;
    }

    public boolean responseBelongsToUs(SIPResponse response) {
        Via topmostVia = response.getTopmostVia();
        String branch = topmostVia.getBranch();
        return branch != null && branch.endsWith(signature);
    }

    public static String getSignature() {
        return signature;
    }

    public static void main(String[] args) {
        HashSet branchIds = new HashSet();
        for (int b = 0; b < 100000; b++) {
            String bid = getInstance().generateBranchId();
            if (branchIds.contains(bid)) {
                throw new RuntimeException("Duplicate Branch ID");
            }
            branchIds.add(bid);
        }
        System.out.println("Done!!");
    }
}
