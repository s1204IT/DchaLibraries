package com.android.gallery3d.data;

import java.util.ArrayList;
import java.util.HashMap;

public class PathMatcher {
    private Node mRoot;
    private ArrayList<String> mVariables = new ArrayList<>();

    public PathMatcher() {
        this.mRoot = new Node();
        this.mRoot = new Node();
    }

    public void add(String str, int i) {
        String[] strArrSplit = Path.split(str);
        Node nodeAddChild = this.mRoot;
        for (String str2 : strArrSplit) {
            nodeAddChild = nodeAddChild.addChild(str2);
        }
        nodeAddChild.setKind(i);
    }

    public int match(Path path) {
        String[] strArrSplit = path.split();
        this.mVariables.clear();
        Node child = this.mRoot;
        for (int i = 0; i < strArrSplit.length; i++) {
            Node child2 = child.getChild(strArrSplit[i]);
            if (child2 == null) {
                child = child.getChild("*");
                if (child != null) {
                    this.mVariables.add(strArrSplit[i]);
                } else {
                    return -1;
                }
            } else {
                child = child2;
            }
        }
        return child.getKind();
    }

    public String getVar(int i) {
        return this.mVariables.get(i);
    }

    public int getIntVar(int i) {
        return Integer.parseInt(this.mVariables.get(i));
    }

    private static class Node {
        private int mKind;
        private HashMap<String, Node> mMap;

        private Node() {
            this.mKind = -1;
        }

        Node addChild(String str) {
            if (this.mMap == null) {
                this.mMap = new HashMap<>();
            } else {
                Node node = this.mMap.get(str);
                if (node != null) {
                    return node;
                }
            }
            Node node2 = new Node();
            this.mMap.put(str, node2);
            return node2;
        }

        Node getChild(String str) {
            if (this.mMap == null) {
                return null;
            }
            return this.mMap.get(str);
        }

        void setKind(int i) {
            this.mKind = i;
        }

        int getKind() {
            return this.mKind;
        }
    }
}
