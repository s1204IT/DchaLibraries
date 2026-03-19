package mf.org.apache.xerces.impl.xs.models;

import mf.org.apache.xerces.impl.dtd.models.CMNode;
import mf.org.apache.xerces.impl.xs.XSComplexTypeDecl;
import mf.org.apache.xerces.impl.xs.XSDeclarationPool;
import mf.org.apache.xerces.impl.xs.XSElementDecl;
import mf.org.apache.xerces.impl.xs.XSModelGroupImpl;
import mf.org.apache.xerces.impl.xs.XSParticleDecl;
import mf.org.apache.xerces.xs.XSTerm;

public class CMBuilder {
    private static final XSEmptyCM fEmptyCM = new XSEmptyCM();
    private XSDeclarationPool fDeclPool;
    private int fLeafCount;
    private final CMNodeFactory fNodeFactory;
    private int fParticleCount;

    public CMBuilder(CMNodeFactory nodeFactory) {
        this.fDeclPool = null;
        this.fDeclPool = null;
        this.fNodeFactory = nodeFactory;
    }

    public void setDeclPool(XSDeclarationPool declPool) {
        this.fDeclPool = declPool;
    }

    public XSCMValidator getContentModel(XSComplexTypeDecl typeDecl, boolean forUPA) {
        XSCMValidator cmValidator;
        short contentType = typeDecl.getContentType();
        if (contentType == 1 || contentType == 0) {
            return null;
        }
        XSParticleDecl particle = (XSParticleDecl) typeDecl.getParticle();
        if (particle == null) {
            return fEmptyCM;
        }
        if (particle.fType == 3 && ((XSModelGroupImpl) particle.fValue).fCompositor == 103) {
            cmValidator = createAllCM(particle);
        } else {
            cmValidator = createDFACM(particle, forUPA);
        }
        this.fNodeFactory.resetNodeCount();
        if (cmValidator == null) {
            XSCMValidator cmValidator2 = fEmptyCM;
            return cmValidator2;
        }
        return cmValidator;
    }

    XSCMValidator createAllCM(XSParticleDecl particle) {
        if (particle.fMaxOccurs == 0) {
            return null;
        }
        XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
        XSAllCM allContent = new XSAllCM(particle.fMinOccurs == 0, group.fParticleCount);
        for (int i = 0; i < group.fParticleCount; i++) {
            allContent.addElement((XSElementDecl) group.fParticles[i].fValue, group.fParticles[i].fMinOccurs == 0);
        }
        return allContent;
    }

    XSCMValidator createDFACM(XSParticleDecl particle, boolean forUPA) {
        this.fLeafCount = 0;
        this.fParticleCount = 0;
        CMNode node = useRepeatingLeafNodes(particle) ? buildCompactSyntaxTree(particle) : buildSyntaxTree(particle, forUPA);
        if (node == null) {
            return null;
        }
        return new XSDFACM(node, this.fLeafCount);
    }

    private CMNode buildSyntaxTree(XSParticleDecl particle, boolean forUPA) {
        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;
        boolean compactedForUPA = false;
        if (forUPA) {
            if (minOccurs > 1) {
                if (maxOccurs > minOccurs || particle.getMaxOccursUnbounded()) {
                    minOccurs = 1;
                    compactedForUPA = true;
                } else {
                    minOccurs = 2;
                    compactedForUPA = true;
                }
            }
            if (maxOccurs > 1) {
                maxOccurs = 2;
                compactedForUPA = true;
            }
        }
        short type = particle.fType;
        CMNode nodeRet = null;
        if (type == 2 || type == 1) {
            CMNodeFactory cMNodeFactory = this.fNodeFactory;
            short s = particle.fType;
            XSTerm xSTerm = particle.fValue;
            int i = this.fParticleCount;
            this.fParticleCount = i + 1;
            int i2 = this.fLeafCount;
            this.fLeafCount = i2 + 1;
            CMNode nodeRet2 = cMNodeFactory.getCMLeafNode(s, xSTerm, i, i2);
            CMNode nodeRet3 = expandContentModel(nodeRet2, minOccurs, maxOccurs);
            if (nodeRet3 != null) {
                nodeRet3.setIsCompactUPAModel(compactedForUPA);
                return nodeRet3;
            }
            return nodeRet3;
        }
        if (type != 3) {
            return null;
        }
        XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
        int count = 0;
        for (int i3 = 0; i3 < group.fParticleCount; i3++) {
            CMNode temp = buildSyntaxTree(group.fParticles[i3], forUPA);
            if (temp != null) {
                compactedForUPA |= temp.isCompactedForUPA();
                count++;
                if (nodeRet == null) {
                    nodeRet = temp;
                } else {
                    nodeRet = this.fNodeFactory.getCMBinOpNode(group.fCompositor, nodeRet, temp);
                }
            }
        }
        if (nodeRet != null) {
            if (group.fCompositor == 101 && count < group.fParticleCount) {
                nodeRet = this.fNodeFactory.getCMUniOpNode(5, nodeRet);
            }
            CMNode nodeRet4 = expandContentModel(nodeRet, minOccurs, maxOccurs);
            nodeRet4.setIsCompactUPAModel(compactedForUPA);
            return nodeRet4;
        }
        return nodeRet;
    }

