package uk.co.terminological.fluentxml;

import java.io.File;
import java.io.OutputStream;
import java.util.Optional;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

public class XmlNode {

	protected Xml xml;
	protected Node rawContext;
	
	protected Node getRaw() {
		return rawContext;
	}
	
	protected Xml getXml() {
		return xml;
	}
	
	/**
	 * @return the raw DOM node underlying this
	 */
	public Node getAsNode() {
		return rawContext;
	}
	
	protected Document getDom() {
		return rawContext.getOwnerDocument();
	}
	
	protected XmlNode(Xml xml, Node node) {
		this.xml = xml;
		this.rawContext = node;
	}
	
	/**
	 * If this is an XmlElement this will give a String rendering of the XML fragment for the current element, 
	 * otherwise an Xml fragment for the parent element -i.e. the {@link #outerXml()} method.
	 * <br/>
	 * If the string content of a node is needed  use the {@link #getTextContent()} method instead
	 * 
	 * @return a string representation of the Xml for XmlElement or the text content for other node types.
	 */
	public String toString() {
		try {
			if (this instanceof XmlElement) {
				return doTransform().asXml();
			} else {
				return outerXml();
			}
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return a string representation of this Xml.
	 */
	public String outerXml() {
		try {
			return doTransform().fragment().asXml();
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return an empty XmlXsl transformer on this node performing an identity transform - this is using an empty {@link javax.xml.transform.Transformer}
	 * @throws XmlException
	 */
	public XmlXsl<? extends XmlNode> doTransform() throws XmlException {
		return doTransform((File) null);
	}
	
	/**
	 * @return an empty XmlXsl transformer on this node with an XSLT from the file
	 * @throws XmlException
	 */
	public XmlXsl<? extends XmlNode> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	/**
	 * Perform an xpath query on this node and return the result
	 * @param xpath
	 * @return an XmlXpath containing the result
	 * @throws XmlException
	 */
	public XmlXPath<? extends XmlNode> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}
	
	protected static <T extends XmlNode> XmlXsl<T> xslt(T node, File xslt) throws XmlException {
		return new XmlXsl<T>(node, xslt);
	}
	
	protected static <T extends XmlNode> XmlXPath<T> xpath(T node, String xpath) throws XmlException {
		return xpath(node,xpath,"ns");
	}
	
	protected static <T extends XmlNode> XmlXPath<T> xpath(T node, String xpath, String defNsAbbr) throws XmlException {
		return new XmlXPath<T>(node, xpath, defNsAbbr);
	}
	
	/**
	 * @return Wraps a Dom node into a fluent XmlNode
	 * @param xml - the current document
	 * @param node - the Dom Node
	 */
	public static XmlNode from(Xml xml, Node node) {
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			return new XmlDocElement(xml, (Element) node);
		} else if (node.getNodeType() == Node.ELEMENT_NODE) {
			return new XmlElement(xml, (Element) node);
		} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			return new XmlAttribute(xml, (Attr) node);
		} else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
			return new XmlText(xml, (Node) node);
		} else {
			return new XmlNode(xml, node);
		}
	}
	
