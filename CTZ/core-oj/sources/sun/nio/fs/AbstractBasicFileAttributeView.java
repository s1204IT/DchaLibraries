package sun.nio.fs;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract class AbstractBasicFileAttributeView implements BasicFileAttributeView, DynamicFileAttributeView {
    private static final String SIZE_NAME = "size";
    private static final String CREATION_TIME_NAME = "creationTime";
    private static final String LAST_ACCESS_TIME_NAME = "lastAccessTime";
    private static final String LAST_MODIFIED_TIME_NAME = "lastModifiedTime";
    private static final String FILE_KEY_NAME = "fileKey";
    private static final String IS_DIRECTORY_NAME = "isDirectory";
    private static final String IS_REGULAR_FILE_NAME = "isRegularFile";
    private static final String IS_SYMBOLIC_LINK_NAME = "isSymbolicLink";
    private static final String IS_OTHER_NAME = "isOther";
    static final Set<String> basicAttributeNames = Util.newSet(SIZE_NAME, CREATION_TIME_NAME, LAST_ACCESS_TIME_NAME, LAST_MODIFIED_TIME_NAME, FILE_KEY_NAME, IS_DIRECTORY_NAME, IS_REGULAR_FILE_NAME, IS_SYMBOLIC_LINK_NAME, IS_OTHER_NAME);

    protected AbstractBasicFileAttributeView() {
    }

    @Override
    public String name() {
        return "basic";
    }

    @Override
    public void setAttribute(String str, Object obj) throws IOException {
        if (str.equals(LAST_MODIFIED_TIME_NAME)) {
            setTimes((FileTime) obj, null, null);
            return;
        }
        if (str.equals(LAST_ACCESS_TIME_NAME)) {
            setTimes(null, (FileTime) obj, null);
            return;
        }
        if (str.equals(CREATION_TIME_NAME)) {
            setTimes(null, null, (FileTime) obj);
            return;
        }
        throw new IllegalArgumentException("'" + name() + ":" + str + "' not recognized");
    }

    static class AttributesBuilder {
        private boolean copyAll;
        private Set<String> names = new HashSet();
        private Map<String, Object> map = new HashMap();

        private AttributesBuilder(Set<String> set, String[] strArr) {
            for (String str : strArr) {
                if (str.equals("*")) {
                    this.copyAll = true;
                } else {
                    if (!set.contains(str)) {
                        throw new IllegalArgumentException("'" + str + "' not recognized");
                    }
                    this.names.add(str);
                }
            }
        }

        static AttributesBuilder create(Set<String> set, String[] strArr) {
            return new AttributesBuilder(set, strArr);
        }

        boolean match(String str) {
            return this.copyAll || this.names.contains(str);
        }

        void add(String str, Object obj) {
            this.map.put(str, obj);
        }

        Map<String, Object> unmodifiableMap() {
            return Collections.unmodifiableMap(this.map);
        }
    }

    final void addRequestedBasicAttributes(BasicFileAttributes basicFileAttributes, AttributesBuilder attributesBuilder) {
        if (attributesBuilder.match(SIZE_NAME)) {
            attributesBuilder.add(SIZE_NAME, Long.valueOf(basicFileAttributes.size()));
        }
        if (attributesBuilder.match(CREATION_TIME_NAME)) {
            attributesBuilder.add(CREATION_TIME_NAME, basicFileAttributes.creationTime());
        }
        if (attributesBuilder.match(LAST_ACCESS_TIME_NAME)) {
            attributesBuilder.add(LAST_ACCESS_TIME_NAME, basicFileAttributes.lastAccessTime());
        }
        if (attributesBuilder.match(LAST_MODIFIED_TIME_NAME)) {
            attributesBuilder.add(LAST_MODIFIED_TIME_NAME, basicFileAttributes.lastModifiedTime());
        }
        if (attributesBuilder.match(FILE_KEY_NAME)) {
            attributesBuilder.add(FILE_KEY_NAME, basicFileAttributes.fileKey());
        }
        if (attributesBuilder.match(IS_DIRECTORY_NAME)) {
            attributesBuilder.add(IS_DIRECTORY_NAME, Boolean.valueOf(basicFileAttributes.isDirectory()));
        }
        if (attributesBuilder.match(IS_REGULAR_FILE_NAME)) {
            attributesBuilder.add(IS_REGULAR_FILE_NAME, Boolean.valueOf(basicFileAttributes.isRegularFile()));
        }
        if (attributesBuilder.match(IS_SYMBOLIC_LINK_NAME)) {
            attributesBuilder.add(IS_SYMBOLIC_LINK_NAME, Boolean.valueOf(basicFileAttributes.isSymbolicLink()));
        }
        if (attributesBuilder.match(IS_OTHER_NAME)) {
            attributesBuilder.add(IS_OTHER_NAME, Boolean.valueOf(basicFileAttributes.isOther()));
        }
    }

    @Override
    public Map<String, Object> readAttributes(String[] strArr) throws IOException {
        AttributesBuilder attributesBuilderCreate = AttributesBuilder.create(basicAttributeNames, strArr);
        addRequestedBasicAttributes(readAttributes(), attributesBuilderCreate);
        return attributesBuilderCreate.unmodifiableMap();
    }
}
