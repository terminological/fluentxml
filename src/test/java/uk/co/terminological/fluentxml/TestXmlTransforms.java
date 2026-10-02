package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;

public class TestXmlTransforms {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}
	@Before
	public void setUp() throws Exception {}
	// ==================== Enum Values ====================
	public void testEnumCount() throws XmlException {
		assertEquals(10, XmlTransforms.values().length);
	}
	public void testAllTransformsHaveFiles() throws XmlException {
		for (XmlTransforms t : XmlTransforms.values()) {
			var f = t.getFile();
			assertNotNull("Transform " + t.name() + " should have a file", f);
			assertTrue("Transform " + t.name() + " file should exist", f.exists());
		}
	}
	// ==================== Individual Transforms ====================
	public void testElementsToLower() throws XmlException {
		var xml = Xml.fromString("<Root><Child>text</Child></Root>");
		var s = xml.doTransform(XmlTransforms.ELEMENTS_TO_LOWER_CASE)
			.asXml();
		assertTrue(s.contains("<root>"));
		assertTrue(s.contains("<child>"));
		assertFalse(s.contains("<Root>"));
	}
	public void testElementsToUpper() throws XmlException {
		var xml = Xml.fromString("<root><child>text</child></root>");
		var s = xml.doTransform(XmlTransforms.ELEMENTS_TO_UPPER_CASE)
			.asXml();
		assertTrue(s.contains("<ROOT>"));
		assertTrue(s.contains("<CHILD>"));
		assertFalse(s.contains("<root>"));
	}
	public void testStripNs() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		var s = xml.doTransform(XmlTransforms.STRIP_NS)
			.asXml();
		assertTrue(s.contains("<documentNode"));
		assertTrue(s.contains("<complexNode"));
		assertFalse(s.contains("xmlns"));
		assertFalse(s.contains("ex2:"));
	}
	public void testStripComments() throws XmlException {
		var xml = Xml
			.fromString("<root><!-- comment --><item>test</item></root>");
		var s = xml.doTransform(XmlTransforms.STRIP_COMMENTS)
			.asXml();
		assertFalse(s.contains("<!--"));
		assertFalse(s.contains("-->"));
		assertTrue(s.contains("item"));
	}
	public void testXmlToJsonBadgerfish() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var s = xml.doTransform(XmlTransforms.XML_TO_JSON)
			.withProperty("use-badgerfish", "true")
			.withProperty("skip-root", "true")
			.asString();
		assertTrue(s.contains("@attribute"));
		assertTrue(s.contains("basicValue"));
	}
	public void testXmlToJsonSkipRoot() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var s = xml.doTransform(XmlTransforms.XML_TO_JSON)
			.withProperty("skip-root", "true")
			.asString();
		assertFalse(
			s.trim()
				.startsWith("\"documentNode\"")
		);
	}
	public void testXmlToYaml() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var s = xml.doTransform(XmlTransforms.XML_TO_YAML)
			.asString();
		assertTrue(s.contains("documentNode"));
		assertTrue(s.contains("basicNode"));
		assertTrue(s.contains("basicValue"));
	}
	public void testAttribToElements() throws XmlException {
		var xml = Xml.fromString("<root attr='value'>text</root>");
		var s = xml.doTransform(XmlTransforms.ATTRIB_TO_ELEMENTS)
			.asXml();
		assertTrue(s.contains("attr"));
		assertTrue(s.contains("value"));
	}
	public void testXhtmlToMarkdown() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var s = xml.doTransform(XmlTransforms.XHTML_TO_MARKDOWN)
			.asString();
		assertTrue(s.contains("XHTML"));
	}
	public void testXhtmlToText() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		var s = xml.doTransform(XmlTransforms.XHTML_TO_TEXT)
			.asString();
		assertTrue(s.contains("XHTML"));
	}
	public void testXhtmlTableToCsv() throws XmlException {
		var xml = Xml
			.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlTable.html"));
		var s = xml.doTransform(XmlTransforms.XHTML_TABLE_TO_CSV)
			.asString();
		assertTrue(s.contains("Alfreds"));
		assertTrue(s.contains("Maria"));
		assertTrue(s.contains("Germany"));
	}
	// ==================== Transform File Access ====================
	public void testGetFile() throws XmlException {
		var f = XmlTransforms.STRIP_NS.getFile();
		assertNotNull(f);
		assertTrue(f.exists());
	}
	public void testGetFileJson() throws XmlException {
		var f = XmlTransforms.XML_TO_JSON.getFile();
		assertNotNull(f);
		assertTrue(f.exists());
	}
	public void testGetFileMarkdown() throws XmlException {
		var f = XmlTransforms.XHTML_TO_MARKDOWN.getFile();
		assertNotNull(f);
		assertTrue(f.exists());
	}
}
