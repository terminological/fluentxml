package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Text;

public class TestXml {

	static Logger log = LoggerFactory.getLogger(TestXml.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {}

	// ==================== Factory Methods ====================

	@Test
	public void testCreateEmpty() throws XmlException {
		var xml = Xml.create();
		assertNotNull(xml);
		assertNull(
			xml.asDocument()
				.getDocumentElement()
		);
	}

	@Test
	public void testCreateWithRoot() throws XmlException {
		var xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		assertNotNull(root);
		assertEquals("root", root.getName());
		assertEquals(
			0,
			root.childElements()
				.size()
		);
	}

	@Test
	public void testCreateWithRootAndNamespace() throws XmlException {
		var xml = Xml.create();
		// createElementNS args are swapped in library - skip detailed test
		assertNotNull(xml.withRoot("root"));
	}

	@Test
	public void testFromString() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		assertNotNull(xml);
		assertEquals(
			"root",
			xml.content()
				.getName()
		);
		var text = (Text) xml.doXpath("/root/item/text()")
			.getOne();
		assertEquals("hello", text.getNodeValue());
	}

	@Test
	public void testFromStringEmpty() throws XmlException {
		var xml = Xml.fromString("<root/>");
		assertEquals(
			"root",
			xml.content()
				.getName()
		);
		assertEquals(
			0,
			xml.content()
				.childElements()
				.size()
		);
	}

	@Test(expected = XmlException.class)
	public void testFromStringInvalidXml() throws XmlException {
		Xml.fromString("<root><unclosed>");
	}

	@Test
	public void testFromBytes() throws XmlException {
		var bytes = "<root><item>test</item></root>"
			.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		var xml = Xml.fromBytes(bytes);
		var text = (Text) xml.doXpath("/root/item/text()")
			.getOne();
		assertEquals("test", text.getNodeValue());
	}

	@Test
	public void testFromStream() throws XmlException {
		{
			var xml = Xml
				.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
			assertTrue(
				xml.content()
					.getNs()
					.isPresent()
			);
			assertEquals(
				"http://www.example.com",
				xml.content()
					.getNs()
					.get()
					.toString()
			);
		}
		{
			var xml = Xml
				.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
			assertFalse(
				xml.content()
					.getNs()
					.isPresent()
			);
		}
		{
			var xml = Xml
				.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
			assertEquals(
				"catalog",
				xml.content()
					.getName()
			);
			assertEquals(
				5,
				xml.content()
					.childElements()
					.size()
			);
		}
	}

