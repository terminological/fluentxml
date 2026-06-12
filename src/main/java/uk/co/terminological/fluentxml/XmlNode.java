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

/**
 * Base class for all fluent XML node types. Provides common operations shared across
 * element, attribute, text, and document node wrappers.
 *
 * <p>This is the polymorphic base for the node type hierarchy:
 * <ul>
 *   <li>{@link XmlDocElement} - represents a document root node</li>
 *   <li>{@link XmlElement} - represents an XML element</li>
 *   <li>{@link XmlAttribute} - represents an XML attribute</li>
 *   <li>{@link XmlText} - represents text content or CDATA</li>
 * </ul>
 *
 * <h2>Type checking and casting</h2>
 * <pre>{@code
 * XmlNode node = ...;
 * if (node.is(XmlElement.class)) {
 *     XmlElement el = node.cast(XmlElement.class);
 * }
 * }</pre>
 *
 * <h2>Tree walking</h2>
 * <pre>{@code
 * xml.content().walkTree().forEach(n -> System.out.println(n.getXPath()));
 * xml.content().walkTree(XmlText.class).forEach(t -> System.out.println(t.getValue()));
 * }</pre>
 *
 * @author rchallen
 * @see XmlElement
 * @see XmlDocElement
 * @see XmlAttribute
 * @see XmlText
 */
public abstract class XmlNode {

	/** The parent Xml document */
	protected Xml xml;
	/** The raw W3C DOM node */
	protected Node rawContext;

	/**
	 * Creates a new XmlNode wrapper.
	 * @param xml the parent document
	 * @param node the raw DOM node
	 */
	protected XmlNode(Xml xml, Node node) {
		this.xml = xml;
		this.rawContext = node;
	}

	// ==================== Accessors ====================

	/**
	 * Returns the underlying raw DOM Node.
	 * @return the raw DOM node
	 */
	public Node getAsNode() {
		return rawContext;
	}

	/**
	 * Returns the raw DOM node (internal access).
	 * @return the raw node
	 */
	protected Node getRaw() {
		return rawContext;
	}

	/**
	 * Returns the parent Xml document.
	 * @return the parent document
	 */
	protected Xml getXml() {
		return xml;
	}

	/**
	 * Returns the owner Document of this node.
	 * @return the DOM Document
	 */
	protected Document getDom() {
		return rawContext.getOwnerDocument();
	}

	/**
	 * Returns the text content of this node (equivalent to DOM's getTextContent()).
	 * @return an Optional containing the text, or empty if null
	 */
	public Optional<String> getTextContent() {
		return Optional.ofNullable(this.rawContext.getTextContent());
	}

	// ==================== Output ====================

	/**
	 * Returns a string representation of this node.
	 * <p>For {@link XmlElement} instances, returns the XML fragment for the element.
	 * For other node types, returns the outer XML (the node plus its children).
	 * <p>To get just the text content, use {@link #getTextContent()} instead.
	 * @return the string representation
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
	 * Returns the outer XML of this node (the node itself plus all its descendants).
	 * @return the outer XML string
	 */
	public String outerXml() {
		try {
			return doTransform().fragment().asXml();
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes this node as XML to the given output stream using an identity transform.
	 * @param out the output stream
	 * @throws XmlException if the write fails
	 */
	public void write(OutputStream out) throws XmlException {
		this.doTransform().write(out);
	}

	// ==================== Transform / Query ====================

	/**
	 * Creates an XSLT transformer for this node with an identity transform.
	 * @return an XmlXsl configured with identity transform
	 * @throws XmlException if the transformer cannot be created
	 */
	public XmlXsl<? extends XmlNode> doTransform() throws XmlException {
		return doTransform((File) null);
	}

	/**
	 * Creates an XSLT transformer for this node with the given stylesheet.
	 * @param xslt the XSLT stylesheet file
	 * @return an XmlXsl configured with the given stylesheet
	 * @throws XmlException if the XSLT cannot be loaded
	 */
	public XmlXsl<? extends XmlNode> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	/**
	 * Executes an XPath 2.0 query relative to this node.
	 * @param xpath the XPath expression
	 * @return an XmlXPath for retrieving results
	 * @throws XmlException if the XPath cannot be compiled
	 */
	public XmlXPath<? extends XmlNode> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}

	// ==================== Navigation ====================

	/**
	 * Returns an XPath-style string path to this node, including positional indices.
	 * <p>For elements: /root[1]/child[1]/grandchild[1]
	 * <p>For attributes: /root[1]/child[1]/@attrName
	 * <p>For text: /root[1]/child[1]/text()[1]
	 * @return the XPath string
	 */
	public String getXPath() {
		Node node = rawContext;
		if (node.getNodeType() == Node.ELEMENT_NODE) return xpathFromElement((Element) node);
		else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			Element el = (Element) ((Attr) node).getOwnerElement();
			return xpathFromElement(el) + "/@" + node.getLocalName();
		} else if (node.getNodeType() == Node.TEXT_NODE) {
			Element el = (Element) node.getParentNode();
			int count = 1;
			Node tmp = node;
			while (tmp.getPreviousSibling() != null) {
				tmp = tmp.getPreviousSibling();
				if (tmp.getNodeType() == Node.TEXT_NODE) count += 1;
			}
			return xpathFromElement(el) + "/text()[" + count + "]";
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
			out.insert(0, "/" + element.getNodeName() + "[" + count + "]");
			try {
				element = (Element) element.getParentNode();
			} catch (ClassCastException e) {
				element = null;
			}
		}
		return out.toString();
	}

