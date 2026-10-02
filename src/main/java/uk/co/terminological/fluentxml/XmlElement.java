package uk.co.terminological.fluentxml;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Represents an XML element in a fluent XML document. Provides fluent methods for
 * building XML trees, querying child elements and attributes, streaming, and JAXB unmarshalling.
 *
 * <h2>Building XML</h2>
 * <pre>{@code
 * XmlElement el = root.withChildElement("item")
 *     .withAttribute("id", "1")
 *     .withText("hello");
 * }</pre>
 *
 * <h2>Querying</h2>
 * <pre>{@code
 * // Get child elements
 * XmlList<XmlElement> children = el.childElements();
 * XmlList<XmlElement> items = el.childElements("item");
 *
 * // Get attributes
 * Optional<String> id = el.getAttributeValue("id");
 *
 * // Stream traversal
 * el.stream().forEach(node -> System.out.println(node.getXPath()));
 * }</pre>
 *
 * <h2>JAXB</h2>
 * <pre>{@code
 * MyObject obj = el.unmarshalAs(MyObject.class);
 * }</pre>
 *
 * @author rchallen
 * @see XmlDocElement
 * @see XmlNode
 * @see XmlAttribute
 * @see XmlText
 */
public class XmlElement extends XmlNode {

	/**
	 * Creates an XmlElement wrapping the given DOM Element.
	 * @param xml the parent document
	 * @param element the raw DOM Element
	 */
	protected XmlElement(Xml xml, Element element) {
		super(xml, element);
	}

	// ==================== Accessors ====================

	/**
	 * Returns the namespace URI of this element, or empty if no namespace.
	 * @return Optional containing the namespace URI, or empty
	 */
	public Optional<URI> getNs() {
		return rawContext.getNamespaceURI() == null ? Optional.empty() : Optional.of(URI.create(rawContext.getNamespaceURI()));
	}

	/**
	 * Returns the raw W3C DOM Element.
	 * @return the underlying Element
	 */
	public Element getAsElement() {
		return (Element) rawContext;
	}

	/**
	 * Returns the element's tag name (local name, or prefixed name).
	 * @return the element name
	 */
	public String getName() {
		return rawContext.getNodeName();
	}

	// ==================== Fluent builders ====================

	/**
	 * Creates a child element and returns it for further fluent modification.
	 * @param name the child element name
	 * @return the newly created child element
	 */
	public XmlElement withChildElement(String name) {
		return withChildElement(name, null);
	}

	/**
	 * Creates a child element with a namespace and returns it for further fluent modification.
	 * @param name the child element name (may include prefix)
	 * @param namespace the namespace URI, or null for no namespace
	 * @return the newly created child element
	 */
	public XmlElement withChildElement(String name, URI namespace) {
		Element el = namespace != null ?
			this.getDom().createElementNS(namespace.toString(), name) :
			this.getDom().createElement(name);
		this.getRaw().appendChild(el);
		return from(el);
	}

	/**
	 * Adds text content to this element without wrapping it.
	 * @param text the text to append
	 * @return this element for chaining
	 */
	public XmlElement appendText(String text) {
		Text tn = this.getDom().createTextNode(text);
		((Element) this.getRaw()).appendChild(tn);
		return this;
	}

	/**
	 * Creates a text child node and returns it as an XmlText wrapper.
	 * @param text the text content
	 * @return an XmlText wrapping the new text node
	 */
	public XmlText withText(String text) {
		Text tn = this.getDom().createTextNode(text);
		((Element) this.getRaw()).appendChild(tn);
		return XmlText.from(tn);
	}

	/**
	 * Returns the parent element. If this element has no parent element
	 * (e.g. it is the document root, whose DOM parent is the Document node),
	 * returns this element unchanged.
	 * @return the parent XmlElement, or this element if there is no parent element
	 */
	public XmlElement up() {
		Node parent = this.getRaw().getParentNode();
		if (parent instanceof Element) {
			return XmlElement.from((Element) parent);
		}
		return this; // document root - no parent element
	}

	// ==================== Attributes ====================

	/**
	 * Returns all attributes of this element.
	 * @return a XmlList of XmlAttribute
	 */
	public XmlList<XmlAttribute> attributes() {
		return XmlList.create(XmlAttribute.class, ((Element) this.getRaw()).getAttributes());
	}

	/**
	 * Returns a stream of all attributes.
	 * @return stream of XmlAttribute
	 */
	public Stream<XmlAttribute> streamAttributes() {
		return attributes().stream();
	}