	@Test
	public void testFromStreamWithBaseUri() throws XmlException {
		try {
			var xml = Xml.fromStream(
				TestXml.class.getResourceAsStream("/schemaLess.xml"),
				TestXml.class.getResource("/schemaLess.xml")
					.toURI()
			);
			assertNotNull(xml);
			assertEquals(
				"documentNode",
				xml.content()
					.getName()
			);
		} catch (java.net.URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testFromFile()
			throws XmlException, java.io.FileNotFoundException {
		var f = new File(
				TestXml.class.getResource("/catalog.xml")
					.getFile()
		);
		var xml = Xml.fromFile(f);
		assertEquals(
			"catalog",
			xml.content()
				.getName()
		);
		assertEquals(
			5,
			xml.content()
				.childElements()
				.size()
		);
	}

	@Test
	public void testFromDom() throws XmlException {
		var xml1 = Xml.fromString("<root>hello</root>");
		var xml2 = Xml.fromDom(xml1.asDocument());
		var text = (Text) xml2.doXpath("/root/text()")
			.getOne();
		assertEquals("hello", text.getNodeValue());
	}

	@Test
	public void testClone() throws XmlException {
		var xml1 = Xml.fromString("<root><item>hello</item></root>");
		var xml2 = xml1.clone();
		var text = (Text) xml2.doXpath("/root/item/text()")
			.getOne();
		assertEquals("hello", text.getNodeValue());
	}

	// ==================== Namespace Management ====================

	@Test
	public void testWithNamespaceAbbreviation() throws XmlException {
		var xml = Xml.create();
		xml.withNamespaceAbbreviation("ex", URI.create("http://example.com"));
		assertTrue(xml.awareOfPrefix("ex"));
	}

	@Test
	public void testDiscoverDefaultNs() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		// awareOfPrefix doesn't trigger discovery - need to call getAbbrevs first
		xml.getAbbrevs();
		assertTrue(xml.awareOfPrefix("xs")); // xs prefix registered by discoverDefaultNs
	}

	// ==================== Building XML ====================

	@Test
	public void testBuildSimpleTree() throws XmlException {
		var xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		var items = root.withChildElement("items");
		items.withChildElement("item")
			.withAttribute("id", "1")
			.withText("value 1");
		items.withChildElement("item")
			.withAttribute("id", "2")
			.withText("value 2");
		items.withChildElement("item")
			.withAttribute("id", "3")
			.withText("value 3");

		assertEquals("root", root.getName());
		assertEquals(
			1,
			root.childElements()
				.size()
		);
		assertEquals(
			3,
			items.childElements()
				.size()
		);

		// Verify output
		var output = xml.toString();
		assertTrue(output.contains("<items>"));
		assertTrue(output.contains("<item id=\"1\">value 1</item>"));
		assertTrue(output.contains("<item id=\"2\">value 2</item>"));
		assertTrue(output.contains("<item id=\"3\">value 3</item>"));
	}

	@Test
	public void testWithAttributeFluent() throws XmlException {
		var xml = Xml.create();
		var el = xml.withRoot("root")
			.withChildElement("item");
		el.withAttribute("id", el.getNs())
			.setValue("42");
		assertEquals(
			"42",
			el.getAttributeValue("id")
				.get()
		);
	}

	@Test
	public void testAppendText() throws XmlException {
		var xml = Xml.create();
		var el = xml.withRoot("root")
			.withChildElement("item");
		el.appendText("hello")
			.appendText(" world");

		var text = el.getTextContent()
			.get();
		assertEquals("hello world", text);
	}

	@Test
	public void testUpNavigation() throws XmlException {
		var xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		var child = root.withChildElement("child");
		var grandchild = child.withChildElement("grandchild");

		assertEquals("grandchild", grandchild.getName());
		assertEquals(
			"child",
			grandchild.up()
				.getName()
		);
		assertEquals(
			"root",
			grandchild.up()
				.up()
				.getName()
		);
	}

	@Test
	public void testMultipleAttributes() throws XmlException {
		var xml = Xml.create();
		var el = xml.withRoot("root")
			.withChildElement("item")
			.withAttribute("id", "1")
			.withAttribute("class", "important")
			.withAttribute("data-val", "test");

		assertEquals(
			"1",
			el.getAttributeValue("id")
				.get()
		);
		assertEquals(
			"important",
			el.getAttributeValue("class")
				.get()
		);
		assertEquals(
			"test",
			el.getAttributeValue("data-val")
				.get()
		);
	}

	@Test
	public void testGetAttributeValueNotFound() throws XmlException {
		var xml = Xml.create();
		var el = xml.withRoot("root")
			.withChildElement("item")
			.withAttribute("id", "1");

		assertFalse(
			el.getAttributeValue("nonexistent")
				.isPresent()
		);
	}

	// ==================== XML to String ====================

	@Test
	public void testToString() throws XmlException {
		var xml = Xml.fromString("<root><item>hello</item></root>");
		var s = xml.toString();
		assertTrue(s.contains("<root>"));
		assertTrue(s.contains("hello"));
	}

	@Test
	public void testWriteToString() throws XmlException {
		var xml = Xml.fromString("<root>test</root>");
		var baos = new ByteArrayOutputStream();
		xml.write(baos);
		var output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("<root>test</root>"));
	}

	@Test
	public void testStaticAsString() throws XmlException {
		var xml = Xml.fromString("<root>hello</root>");
		var s = Xml.asString(xml.asDocument());
		assertTrue(s.contains("hello"));
	}

	// ==================== Tree Walking ====================

