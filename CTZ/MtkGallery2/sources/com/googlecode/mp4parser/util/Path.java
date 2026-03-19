package com.googlecode.mp4parser.util;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Path {
    static final boolean $assertionsDisabled = false;
    static Pattern component = Pattern.compile("(....|\\.\\.)(\\[(.*)\\])?");

    private Path() {
    }

    public static Box getPath(Box box, String str) {
        List<Box> paths = getPaths(box, str);
        if (paths.isEmpty()) {
            return null;
        }
        return paths.get(0);
    }

    public static List<Box> getPaths(Box box, String str) {
        String strSubstring;
        int i;
        if (str.startsWith("/")) {
            while (box.getParent() != null) {
                box = box.getParent();
            }
            return getPaths(box, str.substring(1));
        }
        if (str.isEmpty()) {
            return Collections.singletonList(box);
        }
        int i2 = 0;
        if (str.contains("/")) {
            strSubstring = str.substring(str.indexOf(47) + 1);
            str = str.substring(0, str.indexOf(47));
        } else {
            strSubstring = "";
        }
        Matcher matcher = component.matcher(str);
        if (matcher.matches()) {
            String strGroup = matcher.group(1);
            if ("..".equals(strGroup)) {
                return getPaths(box.getParent(), strSubstring);
            }
            if (matcher.group(2) != null) {
                i = Integer.parseInt(matcher.group(3));
            } else {
                i = -1;
            }
            LinkedList linkedList = new LinkedList();
            for (Box box2 : ((ContainerBox) box).getBoxes()) {
                if (box2.getType().matches(strGroup)) {
                    if (i == -1 || i == i2) {
                        linkedList.addAll(getPaths(box2, strSubstring));
                    }
                    i2++;
                }
            }
            return linkedList;
        }
        throw new RuntimeException(str + " is invalid path.");
    }
}
