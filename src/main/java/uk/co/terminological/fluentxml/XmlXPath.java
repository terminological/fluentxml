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

public class XmlXPath<T extends XmlNode> {

	//Xml root;
	List<T> context = new ArrayList<T>();
	XPath2Expression expr;
	String xpath;
	String defaultNsAbbr;
	
	protected XmlXPath(T context, String xpath, String defaultNsAbbr) throws XmlException {
		//this.root = context.getXml();
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
			// node has no prefix I think.
		}
		try {
			expr = compileXPath(xpath);
		} catch (XPathExpressionException e) {
			throw new XmlException("Could not compile xPath: ",e);
		}
	}

	protected XmlXPath(XmlList<T> contexts, String xpath, String defaultNsAbbr) throws XmlException {
		//this.root = contexts.iterator().next().getXml();
		this.context.addAll(contexts.cache);
		this.defaultNsAbbr = defaultNsAbbr;
		this.xpath = xpath;
		if (contexts.cache.size() > 0) {
			T context = contexts.cache.get(0);
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
				// node has no prefix I think.
			}
			try {
				expr = compileXPath(xpath);
			} catch (XPathExpressionException e) {
				throw new XmlException("Could not compile xPath: ",e);
			}
		}
	}
	
	private StaticContextBuilder getEvaluationContexts() {return getCompileContexts(false);}
	
	private StaticContextBuilder getCompileContexts(boolean deep) {
		StaticContextBuilder scb = new StaticContextBuilder();
		if (context.size() == 0) return scb;
		String defaultContext = context.get(0).getDom().getDocumentElement().getNamespaceURI(); 
		if (defaultContext != null) {
			scb.withDefaultNamespace(defaultContext);
			scb.withNamespace(defaultNsAbbr, defaultContext);
		}
		for (T con: context) {
			con.getXml().discoverDefaultNs();
			if (deep) con.getXml().deepScanNs();
			for (Map.Entry<String, String> entry: con.getXml().getAbbrevs().entrySet()) {
				scb.withNamespace(entry.getKey(), entry.getValue());
			}
		}
		try {
			scb.withTypeModel(new XercesTypeModel(context.get(0).getDom()));
		} catch (Exception e) {
			// Xpath on non schema aware model
			// A default static context builder will work here
		}
		return scb;
	}
	
	private XPath2Expression compileXPath(String xpath) throws XPathExpressionException {
		XPath2Expression tmpExpression;
		try {
			tmpExpression = new Engine().parseExpression(xpath,getCompileContexts(false));
		} catch (StaticNsNameError e) {
			tmpExpression = new Engine().parseExpression(xpath,getCompileContexts(true));
		}
		return tmpExpression;
	}
	
	public <U extends XmlNode> U getOne(Class<U> clazz) throws XmlException {
		XmlList<U> out = getMany(clazz);
		if (out.size() == 0) throw new XmlException("Xpath returns zero result: "+xpath);
		if (out.size() > 1) throw new XmlException("Xpath returns multiple result: "+xpath);
		return out.iterator().next();
	}
	
	public <U extends XmlNode> Optional<U> get(Class<U> clazz) throws XmlException {
		XmlList<U> out = getMany(clazz);
		return out.stream().findFirst();
	}
	
	public Object getOne() throws XmlException {
		Iterator<Object> out = getMany().iterator();
		if (!out.hasNext()) throw new XmlException("Xpath returns zero result: "+xpath);
		Object tmp = out.next();
		if (out.hasNext()) throw new XmlException("Xpath returns multiple result: "+xpath);
		return tmp;
	}
	
	public Optional<Object> get() {
		return getManyAsStream().findFirst();
	}

	public <U extends XmlNode> XmlList<U> getMany(Class<U> clazz) throws XmlException {
		XmlList<U> out = new XmlList<U>();
		StaticContextBuilder con = getEvaluationContexts();
		ResultSequence result = expr.evaluate(new DynamicContextBuilder(con), rawNodeArray(context) );
		out.addAll(result.iterator(), context.get(0).xml);
		return out;
	}
	
	public <U extends XmlNode> Stream<U> getManyAsStream(Class<U> clazz) throws XmlException {
		return getMany(clazz).stream();
	}
	
	private Object[] rawNodeArray(List<T> context2) {
		List<Node> tmp = new ArrayList<Node>();
		for (T t: context2) {
			tmp.add(t.getRaw());
		}
		return tmp.toArray();
	}

	public Iterable<Object> getMany() {
		StaticContextBuilder con = getEvaluationContexts();
		final ResultSequence result = expr.evaluate(new DynamicContextBuilder(con),
				rawNodeArray(context));
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
	
	public Stream<Object> getManyAsStream() {
		return StreamSupport.stream(getMany().spliterator(), false);
	}
}
