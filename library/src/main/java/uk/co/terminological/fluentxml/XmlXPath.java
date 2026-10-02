package uk.co.terminological.fluentxml;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UType;
import net.sf.saxon.xpath.XPathEvaluator;
import net.sf.saxon.xpath.XPathExpressionImpl;

/**
 * Holds the result of an XPath 2.0 query against XML nodes. Provides typed and untyped
 * accessors for single results, lists, and streams.
 *
 * <h2>Usage - Single results</h2>
 * <pre>{@code
 * // Get one result, throw if not exactly one
 * XmlAttribute attr = xml.doXpath("/root/item/@id").getOne(XmlAttribute.class);
 * String value = (String) xml.doXpath("string(/root/item/@id)").getOne();
 *
 * // Get one result, return Optional
 * Optional<XmlElement> item = xml.doXpath("/root/item[1]").get(XmlElement.class);
 * Optional<Object> result = xml.doXpath("count(/root/item)").get();
 * }</pre>
 *
 * <h2>Usage - Multiple results</h2>
 * <pre>{@code
 * // Get all matching as typed list
 * XmlList<XmlElement> items = xml.doXpath("//item").getMany(XmlElement.class);
 *
 * // Get as stream
 * Stream<XmlElement> stream = xml.doXpath("//item").getManyAsStream(XmlElement.class);
 *
 * // Get as untyped iterable
 * Iterable<Object> results = xml.doXpath("//item/@href").getMany();
 * Stream<Object> stream = xml.doXpath("//item/@href").getManyAsStream();
 * }</pre>
 *
 * <h2>Namingpaces</h2>
 * <p>XPath queries require namespace abbreviations. By default, "ns" is used as
 * the abbreviation for the document's default namespace. You can customize this:</p>
 * <pre>{@code
 * xml.doXpath("/documentNode/ex2:complexNode/@attribute", "ex2");
 * }</pre>
 *
 * @author rchallen
 * @see Xml
 * @see XmlNode
 * @see XmlList
 */
public class XmlXPath<T extends XmlNode> {

	/** Context nodes being queried */
	List<T> context = new ArrayList<T>();
	/** Compiled XPath expression */
	XPathExpression expr;
	/** The original XPath expression string */
	String xpath;
	/** Default namespace abbreviation used for queries */
	String defaultNsAbbr;

	/**
	 * Creates an XmlXPath by compiling and preparing an XPath expression.
	 * Automatically discovers namespace mappings from the context node.
	 * @param context the context node
	 * @param xpath the XPath expression string
	 * @param defaultNsAbbr the abbreviation for the default namespace (e.g., "ns")
	 * @throws XmlException if compilation fails
	 */
	protected XmlXPath(T context, String xpath, String defaultNsAbbr) throws XmlException {
		this.context.add(context);
		this.defaultNsAbbr = defaultNsAbbr;
		this.xpath = xpath;
		Node node = context.getRaw();
		try {
			if (node.getNamespaceURI() != null) {
				if (node.getPrefix() == null) {
					context.getXml().withNamespaceAbbreviation(defaultNsAbbr, URI.create(node.getNamespaceURI()));
				} else if (!context.getXml().awareOfPrefix(node.getPrefix())) {
					context.getXml().withNamespaceAbbreviation(node.getPrefix(), URI.create(node.getNamespaceURI()));
				}
			}
		} catch (IllegalArgumentException e) {
			// node has no prefix
		}
		try {
			expr = compileXPath(xpath);
		} catch (XPathExpressionException e) {
			throw new XmlException("Could not compile xPath: ", e);
		}
	}

	/**
	 * Creates an XmlXPath from a list of context nodes.
	 * @param contexts the list of context nodes
	 * @param xpath the XPath expression string
	 * @param defaultNsAbbr the abbreviation for the default namespace
	 * @throws XmlException if compilation fails
	 */
	protected XmlXPath(XmlList<T> contexts, String xpath, String defaultNsAbbr) throws XmlException {
		this.context.addAll(contexts.cache);
		this.defaultNsAbbr = defaultNsAbbr;
		this.xpath = xpath;
		if (contexts.cache.size() > 0) {
			T ctx = contexts.cache.get(0);
			Node node = ctx.getRaw();
			try {
				if (node.getNamespaceURI() != null) {
					if (node.getPrefix() == null) {
						ctx.getXml().withNamespaceAbbreviation(defaultNsAbbr, URI.create(node.getNamespaceURI()));
					} else if (!ctx.getXml().awareOfPrefix(node.getPrefix())) {
						ctx.getXml().withNamespaceAbbreviation(node.getPrefix(), URI.create(node.getNamespaceURI()));
					}
				}
			} catch (IllegalArgumentException e) {
				// node has no prefix
			}
			try {
				expr = compileXPath(xpath);
			} catch (XPathExpressionException e) {
				throw new XmlException("Could not compile xPath: ", e);
			}
		}
	}

