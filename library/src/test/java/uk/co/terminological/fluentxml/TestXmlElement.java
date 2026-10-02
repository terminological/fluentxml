package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXmlElement {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {
	}

	// ==================== Factory ====================

	@Test
	public void testFromElement() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		org.w3c.dom.Element domEl = (org.w3c.dom.Element) xml.asDocument().getDocumentElement();
		XmlElement el = XmlElement.from(domEl);
		assertEquals("root", el.getName());
	}

	// ==================== Building ====================

	@Test
	public void testWithChildElement() throws XmlException {
		Xml xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		XmlElement child = root.withChildElement("child");
		assertEquals("child", child.getName());
		assertEquals(1, root.childElements().size());
	}

	@Test
	public void testMultipleChildElements() throws XmlException {
		Xml xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		root.withChildElement("a");
		root.withChildElement("b");
		root.withChildElement("c");
		assertEquals(3, root.childElements().size());
	}

	@Test
	public void testWithAttributeString() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item");
		el.withAttribute("id", "123");
		assertEquals("123", el.getAttributeValue("id").get());
	}

	@Test
	public void testWithText() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item");
		XmlText text = el.withText("hello world");
		assertEquals("hello world", text.getValue());
		assertEquals("hello world", el.getTextContent().get().trim());
	}

	@Test
	public void testAppendText() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item");
		el.appendText("part1").appendText("part2");
		assertEquals("part1part2", el.getTextContent().get());
	}

	@Test

	public void testComplexBuild() throws XmlException {
		Xml xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		XmlElement items = root.withChildElement("items");
		items.withChildElement("item").withAttribute("id", "1").withText("first");
		items.withChildElement("item").withAttribute("id", "2").withText("second");
		root.withChildElement("meta").withAttribute("version", "1.0")
			.withChildElement("name").withText("test");

		// Verify structure
		assertEquals("root", root.getName());
		assertEquals(2, items.childElements().size());

		// Verify attributes
		XmlList<XmlElement> list = items.childElements("item");
		assertEquals("1", list.stream().findFirst().get().getAttributeValue("id").get());
	}

	// ==================== Querying ====================

	@Test
	public void testChildElements() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(5, catalog.childElements().size());
	}

	@Test
	public void testChildElementsFiltered() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(5, catalog.childElements("book").size());
	}

	@Test
	public void testAttributes() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement node = xml.doXpath("/documentNode/basicNode").getOne(XmlElement.class);
		assertEquals(1, node.attributes().size());
	}

	@Test
	public void testGetAttributeValue() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement node = xml.doXpath("/documentNode/basicNode").getOne(XmlElement.class);
		Optional<String> val = node.getAttributeValue("attribute");
		assertTrue(val.isPresent());
		assertEquals("basicValue", val.get());
	}

	@Test
	public void testGetAttributeValueNotFound() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement node = xml.doXpath("/documentNode/basicNode").getOne(XmlElement.class);
		assertFalse(node.getAttributeValue("nonexistent").isPresent());
	}

	// ==================== Streaming ====================

	@Test
	public void testStreamChildElements() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(5, catalog.streamChildElements().count());
	}

	@Test
	public void testStreamTree() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		long count = catalog.stream().filter(e -> e.getName().equals("book")).count();
		assertEquals(5, count);
	}

	@Test
	public void testStreamIncludesSelf() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		long count = catalog.stream().filter(e -> e.getName().equals("catalog")).count();
		assertEquals(1, count);
	}

	// ==================== Namespace ====================

	@Test
	public void testGetNsNoNamespace() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement el = xml.content();
		assertFalse(el.getNs().isPresent());
	}

	@Test
	public void testGetNsWithNamespace() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		XmlElement el = xml.content();
		assertTrue(el.getNs().isPresent());
		assertEquals("http://www.example.com", el.getNs().get().toString());
	}

	// ==================== getName ====================

	@Test
	public void testGetName() throws XmlException {
		Xml xml = Xml.fromString("<root><child>test</child></root>");
		assertEquals("root", xml.content().getName());
		assertEquals("child", xml.doXpath("/root/child").getOne(XmlElement.class).getName());
	}

	// ==================== asElement ====================

	@Test
	public void testAsElement() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		XmlElement el = xml.content();
		assertEquals("root", el.getAsElement().getTagName());
	}

	// ==================== toString ====================

	@Test
	public void testToString() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		String s = xml.content().toString();
		assertTrue(s.contains("root"));
		assertTrue(s.contains("test"));
	}

	// ==================== doTransform ====================

	@Test
	public void testDoTransformBuiltIn() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		XmlElement el = xml.content();
		String result = el.doTransform(XmlTransforms.STRIP_NS).asXml();
		assertTrue(result.contains("<documentNode"));
		assertFalse(result.contains("xmlns:ex2"));
	}

	// ==================== doXpath ====================

	@Test
	public void testDoXpathSelf() throws XmlException {
		Xml xml = Xml.fromString("<root><child>hello</child></root>");
		XmlElement root = xml.content();
		XmlElement child = root.doXpath("./child").getOne(XmlElement.class);
		assertEquals("child", child.getName());
	}

	// ==================== Write ====================

	@Test
	public void testWriteElement() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		XmlElement el = xml.content();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		el.write(baos);
		String output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("test"));
	}
}
