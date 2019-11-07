package uk.co.terminological.fluentxml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.wst.xml.xpath2.api.Item;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
// import org.exolab.castor.xml.NodeType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.NodeIterator;

public class XmlList<T extends XmlNode> implements Iterable<T> {

	ArrayList<T> cache = new ArrayList<T>();
	
	@Override
	public Iterator<T> iterator() {
		return cache.iterator();
	}

	@SuppressWarnings("unchecked")
	private T convertNode(Node node, Xml xml) throws XmlException {
		//TODO move somewhere else.
		try {
			if (node instanceof Document) {
				return (T) Xml.fromDom((Document) node).content();
			} else if (node instanceof Element) {
				return (T) XmlElement.from(xml, node);
			} else if (node instanceof Attr) {
				return (T) XmlAttribute.from(xml, node);
			} else if (Text.class.isAssignableFrom(node.getClass())) { //node instanceof Text) {
				return (T) XmlText.from(xml, node);
			} else {
				return (T) XmlNode.from(xml, node);
			}
		} catch (ClassCastException e) {
			throw new XmlException("Incorrect type for list: ",e);
		}
	}
	
	protected XmlList<T> addAll(Iterator<Item> iterator, Xml xml) throws XmlException {
		while (iterator.hasNext()) {
			Node node = (Node) iterator.next().getNativeValue();
			cache.add(convertNode(node,xml));
		}
		return this;
	}
	
	protected XmlList<T> addAll(NodeIterator it, Xml xml) throws XmlException {
		Node node = it.nextNode();
		while (node != null) {
			cache.add(convertNode(node,xml));
			node = it.nextNode();
		}
		return this;
	}

	public int size() {return cache.size();}

	@SuppressWarnings("unchecked")
	public static <T extends XmlNode> XmlList<T> create(Class<T> class1, NodeList childNodes) {
		short accepted;
		if (class1.equals(XmlElement.class)) accepted = Node.ELEMENT_NODE;
		else if (class1.equals(XmlDocElement.class)) accepted = Node.DOCUMENT_NODE;
		else if (class1.equals(XmlAttribute.class)) accepted = Node.ATTRIBUTE_NODE;
		else if (class1.equals(XmlText.class)) accepted = Node.TEXT_NODE;
		else throw new RuntimeException("unsupported type");
		XmlList<T> out = new XmlList<T>();
		for (int i=0; i<childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n.getNodeType() == accepted) {
				out.cache.add((T) XmlNode.from(n));
			}
		}
		return out;
	}
	
	public XmlXPath<T> doXpath(String xpath) throws XmlException {
		return doXpath(xpath,"ns");
	}
	
	
	public XmlXPath<T> doXpath(String xpath, String defNsAbbr) throws XmlException {
		return new XmlXPath<T>(this, xpath, defNsAbbr);
	}
	
	public Stream<T> stream() {
		return cache.stream();
	}
	
	public Optional<T> findFirst() {
		return cache.stream().findFirst();
	}

	public static XmlList<XmlAttribute> create(Class<XmlAttribute> class1, NamedNodeMap attributes) {
		XmlList<XmlAttribute> out = new XmlList<XmlAttribute>();
		for (int i=0; i<attributes.getLength(); i++) {
			Node n = attributes.item(i);
			out.cache.add((XmlAttribute) XmlAttribute.from(n));
		}
		return out;
	}
}