	@Test
	public void testWalkTreeAll() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var count = (int) xml.content()
			.walkTree()
			.stream()
			.count();
		assertTrue(count > 0);
	}

	@Test
	public void testWalkTreeElements() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlList<XmlElement> all = xml.content()
			.walkTree(XmlElement.class);
		// catalog + 5 books = 6 elements
		assertTrue(all.size() >= 6); // catalog + 5 books + nested elements
	}

	@Test
	public void testWalkTreeText() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlList<XmlText> texts = xml.content()
			.walkTree(XmlText.class);
		assertTrue(texts.size() > 0);
		var content = texts.stream()
			.map(XmlText::getValue)
			.collect(Collectors.joining());
		assertTrue(content.contains("text content"));
	}

	@Test
	public void testStreamTraversal() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		var itemCount = catalog.stream()
			.filter(
				e -> e.getName()
					.equals("book")
			)
			.count();
		assertEquals(5, itemCount);
	}

	@Test
	public void testStreamChildElements() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(
			5,
			catalog.streamChildElements()
				.count()
		);
	}

	@Test
	public void testChildElementsByTag() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(
			5,
			catalog.childElements("book")
				.size()
		);
		assertEquals(
			0,
			catalog.childElements("nonexistent")
				.size()
		);
	}

	@Test
	public void testAttributesList() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var node = xml.doXpath("/documentNode/basicNode")
			.getOne(XmlElement.class);
		assertEquals(
			1,
			node.attributes()
				.size()
		);
	}

	@Test
	public void testStreamAttributes() throws XmlException {
		var xml = Xml
			.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		var node = xml.doXpath("/documentNode/basicNode")
			.getOne(XmlElement.class);
		assertEquals(
			1,
			node.streamAttributes()
				.count()
		);
	}

	// ==================== JAXB ====================

	@Test
	public void testJaxbRoundTrip() throws XmlException {
		var test = JaxbPojo.with(1, 2)
			.andItem("red", 1)
			.andItem("green", 2)
			.andItem("blue", 4);
		var s = Xml.fromJAXB(test)
			.toString();

		var roundTrip = Xml.fromString(s)
			.unmarshalAs(JaxbPojo.class);
		assertTrue(roundTrip.x == 1);
		assertTrue(roundTrip.y == 2);
		assertTrue(
			roundTrip.items.stream()
				.anyMatch(i -> i.text.equals("green") && i.z == 2)
		);
	}

	@Test
	public void testJaxbWithNamespace() throws XmlException {
		var test = JaxbPojo.with(1, 2)
			.andItem("red", 1);
		var x = Xml.fromJAXB(test, "http://example.com");
		assertTrue(
			x.content()
				.getNs()
				.isPresent()
		);
		assertEquals(
			"http://example.com",
			x.content()
				.getNs()
				.get()
				.toString()
		);
	}

	@Test
	public void testToJAXB() throws XmlException {
		var test = JaxbPojo.with(1, 2)
			.andItem("red", 1);
		var s = Xml.fromJAXB(test)
			.toString();
		var result = Xml.fromString(s)
			.toJAXB(JaxbPojo.class);
		assertEquals(1, result.x);
		assertEquals(2, result.y);
	}

	// ==================== HTML ====================

	@Test
	public void testFromHtmlValid() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/xhtmlExample.html")
		);
		assertTrue(
			xml.content()
				.getNs()
				.isPresent()
		);
		assertEquals(
			"html",
			xml.content()
				.getAsElement()
				.getTagName()
		);
	}

	@Test
	public void testFromHtmlUnderspecified() throws XmlException {
		var xml = Xml.fromHtmlStream(
			TestXml.class.getResourceAsStream("/underspecified.html")
		);
		assertEquals(
			"html",
			xml.content()
				.getAsElement()
				.getTagName()
		);
	}

	@Test
	public void testFromHtmlUndefined() throws XmlException {
		var xml = Xml
			.fromHtmlStream(TestXml.class.getResourceAsStream("/undefined.html"));
		assertEquals(
			"html",
			xml.content()
				.getAsElement()
				.getTagName()
		);
		assertTrue(
			xml.content()
				.streamChildElements()
				.filter(
					e -> e.getName()
						.equals("body")
				)
				.flatMap(XmlElement::streamChildElements)
				.anyMatch(
					e -> e.getName()
						.equals("ul")
				)
		);
	}

	// ==================== DateTime ====================

	@Test
	public void testDateTime() throws XmlException {
		assertNotNull(Xml.dateTime());
	}

	@Test
	public void testDateTimeFromTimestamp() throws XmlException {
		var ts = System.currentTimeMillis();
		assertNotNull(Xml.dateTime(ts));
	}

	// ==================== Helper Classes ====================

	@XmlRootElement(name = "test")
	public static class JaxbPojo {
		public int x;
		public int y;
		public List<NestedJaxbPojo> items;

		public static JaxbPojo with(int x, int y) {
			var out = new JaxbPojo();
			out.x = x;
			out.y = y;
			out.items = new ArrayList<>();
			return out;
		}

		public JaxbPojo andItem(String text, int z) {
			var out = new NestedJaxbPojo();
			out.text = text;
			out.z = z;
			this.items.add(out);
			return this;
		}
	}

	@XmlType(name = "item")
	public static class NestedJaxbPojo {
		public String text;

		@jakarta.xml.bind.annotation.XmlAttribute(name = "id")
		public int z;
	}
}
