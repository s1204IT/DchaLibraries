package javax.security.auth;

import java.security.DomainCombiner;
import java.security.ProtectionDomain;

public class SubjectDomainCombiner implements DomainCombiner {
    public SubjectDomainCombiner(Subject subject) {
    }

    public Subject getSubject() {
        return null;
    }

    @Override
    public ProtectionDomain[] combine(ProtectionDomain[] protectionDomainArr, ProtectionDomain[] protectionDomainArr2) {
        return null;
    }
}