    private CMNode expandContentModel(CMNode node, int minOccurs, int maxOccurs) {
        CMNode nodeRet = null;
        if (minOccurs != 1 || maxOccurs != 1) {
            if (minOccurs == 0 && maxOccurs == 1) {
                CMNode nodeRet2 = this.fNodeFactory.getCMUniOpNode(5, node);
                return nodeRet2;
            }
            if (minOccurs != 0 || maxOccurs != -1) {
                if (minOccurs != 1 || maxOccurs != -1) {
                    if (maxOccurs == -1) {
                        CMNode nodeRet3 = this.fNodeFactory.getCMUniOpNode(6, node);
                        return this.fNodeFactory.getCMBinOpNode(102, multiNodes(node, minOccurs - 1, true), nodeRet3);
                    }
                    if (minOccurs > 0) {
                        nodeRet = multiNodes(node, minOccurs, false);
                    }
                    if (maxOccurs > minOccurs) {
                        CMNode node2 = this.fNodeFactory.getCMUniOpNode(5, node);
                        if (nodeRet == null) {
                            CMNode nodeRet4 = multiNodes(node2, maxOccurs - minOccurs, false);
                            return nodeRet4;
                        }
                        return this.fNodeFactory.getCMBinOpNode(102, nodeRet, multiNodes(node2, maxOccurs - minOccurs, true));
                    }
                    return nodeRet;
                }
                CMNode nodeRet5 = this.fNodeFactory.getCMUniOpNode(6, node);
                return nodeRet5;
            }
            CMNode nodeRet6 = this.fNodeFactory.getCMUniOpNode(4, node);
            return nodeRet6;
        }
        return node;
    }

    private CMNode multiNodes(CMNode node, int num, boolean copyFirst) {
        if (num == 0) {
            return null;
        }
        if (num == 1) {
            return copyFirst ? copyNode(node) : node;
        }
        int num1 = num / 2;
        return this.fNodeFactory.getCMBinOpNode(102, multiNodes(node, num1, copyFirst), multiNodes(node, num - num1, true));
    }

    private CMNode copyNode(CMNode node) {
        int type = node.type();
        if (type == 101 || type == 102) {
            XSCMBinOp bin = (XSCMBinOp) node;
            return this.fNodeFactory.getCMBinOpNode(type, copyNode(bin.getLeft()), copyNode(bin.getRight()));
        }
        if (type == 4 || type == 6 || type == 5) {
            XSCMUniOp uni = (XSCMUniOp) node;
            return this.fNodeFactory.getCMUniOpNode(type, copyNode(uni.getChild()));
        }
        if (type == 1 || type == 2) {
            XSCMLeaf leaf = (XSCMLeaf) node;
            CMNodeFactory cMNodeFactory = this.fNodeFactory;
            int iType = leaf.type();
            Object leaf2 = leaf.getLeaf();
            int particleId = leaf.getParticleId();
            int i = this.fLeafCount;
            this.fLeafCount = i + 1;
            return cMNodeFactory.getCMLeafNode(iType, leaf2, particleId, i);
        }
        return node;
    }

