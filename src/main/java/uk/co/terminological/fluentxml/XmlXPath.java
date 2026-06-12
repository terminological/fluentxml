package uk.co.terminological.fluentxml;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.xpath.XPathExpressionException;

import org.eclipse.wst.xml.xpath2.api.Item;
import org.eclipse.wst.xml.xpath2.api.ResultSequence;
import org.eclipse.wst.xml.xpath2.api.XPath2Expression;
import org.eclipse.wst.xml.xpath2.processor.Engine;
import org.eclipse.wst.xml.xpath2.processor.internal.types.xerces.XercesTypeModel;
import org.eclipse.wst.xml.xpath2.processor.util.DynamicContextBuilder;
import org.eclipse.wst.xml.xpath2.processor.util.StaticContextBuilder;
import org.w3c.dom.Node;

import org.eclipse.wst.xml.xpath2.processor.internal.StaticNsNameError;

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
	XPath2Expression expr;
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

	private StaticContextBuilder getEvaluationContexts() {
		return getCompileContexts(false);
	}

	private StaticContextBuilder getCompileContexts(boolean deep) {
		StaticContextBuilder scb = new StaticContextBuilder();
		if (context.size() == 0) return scb;
		String defaultContext = context.get(0).getDom().getDocumentElement().getNamespaceURI();
		if (defaultContext != null) {
			scb.withDefaultNamespace(defaultContext);
			scb.withNamespace(defaultNsAbbr, defaultContext);
		}
		for (T con : context) {
			con.getXml().discoverDefaultNs();
			if (deep) con.getXml().deepScanNs();
			for (Map.Entry<String, String> entry : con.getXml().getAbbrevs().entrySet()) {
				scb.withNamespace(entry.getKey(), entry.getValue());
			}
		}
		try {
			scb.withTypeModel(new XercesTypeModel(context.get(0).getDom()));
		} catch (Exception e) {
			// Xpath on non schema aware model
		}
		return scb;
	}

	private XPath2Expression compileXPath(String xpath) throws XPathExpressionException {
		XPath2Expression tmpExpression;
		try {
			tmpExpression = new Engine().parseExpression(xpath, getCompileContexts(false));
		} catch (StaticNsNameError e) {
			tmpExpression = new Engine().parseExpression(xpath, getCompileContexts(true));
		}
		return tmpExpression;
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
		StaticContextBuilder con = getEvaluationContexts();
		ResultSequence result = expr.evaluate(new DynamicContextBuilder(con), rawNodeArray(context));
		out.addAll(result.iterator(), context.get(0).xml);
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
		StaticContextBuilder con = getEvaluationContexts();
		final ResultSequence result = expr.evaluate(new DynamicContextBuilder(con), rawNodeArray(context));
		return new Iterable<Object>() {
			final ResultSequence result2 = result;

			@Override
			public Iterator<Object> iterator() {
				final Iterator<Item> items = result2.iterator();
				return new Iterator<Object>() {
					@Override
					public boolean hasNext() {
						return items.hasNext();
					}

					@Override
					public Object next() {
						return items.next().getNativeValue();
					}

					@Override
					public void remove() {
						items.remove();
					}
				};
			}
		};
	}

	/**
	 * Gets all results as a Java Stream of untyped objects.
	 * @return a Stream of raw objects
	 */
	public Stream<Object> getManyAsStream() {
		return StreamSupport.stream(getMany().spliterator(), false);
	}

	// ==================== Internal helpers ====================

	private Object[] rawNodeArray(List<T> context2) {
		List<Node> tmp = new ArrayList<>();
		for (T t : context2) {
			tmp.add(t.getRaw());
		}
		return tmp.toArray();
	}
}