	/**
	 * Gets an attribute value by name (no namespace).
	 * @param attr the attribute name
	 * @return Optional containing the value, or empty if not found
	 */
	public Optional<String> getAttributeValue(String attr) {
		String tmp = ((Element) this.getRaw()).getAttribute(attr);
		return tmp.isEmpty() ? Optional.empty() : Optional.of(tmp);
	}

	/**
	 * Gets a namespaced attribute value.
	 * @param attr the attribute local name
	 * @param namespace the namespace URI
	 * @return Optional containing the value, or empty if not found
	 */
	public Optional<String> getAttributeValue(String attr, URI namespace) {
		String tmp = ((Element) this.getRaw()).getAttributeNS(namespace.toString(), attr);
		return tmp.isEmpty() ? Optional.empty() : Optional.of(tmp);
	}

	/**
	 * Creates or updates an attribute and returns it as an XmlAttribute.
	 * @param attr the attribute name
	 * @param namespace the namespace URI
	 * @param value the attribute value
	 * @return this element for chaining
	 */
	public XmlElement withAttribute(String attr, URI namespace, String value) {
		withAttribute(attr, Optional.of(namespace)).setValue(value);
		return this;
	}

	/**
	 * Creates or updates an attribute (uses the element's own namespace).
	 * @param attr the attribute name
	 * @param value the attribute value
	 * @return this element for chaining
	 */
	public XmlElement withAttribute(String attr, String value) {
		withAttribute(attr, this.getNs()).setValue(value);
		return this;
	}

	/**
	 * Creates an attribute and returns it as an XmlAttribute for fluent modification.
	 * @param attr the attribute name
	 * @return an XmlAttribute for setting the value
	 */
	public XmlAttribute withAttribute(String attr) {
		return withAttribute(attr, this.getNs());
	}

	/**
	 * Creates an attribute with an optional namespace and returns it for fluent modification.
	 * @param attr the attribute name
	 * @param namespace the namespace URI, or Optional.empty() for no namespace
	 * @return an XmlAttribute for setting the value
	 */
	public XmlAttribute withAttribute(String attr, Optional<URI> namespace) {
		if (namespace.isPresent()) {
			Attr at = this.getDom().createAttributeNS(namespace.get().toString(), attr);
			((Element) this.getRaw()).setAttributeNode(at);
			return XmlAttribute.from(at);
		} else {
			Attr at = this.getDom().createAttribute(attr);
			((Element) this.getRaw()).setAttributeNodeNS(at);
			return XmlAttribute.from(at);
		}
	}

	// ==================== Child elements ====================

	/**
	 * Returns all direct child elements.
	 * @return a XmlList of child XmlElements
	 */
	public XmlList<XmlElement> childElements() {
		return XmlList.create(XmlElement.class, ((Element) this.getRaw()).getChildNodes());
	}

	/**
	 * Returns direct child elements with the given tag name.
	 * @param tagName the element name to filter by
	 * @return a XmlList of matching child XmlElements
	 */
	public XmlList<XmlElement> childElements(String tagName) {
		return XmlList.create(XmlElement.class, ((Element) this.getRaw()).getElementsByTagName(tagName));
	}

	/**
	 * Returns a stream of direct child elements.
	 * @return a Stream of XmlElement
	 */
	public Stream<XmlElement> streamChildElements() {
		return childElements().stream();
	}

	/**
	 * Returns a stream of this element and all its descendants (depth-first).
	 * @return a Stream of XmlElement
	 */
	public Stream<XmlElement> stream() {
		return Stream.concat(
				Stream.of(this),
				streamChildElements().flatMap(e -> e.stream()));
	}

	// ==================== JAXB ====================

	/**
	 * Unmarshals this element into a JAXB-annotated object.
	 * @param clzz the target class
	 * @param <T> the target type
	 * @return the unmarshalled object
	 * @throws XmlException if unmarshalling fails
	 */
	@SuppressWarnings("unchecked")
	public <T> T unmarshalAs(Class<T> clzz) throws XmlException {
		try {
			Map<String, Object> properties = new HashMap<>();
			properties.put(JAXBContextProperties.NAMESPACE_PREFIX_MAPPER, xml.getNsPrefixMapper());
			if (this.getNs().isPresent()) properties.put(JAXBContextProperties.DEFAULT_TARGET_NAMESPACE, this.getNs().get());
			JAXBContext jc = JAXBContextFactory.createContext(new Class<?>[] {clzz}, properties);
			Unmarshaller u = jc.createUnmarshaller();
			return (T) u.unmarshal(this.getRaw());
		} catch (ClassCastException | JAXBException e) {
			throw new XmlException("Could not convert Xml to " + clzz.getCanonicalName(), e);
		}
	}

	// ==================== Transform ====================

