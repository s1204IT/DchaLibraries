package org.apache.xpath;

import javax.xml.transform.TransformerException;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

public class XPathAPI {
    public static Node selectSingleNode(Node node, String str) throws TransformerException {
        return selectSingleNode(node, str, node);
    }

    public static Node selectSingleNode(Node node, String str, Node node2) throws TransformerException {
        return selectNodeIterator(node, str, node2).nextNode();
    }

    public static NodeIterator selectNodeIterator(Node node, String str) throws TransformerException {
        return selectNodeIterator(node, str, node);
    }

    public static NodeIterator selectNodeIterator(Node node, String str, Node node2) throws TransformerException {
        return eval(node, str, node2).nodeset();
    }

    public static NodeList selectNodeList(Node node, String str) throws TransformerException {
        return selectNodeList(node, str, node);
    }

    public static NodeList selectNodeList(Node node, String str, Node node2) throws TransformerException {
        return eval(node, str, node2).nodelist();
    }

    public static XObject eval(Node node, String str) throws TransformerException {
        return eval(node, str, node);
    }

    public static XObject eval(Node node, String str, Node node2) throws TransformerException {
        XPathContext xPathContext = new XPathContext(false);
        if (node2.getNodeType() == 9) {
            node2 = ((Document) node2).getDocumentElement();
        }
        PrefixResolverDefault prefixResolverDefault = new PrefixResolverDefault(node2);
        return new XPath(str, null, prefixResolverDefault, 0, null).execute(xPathContext, xPathContext.getDTMHandleFromNode(node), prefixResolverDefault);
    }

    public static XObject eval(Node node, String str, PrefixResolver prefixResolver) throws TransformerException {
        XPath xPath = new XPath(str, null, prefixResolver, 0, null);
        XPathContext xPathContext = new XPathContext(false);
        return xPath.execute(xPathContext, xPathContext.getDTMHandleFromNode(node), prefixResolver);
    }
}
