package uk.co.terminological.fluentxml;

import java.io.File;
import java.net.URL;

/**
 * Built-in XSLT transforms available through the {@link XmlXsl} API.
 * Each enum constant references a bundled XSLT stylesheet packaged with the library.
 *
 * <h2>Available transforms</h2>
 * <table>
 * <caption>Available built-in XSLT transforms</caption>
 * <tr><th>Constant</th><th>Description</th></tr>
 * <tr><td>{@code ELEMENTS_TO_LOWER_CASE}</td><td>Lowercase all element names</td></tr>
 * <tr><td>{@code ELEMENTS_TO_UPPER_CASE}</td><td>Uppercase all element names</td></tr>
 * <tr><td>{@code ATTRIB_TO_ELEMENTS}</td><td>Convert attributes to child elements</td></tr>
 * <tr><td>{@code STRIP_NS}</td><td>Remove all namespace declarations and prefixes</td></tr>
 * <tr><td>{@code STRIP_COMMENTS}</td><td>Remove XML comments</td></tr>
 * <tr><td>{@code XML_TO_YAML}</td><td>Convert XML to YAML format</td></tr>
 * <tr><td>{@code XML_TO_JSON}</td><td>Convert XML to JSON (supports BadgerFish, RabbitFish, RayFish)</td></tr>
 * <tr><td>{@code XHTML_TO_MARKDOWN}</td><td>Convert XHTML to Markdown</td></tr>
 * <tr><td>{@code XHTML_TO_TEXT}</td><td>Convert XHTML to plain text</td></tr>
 * <tr><td>{@code XHTML_TABLE_TO_CSV}</td><td>Convert XHTML tables to CSV</td></tr>
 * </table>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Convert to JSON with BadgerFish convention
 * String json = xml.doTransform(XmlTransforms.XML_TO_JSON)
 *     .withProperty("use-badgerfish", "true")
 *     .withProperty("skip-root", "true")
 *     .asString();
 *
 * // Strip namespaces
 * String plain = xml.doTransform(XmlTransforms.STRIP_NS).asXml();
 *
 * // Convert XHTML to Markdown
 * String md = xml.doTransform(XmlTransforms.XHTML_TO_MARKDOWN).asString();
 * }</pre>
 *
 * @author rchallen
 * @see XmlXsl
 */
public enum XmlTransforms {

	/** Lowercase all element names in the document */
	ELEMENTS_TO_LOWER_CASE("xslt/elements-to-lower-case.xsl"),

	/** Uppercase all element names in the document */
	ELEMENTS_TO_UPPER_CASE("xslt/elements-to-upper-case.xsl"),

	/** Convert all attributes to child elements (attribute name becomes element name) */
	ATTRIB_TO_ELEMENTS("xslt/attrib-to-elements.xsl"),

	/** Remove all namespace declarations and prefixes */
	STRIP_NS("xslt/strip-namespace.xsl"),

	/** Remove XML comments from the document */
	STRIP_COMMENTS("xslt/strip-comments.xsl"),

	/** Convert XML to YAML format */
	XML_TO_YAML("xslt/xml-to-yaml.xsl"),

	/**
	 * Convert XML to JSON format.
	 * Supports parameters: use-badgerfish, use-rabbitfish, use-rayfish,
	 * use-namespaces, skip-root, jsonp, debug
	 */
	XML_TO_JSON("xslt/xml-to-json.xsl"),

	/** Convert XHTML to Markdown format. Supports parameters: h-style, a-style, img-style, table-style */
	XHTML_TO_MARKDOWN("xslt/xhtml-to-markdown.xsl"),

	/** Convert XHTML to plain text */
	XHTML_TO_TEXT("xslt/xhtml-to-text.xsl"),

	/** Convert XHTML tables to CSV format */
	XHTML_TABLE_TO_CSV("xslt/xhtml-table-to-csv.xsl"),
	;

	/** The underlying stylesheet file */
	File inFile;

	XmlTransforms(String filename) {
		URL resourceFile = XmlTransforms.class.getClassLoader().getResource(filename);
		inFile = new File(resourceFile.getFile());
	}

	/**
	 * Returns the file path to the XSLT stylesheet.
	 * @return the stylesheet file
	 */
	public File getFile() {
		return inFile;
	}
}
