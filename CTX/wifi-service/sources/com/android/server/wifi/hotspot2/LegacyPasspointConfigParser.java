package com.android.server.wifi.hotspot2;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LegacyPasspointConfigParser {
    private static final String END_OF_INTERNAL_NODE_INDICATOR = ".";
    private static final String LONG_ARRAY_SEPARATOR = ",";
    private static final char START_OF_INTERNAL_NODE_INDICATOR = '+';
    private static final char STRING_PREFIX_INDICATOR = ':';
    private static final char STRING_VALUE_INDICATOR = '=';
    private static final String TAG = "LegacyPasspointConfigParser";
    private static final String TAG_CREDENTIAL = "Credential";
    private static final String TAG_FQDN = "FQDN";
    private static final String TAG_FRIENDLY_NAME = "FriendlyName";
    private static final String TAG_HOMESP = "HomeSP";
    private static final String TAG_IMSI = "IMSI";
    private static final String TAG_MANAGEMENT_TREE = "MgmtTree";
    private static final String TAG_PER_PROVIDER_SUBSCRIPTION = "PerProviderSubscription";
    private static final String TAG_REALM = "Realm";
    private static final String TAG_ROAMING_CONSORTIUM_OI = "RoamingConsortiumOI";
    private static final String TAG_SIM = "SIM";

    private static abstract class Node {
        private final String mName;

        public abstract List<Node> getChildren();

        public abstract String getValue();

        Node(String str) {
            this.mName = str;
        }

        public String getName() {
            return this.mName;
        }
    }

    private static class InternalNode extends Node {
        private final List<Node> mChildren;

        InternalNode(String str, List<Node> list) {
            super(str);
            this.mChildren = list;
        }

        @Override
        public List<Node> getChildren() {
            return this.mChildren;
        }

        @Override
        public String getValue() {
            return null;
        }
    }

    private static class LeafNode extends Node {
        private final String mValue;

        LeafNode(String str, String str2) {
            super(str);
            this.mValue = str2;
        }

        @Override
        public List<Node> getChildren() {
            return null;
        }

        @Override
        public String getValue() {
            return this.mValue;
        }
    }

    public Map<String, LegacyPasspointConfig> parseConfig(String str) throws IOException {
        HashMap map = new HashMap();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(str));
        bufferedReader.readLine();
        Node nodeBuildNode = buildNode(bufferedReader);
        if (nodeBuildNode == null || nodeBuildNode.getChildren() == null) {
            Log.d(TAG, "Empty configuration data");
            return map;
        }
        if (!TextUtils.equals(TAG_MANAGEMENT_TREE, nodeBuildNode.getName())) {
            throw new IOException("Unexpected root node: " + nodeBuildNode.getName());
        }
        Iterator<Node> it = nodeBuildNode.getChildren().iterator();
        while (it.hasNext()) {
            LegacyPasspointConfig legacyPasspointConfigProcessPpsNode = processPpsNode(it.next());
            map.put(legacyPasspointConfigProcessPpsNode.mFqdn, legacyPasspointConfigProcessPpsNode);
        }
        return map;
    }

    private static Node buildNode(BufferedReader bufferedReader) throws IOException {
        String line;
        do {
            line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
        } while (line.isEmpty());
        if (line == null) {
            return null;
        }
        String strTrim = line.trim();
        if (TextUtils.equals(END_OF_INTERNAL_NODE_INDICATOR, strTrim)) {
            return null;
        }
        Pair<String, String> line2 = parseLine(strTrim.getBytes(StandardCharsets.UTF_8));
        if (line2.second != null) {
            return new LeafNode((String) line2.first, (String) line2.second);
        }
        ArrayList arrayList = new ArrayList();
        while (true) {
            Node nodeBuildNode = buildNode(bufferedReader);
            if (nodeBuildNode != null) {
                arrayList.add(nodeBuildNode);
            } else {
                return new InternalNode((String) line2.first, arrayList);
            }
        }
    }

    private static LegacyPasspointConfig processPpsNode(Node node) throws IOException {
        if (node.getChildren() == null || node.getChildren().size() != 1) {
            throw new IOException("PerProviderSubscription node should contain one instance node");
        }
        if (!TextUtils.equals(TAG_PER_PROVIDER_SUBSCRIPTION, node.getName())) {
            throw new IOException("Unexpected name for PPS node: " + node.getName());
        }
        Node node2 = node.getChildren().get(0);
        if (node2.getChildren() == null) {
            throw new IOException("PPS instance node doesn't contained any children");
        }
        LegacyPasspointConfig legacyPasspointConfig = new LegacyPasspointConfig();
        for (Node node3 : node2.getChildren()) {
            String name = node3.getName();
            byte b = -1;
            int iHashCode = name.hashCode();
            if (iHashCode != -2127810660) {
                if (iHashCode == 1310049399 && name.equals(TAG_CREDENTIAL)) {
                    b = 1;
                }
            } else if (name.equals(TAG_HOMESP)) {
                b = 0;
            }
            switch (b) {
                case 0:
                    processHomeSPNode(node3, legacyPasspointConfig);
                    break;
                case 1:
                    processCredentialNode(node3, legacyPasspointConfig);
                    break;
                default:
                    Log.d(TAG, "Ignore uninterested field under PPS instance: " + node3.getName());
                    break;
            }
        }
        if (legacyPasspointConfig.mFqdn == null) {
            throw new IOException("PPS instance missing FQDN");
        }
        return legacyPasspointConfig;
    }

    private static void processHomeSPNode(Node node, LegacyPasspointConfig legacyPasspointConfig) throws IOException {
        if (node.getChildren() == null) {
            throw new IOException("HomeSP node should contain at least one child node");
        }
        for (Node node2 : node.getChildren()) {
            String name = node2.getName();
            byte b = -1;
            int iHashCode = name.hashCode();
            if (iHashCode != 2165397) {
                if (iHashCode != 542998228) {
                    if (iHashCode == 626253302 && name.equals(TAG_FRIENDLY_NAME)) {
                        b = 1;
                    }
                } else if (name.equals(TAG_ROAMING_CONSORTIUM_OI)) {
                    b = 2;
                }
            } else if (name.equals("FQDN")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    legacyPasspointConfig.mFqdn = getValue(node2);
                    break;
                case 1:
                    legacyPasspointConfig.mFriendlyName = getValue(node2);
                    break;
                case 2:
                    legacyPasspointConfig.mRoamingConsortiumOis = parseLongArray(getValue(node2));
                    break;
                default:
                    Log.d(TAG, "Ignore uninterested field under HomeSP: " + node2.getName());
                    break;
            }
        }
    }

    private static void processCredentialNode(Node node, LegacyPasspointConfig legacyPasspointConfig) throws IOException {
        if (node.getChildren() == null) {
            throw new IOException("Credential node should contain at least one child node");
        }
        for (Node node2 : node.getChildren()) {
            String name = node2.getName();
            byte b = -1;
            int iHashCode = name.hashCode();
            if (iHashCode != 82103) {
                if (iHashCode == 78834287 && name.equals("Realm")) {
                    b = 0;
                }
            } else if (name.equals(TAG_SIM)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    legacyPasspointConfig.mRealm = getValue(node2);
                    break;
                case 1:
                    processSimNode(node2, legacyPasspointConfig);
                    break;
                default:
                    Log.d(TAG, "Ignore uninterested field under Credential: " + node2.getName());
                    break;
            }
        }
    }

    private static void processSimNode(Node node, LegacyPasspointConfig legacyPasspointConfig) throws IOException {
        if (node.getChildren() == null) {
            throw new IOException("SIM node should contain at least one child node");
        }
        for (Node node2 : node.getChildren()) {
            String name = node2.getName();
            byte b = -1;
            if (name.hashCode() == 2251386 && name.equals(TAG_IMSI)) {
                b = 0;
            }
            if (b == 0) {
                legacyPasspointConfig.mImsi = getValue(node2);
            } else {
                Log.d(TAG, "Ignore uninterested field under SIM: " + node2.getName());
            }
        }
    }

    private static Pair<String, String> parseLine(byte[] bArr) throws IOException {
        Pair<String, Integer> string = parseString(bArr, 0);
        int iIntValue = ((Integer) string.second).intValue();
        try {
            if (bArr[iIntValue] == 43) {
                return Pair.create((String) string.first, null);
            }
            if (bArr[iIntValue] != 61) {
                throw new IOException("Invalid line - missing both node and value indicator: " + new String(bArr, StandardCharsets.UTF_8));
            }
            return Pair.create((String) string.first, (String) parseString(bArr, iIntValue + 1).first);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Invalid line - " + e.getMessage() + ": " + new String(bArr, StandardCharsets.UTF_8));
        }
    }

    private static Pair<String, Integer> parseString(byte[] bArr, int i) throws IOException {
        int i2 = i;
        while (true) {
            if (i2 < bArr.length) {
                if (bArr[i2] == 58) {
                    break;
                }
                i2++;
            } else {
                i2 = -1;
                break;
            }
        }
        if (i2 == -1) {
            throw new IOException("Invalid line - missing string prefix: " + new String(bArr, StandardCharsets.UTF_8));
        }
        try {
            int length = Integer.parseInt(new String(bArr, i, i2 - i, StandardCharsets.UTF_8), 16);
            int i3 = i2 + 1;
            if (i3 + length > bArr.length) {
                length = bArr.length - i3;
            }
            return Pair.create(new String(bArr, i3, length, StandardCharsets.UTF_8), Integer.valueOf(i3 + length));
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new IOException("Invalid line - " + e.getMessage() + ": " + new String(bArr, StandardCharsets.UTF_8));
        }
    }

    private static long[] parseLongArray(String str) throws IOException {
        String[] strArrSplit = str.split(LONG_ARRAY_SEPARATOR);
        long[] jArr = new long[strArrSplit.length];
        for (int i = 0; i < jArr.length; i++) {
            try {
                jArr[i] = Long.parseLong(strArrSplit[i], 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid long integer value: " + strArrSplit[i]);
            }
        }
        return jArr;
    }

    private static String getValue(Node node) throws IOException {
        if (node.getValue() == null) {
            throw new IOException("Attempt to retreive value from non-leaf node: " + node.getName());
        }
        return node.getValue();
    }
}