	/**
	 * @return Wraps a Dom node into a fluent XmlNode
	 * @param node - the Dom Node
	 */
	public static XmlNode from(Node node) {
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			return from(Xml.fromDom((Document) node), (Element) node);
		} else {
			return from(Xml.fromDom(node.getOwnerDocument()), (Element) node);
		} 
	}
	
	/**
	 * @return Wraps a string representation of the xpath to the current node including the index of the node
	 */
	public String getXPath() {
		Node node = rawContext;
		if (node.getNodeType() == Node.ELEMENT_NODE ) return xpathFromElement((Element) node);
		else if (node.getNodeType() == Node.ATTRIBUTE_NODE ) {
			Element el = (Element) ((Attr) node).getOwnerElement();
			return xpathFromElement(el) + "/@"+ node.getLocalName();
		} else if (node.getNodeType() == Node.TEXT_NODE ) {
			Element el = (Element) node.getParentNode();
			int count = 1;
			Node tmp = node;
			while (tmp.getPreviousSibling() != null) {
				tmp = tmp.getPreviousSibling();
				if (tmp.getNodeType() == Node.TEXT_NODE) count += 1;
			}
			return xpathFromElement(el) + "/text()["+count+"]";
		} else {
			return node.getNodeName();
		}
	}
	
	private String xpathFromElement(Element element) {
		StringBuilder out = new StringBuilder();
		while (element != null) {
			int count = 1;
			Node node = element;
			while (node.getPreviousSibling() != null) {
				node = node.getPreviousSibling();
				if (node.getNodeType() == Node.ELEMENT_NODE 
						&& node.getNodeName().equals(element.getNodeName())) count += 1;
			}
			out.insert(0, "/"+element.getNodeName()+"["+count+"]");
			try {
				element = (Element) element.getParentNode();
			} catch (ClassCastException e) {
				element = null;
			}
		}
		return out.toString();
	}
	
	/**
	 * @return a XmlList of XmlNodes in tree walking order
	 */
	public XmlList<XmlNode> walkTree() {
		return walkTree(XmlNode.class);
	}
	
	/**
	 * 
	 * @param type - The desired type of returned node e.g. XmlElement.class
	 * @return an XmlList of XmlNodes in tree walking order where the nodes are all of the given type
	 */
	public <X extends XmlNode> XmlList<X> walkTree(Class<X> type) {
		
		int whatToShow = NodeFilter.SHOW_ALL;
		if (XmlElement.class.isAssignableFrom(type)) whatToShow = NodeFilter.SHOW_ELEMENT;
		if (XmlText.class.isAssignableFrom(type)) whatToShow = NodeFilter.SHOW_TEXT;
		if (XmlAttribute.class.isAssignableFrom(type)) whatToShow = NodeFilter.SHOW_ATTRIBUTE;
		
		DocumentTraversal traversal = (DocumentTraversal) getDom();
		 
	    NodeIterator iterator = traversal.createNodeIterator(
	      this.getRaw(), whatToShow, null, true);
	    try {
			return new XmlList<X>().addAll(iterator, getXml());
		} catch (XmlException e) {
			// This type casting exception has been checked for by above
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return equality is delegated to DOM's {@link Node}.equals()
	 */
	public boolean equals(XmlNode other) {
		if (other == null) return false;
		else return this.getRaw().equals(other.getRaw());
	}
	
	/**
	 * @return checks the type of this node without resorting to DOM e.g. XmlElement.class
	 */
	public <Y extends XmlNode> boolean is(Class<Y> type) {
		return type.isAssignableFrom(getClass());
	}
	
	/**
	 * @return an unchecked cast of this node to the specified type e.g. XmlElement.class
	 * @throws this method will throw a RuntimeException if it cannot be cast.
	 */
	public <Y extends XmlNode> Y as(Class<Y> type) {
		try {
			return cast(type);
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return a checked cast of this node to the specified type e.g. XmlElement.class
	 * @throws this method will throw an XmlException if it cannot be cast.
	 */
	@SuppressWarnings("unchecked")
	public <T extends XmlNode> T cast(Class<T> type) throws XmlException {
		Node node = this.getRaw();
		try {
			if (node instanceof Document) {
				return (T) Xml.fromDom((Document) node).content();
			} else if (node instanceof Element) {
				return (T) XmlElement.from(xml, node);
			} else if (node instanceof Attr) {
				return (T) XmlAttribute.from(xml, node);
			} else if (Text.class.isAssignableFrom(node.getClass())) { 
				return (T) XmlText.from(xml, node);
			} else {
				return (T) XmlNode.from(xml, node);
			}
		} catch (ClassCastException e) {
			throw new XmlException("Incorrect type for list: ",e);
		}
	}
	
	/**
	 * get the text content of the node as per w3c DOM.getTextContent() method. 
	 * @return
	 */
	public Optional<String> getTextContent() {
		return Optional.ofNullable(this.rawContext.getTextContent());
	}
	
	/**
	 * Writes the current node to the output stream by doing a {@link #doTransform()} and
	 * setting the stream output source to the given output stream.
	 * @param out
	 * @throws XmlException
	 */
	public void write(OutputStream out) throws XmlException {
		this.doTransform().write(out);
	}
}