	private XPathExpression compileXPath(String xpath) throws XPathExpressionException {
		try {
			return evaluator(false).compile(xpath);
		} catch (XPathExpressionException e) {
			// Probably an abbreviation that is only declared deeper in the document
			return evaluator(true).compile(xpath);
		}
	}

	private XPathEvaluator evaluator(boolean deep) {
		XPathEvaluator evaluator = new XPathEvaluator();
		evaluator.setNamespaceContext(namespaces(deep));
		String defaultNs = defaultNamespace();
		if (defaultNs != null) evaluator.getStaticContext().setDefaultElementNamespace(NamespaceUri.of(defaultNs));
		return evaluator;
	}

	/**
	 * Namespace context backed by the abbreviations known to the queried document: those
	 * declared on its root element, those discovered by a deeper scan, and those registered
	 * by the user with withNamespaceAbbreviation. Unprefixed names resolve to the default
	 * namespace of the document, because JAXP exposes no XPath 2 default element namespace.
	 * @param deep whether to deep scan for abbreviations before resolving
	 * @return the namespace context to compile against
	 */
	private NamespaceContext namespaces(boolean deep) {
		return new NamespaceContext() {
			@Override
			public String getNamespaceURI(String prefix) {
				if (prefix == null) throw new IllegalArgumentException("XPath namespace prefix is null");
				if (prefix.equals(XMLConstants.XML_NS_PREFIX)) return XMLConstants.XML_NS_URI;
				if (context.isEmpty()) return XMLConstants.NULL_NS_URI;
				Xml xml = context.get(0).getXml();
				xml.discoverDefaultNs();
				if (deep) xml.deepScanNs();
				if (prefix.isEmpty()) {
					String def = defaultNamespace();
					return def == null ? XMLConstants.NULL_NS_URI : def;
				}
				String uri = xml.getAbbrevs().get(prefix);
				if (uri == null && prefix.equals(defaultNsAbbr)) uri = defaultNamespace();
				return uri == null ? XMLConstants.NULL_NS_URI : uri;
			}

			@Override
			public String getPrefix(String namespaceURI) {
				Iterator<String> prefixes = getPrefixes(namespaceURI);
				return prefixes.hasNext() ? prefixes.next() : null;
			}

			@Override
			public Iterator<String> getPrefixes(String namespaceURI) {
				if (context.isEmpty()) return Collections.<String>emptyList().iterator();
				List<String> prefixes = new ArrayList<String>();
				for (Map.Entry<String, String> entry : context.get(0).getXml().getAbbrevs().entrySet()) {
					if (entry.getValue().equals(namespaceURI)) prefixes.add(entry.getKey());
				}
				return prefixes.iterator();
			}
		};
	}

	private String defaultNamespace() {
		if (context.isEmpty()) return null;
		Node root = context.get(0).getDom().getDocumentElement();
		return root == null ? null : root.getNamespaceURI();
	}

	/**
	 * The JAXP return type to ask Saxon for: a node set for path expressions, otherwise the
	 * atomic type the expression was statically typed as.
	 * @return the QName return type for evaluation
	 */
	private QName returnType() {
		ItemType primary = ((XPathExpressionImpl) expr).getInternalExpression().getStaticType().getPrimaryType();
		if (!primary.isAtomicType()) return XPathConstants.NODESET;
		UType type = primary.getUType();
		if (type.equals(UType.NUMERIC)) return XPathConstants.NUMBER;
		if (type.equals(UType.BOOLEAN)) return XPathConstants.BOOLEAN;
		return XPathConstants.STRING;
	}

