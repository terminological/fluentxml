package uk.co.terminological.fluentxml;

import java.io.File;
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
	
	//Fluent actions
	
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
	
	public String outerXml() {
		try {
			return doTransform().fragment().asXml();
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}
	
	public XmlXsl<? extends XmlNode> doTransform() throws XmlException {
		return doTransform((File) null);
	}
	
	public XmlXsl<? extends XmlNode> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

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
	
	public static XmlNode from(Node node) {
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			return from(Xml.fromDom((Document) node), (Element) node);
		} else {
			return from(Xml.fromDom(node.getOwnerDocument()), (Element) node);
		} 
	}
	
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
	
	public XmlList<XmlNode> walkTree() {
		return walkTree(XmlNode.class);
	}
	
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
	
	public boolean equals(XmlNode other) {
		if (other == null) return false;
		else return this.getRaw().equals(other.getRaw());
	}
	
	public <Y extends XmlNode> boolean is(Class<Y> type) {
		return type.isAssignableFrom(getClass());
	}
	
	public <Y extends XmlNode> Y as(Class<Y> type) {
		try {
			return cast(type);
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}
	
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
	
	public Optional<String> getTextContent() {
		return Optional.ofNullable(this.rawContext.getTextContent());
	}
}
