package com.android.common.content;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ProjectionMap extends HashMap<String, String> {
    private String[] mColumns;

    public static class Builder {
        private ProjectionMap mMap = new ProjectionMap();

        public Builder add(String str) {
            this.mMap.putColumn(str, str);
            return this;
        }

        public Builder add(String str, String str2) {
            this.mMap.putColumn(str, str2 + " AS " + str);
            return this;
        }

        public Builder addAll(ProjectionMap projectionMap) {
            for (Map.Entry<String, String> entry : projectionMap.entrySet()) {
                this.mMap.putColumn(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public ProjectionMap build() {
            String[] strArr = new String[this.mMap.size()];
            this.mMap.keySet().toArray(strArr);
            Arrays.sort(strArr);
            this.mMap.mColumns = strArr;
            return this.mMap;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String[] getColumnNames() {
        return this.mColumns;
    }

    private void putColumn(String str, String str2) {
        super.put(str, str2);
    }

    @Override
    public String put(String str, String str2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> map) {
        throw new UnsupportedOperationException();
    }
}
