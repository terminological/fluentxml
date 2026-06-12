package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Tests for {@link XmlElement#fromUnsafe(Xml, Node)} and the deprecated
 * {@link XmlElement#from(Xml, Node)}.
 *
 * <p>These tests verify that:</p>
 * <ul>
 *   <li>{@code fromUnsafe} correctly accepts element nodes</li>
 *   <li>{@code fromUnsafe} throws {@link NotAnElementException} for text, attribute, and document nodes</li>
 *   <li>The deprecated {@code from(Xml, Node)} still works for elements but will throw ClassCastException for non-elements</li>
 * </ul>
 */
public class TestXmlElementFrom {

	// ==================== fromUnsafe — element node (success) ====================

	@Test
	public void testFromUnsafe_ElementNode_Succeeds() throws XmlException {
		Xml xml = Xml.fromString("<root><item id='1'>hello</item><text>world</text></root>");
		Element domEl = (Element) xml.asDocument().getDocumentElement().getFirstChild();
		XmlElement result = XmlElement.fromUnsafe(xml, domEl);
		assertEquals("item", result.getName());
		assertEquals("1", result.getAttributeValue("id").get());
		assertEquals("hello", result.getTextContent().get());
	}

	@Test
	public void testFromUnsafe_RootElement_Succeeds() throws XmlException {
		Xml xml = Xml.fromString("<root><item>hello</item><text>world</text></root>");
		Element root = xml.asDocument().getDocumentElement();
		XmlElement result = XmlElement.fromUnsafe(xml, root);
		assertEquals("root", result.getName());
		assertEquals(2, result.childElements().size());
	}

	// ==================== fromUnsafe — text node (failure) ====================

	@Test(expected = NotAnElementException.class)
	public void testFromUnsafe_TextNode_Throws() throws XmlException {
		Xml xml = Xml.fromString("<root><text>world</text></root>");
		XmlNode textEl = xml.content().childElements().stream()
			.filter(e -> "text".equals(e.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("text element not found"));
		Node textNode = textEl.getRaw().getFirstChild();
		XmlElement.fromUnsafe(xml, textNode);
	}

	// ==================== fromUnsafe — attribute node (failure) ====================

	@Test(expected = NotAnElementException.class)
	public void testFromUnsafe_AttrNode_Throws() throws XmlException {
		Xml xml = Xml.fromString("<root><item id='1'>hello</item></root>");
		Element domEl = (Element) xml.asDocument().getDocumentElement().getFirstChild();
		Attr attrNode = (Attr) domEl.getAttributes().getNamedItem("id");
		XmlElement.fromUnsafe(xml, attrNode);
	}

	// ==================== fromUnsafe — document node (failure) ====================

	@Test(expected = NotAnElementException.class)
	public void testFromUnsafe_DocumentNode_Throws() throws XmlException {
		Xml xml = Xml.fromString("<root>hello</root>");
		Document doc = xml.asDocument();
		XmlElement.fromUnsafe(xml, doc);
	}

	// ==================== Deprecated from(Xml, Node) — element (still works) ====================

	@SuppressWarnings("deprecation")
	@Test
	public void testDeprecatedFrom_ElementNode_Succeeds() throws XmlException {
		Xml xml = Xml.fromString("<root><item id='1'>hello</item></root>");
		Element domEl = (Element) xml.asDocument().getDocumentElement().getFirstChild();
		XmlElement result = XmlElement.from(xml, domEl);
		assertEquals("item", result.getName());
	}

	// ==================== Deprecated from(Xml, Node) — text node (ClassCastException) ====================

	@SuppressWarnings("deprecation")
	@Test
	public void testDeprecatedFrom_TextNode_ThrowsClassCastException() throws XmlException {
		Xml xml = Xml.fromString("<root><text>world</text></root>");
		XmlNode textEl = xml.content().childElements().stream()
			.filter(e -> "text".equals(e.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("text element not found"));
		Node textNode = textEl.getRaw().getFirstChild();
		try {
			@SuppressWarnings("unused")
			XmlElement result = XmlElement.from(xml, textNode);
			fail("Should have thrown ClassCastException");
		} catch (ClassCastException e) {
			// expected — the old from() does a blind cast to Element
		}
	}

	// ==================== NotAnElementException content ====================

	@Test
	public void testNotAnElementException_MessageContainsNodeType() throws XmlException {
		Xml xml = Xml.fromString("<root><text>world</text></root>");
		XmlNode textEl = xml.content().childElements().stream()
			.filter(e -> "text".equals(e.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("text element not found"));
		Node textNode = textEl.getRaw().getFirstChild();

		try {
			XmlElement.fromUnsafe(xml, textNode);
			fail("Should have thrown NotAnElementException");
		} catch (NotAnElementException e) {
			String msg = e.getMessage();
			assertTrue("Message should mention TEXT_NODE (" + Node.TEXT_NODE + "): " + msg,
				msg.contains(String.valueOf(Node.TEXT_NODE)));
			assertTrue("Message should mention text: " + msg,
				msg.toLowerCase().contains("text"));
		}
	}

	@Test
	public void testNotAnElementException_WithCause() {
		NotAnElementException ex = new NotAnElementException("test", new RuntimeException("root"));
		assertEquals("test", ex.getMessage());
		assertEquals("root", ex.getCause().getMessage());
	}

	// ==================== withAttribute with namespace on element fromUnsafe ====================

	@Test
	public void testFromUnsafe_ElementSupportsFluentAPI() throws XmlException {
		Xml xml = Xml.fromString("<root><item id='1'>hello</item></root>");
		Element domEl = (Element) xml.asDocument().getDocumentElement().getFirstChild();
		XmlElement el = XmlElement.fromUnsafe(xml, domEl);

		java.net.URI ns = java.net.URI.create("http://example.com/ns");
		el.withAttribute("id", ns, "2");
		assertEquals("2", el.getAttributeValue("id").get());
	}
}
