package com.android.gallery3d.util;

import com.android.gallery3d.common.Utils;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileData {
    private static final String TAG = "ProfileData";
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

        public Node(Node parent, int id) {
            this.parent = parent;
            this.id = id;
        }
    }

    public void reset() {
        this.mRoot = new Node(null, -1);
        this.mNameToId.clear();
        this.mNextId = 0;
    }

    private int nameToId(String name) {
        Integer id = this.mNameToId.get(name);
        if (id == null) {
            int i = this.mNextId + 1;
            this.mNextId = i;
            id = Integer.valueOf(i);
            this.mNameToId.put(name, id);
        }
        return id.intValue();
    }

    public void addSample(String[] stack) {
        int[] ids = new int[stack.length];
        for (int i = 0; i < stack.length; i++) {
            ids[i] = nameToId(stack[i]);
        }
        Node node = this.mRoot;
        for (int i2 = stack.length - 1; i2 >= 0; i2--) {
            if (node.children == null) {
                node.children = new ArrayList<>();
            }
            int id = ids[i2];
            ArrayList<Node> children = node.children;
            int j = 0;
            while (j < children.size() && children.get(j).id != id) {
                j++;
            }
            if (j == children.size()) {
                children.add(new Node(node, id));
            }
            Node node2 = children.get(j);
            node = node2;
        }
        node.sampleCount++;
    }

    public void dumpToFile(String filename) {
        try {
            this.mOut = new DataOutputStream(new FileOutputStream(filename));
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
        } catch (IOException ex) {
            android.util.Log.w("Failed to dump to file", ex);
        } finally {
            Utils.closeSilently(this.mOut);
        }
    }

    private void writeOneStack(Node node, int depth) throws IOException {
        writeInt(node.sampleCount);
        writeInt(depth);
        while (true) {
            int depth2 = depth;
            depth = depth2 - 1;
            if (depth2 > 0) {
                writeInt(node.id);
                node = node.parent;
            } else {
                return;
            }
        }
    }

    private void writeAllStacks(Node node, int depth) throws IOException {
        if (node.sampleCount > 0) {
            writeOneStack(node, depth);
        }
        ArrayList<Node> children = node.children;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                writeAllStacks(children.get(i), depth + 1);
            }
        }
    }

    private void writeAllSymbols() throws IOException {
        for (Map.Entry<String, Integer> entry : this.mNameToId.entrySet()) {
            this.mOut.writeBytes(String.format("0x%x %s\n", entry.getValue(), entry.getKey()));
        }
    }

    private void writeInt(int v) throws IOException {
        this.mScratch[0] = (byte) v;
        this.mScratch[1] = (byte) (v >> 8);
        this.mScratch[2] = (byte) (v >> 16);
        this.mScratch[3] = (byte) (v >> 24);
        this.mOut.write(this.mScratch);
    }
}
