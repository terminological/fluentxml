package uk.co.terminological.fluentxml;

import java.io.File;

import uk.co.terminological.fluentxml.css.CssElementCombinatorPairsToXpath;
import uk.co.terminological.fluentxml.css.CssSelectorStringSplitterException;
import org.w3c.dom.Element;

/**
 * Represents the root element of an XML document. Extends XmlElement with CSS selector support.
 *
 * <h2>CSS Selector queries</h2>
 * <pre>{@code
 * XmlDocElement root = Xml.fromFile("page.html").content();
 * XmlList<XmlElement> links = root.doCssSelection("a[href]").getMany(XmlElement.class);
 * }</pre>
 *
 * @author rchallen
 * @see XmlElement
 * @see XmlXPath
 */
public class XmlDocElement extends XmlElement {

	/**
	 * Creates an XmlDocElement wrapping the given DOM Element as document root.
	 * @param xml the parent document
	 * @param dom the root DOM Element
	 */
	protected XmlDocElement(Xml xml, Element dom) {
		super(xml, dom);
	}

	@Override
	public XmlXsl<XmlDocElement> doTransform(File xslt) throws XmlException {
		return XmlNode.xslt(this, xslt);
	}

	@Override
	public XmlXPath<XmlDocElement> doXpath(String xpath) throws XmlException {
		return XmlNode.xpath(this, xpath);
	}

	/**
	 * Executes a CSS selector query on the document, converting it to XPath internally.
	 * <p>Uses the vendored {@link CssElementCombinatorPairsToXpath} converter to turn CSS
	 * selectors like "div &gt; p.class" into XPath expressions.</p>
	 * @param selector the CSS selector string (e.g., "div.class", "a[href]", "ul &gt; li:first-child")
	 * @return an XmlXPath for retrieving matching elements
	 * @throws XmlException if the selector is invalid or the XPath cannot be compiled
	 */
	public XmlXPath<XmlDocElement> doCssSelection(String selector) throws XmlException {
		try {
			String xpath = new CssElementCombinatorPairsToXpath().convertCssSelectorStringToXpathString(selector);
			return XmlNode.xpath(this, xpath);
		} catch (CssSelectorStringSplitterException e) {
			throw new XmlException("Invalid css selector", e);
		}
	}

	/**
	 * Creates an XmlDocElement from a raw DOM Element.
	 * @param element the DOM Element (should be a document root)
	 * @return an XmlDocElement wrapping the element
	 */
	public static XmlDocElement from(Element element) {
		return new XmlDocElement(Xml.fromDom(element.getOwnerDocument()), element);
	}
}
