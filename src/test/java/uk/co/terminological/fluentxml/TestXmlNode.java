package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;

public class TestXmlNode {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}
	@Before
	public void setUp() throws Exception {}
	// ==================== Type Checking ====================
	@Test
	public void testIsType() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		XmlElement el = xml.content();
		assertTrue(el.is(XmlElement.class));
		assertFalse(el.is(XmlText.class));
		assertFalse(el.is(XmlAttribute.class));
	}
	@Test
	public void testCastType() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		XmlNode node = xml.content();
		var casted = node.cast(XmlElement.class);
		assertEquals("root", casted.getName());
	}
	@Test
	public void testAsType() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		XmlNode node = xml.content();
		var casted = node.as(XmlElement.class);
		assertEquals("root", casted.getName());
	}
	@Test
	public void testGetXPathElement() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var el = xml.doXpath("/documentNode/complexNode")
			.getOne(XmlElement.class);
		var xpath = el.getXPath();
		assertTrue(xpath.contains("/documentNode[1]/complexNode[1]"));
	}
	@Test
	public void testGetXPathAttribute() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var attrNode = (Attr) xml.doXpath("/documentNode/complexNode/@attribute")
			.getOne();
		var attr = XmlAttribute.from(attrNode);
		var xpath = attr.getXPath();
		assertTrue(xpath.contains("/documentNode[1]/complexNode[1]/@attribute"));
	}
	@Test
	public void testGetXPathText() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var textNode = (Text) xml.doXpath("/documentNode/basicNode/text()")
			.getOne();
		var text = XmlText.from(textNode);
		var xpath = text.getXPath();
		assertTrue(xpath.contains("/documentNode[1]/basicNode[1]/text()[1]"));
	}
	@Test
	public void testGetXPathSameNameSiblings() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		var first = xml.doXpath("/catalog/book[1]")
			.getOne(XmlElement.class);
		var second = xml.doXpath("/catalog/book[2]")
			.getOne(XmlElement.class);
		var xpath1 = first.getXPath();
		var xpath2 = second.getXPath();
		assertTrue(xpath1.contains("[1]"));
		assertTrue(xpath2.contains("[2]"));
	}
	// ==================== Equality ====================
	@Test
	public void testEquals() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var attrNode1 = (Attr) xml
			.doXpath("/documentNode/complexNode/@attribute")
			.getOne();
		var attrNode2 = (Attr) xml
			.doXpath("/documentNode/complexNode/@attribute")
			.getOne();
		var attr1 = XmlAttribute.from(attrNode1);
		var attr2 = XmlAttribute.from(attrNode2);
		assertTrue(attr1.equals(attr2));
	}
	@Test
	public void testEqualsNull() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement el = xml.content();
		assertFalse(el.equals(null));
	}
	// ==================== Text Content ====================
	@Test
	public void testGetTextContent() throws XmlException {
		var xml = Xml.fromString("<root>hello world</root>");
		XmlElement el = xml.content();
		var text = el.getTextContent();
		assertTrue(text.isPresent());
		assertEquals(
			"hello world",
			text.get()
				.trim()
		);
	}
	@Test
	public void testGetTextContentEmpty() throws XmlException {
		var xml = Xml.fromString("<root/>");
		XmlElement el = xml.content();
		var text = el.getTextContent();
		assertTrue(text.isPresent());
		assertEquals("", text.get());
	}
	// ==================== Outer XML ====================
	@Test
	public void testOuterXml() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var el = xml.doXpath("/documentNode/complexNode")
			.getOne(XmlElement.class);
		var outer = el.outerXml();
		assertTrue(outer.contains("complexNode"));
		assertTrue(outer.contains("attribute=\"complexValue\""));
		assertTrue(outer.contains("child1"));
	}
	// ==================== Tree Walking ====================
	@Test
	public void testWalkTreeDefault() throws XmlException {
		var xml = Xml.fromString("<root><a><b>text</b></a></root>");
		var count = (int) xml.content()
			.walkTree()
			.stream()
			.count();
		assertTrue(count >= 3);
	}
	@Test
	public void testWalkTreeFiltered() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlList<XmlElement> elements = xml.content()
			.walkTree(XmlElement.class);
		List<String> names = elements.stream()
			.map(XmlElement::getName)
			.collect(java.util.stream.Collectors.toList());
		assertTrue(names.contains("catalog"));
		assertTrue(names.contains("book"));
		assertTrue(names.contains("author"));
	}
	@Test
	public void testWalkTreeTextOnly() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlList<XmlText> texts = xml.content()
			.walkTree(XmlText.class);
		assertTrue(texts.size() > 0);
		var allText = texts.stream()
			.map(XmlText::getValue)
			.collect(java.util.stream.Collectors.joining(""));
		assertTrue(allText.contains("XML Developer's Guide"));
	}
	// ==================== Factory Methods ====================
	@Test
	public void testNodeFromXmlElement() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		var domEl = (org.w3c.dom.Element) xml.asDocument()
			.getDocumentElement()
			.getFirstChild();
		var wrapped = XmlNode.from(domEl);
		assertTrue(wrapped.is(XmlElement.class));
		// Node.from on element returns XmlElement
	}
	@Test
	public void testNodeFromXmlText() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		var textNode = (Text) xml.doXpath("/root/text()")
			.getOne();
		var wrapped = XmlNode.from(textNode);
		assertTrue(wrapped.is(XmlText.class));
	}
	@Test
	public void testNodeFromXmlAttribute() throws XmlException {
		var xml = Xml.fromString("<root item='1'/>");
		var attrNode = (Attr) xml.doXpath("/root/@item")
			.getOne();
		var wrapped = XmlNode.from(attrNode);
		assertTrue(wrapped.is(XmlAttribute.class));
	}
	@Test
	public void testNodeFromDocument() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		var wrapped = XmlNode.from(xml.asDocument());
		assertTrue(wrapped.is(XmlDocElement.class));
	}
	// ==================== toString ====================
	@Test
	public void testXmlNodeToString() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var el = xml.doXpath("/documentNode/complexNode")
			.getOne(XmlElement.class);
		var s = el.toString();
		assertTrue(s.contains("complexNode"));
	}
	@Test
	public void testXmlDocToString() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var s = xml.content()
			.toString();
		assertTrue(s.contains("documentNode"));
	}
	// ==================== Write ====================
	@Test
	public void testXmlNodeWrite() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		var baos = new ByteArrayOutputStream();
		xml.content()
			.write(baos);
		var output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("test"));
	}
	// ==================== doTransform on Node ====================
	@Test
	public void testNodeDoTransform() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		XmlElement el = xml.content();
		var output = el.doTransform()
			.asXml();
		assertTrue(output.contains("test"));
	}
	// ==================== doXpath on Node ====================
	@Test
	public void testNodeDoXpath() throws XmlException {
		var xml = Xml.fromString("<root><child>hello</child></root>");
		XmlElement root = xml.content();
		var child = root.doXpath("./child")
			.getOne(XmlElement.class);
		assertEquals("child", child.getName());
	}
	// ==================== Raw Access ====================
	@Test
	public void testGetAsNode() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		XmlElement el = xml.content();
		assertNotNull(el.getAsNode());
	}
	// ==================== Helper ====================
	// private ByteArrayOutputStream baos;
}
