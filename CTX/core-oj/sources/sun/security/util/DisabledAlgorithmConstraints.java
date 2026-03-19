package sun.security.util;

import java.security.AlgorithmParameters;
import java.security.CryptoPrimitive;
import java.security.Key;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisabledAlgorithmConstraints extends AbstractAlgorithmConstraints {
    public static final String PROPERTY_CERTPATH_DISABLED_ALGS = "jdk.certpath.disabledAlgorithms";
    public static final String PROPERTY_JAR_DISABLED_ALGS = "jdk.jar.disabledAlgorithms";
    public static final String PROPERTY_TLS_DISABLED_ALGS = "jdk.tls.disabledAlgorithms";
    private static final Debug debug = Debug.getInstance("certpath");
    private final Constraints algorithmConstraints;
    private final String[] disabledAlgorithms;

    public DisabledAlgorithmConstraints(String str) {
        this(str, new AlgorithmDecomposer());
    }

    public DisabledAlgorithmConstraints(String str, AlgorithmDecomposer algorithmDecomposer) {
        super(algorithmDecomposer);
        this.disabledAlgorithms = getAlgorithms(str);
        this.algorithmConstraints = new Constraints(this.disabledAlgorithms);
    }

    @Override
    public final boolean permits(Set<CryptoPrimitive> set, String str, AlgorithmParameters algorithmParameters) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException("No cryptographic primitive specified");
        }
        return checkAlgorithm(this.disabledAlgorithms, str, this.decomposer);
    }

    @Override
    public final boolean permits(Set<CryptoPrimitive> set, Key key) {
        return checkConstraints(set, "", key, null);
    }

    @Override
    public final boolean permits(Set<CryptoPrimitive> set, String str, Key key, AlgorithmParameters algorithmParameters) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("No algorithm name specified");
        }
        return checkConstraints(set, str, key, algorithmParameters);
    }

    public final void permits(Set<CryptoPrimitive> set, CertConstraintParameters certConstraintParameters) throws CertPathValidatorException {
        checkConstraints(set, certConstraintParameters);
    }

    public final void permits(Set<CryptoPrimitive> set, X509Certificate x509Certificate) throws CertPathValidatorException {
        checkConstraints(set, new CertConstraintParameters(x509Certificate));
    }

    public boolean checkProperty(String str) {
        String lowerCase = str.toLowerCase(Locale.ENGLISH);
        for (String str2 : this.disabledAlgorithms) {
            if (str2.toLowerCase(Locale.ENGLISH).indexOf(lowerCase) >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean checkConstraints(Set<CryptoPrimitive> set, String str, Key key, AlgorithmParameters algorithmParameters) {
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null");
        }
        if ((str != null && str.length() != 0 && !permits(set, str, algorithmParameters)) || !permits(set, key.getAlgorithm(), null)) {
            return false;
        }
        return this.algorithmConstraints.permits(key);
    }

    private void checkConstraints(Set<CryptoPrimitive> set, CertConstraintParameters certConstraintParameters) throws CertPathValidatorException {
        X509Certificate certificate = certConstraintParameters.getCertificate();
        String sigAlgName = certificate.getSigAlgName();
        if (permits(set, sigAlgName, null)) {
            if (!permits(set, certificate.getPublicKey().getAlgorithm(), null)) {
                throw new CertPathValidatorException("Algorithm constraints check failed on disabled public key algorithm: " + sigAlgName, null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
            }
            this.algorithmConstraints.permits(certConstraintParameters);
            return;
        }
        throw new CertPathValidatorException("Algorithm constraints check failed on disabled signature algorithm: " + sigAlgName, null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
    }

    private static class Constraints {
        private static final Pattern keySizePattern = Pattern.compile("keySize\\s*(<=|<|==|!=|>|>=)\\s*(\\d+)");
        private Map<String, Set<Constraint>> constraintsMap = new HashMap();

        public Constraints(String[] strArr) {
            Constraint jdkcaconstraint;
            int length = strArr.length;
            int i = 0;
            int i2 = 0;
            while (i2 < length) {
                String str = strArr[i2];
                if (str != null && !str.isEmpty()) {
                    String strTrim = str.trim();
                    if (DisabledAlgorithmConstraints.debug != null) {
                        DisabledAlgorithmConstraints.debug.println("Constraints: " + strTrim);
                    }
                    int iIndexOf = strTrim.indexOf(32);
                    if (iIndexOf > 0) {
                        String strHashName = AlgorithmDecomposer.hashName(strTrim.substring(i, iIndexOf).toUpperCase(Locale.ENGLISH));
                        String[] strArrSplit = strTrim.substring(iIndexOf + 1).split("&");
                        int length2 = strArrSplit.length;
                        int i3 = i;
                        Constraint constraint = null;
                        Constraint constraint2 = null;
                        int i4 = i3;
                        while (i4 < length2) {
                            String strTrim2 = strArrSplit[i4].trim();
                            Matcher matcher = keySizePattern.matcher(strTrim2);
                            if (matcher.matches()) {
                                if (DisabledAlgorithmConstraints.debug != null) {
                                    DisabledAlgorithmConstraints.debug.println("Constraints set to keySize: " + strTrim2);
                                }
                                jdkcaconstraint = new KeySizeConstraint(strHashName, Constraint.Operator.of(matcher.group(1)), Integer.parseInt(matcher.group(2)));
                            } else if (strTrim2.equalsIgnoreCase("jdkCA")) {
                                if (DisabledAlgorithmConstraints.debug != null) {
                                    DisabledAlgorithmConstraints.debug.println("Constraints set to jdkCA.");
                                }
                                if (i3 != 0) {
                                    throw new IllegalArgumentException("Only one jdkCA entry allowed in property. Constraint: " + strTrim);
                                }
                                jdkcaconstraint = new jdkCAConstraint(strHashName);
                                i3 = 1;
                            } else {
                                jdkcaconstraint = constraint;
                            }
                            if (constraint2 == null) {
                                if (!this.constraintsMap.containsKey(strHashName)) {
                                    this.constraintsMap.putIfAbsent(strHashName, new HashSet());
                                }
                                if (jdkcaconstraint != null) {
                                    this.constraintsMap.get(strHashName).add(jdkcaconstraint);
                                }
                            } else {
                                constraint2.nextConstraint = jdkcaconstraint;
                            }
                            i4++;
                            constraint = jdkcaconstraint;
                            constraint2 = constraint;
                        }
                    } else {
                        this.constraintsMap.putIfAbsent(strTrim.toUpperCase(Locale.ENGLISH), new HashSet());
                    }
                }
                i2++;
                i = 0;
            }
        }

        private Set<Constraint> getConstraints(String str) {
            return this.constraintsMap.get(str);
        }

        public boolean permits(Key key) {
            Set<Constraint> constraints = getConstraints(key.getAlgorithm());
            if (constraints == null) {
                return true;
            }
            Iterator<Constraint> it = constraints.iterator();
            while (it.hasNext()) {
                if (!it.next().permits(key)) {
                    if (DisabledAlgorithmConstraints.debug != null) {
                        DisabledAlgorithmConstraints.debug.println("keySizeConstraint: failed key constraint check " + KeyUtil.getKeySize(key));
                        return false;
                    }
                    return false;
                }
            }
            return true;
        }

        public void permits(CertConstraintParameters certConstraintParameters) throws CertPathValidatorException {
            X509Certificate certificate = certConstraintParameters.getCertificate();
            if (DisabledAlgorithmConstraints.debug != null) {
                DisabledAlgorithmConstraints.debug.println("Constraints.permits(): " + certificate.getSigAlgName());
            }
            Set<String> setDecomposeOneHash = AlgorithmDecomposer.decomposeOneHash(certificate.getSigAlgName());
            if (setDecomposeOneHash == null || setDecomposeOneHash.isEmpty()) {
                return;
            }
            setDecomposeOneHash.add(certificate.getPublicKey().getAlgorithm());
            Iterator<String> it = setDecomposeOneHash.iterator();
            while (it.hasNext()) {
                Set<Constraint> constraints = getConstraints(it.next());
                if (constraints != null) {
                    Iterator<Constraint> it2 = constraints.iterator();
                    while (it2.hasNext()) {
                        it2.next().permits(certConstraintParameters);
                    }
                }
            }
        }
    }

    private static abstract class Constraint {
        String algorithm;
        Constraint nextConstraint;

        public abstract void permits(CertConstraintParameters certConstraintParameters) throws CertPathValidatorException;

        private Constraint() {
            this.nextConstraint = null;
        }

        enum Operator {
            EQ,
            NE,
            LT,
            LE,
            GT,
            GE;

            static Operator of(String str) {
                byte b;
                int iHashCode = str.hashCode();
                if (iHashCode != 60) {
                    if (iHashCode != 62) {
                        if (iHashCode != 1084) {
                            if (iHashCode != 1921) {
                                if (iHashCode != 1952) {
                                    b = (iHashCode == 1983 && str.equals(">=")) ? (byte) 5 : (byte) -1;
                                } else if (str.equals("==")) {
                                    b = 0;
                                }
                            } else if (str.equals("<=")) {
                                b = 3;
                            }
                        } else if (str.equals("!=")) {
                            b = 1;
                        }
                    } else if (str.equals(">")) {
                        b = 4;
                    }
                } else if (str.equals("<")) {
                    b = 2;
                }
                switch (b) {
                    case 0:
                        return EQ;
                    case 1:
                        return NE;
                    case 2:
                        return LT;
                    case 3:
                        return LE;
                    case 4:
                        return GT;
                    case 5:
                        return GE;
                    default:
                        throw new IllegalArgumentException("Error in security property. " + str + " is not a legal Operator");
                }
            }
        }

        public boolean permits(Key key) {
            return true;
        }
    }

    private static class jdkCAConstraint extends Constraint {
        jdkCAConstraint(String str) {
            super();
            this.algorithm = str;
        }

        @Override
        public void permits(CertConstraintParameters certConstraintParameters) throws CertPathValidatorException {
            if (DisabledAlgorithmConstraints.debug != null) {
                DisabledAlgorithmConstraints.debug.println("jdkCAConstraints.permits(): " + this.algorithm);
            }
            if (certConstraintParameters.isTrustedMatch()) {
                if (this.nextConstraint != null) {
                    this.nextConstraint.permits(certConstraintParameters);
                    return;
                }
                throw new CertPathValidatorException("Algorithm constraints check failed on certificate anchor limits", null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
            }
        }
    }

    private static class KeySizeConstraint extends Constraint {
        private int maxSize;
        private int minSize;
        private int prohibitedSize;

        public KeySizeConstraint(String str, Constraint.Operator operator, int i) {
            super();
            this.prohibitedSize = -1;
            this.algorithm = str;
            switch (operator) {
                case EQ:
                    this.minSize = 0;
                    this.maxSize = Integer.MAX_VALUE;
                    this.prohibitedSize = i;
                    break;
                case NE:
                    this.minSize = i;
                    this.maxSize = i;
                    break;
                case LT:
                    this.minSize = i;
                    this.maxSize = Integer.MAX_VALUE;
                    break;
                case LE:
                    this.minSize = i + 1;
                    this.maxSize = Integer.MAX_VALUE;
                    break;
                case GT:
                    this.minSize = 0;
                    this.maxSize = i;
                    break;
                case GE:
                    this.minSize = 0;
                    this.maxSize = i > 1 ? i - 1 : 0;
                    break;
                default:
                    this.minSize = Integer.MAX_VALUE;
                    this.maxSize = -1;
                    break;
            }
        }

        @Override
        public void permits(CertConstraintParameters certConstraintParameters) throws CertPathValidatorException {
            if (!permitsImpl(certConstraintParameters.getCertificate().getPublicKey())) {
                if (this.nextConstraint != null) {
                    this.nextConstraint.permits(certConstraintParameters);
                    return;
                }
                throw new CertPathValidatorException("Algorithm constraints check failed on keysize limits", null, null, -1, CertPathValidatorException.BasicReason.ALGORITHM_CONSTRAINED);
            }
        }

        @Override
        public boolean permits(Key key) {
            if (this.nextConstraint == null || !this.nextConstraint.permits(key)) {
                if (DisabledAlgorithmConstraints.debug != null) {
                    DisabledAlgorithmConstraints.debug.println("KeySizeConstraints.permits(): " + this.algorithm);
                }
                return permitsImpl(key);
            }
            return true;
        }

        private boolean permitsImpl(Key key) {
            if (this.algorithm.compareToIgnoreCase(key.getAlgorithm()) != 0) {
                return true;
            }
            int keySize = KeyUtil.getKeySize(key);
            if (keySize == 0) {
                return false;
            }
            if (keySize > 0) {
                return keySize >= this.minSize && keySize <= this.maxSize && this.prohibitedSize != keySize;
            }
            return true;
        }
    }
}