	/**
	 * Walks the DOM tree from this node and returns all descendant nodes in document order.
	 * @return a XmlList containing all nodes (elements, text, attributes)
	 */
	public XmlList<XmlNode> walkTree() {
		return walkTree(XmlNode.class);
	}

	/**
	 * Walks the DOM tree from this node, filtering by node type.
	 * @param type the desired node type (e.g., XmlElement.class, XmlText.class)
	 * @param <X> the node type
	 * @return a XmlList containing only nodes of the specified type
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
			throw new RuntimeException(e);
		}
	}

	// ==================== Type checking ====================

	/**
	 * Checks if this node is equal to another XmlNode (delegated to DOM.equals()).
	 * @param other the other node
	 * @return true if the underlying DOM nodes are equal
	 */
	public boolean equals(XmlNode other) {
		if (other == null) return false;
		else return this.getRaw().equals(other.getRaw());
	}

	/**
	 * Checks if this node is of the specified type without casting.
	 * @param type the node type class
	 * @param <Y> the node type
	 * @return true if this node is an instance of the type
	 */
	public <Y extends XmlNode> boolean is(Class<Y> type) {
		return type.isAssignableFrom(getClass());
	}

	/**
	 * Casts this node to the specified type, throwing a RuntimeException on failure.
	 * @param type the target type
	 * @param <Y> the target type
	 * @return the casted node
	 */
	public <Y extends XmlNode> Y as(Class<Y> type) {
		try {
			return cast(type);
		} catch (XmlException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Casts this node to the specified type, throwing an XmlException on failure.
	 * @param type the target type
	 * @param <T> the target type
	 * @return the casted node
	 * @throws XmlException if the cast fails
	 */
	@SuppressWarnings("unchecked")
	public <T extends XmlNode> T cast(Class<T> type) throws XmlException {
		Node node = this.getRaw();
		try {
			if (node instanceof Document) {
				return (T) Xml.fromDom((Document) node).content();
			} else if (node instanceof Element) {
				return (T) XmlNode.from(xml, node);
			} else if (node instanceof Attr) {
				return (T) XmlAttribute.from(xml, node);
			} else if (Text.class.isAssignableFrom(node.getClass())) {
				return (T) XmlText.from(xml, node);
			} else {
				return (T) XmlNode.from(xml, node);
			}
		} catch (ClassCastException e) {
			throw new XmlException("Incorrect type for cast: ", e);
		}
	}

	// ==================== Factory ====================

	/**
	 * Wraps a raw DOM node into the appropriate fluent XmlNode subclass.
	 * @param xml the parent document
	 * @param node the raw DOM node
	 * @return an XmlNode of the correct type for the given DOM node
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
			// For other node types (PROCESSING_INSTRUCTION, COMMENT, etc.),
			// return the raw node's name as a fallback string representation
			return new XmlNodeFallback(xml, node);
		}
	}

	/** Fallback for non-standard node types (comments, processing instructions, etc.) */
	private static class XmlNodeFallback extends XmlNode {
		protected XmlNodeFallback(Xml xml, Node node) {
			super(xml, node);
		}
		@Override
		public String toString() {
			return rawContext.getNodeName();
		}
	}

	/**
	 * Wraps a raw DOM node into the appropriate fluent XmlNode subclass.
	 * Creates a new Xml document wrapper automatically.
	 * @param node the raw DOM node
	 * @return an XmlNode of the correct type
	 */
	public static XmlNode from(Node node) {
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			return Xml.fromDom((Document) node).content();
		} else if (node.getNodeType() == Node.ELEMENT_NODE) {
			return XmlElement.from((Element) node);
		} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			return XmlAttribute.from((Attr) node);
		} else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
			return XmlText.from(node);
		} else {
			return new XmlNodeFallback(Xml.fromDom(node.getOwnerDocument()), node);
		}
	}

	// ==================== Static helpers ====================

	protected static <T extends XmlNode> XmlXsl<T> xslt(T node, File xslt) throws XmlException {
		return new XmlXsl<T>(node, xslt);
	}

	protected static <T extends XmlNode> XmlXPath<T> xpath(T node, String xpath) throws XmlException {
		return xpath(node, xpath, "ns");
	}

	protected static <T extends XmlNode> XmlXPath<T> xpath(T node, String xpath, String defNsAbbr) throws XmlException {
		return new XmlXPath<T>(node, xpath, defNsAbbr);
	}
}
