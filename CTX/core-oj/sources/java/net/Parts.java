package java.net;

class Parts {
    String path;
    String query;
    String ref;

    Parts(String str, String str2) {
        int iIndexOf = str.indexOf(35);
        this.ref = iIndexOf < 0 ? null : str.substring(iIndexOf + 1);
        str = iIndexOf >= 0 ? str.substring(0, iIndexOf) : str;
        int iLastIndexOf = str.lastIndexOf(63);
        if (iLastIndexOf != -1) {
            this.query = str.substring(iLastIndexOf + 1);
            this.path = str.substring(0, iLastIndexOf);
        } else {
            this.path = str;
        }
        if (this.path != null && this.path.length() > 0 && this.path.charAt(0) != '/' && str2 != null && !str2.isEmpty()) {
            this.path = '/' + this.path;
        }
    }

    String getPath() {
        return this.path;
    }

    String getQuery() {
        return this.query;
    }

    String getRef() {
        return this.ref;
    }
}
