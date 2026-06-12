package uk.co.terminological.fluentxml;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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
	public void setUp() throws Exception {
	}
	// ==================== Type Checking ====================
	@Test
	public void testIsType() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		XmlElement el = xml.content();
		assertTrue(el.is(XmlElement.class));
		assertFalse(el.is(XmlText.class));
		assertFalse(el.is(XmlAttribute.class));
	}
	@Test
	public void testCastType() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		XmlNode node = xml.content();
		XmlElement casted = node.cast(XmlElement.class);
		assertEquals("root", casted.getName());
	}
	@Test
	public void testAsType() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		XmlNode node = xml.content();
		XmlElement casted = node.as(XmlElement.class);
		assertEquals("root", casted.getName());
	}
	@Test
	public void testGetXPathElement() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement el = xml.doXpath("/documentNode/complexNode").getOne(XmlElement.class);
		String xpath = el.getXPath();
		assertTrue(xpath.contains("/documentNode[1]/complexNode[1]"));
	}
	@Test
	public void testGetXPathAttribute() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		Attr attrNode = (Attr) xml.doXpath("/documentNode/complexNode/@attribute").getOne();
		XmlAttribute attr = XmlAttribute.from(attrNode);
		String xpath = attr.getXPath();
		assertTrue(xpath.contains("/documentNode[1]/complexNode[1]/@attribute"));
	}
	@Test
	public void testGetXPathText() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		Text textNode = (Text) xml.doXpath("/documentNode/basicNode/text()").getOne();
		XmlText text = XmlText.from(textNode);
		String xpath = text.getXPath();
		assertTrue(xpath.contains("/documentNode[1]/basicNode[1]/text()[1]"));
	}
	@Test
	public void testGetXPathSameNameSiblings() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement first = xml.doXpath("/catalog/book[1]").getOne(XmlElement.class);
		XmlElement second = xml.doXpath("/catalog/book[2]").getOne(XmlElement.class);
		String xpath1 = first.getXPath();
		String xpath2 = second.getXPath();
		assertTrue(xpath1.contains("[1]"));
		assertTrue(xpath2.contains("[2]"));
	}
	// ==================== Equality ====================
	@Test
	public void testEquals() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		Attr attrNode1 = (Attr) xml.doXpath("/documentNode/complexNode/@attribute").getOne();
		Attr attrNode2 = (Attr) xml.doXpath("/documentNode/complexNode/@attribute").getOne();
		XmlAttribute attr1 = XmlAttribute.from(attrNode1);
		XmlAttribute attr2 = XmlAttribute.from(attrNode2);
		assertTrue(attr1.equals(attr2));
	}
	@Test
	public void testEqualsNull() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement el = xml.content();
		assertFalse(el.equals(null));
	}
	// ==================== Text Content ====================
	@Test
	public void testGetTextContent() throws XmlException {
		Xml xml = Xml.fromString("<root>hello world</root>");
		XmlElement el = xml.content();
		Optional<String> text = el.getTextContent();
		assertTrue(text.isPresent());
		assertEquals("hello world", text.get().trim());
	}
	@Test
	public void testGetTextContentEmpty() throws XmlException {
		Xml xml = Xml.fromString("<root/>");
		XmlElement el = xml.content();
		Optional<String> text = el.getTextContent();
		assertTrue(text.isPresent());
		assertEquals("", text.get());
	}
	// ==================== Outer XML ====================
	@Test
	public void testOuterXml() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement el = xml.doXpath("/documentNode/complexNode").getOne(XmlElement.class);
		String outer = el.outerXml();
		assertTrue(outer.contains("complexNode"));
		assertTrue(outer.contains("attribute=\"complexValue\""));
		assertTrue(outer.contains("child1"));
	}
	// ==================== Tree Walking ====================
	@Test
	public void testWalkTreeDefault() throws XmlException {
		Xml xml = Xml.fromString("<root><a><b>text</b></a></root>");
		int count = (int) xml.content().walkTree().stream().count();
		assertTrue(count >= 3);
	}
	@Test
	public void testWalkTreeFiltered() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlList<XmlElement> elements = xml.content().walkTree(XmlElement.class);
		List<String> names = elements.stream().map(XmlElement::getName).collect(java.util.stream.Collectors.toList());
		assertTrue(names.contains("catalog"));
		assertTrue(names.contains("book"));
		assertTrue(names.contains("author"));
	}
	@Test
	public void testWalkTreeTextOnly() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlList<XmlText> texts = xml.content().walkTree(XmlText.class);
		assertTrue(texts.size() > 0);
		String allText = texts.stream().map(XmlText::getValue).collect(java.util.stream.Collectors.joining(""));
		assertTrue(allText.contains("XML Developer's Guide"));
	}
	// ==================== Factory Methods ====================
	@Test
	public void testNodeFromXmlElement() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item></root>");
		org.w3c.dom.Element domEl = (org.w3c.dom.Element) xml.asDocument().getDocumentElement().getFirstChild();
		XmlNode wrapped = XmlNode.from(domEl);
		assertTrue(wrapped.is(XmlElement.class));
		// Node.from on element returns XmlElement
	}
	@Test
	public void testNodeFromXmlText() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Text textNode = (Text) xml.doXpath("/root/text()").getOne();
		XmlNode wrapped = XmlNode.from(textNode);
		assertTrue(wrapped.is(XmlText.class));
	}
	@Test
	public void testNodeFromXmlAttribute() throws XmlException {
		Xml xml = Xml.fromString("<root item='1'/>");
		Attr attrNode = (Attr) xml.doXpath("/root/@item").getOne();
		XmlNode wrapped = XmlNode.from(attrNode);
		assertTrue(wrapped.is(XmlAttribute.class));
	}
	@Test
	public void testNodeFromDocument() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		XmlNode wrapped = XmlNode.from(xml.asDocument());
		assertTrue(wrapped.is(XmlDocElement.class));
	}
	// ==================== toString ====================
	@Test
	public void testXmlNodeToString() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		XmlElement el = xml.doXpath("/documentNode/complexNode").getOne(XmlElement.class);
		String s = el.toString();
		assertTrue(s.contains("complexNode"));
	}
	@Test
	public void testXmlDocToString() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/schemaLess.xml"));
		String s = xml.content().toString();
		assertTrue(s.contains("documentNode"));
	}
	// ==================== Write ====================
	@Test
	public void testXmlNodeWrite() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		xml.content().write(baos);
		String output = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(output.contains("test"));
	}
	// ==================== doTransform on Node ====================
	@Test
	public void testNodeDoTransform() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		XmlElement el = xml.content();
		String output = el.doTransform().asXml();
		assertTrue(output.contains("test"));
	}
	// ==================== doXpath on Node ====================
	@Test
	public void testNodeDoXpath() throws XmlException {
		Xml xml = Xml.fromString("<root><child>hello</child></root>");
		XmlElement root = xml.content();
		XmlElement child = root.doXpath("./child").getOne(XmlElement.class);
		assertEquals("child", child.getName());
	}
	// ==================== Raw Access ====================
	@Test
	public void testGetAsNode() throws XmlException {
		Xml xml = Xml.fromString("<root>test</root>");
		XmlElement el = xml.content();
		assertNotNull(el.getAsNode());
	}
	// ==================== Helper ====================
	private ByteArrayOutputStream baos;
}