	@Override
	public XmlXsl<? extends XmlElement> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	@Override
	public XmlXPath<? extends XmlElement> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}

	/**
	 * Applies a built-in transform to this element.
	 * @param switchNs one of the {@link XmlTransforms} built-in transforms
	 * @return an XmlXsl for the transform
	 * @throws XmlException if the transform cannot be initialized
	 */
	public XmlXsl<? extends XmlElement> doTransform(XmlTransforms switchNs) throws XmlException {
		return this.doTransform(switchNs.getFile());
	}

	/**
	 * Creates an XmlElement from a raw DOM Element.
	 * @param element the DOM Element
	 * @return an XmlElement wrapping the element
	 */
	public static XmlElement from(Element element) {
		return new XmlElement(Xml.fromDom(element.getOwnerDocument()), element);
	}

	/**
	 * Creates an XmlElement from a raw DOM Node, using the given Xml as context.
	 * <p><b>Deprecated:</b> this method silently casts the node to Element, which
	 * throws {@link ClassCastException} if the node is not an element. Use
	 * {@link #fromUnsafe(Xml, Node)} for explicit error handling, or
	 * {@link XmlNode#from(Xml, Node)} for type-safe dispatch to the correct
	 * XmlNode subclass.</p>
	 * @param xml the parent Xml document
	 * @param node the DOM node
	 * @return an XmlElement wrapping the node
	 * @deprecated Use {@link #fromUnsafe(Xml, Node)} for explicit exception handling,
	 *             or {@link XmlNode#from(Xml, Node)} for type-safe node dispatch.
	 */
	/**
	 * Creates an XmlElement from a raw DOM Node, using the given Xml as context.
	 * <p><b>Deprecated:</b> this method performs an unchecked cast to Element and will
	 * throw {@link ClassCastException} if the node is not an element. Use
	 * {@link #fromUnsafe(Xml, Node)} for explicit error handling, or
	 * {@link XmlNode#from(Xml, Node)} for type-safe dispatch to the correct
	 * XmlNode subclass.</p>
	 * @param xml the parent Xml document
	 * @param node the DOM node
	 * @return an XmlElement wrapping the node
	 * @deprecated Blind cast — use {@link #fromUnsafe(Xml, Node)} or {@link XmlNode#from(Xml, Node)}
	 */
	@Deprecated(since = "1.1", forRemoval = true)
	public static XmlElement from(Xml xml, org.w3c.dom.Node node) {
		return new XmlElement(xml, (Element) node);
	}

	/**
	 * Creates an XmlElement from a raw DOM Node, using the given Xml as context.
	 * <p>The node must be of type {@link Node#ELEMENT_NODE}. If the node is a text node,
	 * attribute, comment, or any other non-element type, a {@link NotAnElementException}
	 * is thrown instead of silently casting and potentially failing later.</p>
	 *
	 * <h3>Example</h3>
	 * <pre>{@code
	 * try {
	 *     XmlElement el = XmlElement.fromUnsafe(xml, someNode);
	 * } catch (NotAnElementException e) {
	 *     // someNode is not an element — handle gracefully
	 * }
	 * }</pre>
	 *
	 * @param xml the parent Xml document
	 * @param node the DOM node — must be an element
	 * @return an XmlElement wrapping the node
	 * @throws NotAnElementException if the node is not an element
	 */
	/**
	 * Returns a human-readable node type name for error messages.
	 */
	private static String nodeTypeName(short type) {
		switch (type) {
			case Node.ELEMENT_NODE: return "ELEMENT_NODE";
			case Node.ATTRIBUTE_NODE: return "ATTRIBUTE_NODE";
			case Node.TEXT_NODE: return "TEXT_NODE";
			case Node.CDATA_SECTION_NODE: return "CDATA_SECTION_NODE";
			case Node.COMMENT_NODE: return "COMMENT_NODE";
			case Node.DOCUMENT_NODE: return "DOCUMENT_NODE";
			case Node.PROCESSING_INSTRUCTION_NODE: return "PROCESSING_INSTRUCTION_NODE";
			default: return "UNKNOWN(" + type + ")";
		}
	}

	public static XmlElement fromUnsafe(Xml xml, org.w3c.dom.Node node) throws NotAnElementException {
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			String context = (node.getNodeValue() != null && !node.getNodeValue().trim().isEmpty())
					? " value '" + node.getNodeValue().trim() + "'"
					: (node.getNodeName() != null ? " named '" + node.getNodeName() + "'" : "");
			throw new NotAnElementException(
					"Expected an element node but got " + nodeTypeName(node.getNodeType()) +
					" (" + node.getNodeType() + ")" +
					" (" + node.getClass().getSimpleName() + ")" + context);
		}
		return new XmlElement(xml, (Element) node);
	}
}
