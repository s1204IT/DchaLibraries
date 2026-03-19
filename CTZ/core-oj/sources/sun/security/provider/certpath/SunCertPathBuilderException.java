package sun.security.provider.certpath;

import java.security.cert.CertPathBuilderException;

public class SunCertPathBuilderException extends CertPathBuilderException {
    private static final long serialVersionUID = -7814288414129264709L;
    private transient AdjacencyList adjList;

    public SunCertPathBuilderException() {
    }

    public SunCertPathBuilderException(String str) {
        super(str);
    }

    public SunCertPathBuilderException(Throwable th) {
        super(th);
    }

    public SunCertPathBuilderException(String str, Throwable th) {
        super(str, th);
    }

    SunCertPathBuilderException(String str, AdjacencyList adjacencyList) {
        this(str);
        this.adjList = adjacencyList;
    }

    SunCertPathBuilderException(String str, Throwable th, AdjacencyList adjacencyList) {
        this(str, th);
        this.adjList = adjacencyList;
    }

    public AdjacencyList getAdjacencyList() {
        return this.adjList;
    }
}
