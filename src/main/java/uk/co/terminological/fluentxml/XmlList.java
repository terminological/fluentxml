package uk.co.terminological.fluentxml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.wst.xml.xpath2.api.Item;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.NodeIterator;

/**
 * A typed, iterable collection of XmlNode objects. Wraps DOM node collections
 * (NodeList, NamedNodeMap, NodeIterator, XPath result sets) into a fluent,
 * stream-capable list with XPath query support.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // From child elements
 * XmlList<XmlElement> children = element.childElements();
 *
 * // From XPath results
 * XmlList<XmlElement> items = xml.doXpath("//item").getMany(XmlElement.class);
 *
 * // Streaming and operations
 * List<XmlElement> all = items.list();
 * Optional<XmlElement> first = items.findFirst();
 * items.stream().forEach(e -> System.out.println(e.getName()));
 *
 * // Chained XPath
 * XmlList<XmlElement> deeper = items.doXpath("./child").getMany(XmlElement.class);
 * }</pre>
 *
 * <h2>Type safety</h2>
 * XmlList is generic and type-safe. Use {@code XmlElement.class}, {@code XmlText.class},
 * {@code XmlAttribute.class}, or {@code XmlDocElement.class} to filter by type.
 *
 * @author rchallen
 * @see XmlNode
 * @see XmlElement
 * @see XmlText
 * @see XmlAttribute
 * @see XmlXPath
 */
public class XmlList<T extends XmlNode> implements Iterable<T> {

	/** Internal cache of wrapped nodes */
	ArrayList<T> cache = new ArrayList<T>();

	/**
	 * Returns an iterator over the cached nodes.
	 * @return an iterator
	 */
	@Override
	public Iterator<T> iterator() {
		return cache.iterator();
	}

	// ==================== Creation ====================

	/**
	 * Creates an XmlList from a NodeList, filtering by type.
	 * @param class1 the node type to filter by (XmlElement, XmlDocElement, XmlAttribute, XmlText)
	 * @param childNodes the DOM NodeList to convert
	 * @param <T> the node type
	 * @return a new XmlList containing matching nodes
	 * @throws RuntimeException if the type is unsupported
	 */
	@SuppressWarnings("unchecked")
	public static <T extends XmlNode> XmlList<T> create(Class<T> class1, NodeList childNodes) {
		short accepted;
		if (XmlElement.class.isAssignableFrom(class1)) accepted = Node.ELEMENT_NODE;
		else if (XmlDocElement.class.isAssignableFrom(class1)) accepted = Node.DOCUMENT_NODE;
		else if (XmlAttribute.class.isAssignableFrom(class1)) accepted = Node.ATTRIBUTE_NODE;
		else if (XmlText.class.isAssignableFrom(class1)) accepted = Node.TEXT_NODE;
		else throw new RuntimeException("unsupported type");
		XmlList<T> out = new XmlList<T>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n.getNodeType() == accepted) {
				out.cache.add((T) XmlNode.from(n));
			}
		}
		return out;
	}

	/**
	 * Creates an XmlList of XmlAttribute from a NamedNodeMap of attributes.
	 * @param class1 must be XmlAttribute.class
	 * @param attributes the DOM NamedNodeMap of attributes
	 * @return a new XmlList of XmlAttribute
	 */
	public static XmlList<XmlAttribute> create(Class<XmlAttribute> class1, NamedNodeMap attributes) {
		XmlList<XmlAttribute> out = new XmlList<XmlAttribute>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node n = attributes.item(i);
			out.cache.add((XmlAttribute) XmlAttribute.from(n));
		}
		return out;
	}

	// ==================== Population ====================

	/**
	 * Adds all nodes from an XPath result iterator (internal use).
	 * @param iterator the XPath Item iterator
	 * @param xml the parent document context
	 * @return this list for chaining
	 * @throws XmlException if node conversion fails
	 */
	protected XmlList<T> addAll(Iterator<Item> iterator, Xml xml) throws XmlException {
		while (iterator.hasNext()) {
			Node node = (Node) iterator.next().getNativeValue();
			cache.add(convertNode(node, xml));
		}
		return this;
	}

	/**
	 * Adds all nodes from a DOM NodeIterator (internal use).
	 * @param it the DOM NodeIterator
	 * @param xml the parent document context
	 * @return this list for chaining
	 * @throws XmlException if node conversion fails
	 */
	protected XmlList<T> addAll(NodeIterator it, Xml xml) throws XmlException {
		Node node = it.nextNode();
		while (node != null) {
			cache.add(convertNode(node, xml));
			node = it.nextNode();
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	private T convertNode(Node node, Xml xml) throws XmlException {
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
			throw new XmlException("Incorrect type for list: ", e);
		}
	}

	// ==================== Accessors ====================

	/**
	 * Returns the number of nodes in the list.
	 * @return the list size
	 */
	public int size() {
		return cache.size();
	}

	/**
	 * Returns the underlying list (not a copy).
	 * @return the backing list
	 */
	public List<T> list() {
		return cache;
	}

	/**
	 * Returns the first element, or empty if the list is empty.
	 * @return Optional containing the first element
	 */
	public Optional<T> findFirst() {
		return cache.stream().findFirst();
	}

	/**
	 * Returns a Java Stream of the nodes.
	 * @return a Stream of T
	 */
	public Stream<T> stream() {
		return cache.stream();
	}

	// ==================== XPath ====================

	/**
	 * Executes an XPath 2.0 query on all nodes in this list, using "ns" as the default namespace abbreviation.
	 * @param xpath the XPath expression
	 * @return an XmlXPath for retrieving results
	 * @throws XmlException if the XPath cannot be compiled
	 */
	public XmlXPath<T> doXpath(String xpath) throws XmlException {
		return doXpath(xpath, "ns");
	}

	/**
	 * Executes an XPath 2.0 query on all nodes in this list.
	 * @param xpath the XPath expression
	 * @param defNsAbbr the abbreviation for the default namespace
	 * @return an XmlXPath for retrieving results
	 * @throws XmlException if the XPath cannot be compiled
	 */
	public XmlXPath<T> doXpath(String xpath, String defNsAbbr) throws XmlException {
		return new XmlXPath<T>(this, xpath, defNsAbbr);
	}
}