	private Object evaluate(T contextNode, QName returnType) throws XmlException {
		try {
			return expr.evaluate(contextNode.getRaw(), returnType);
		} catch (XPathExpressionException e) {
			throw new XmlException("Could not evaluate xPath: " + xpath, e);
		}
	}

	/** Same as evaluate but for the untyped accessors, which do not declare XmlException. */
	private Object evaluateLenient(T contextNode, QName returnType) {
		try {
			return evaluate(contextNode, returnType);
		} catch (XmlException e) {
			throw new RuntimeException("Could not evaluate xPath: " + xpath, e);
		}
	}

	// ==================== Single result accessors ====================

	/**
	 * Gets exactly one result of the specified type. Throws if zero or multiple results.
	 * @param clazz the expected node type
	 * @param <U> the node type
	 * @return the single matching node
	 * @throws XmlException if zero or multiple results
	 */
	public <U extends XmlNode> U getOne(Class<U> clazz) throws XmlException {
		XmlList<U> out = getMany(clazz);
		if (out.size() == 0) throw new XmlException("Xpath returns zero result: " + xpath);
		if (out.size() > 1) throw new XmlException("Xpath returns multiple result: " + xpath);
		return out.iterator().next();
	}

	/**
	 * Gets one result of the specified type as an Optional. Returns empty if no matches.
	 * @param clazz the expected node type
	 * @param <U> the node type
	 * @return Optional containing the first match, or empty
	 */
	public <U extends XmlNode> Optional<U> get(Class<U> clazz) throws XmlException {
		XmlList<U> out = getMany(clazz);
		return out.stream().findFirst();
	}

	/**
	 * Gets exactly one result as an untyped Object. Throws if zero or multiple results.
	 * @return the single result
	 * @throws XmlException if zero or multiple results
	 */
	public Object getOne() throws XmlException {
		Iterator<Object> out = getMany().iterator();
		if (!out.hasNext()) throw new XmlException("Xpath returns zero result: " + xpath);
		Object tmp = out.next();
		if (out.hasNext()) throw new XmlException("Xpath returns multiple result: " + xpath);
		return tmp;
	}

	/**
	 * Gets one result as an untyped Optional. Returns empty if no matches.
	 * @return Optional containing the first result, or empty
	 */
	public Optional<Object> get() {
		return getManyAsStream().findFirst();
	}

	// ==================== Multiple result accessors ====================

	/**
	 * Gets all results as a typed XmlList.
	 * @param clazz the expected node type
	 * @param <U> the node type
	 * @return a XmlList containing all matching nodes
	 * @throws XmlException if evaluation fails
	 */
	public <U extends XmlNode> XmlList<U> getMany(Class<U> clazz) throws XmlException {
		XmlList<U> out = new XmlList<U>();
		if (expr == null) return out;
		for (T contextNode : context) {
			out.addAll((NodeList) evaluate(contextNode, XPathConstants.NODESET), context.get(0).getXml());
		}
		return out;
	}

	/**
	 * Gets all results as a Java Stream of typed nodes.
	 * @param clazz the expected node type
	 * @param <U> the node type
	 * @return a Stream of matching nodes
	 * @throws XmlException if evaluation fails
	 */
	public <U extends XmlNode> Stream<U> getManyAsStream(Class<U> clazz) throws XmlException {
		return getMany(clazz).stream();
	}

	/**
	 * Gets all results as an untyped Iterable.
	 * @return an Iterable of raw objects (Nodes, strings, numbers, etc.)
	 */
	public Iterable<Object> getMany() {
		List<Object> out = new ArrayList<Object>();
		if (expr == null) return out;
		QName returnType = returnType();
		for (T contextNode : context) {
			Object result = evaluateLenient(contextNode, returnType);
			if (result instanceof NodeList) {
				NodeList nodes = (NodeList) result;
				for (int i = 0; i < nodes.getLength(); i++) {
					out.add(nodes.item(i));
				}
			} else if (result != null) {
				out.add(result);
			}
		}
		return out;
	}

	/**
	 * Gets all results as a Java Stream of untyped objects.
	 * @return a Stream of raw objects
	 */
	public Stream<Object> getManyAsStream() {
		return StreamSupport.stream(getMany().spliterator(), false);
	}

	// ==================== Internal helpers ====================
}
