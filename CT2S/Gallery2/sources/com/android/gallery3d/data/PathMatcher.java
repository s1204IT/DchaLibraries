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

    public void add(String pattern, int kind) {
        String[] segments = Path.split(pattern);
        Node current = this.mRoot;
        for (String str : segments) {
            current = current.addChild(str);
        }
        current.setKind(kind);
    }

    public int match(Path path) {
        String[] segments = path.split();
        this.mVariables.clear();
        Node current = this.mRoot;
        for (int i = 0; i < segments.length; i++) {
            Node next = current.getChild(segments[i]);
            if (next == null) {
                next = current.getChild("*");
                if (next != null) {
                    this.mVariables.add(segments[i]);
                } else {
                    return -1;
                }
            }
            current = next;
        }
        return current.getKind();
    }

    public String getVar(int index) {
        return this.mVariables.get(index);
    }

    public int getIntVar(int index) {
        return Integer.parseInt(this.mVariables.get(index));
    }

    private static class Node {
        private int mKind;
        private HashMap<String, Node> mMap;

        private Node() {
            this.mKind = -1;
        }

        Node addChild(String segment) {
            if (this.mMap == null) {
                this.mMap = new HashMap<>();
            } else {
                Node node = this.mMap.get(segment);
                if (node != null) {
                    return node;
                }
            }
            Node n = new Node();
            this.mMap.put(segment, n);
            return n;
        }

        Node getChild(String segment) {
            if (this.mMap == null) {
                return null;
            }
            return this.mMap.get(segment);
        }

        void setKind(int kind) {
            this.mKind = kind;
        }

        int getKind() {
            return this.mKind;
        }
    }
}
