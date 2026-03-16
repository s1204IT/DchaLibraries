package com.googlecode.mp4parser.util;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Path {
    static final boolean $assertionsDisabled;
    static Pattern component;

    static {
        $assertionsDisabled = !Path.class.desiredAssertionStatus();
        component = Pattern.compile("(....|\\.\\.)(\\[(.*)\\])?");
    }

    private Path() {
    }

    public static Box getPath(Box box, String path) {
        List<Box> all = getPaths(box, path);
        if (all.isEmpty()) {
            return null;
        }
        return all.get(0);
    }

    public static List<Box> getPaths(Box box, String path) {
        String now;
        String later;
        if (path.startsWith("/")) {
            Box isoFile = box;
            while (isoFile.getParent() != null) {
                isoFile = isoFile.getParent();
            }
            if ($assertionsDisabled || (isoFile instanceof IsoFile)) {
                return getPaths(isoFile, path.substring(1));
            }
            throw new AssertionError(isoFile.getType() + " has no parent");
        }
        if (path.isEmpty()) {
            return Collections.singletonList(box);
        }
        if (path.contains("/")) {
            later = path.substring(path.indexOf(47) + 1);
            now = path.substring(0, path.indexOf(47));
        } else {
            now = path;
            later = "";
        }
        Matcher m = component.matcher(now);
        if (m.matches()) {
            String type = m.group(1);
            if ("..".equals(type)) {
                return getPaths(box.getParent(), later);
            }
            int index = -1;
            if (m.group(2) != null) {
                String indexString = m.group(3);
                index = Integer.parseInt(indexString);
            }
            List<Box> children = new LinkedList<>();
            int currentIndex = 0;
            for (Box box1 : ((ContainerBox) box).getBoxes()) {
                if (box1.getType().matches(type)) {
                    if (index == -1 || index == currentIndex) {
                        children.addAll(getPaths(box1, later));
                    }
                    currentIndex++;
                }
            }
            return children;
        }
        throw new RuntimeException(now + " is invalid path.");
    }
}
