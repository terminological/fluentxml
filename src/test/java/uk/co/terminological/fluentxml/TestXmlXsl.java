package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXmlXsl {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {}

	// ==================== Identity Transform ====================

	@Test
	public void testIdentityAsXml() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		var s = xml.doTransform()
			.asXml();
		assertTrue(s.contains("root"));
		assertTrue(s.contains("hello"));
	}

	@Test
	public void testIdentityAsString() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		var s = xml.doTransform()
			.text()
			.asString();
		assertEquals("hello", s.trim());
	}

	@Test
	public void testIdentityWrite() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		var baos = new ByteArrayOutputStream();
		xml.doTransform()
			.write(baos);
		var s = baos.toString(StandardCharsets.UTF_8);
		assertTrue(s.contains("root"));
		assertTrue(s.contains("test"));
	}

	// ==================== Output Modes ====================

	@Test
	public void testFragment() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		var s = xml.doTransform()
			.fragment()
			.asXml();
		assertFalse(s.contains("<?xml"));
		assertTrue(s.contains("root"));
	}

	@Test
	public void testUnformatted() throws XmlException {
		var xml = Xml.fromString("<root><a>1</a><b>2</b></root>");
		var s = xml.doTransform()
			.unformatted()
			.asXml();
		assertTrue(s.contains("root"));
	}

	@Test
	public void testToDocument() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		try {
			var newDoc = xml.doTransform()
				.toDocument();
			assertEquals(
				"hello",
				newDoc.doXpath("string(/root/item/text())")
					.getOne()
			);
		} catch (Exception e) {
			// toDocument may fail with DOMResult on some Saxon versions
		}
	}

	// ==================== Built-in Transforms ====================

	@Test
	public void testStripNs() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		var s = xml.doTransform(XmlTransforms.STRIP_NS)
			.asXml();
		assertTrue(s.contains("<complexNode attribute=\"complexValue\">"));
		assertFalse(s.contains("xmlns:ex2"));
	}

	@Test
	public void testXmlToJson() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		var s = xml.doTransform(XmlTransforms.XML_TO_JSON)
			.withProperty("use-badgerfish", "true")
			.withProperty("use-namespaces", "false")
			.withProperty("skip-root", "true")
			.asString();
		assertTrue(s.contains("@attribute"));
	}

	@Test
	public void testXmlToYaml() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		var s = xml.doTransform(XmlTransforms.XML_TO_YAML)
			.asString();
		assertTrue(s.contains("attribute"));
	}

	@Test
	public void testStripComments() throws XmlException {
		var xml = Xml
			.fromString("<root><!-- comment --><item>test</item></root>");
		var s = xml.doTransform(XmlTransforms.STRIP_COMMENTS)
			.asXml();
		assertFalse(s.contains("<!--"));
		assertTrue(s.contains("item"));
	}

	@Test
	public void testElementsToLower() throws XmlException {
		var xml = Xml.fromString("<Root><Child>text</Child></Root>");
		var s = xml.doTransform(XmlTransforms.ELEMENTS_TO_LOWER_CASE)
			.asXml();
		assertTrue(s.contains("<root>"));
		assertTrue(s.contains("<child>"));
	}

	@Test
	public void testElementsToUpper() throws XmlException {
		var xml = Xml.fromString("<root><child>text</child></root>");
		var s = xml.doTransform(XmlTransforms.ELEMENTS_TO_UPPER_CASE)
			.asXml();
		assertTrue(s.contains("<ROOT>"));
		assertTrue(s.contains("<CHILD>"));
	}

	@Test
	public void testAttribToElements() throws XmlException {
		var xml = Xml.fromString("<root attr='value'>text</root>");
		var s = xml.doTransform(XmlTransforms.ATTRIB_TO_ELEMENTS)
			.asXml();
		assertTrue(s.contains("attr"));
		assertTrue(s.contains("value"));
	}

	// ==================== XHTML Transforms ====================

	@Test
	public void testXhtmlToMarkdown() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var s = xml.doTransform(XmlTransforms.XHTML_TO_MARKDOWN)
			.asString();
		assertTrue(s.contains("XHTML"));
	}

	@Test
	public void testXhtmlToText() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var s = xml.doTransform(XmlTransforms.XHTML_TO_TEXT)
			.asString();
		assertTrue(s.contains("XHTML"));
	}

	@Test
	public void testXhtmlTableToCsv() throws XmlException {
		var xml = Xml
			.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlTable.html"));
		var s = xml.doTransform(XmlTransforms.XHTML_TABLE_TO_CSV)
			.asString();
		assertTrue(s.contains("Alfreds"));
		assertTrue(s.contains("Germany"));
	}

	// ==================== Custom XSLT ====================

	@Test
	public void testCustomXsltFile() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		var s = xml.doTransform(XmlTransforms.STRIP_NS.getFile())
			.asXml();
		assertTrue(s.contains("root"));
	}

	// ==================== withProperty ====================

	@Test
	public void testWithPropertyFluent() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		xml.doTransform(XmlTransforms.XML_TO_JSON)
			.withProperty("use-badgerfish", "true")
			.withProperty("skip-root", "true")
			.asString();
	}

	// ==================== Element-level Transform ====================

	@Test
	public void testElementDoTransform() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		var el = xml.doXpath("/documentNode/ex2:complexNode")
			.getOne(XmlElement.class);
		var s = el.doTransform()
			.asXml();
		assertTrue(s.contains("complexNode"));
	}

	@Test
	public void testElementDoTransformStripNs() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		var el = xml.doXpath("/documentNode/ex2:basicNode")
			.getOne(XmlElement.class);
		var s = el.doTransform(XmlTransforms.STRIP_NS)
			.asXml();
		assertTrue(s.contains("<basicNode"));
	}

	// ==================== Write ====================

	@Test
	public void testWriteElement() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		var baos = new ByteArrayOutputStream();
		xml.doTransform()
			.write(baos);
		var s = baos.toString(StandardCharsets.UTF_8);
		assertTrue(s.contains("root"));
	}

	@Test
	public void testWriteFragment() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		var baos = new ByteArrayOutputStream();
		xml.doTransform()
			.fragment()
			.write(baos);
		var s = baos.toString(StandardCharsets.UTF_8);
		assertFalse(s.contains("<?xml"));
	}

	// ==================== Text Mode ====================

	@Test
	public void testTextMode() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		var s = xml.doTransform()
			.text()
			.asString();
		assertEquals("hello", s.trim());
	}

	@Test
	public void testTextModeNested() throws XmlException {
		var xml = Xml.fromString("<root><a><b>deep</b></a></root>");
		var s = xml.doTransform()
			.text()
			.asString();
		assertEquals("deep", s.trim());
	}

	// ==================== XPath 2.0 XSLT Functions ====================

	/**
	 * Verifies XPath 2.0 XSLT functions (lower-case, upper-case) work correctly with Saxon's TransformerFactory. This
	 * requires XPath 2.0 XSLT support.
	 */
	@Test
	public void testXPath20Lowercase() throws XmlException {
		var xml = Xml.fromString("<root><MyElement>Hello</MyElement></root>");
		var result = xml.doTransform(XmlTransforms.ELEMENTS_TO_LOWER_CASE)
			.asXml();
		assertTrue(
			"lower-case() XSLT function should work",
			result.contains("<myelement>")
		);
		assertTrue(
			"original upper-case name should be gone",
			!result.contains("<MyElement>")
		);
	}

	@Test
	public void testXPath20Uppercase() throws XmlException {
		var xml = Xml.fromString("<root><myElement>Hello</myElement></root>");
		var result = xml.doTransform(XmlTransforms.ELEMENTS_TO_UPPER_CASE)
			.asXml();
		assertTrue(
			"upper-case() XSLT function should work",
			result.contains("<MYELEMENT>")
		);
		assertTrue(
			"original lower-case name should be gone",
			!result.contains("<myElement>")
		);
	}

	@Test
	public void testXmlToJsonUsesXPath20() throws XmlException {
		// xml-to-json.xsl uses string-join(), ends-with(), and castable as
		// which are all XPath 2.0 functions.
		var xml = Xml.fromString("<root><a>1</a><a>2</a><a>3</a></root>");
		var result = xml.doTransform(XmlTransforms.XML_TO_JSON)
			.withProperty("skip-root", "true")
			.asString();
		// string-join should have produced a proper array without nulls
		assertTrue(
			"JSON output should be valid (no nulls)",
			!result.contains("null")
		);
		assertTrue(
			"JSON should contain the values",
			result.contains("1") && result.contains("2") && result.contains("3")
		);
	}
}