    private CMNode buildCompactSyntaxTree(XSParticleDecl particle) {
        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;
        short type = particle.fType;
        CMNode nodeRet = null;
        if (type == 2 || type == 1) {
            return buildCompactSyntaxTree2(particle, minOccurs, maxOccurs);
        }
        if (type != 3) {
            return null;
        }
        XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
        if (group.fParticleCount == 1 && (minOccurs != 1 || maxOccurs != 1)) {
            return buildCompactSyntaxTree2(group.fParticles[0], minOccurs, maxOccurs);
        }
        int count = 0;
        for (int i = 0; i < group.fParticleCount; i++) {
            CMNode temp = buildCompactSyntaxTree(group.fParticles[i]);
            if (temp != null) {
                count++;
                if (nodeRet == null) {
                    nodeRet = temp;
                } else {
                    nodeRet = this.fNodeFactory.getCMBinOpNode(group.fCompositor, nodeRet, temp);
                }
            }
        }
        if (nodeRet != null && group.fCompositor == 101 && count < group.fParticleCount) {
            return this.fNodeFactory.getCMUniOpNode(5, nodeRet);
        }
        return nodeRet;
    }

    private CMNode buildCompactSyntaxTree2(XSParticleDecl particle, int minOccurs, int maxOccurs) {
        if (minOccurs == 1 && maxOccurs == 1) {
            CMNodeFactory cMNodeFactory = this.fNodeFactory;
            short s = particle.fType;
            XSTerm xSTerm = particle.fValue;
            int i = this.fParticleCount;
            this.fParticleCount = i + 1;
            int i2 = this.fLeafCount;
            this.fLeafCount = i2 + 1;
            return cMNodeFactory.getCMLeafNode(s, xSTerm, i, i2);
        }
        if (minOccurs == 0 && maxOccurs == 1) {
            CMNodeFactory cMNodeFactory2 = this.fNodeFactory;
            short s2 = particle.fType;
            XSTerm xSTerm2 = particle.fValue;
            int i3 = this.fParticleCount;
            this.fParticleCount = i3 + 1;
            int i4 = this.fLeafCount;
            this.fLeafCount = i4 + 1;
            return this.fNodeFactory.getCMUniOpNode(5, cMNodeFactory2.getCMLeafNode(s2, xSTerm2, i3, i4));
        }
        if (minOccurs != 0 || maxOccurs != -1) {
            if (minOccurs == 1 && maxOccurs == -1) {
                CMNodeFactory cMNodeFactory3 = this.fNodeFactory;
                short s3 = particle.fType;
                XSTerm xSTerm3 = particle.fValue;
                int i5 = this.fParticleCount;
                this.fParticleCount = i5 + 1;
                int i6 = this.fLeafCount;
                this.fLeafCount = i6 + 1;
                return this.fNodeFactory.getCMUniOpNode(6, cMNodeFactory3.getCMLeafNode(s3, xSTerm3, i5, i6));
            }
            CMNodeFactory cMNodeFactory4 = this.fNodeFactory;
            short s4 = particle.fType;
            XSTerm xSTerm4 = particle.fValue;
            int i7 = this.fParticleCount;
            this.fParticleCount = i7 + 1;
            int i8 = this.fLeafCount;
            this.fLeafCount = i8 + 1;
            CMNode nodeRet = cMNodeFactory4.getCMRepeatingLeafNode(s4, xSTerm4, minOccurs, maxOccurs, i7, i8);
            if (minOccurs == 0) {
                return this.fNodeFactory.getCMUniOpNode(4, nodeRet);
            }
            return this.fNodeFactory.getCMUniOpNode(6, nodeRet);
        }
        CMNodeFactory cMNodeFactory5 = this.fNodeFactory;
        short s5 = particle.fType;
        XSTerm xSTerm5 = particle.fValue;
        int i9 = this.fParticleCount;
        this.fParticleCount = i9 + 1;
        int i10 = this.fLeafCount;
        this.fLeafCount = i10 + 1;
        return this.fNodeFactory.getCMUniOpNode(4, cMNodeFactory5.getCMLeafNode(s5, xSTerm5, i9, i10));
    }

    private boolean useRepeatingLeafNodes(XSParticleDecl particle) {
        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;
        short type = particle.fType;
        if (type == 3) {
            XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
            if (minOccurs != 1 || maxOccurs != 1) {
                int i = group.fParticleCount;
                if (i == 1) {
                    XSParticleDecl particle2 = group.fParticles[0];
                    short type2 = particle2.fType;
                    if ((type2 == 1 || type2 == 2) && particle2.fMinOccurs == 1 && particle2.fMaxOccurs == 1) {
                        return true;
                    }
                    return false;
                }
                if (group.fParticleCount == 0) {
                    return true;
                }
                return false;
            }
            for (int i2 = 0; i2 < group.fParticleCount; i2++) {
                if (!useRepeatingLeafNodes(group.fParticles[i2])) {
                    return false;
                }
            }
        }
        return true;
    }
}
