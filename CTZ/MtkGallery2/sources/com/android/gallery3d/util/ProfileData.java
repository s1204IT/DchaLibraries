package com.android.gallery3d.util;

import com.android.gallery3d.common.Utils;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileData {
    private static final String TAG = "Gallery2/ProfileData";
    private int mNextId;
    private DataOutputStream mOut;
    private byte[] mScratch = new byte[4];
    private Node mRoot = new Node(null, -1);
    private HashMap<String, Integer> mNameToId = new HashMap<>();

    private static class Node {
        public ArrayList<Node> children;
        public int id;
        public Node parent;
        public int sampleCount;

        public Node(Node node, int i) {
            this.parent = node;
            this.id = i;
        }
    }

    public void reset() {
        this.mRoot = new Node(null, -1);
        this.mNameToId.clear();
        this.mNextId = 0;
    }

    private int nameToId(String str) {
        Integer numValueOf = this.mNameToId.get(str);
        if (numValueOf == null) {
            int i = this.mNextId + 1;
            this.mNextId = i;
            numValueOf = Integer.valueOf(i);
            this.mNameToId.put(str, numValueOf);
        }
        return numValueOf.intValue();
    }

    public void addSample(String[] strArr) {
        int[] iArr = new int[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            iArr[i] = nameToId(strArr[i]);
        }
        Node node = this.mRoot;
        for (int length = strArr.length - 1; length >= 0; length--) {
            if (node.children == null) {
                node.children = new ArrayList<>();
            }
            int i2 = iArr[length];
            ArrayList<Node> arrayList = node.children;
            int i3 = 0;
            while (i3 < arrayList.size() && arrayList.get(i3).id != i2) {
                i3++;
            }
            if (i3 == arrayList.size()) {
                arrayList.add(new Node(node, i2));
            }
            node = arrayList.get(i3);
        }
        node.sampleCount++;
    }

    public void dumpToFile(String str) {
        try {
            try {
                this.mOut = new DataOutputStream(new FileOutputStream(str));
                writeInt(0);
                writeInt(3);
                writeInt(1);
                writeInt(20000);
                writeInt(0);
                writeAllStacks(this.mRoot, 0);
                writeInt(0);
                writeInt(1);
                writeInt(0);
                writeAllSymbols();
            } catch (IOException e) {
                com.mediatek.gallery3d.util.Log.w("Failed to dump to file", e);
            }
        } finally {
            Utils.closeSilently(this.mOut);
        }
    }

    private void writeOneStack(Node node, int i) throws IOException {
        writeInt(node.sampleCount);
        writeInt(i);
        while (true) {
            int i2 = i - 1;
            if (i > 0) {
                writeInt(node.id);
                node = node.parent;
                i = i2;
            } else {
                return;
            }
        }
    }

    private void writeAllStacks(Node node, int i) throws IOException {
        if (node.sampleCount > 0) {
            writeOneStack(node, i);
        }
        ArrayList<Node> arrayList = node.children;
        if (arrayList != null) {
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                writeAllStacks(arrayList.get(i2), i + 1);
            }
        }
    }

    private void writeAllSymbols() throws IOException {
        for (Map.Entry<String, Integer> entry : this.mNameToId.entrySet()) {
            this.mOut.writeBytes(String.format("0x%x %s\n", entry.getValue(), entry.getKey()));
        }
    }

    private void writeInt(int i) throws IOException {
        this.mScratch[0] = (byte) i;
        this.mScratch[1] = (byte) (i >> 8);
        this.mScratch[2] = (byte) (i >> 16);
        this.mScratch[3] = (byte) (i >> 24);
        this.mOut.write(this.mScratch);
    }
}
