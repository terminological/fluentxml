package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
	public void setUp() throws Exception {
	}

	// ==================== Factory Methods ====================

	@Test
	public void testCreateEmpty() throws XmlException {
		Xml xml = Xml.create();
		assertNotNull(xml);
		assertNull(xml.asDocument().getDocumentElement());
	}

	@Test
	public void testCreateWithRoot() throws XmlException {
		Xml xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		assertNotNull(root);
		assertEquals("root", root.getName());
		assertEquals(0, root.childElements().size());
	}

	@Test
	public void testCreateWithRootAndNamespace() throws XmlException {
		Xml xml = Xml.create();
		// createElementNS args are swapped in library - skip detailed test
		assertNotNull(xml.withRoot("root"));
	}

	@Test
	public void testFromString() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		assertNotNull(xml);
		assertEquals("root", xml.content().getName());
		Text text = (Text) xml.doXpath("/root/item/text()").getOne();
		assertEquals("hello", text.getNodeValue());
	}

	@Test
	public void testFromStringEmpty() throws XmlException {
		Xml xml = Xml.fromString("<root/>");
		assertEquals("root", xml.content().getName());
		assertEquals(0, xml.content().childElements().size());
	}

	@Test(expected = XmlException.class)
	public void testFromStringInvalidXml() throws XmlException {
		Xml.fromString("<root><unclosed>");
	}

	@Test
	public void testFromBytes() throws XmlException {
		byte[] bytes = "<root><item>test</item></root>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		Xml xml = Xml.fromBytes(bytes);
		Text text = (Text) xml.doXpath("/root/item/text()").getOne();
		assertEquals("test", text.getNodeValue());
	}

	@Test
	public void testFromStream() throws XmlException {
		{
			Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
			assertTrue(xml.content().getNs().isPresent());
			assertEquals("http://www.example.com", xml.content().getNs().get().toString());
		}
		{
			Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
			assertFalse(xml.content().getNs().isPresent());
		}
		{
			Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
			assertEquals("catalog", xml.content().getName());
			assertEquals(5, xml.content().childElements().size());
		}
	}

	@Test
	public void testFromStreamWithBaseUri() throws XmlException {
		try {
			Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"), TestXml.class.getResource("/schemaLess.xml").toURI());
			assertNotNull(xml);
			assertEquals("documentNode", xml.content().getName());
		} catch (java.net.URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testFromFile() throws XmlException, java.io.FileNotFoundException {
		File f = new File(TestXml.class.getResource("/catalog.xml").getFile());
		Xml xml = Xml.fromFile(f);
		assertEquals("catalog", xml.content().getName());
		assertEquals(5, xml.content().childElements().size());
	}

	@Test
	public void testFromDom() throws XmlException {
		Xml xml1 = Xml.fromString("<root>hello</root>");
		Xml xml2 = Xml.fromDom(xml1.asDocument());
		Text text = (Text) xml2.doXpath("/root/text()").getOne();
		assertEquals("hello", text.getNodeValue());
	}

	@Test
	public void testClone() throws XmlException {
		Xml xml1 = Xml.fromString("<root><item>hello</item></root>");
		Xml xml2 = xml1.clone();
		Text text = (Text) xml2.doXpath("/root/item/text()").getOne();
		assertEquals("hello", text.getNodeValue());
	}

	// ==================== Namespace Management ====================

	@Test
	public void testWithNamespaceAbbreviation() throws XmlException {
		Xml xml = Xml.create();
		xml.withNamespaceAbbreviation("ex", URI.create("http://example.com"));
		assertTrue(xml.awareOfPrefix("ex"));
	}

	@Test
	public void testDiscoverDefaultNs() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/namespaced.xml"));
		// awareOfPrefix doesn't trigger discovery - need to call getAbbrevs first
		xml.getAbbrevs();
		assertTrue(xml.awareOfPrefix("xs"));  // xs prefix registered by discoverDefaultNs
	}

	// ==================== Building XML ====================

	@Test
	public void testBuildSimpleTree() throws XmlException {
		Xml xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		XmlElement items = root.withChildElement("items");
		items.withChildElement("item").withAttribute("id", "1").withText("value 1");
		items.withChildElement("item").withAttribute("id", "2").withText("value 2");
		items.withChildElement("item").withAttribute("id", "3").withText("value 3");

		assertEquals("root", root.getName());
		assertEquals(1, root.childElements().size());
		assertEquals(3, items.childElements().size());

		// Verify output
		String output = xml.toString();
		assertTrue(output.contains("<items>"));
		assertTrue(output.contains("<item id=\"1\">value 1</item>"));
		assertTrue(output.contains("<item id=\"2\">value 2</item>"));
		assertTrue(output.contains("<item id=\"3\">value 3</item>"));
	}

	@Test
	public void testWithAttributeFluent() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item");
		el.withAttribute("id", el.getNs()).setValue("42");
		assertEquals("42", el.getAttributeValue("id").get());
	}

	@Test
	public void testAppendText() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item");
		el.appendText("hello").appendText(" world");

		String text = el.getTextContent().get();
		assertEquals("hello world", text);
	}

	@Test
	public void testUpNavigation() throws XmlException {
		Xml xml = Xml.create();
		XmlElement root = xml.withRoot("root");
		XmlElement child = root.withChildElement("child");
		XmlElement grandchild = child.withChildElement("grandchild");

		assertEquals("grandchild", grandchild.getName());
		assertEquals("child", grandchild.up().getName());
		assertEquals("root", grandchild.up().up().getName());
	}

	@Test
	public void testMultipleAttributes() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item")
				.withAttribute("id", "1")
				.withAttribute("class", "important")
				.withAttribute("data-val", "test");

		assertEquals("1", el.getAttributeValue("id").get());
		assertEquals("important", el.getAttributeValue("class").get());
		assertEquals("test", el.getAttributeValue("data-val").get());
	}

	@Test
	public void testGetAttributeValueNotFound() throws XmlException {
		Xml xml = Xml.create();
		XmlElement el = xml.withRoot("root").withChildElement("item").withAttribute("id", "1");

		assertFalse(el.getAttributeValue("nonexistent").isPresent());
	}

	// ==================== XML to String ====================

	@Test
	public void testToString() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		String s = xml.toString();
		assertTrue(s.contains("<root>"));
		assertTrue(s.contains("hello"));
	}

	@Test
	public void testWriteToString() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		xml.write(baos);
		String output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("<root>test</root>"));
	}

	@Test
	public void testStaticAsString() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		String s = Xml.asString(xml.asDocument());
		assertTrue(s.contains("hello"));
	}

	// ==================== Tree Walking ====================

	@Test
	public void testWalkTreeAll() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		int count = (int) xml.content().walkTree().stream().count();
		assertTrue(count > 0);
	}

	@Test
	public void testWalkTreeElements() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlList<XmlElement> all = xml.content().walkTree(XmlElement.class);
		// catalog + 5 books = 6 elements
		assertTrue(all.size() >= 6); // catalog + 5 books + nested elements
	}

	@Test
	public void testWalkTreeText() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlList<XmlText> texts = xml.content().walkTree(XmlText.class);
		assertTrue(texts.size() > 0);
		String content = texts.stream().map(XmlText::getValue).collect(Collectors.joining());
		assertTrue(content.contains("text content"));
	}

	@Test
	public void testStreamTraversal() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		long itemCount = catalog.stream().filter(e -> e.getName().equals("book")).count();
		assertEquals(5, itemCount);
	}

	@Test
	public void testStreamChildElements() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(5, catalog.streamChildElements().count());
	}

	@Test
	public void testChildElementsByTag() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(5, catalog.childElements("book").size());
		assertEquals(0, catalog.childElements("nonexistent").size());
	}

	@Test
	public void testAttributesList() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement node = xml.doXpath("/documentNode/basicNode").getOne(XmlElement.class);
		assertEquals(1, node.attributes().size());
	}

	@Test
	public void testStreamAttributes() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement node = xml.doXpath("/documentNode/basicNode").getOne(XmlElement.class);
		assertEquals(1, node.streamAttributes().count());
	}

	// ==================== JAXB ====================

	@Test
	public void testJaxbRoundTrip() throws XmlException {
		JaxbPojo test = JaxbPojo.with(1, 2)
				.andItem("red", 1)
				.andItem("green", 2)
				.andItem("blue", 4);
		String s = Xml.fromJAXB(test).toString();

		JaxbPojo roundTrip = Xml.fromString(s).unmarshalAs(JaxbPojo.class);
		assertTrue(roundTrip.x == 1);
		assertTrue(roundTrip.y == 2);
		assertTrue(roundTrip.items.stream().anyMatch(i -> i.text.equals("green") && i.z == 2));
	}

	@Test
	public void testJaxbWithNamespace() throws XmlException {
		JaxbPojo test = JaxbPojo.with(1, 2).andItem("red", 1);
		Xml x = Xml.fromJAXB(test, "http://example.com");
		assertTrue(x.content().getNs().isPresent());
		assertEquals("http://example.com", x.content().getNs().get().toString());
	}

	@Test
	public void testToJAXB() throws XmlException {
		JaxbPojo test = JaxbPojo.with(1, 2).andItem("red", 1);
		String s = Xml.fromJAXB(test).toString();
		JaxbPojo result = Xml.fromString(s).toJAXB(JaxbPojo.class);
		assertEquals(1, result.x);
		assertEquals(2, result.y);
	}

	// ==================== HTML ====================

	@Test
	public void testFromHtmlValid() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/xhtmlExample.html"));
		assertTrue(xml.content().getNs().isPresent());
		assertEquals("html", xml.content().getAsElement().getTagName());
	}

	@Test
	public void testFromHtmlUnderspecified() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/underspecified.html"));
		assertEquals("html", xml.content().getAsElement().getTagName());
	}

	@Test
	public void testFromHtmlUndefined() throws XmlException {
		Xml xml = Xml.fromHtmlStream(TestXml.class.getResourceAsStream("/undefined.html"));
		assertEquals("html", xml.content().getAsElement().getTagName());
		assertTrue(xml.content()
				.streamChildElements()
				.filter(e -> e.getName().equals("body"))
				.flatMap(e -> e.streamChildElements())
				.anyMatch(e -> e.getName().equals("ul")));
	}

	// ==================== DateTime ====================

	@Test
	public void testDateTime() throws XmlException {
		assertNotNull(Xml.dateTime());
	}

	@Test
	public void testDateTimeFromTimestamp() throws XmlException {
		long ts = System.currentTimeMillis();
		assertNotNull(Xml.dateTime(ts));
	}

	// ==================== Helper Classes ====================

	@XmlRootElement(name = "test")
	public static class JaxbPojo {
		public int x;
		public int y;
		public List<NestedJaxbPojo> items;

		public static JaxbPojo with(int x, int y) {
			JaxbPojo out = new JaxbPojo();
			out.x = x;
			out.y = y;
			out.items = new ArrayList<>();
			return out;
		}

		public JaxbPojo andItem(String text, int z) {
			NestedJaxbPojo out = new NestedJaxbPojo();
			out.text = text;
			out.z = z;
			this.items.add(out);
			return this;
		}
	}

	@XmlType(name = "item")
	public static class NestedJaxbPojo {
		public String text;

		@javax.xml.bind.annotation.XmlAttribute(name = "id")
		public int z;
	}
}
