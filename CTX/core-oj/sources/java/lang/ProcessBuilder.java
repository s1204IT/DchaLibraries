package java.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ProcessBuilder {
    static final boolean $assertionsDisabled = false;
    private List<String> command;
    private File directory;
    private Map<String, String> environment;
    private boolean redirectErrorStream;
    private Redirect[] redirects;

    public ProcessBuilder(List<String> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        this.command = list;
    }

    public ProcessBuilder(String... strArr) {
        this.command = new ArrayList(strArr.length);
        for (String str : strArr) {
            this.command.add(str);
        }
    }

    public ProcessBuilder command(List<String> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        this.command = list;
        return this;
    }

    public ProcessBuilder command(String... strArr) {
        this.command = new ArrayList(strArr.length);
        for (String str : strArr) {
            this.command.add(str);
        }
        return this;
    }

    public List<String> command() {
        return this.command;
    }

    public Map<String, String> environment() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("getenv.*"));
        }
        if (this.environment == null) {
            this.environment = ProcessEnvironment.environment();
        }
        return this.environment;
    }

    ProcessBuilder environment(String[] strArr) {
        if (strArr != null) {
            this.environment = ProcessEnvironment.emptyEnvironment(strArr.length);
            for (String strReplaceFirst : strArr) {
                if (strReplaceFirst.indexOf(0) != -1) {
                    strReplaceFirst = strReplaceFirst.replaceFirst("\u0000.*", "");
                }
                int iIndexOf = strReplaceFirst.indexOf(61, 0);
                if (iIndexOf != -1) {
                    this.environment.put(strReplaceFirst.substring(0, iIndexOf), strReplaceFirst.substring(iIndexOf + 1));
                }
            }
        }
        return this;
    }

    public File directory() {
        return this.directory;
    }

    public ProcessBuilder directory(File file) {
        this.directory = file;
        return this;
    }

    static class NullInputStream extends InputStream {
        static final NullInputStream INSTANCE = new NullInputStream();

        private NullInputStream() {
        }

        @Override
        public int read() {
            return -1;
        }

        @Override
        public int available() {
            return 0;
        }
    }

    static class NullOutputStream extends OutputStream {
        static final NullOutputStream INSTANCE = new NullOutputStream();

        private NullOutputStream() {
        }

        @Override
        public void write(int i) throws IOException {
            throw new IOException("Stream closed");
        }
    }

    public static abstract class Redirect {
        static final boolean $assertionsDisabled = false;
        public static final Redirect PIPE = new Redirect() {
            @Override
            public Type type() {
                return Type.PIPE;
            }

            public String toString() {
                return type().toString();
            }
        };
        public static final Redirect INHERIT = new Redirect() {
            @Override
            public Type type() {
                return Type.INHERIT;
            }

            public String toString() {
                return type().toString();
            }
        };

        public enum Type {
            PIPE,
            INHERIT,
            READ,
            WRITE,
            APPEND
        }

        public abstract Type type();

        public File file() {
            return null;
        }

        boolean append() {
            throw new UnsupportedOperationException();
        }

        public static Redirect from(final File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Redirect() {
                {
                    super();
                }

                @Override
                public Type type() {
                    return Type.READ;
                }

                @Override
                public File file() {
                    return file;
                }

                public String toString() {
                    return "redirect to read from file \"" + ((Object) file) + "\"";
                }
            };
        }

        public static Redirect to(final File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Redirect() {
                {
                    super();
                }

                @Override
                public Type type() {
                    return Type.WRITE;
                }

                @Override
                public File file() {
                    return file;
                }

                public String toString() {
                    return "redirect to write to file \"" + ((Object) file) + "\"";
                }

                @Override
                boolean append() {
                    return false;
                }
            };
        }

        public static Redirect appendTo(final File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Redirect() {
                {
                    super();
                }

                @Override
                public Type type() {
                    return Type.APPEND;
                }

                @Override
                public File file() {
                    return file;
                }

                public String toString() {
                    return "redirect to append to file \"" + ((Object) file) + "\"";
                }

                @Override
                boolean append() {
                    return true;
                }
            };
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Redirect)) {
                return false;
            }
            Redirect redirect = (Redirect) obj;
            if (redirect.type() != type()) {
                return false;
            }
            return file().equals(redirect.file());
        }

        public int hashCode() {
            File file = file();
            if (file == null) {
                return super.hashCode();
            }
            return file.hashCode();
        }

        private Redirect() {
        }
    }

    private Redirect[] redirects() {
        if (this.redirects == null) {
            this.redirects = new Redirect[]{Redirect.PIPE, Redirect.PIPE, Redirect.PIPE};
        }
        return this.redirects;
    }

    public ProcessBuilder redirectInput(Redirect redirect) {
        if (redirect.type() == Redirect.Type.WRITE || redirect.type() == Redirect.Type.APPEND) {
            throw new IllegalArgumentException("Redirect invalid for reading: " + ((Object) redirect));
        }
        redirects()[0] = redirect;
        return this;
    }

    public ProcessBuilder redirectOutput(Redirect redirect) {
        if (redirect.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + ((Object) redirect));
        }
        redirects()[1] = redirect;
        return this;
    }

    public ProcessBuilder redirectError(Redirect redirect) {
        if (redirect.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + ((Object) redirect));
        }
        redirects()[2] = redirect;
        return this;
    }

    public ProcessBuilder redirectInput(File file) {
        return redirectInput(Redirect.from(file));
    }

    public ProcessBuilder redirectOutput(File file) {
        return redirectOutput(Redirect.to(file));
    }

    public ProcessBuilder redirectError(File file) {
        return redirectError(Redirect.to(file));
    }

    public Redirect redirectInput() {
        return this.redirects == null ? Redirect.PIPE : this.redirects[0];
    }

    public Redirect redirectOutput() {
        return this.redirects == null ? Redirect.PIPE : this.redirects[1];
    }

    public Redirect redirectError() {
        return this.redirects == null ? Redirect.PIPE : this.redirects[2];
    }

    public ProcessBuilder inheritIO() {
        Arrays.fill(redirects(), Redirect.INHERIT);
        return this;
    }

    public boolean redirectErrorStream() {
        return this.redirectErrorStream;
    }

    public ProcessBuilder redirectErrorStream(boolean z) {
        this.redirectErrorStream = z;
        return this;
    }

    public Process start() throws IOException {
        String[] strArr = (String[]) ((String[]) this.command.toArray(new String[this.command.size()])).clone();
        for (String str : strArr) {
            if (str == null) {
                throw new NullPointerException();
            }
        }
        String str2 = strArr[0];
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkExec(str2);
        }
        String string = this.directory == null ? null : this.directory.toString();
        for (int i = 1; i < strArr.length; i++) {
            if (strArr[i].indexOf(0) >= 0) {
                throw new IOException("invalid null character in command");
            }
        }
        try {
            return ProcessImpl.start(strArr, this.environment, string, this.redirects, this.redirectErrorStream);
        } catch (IOException | IllegalArgumentException e) {
            e = e;
            String str3 = ": " + e.getMessage();
            if ((e instanceof IOException) && securityManager != null) {
                try {
                    securityManager.checkRead(str2);
                } catch (SecurityException e2) {
                    e = e2;
                    str3 = "";
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot run program \"");
            sb.append(str2);
            sb.append("\"");
            sb.append(string == null ? "" : " (in directory \"" + string + "\")");
            sb.append(str3);
            throw new IOException(sb.toString(), e);
        }
    }
}
