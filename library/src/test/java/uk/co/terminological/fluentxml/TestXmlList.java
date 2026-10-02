package uk.co.terminological.fluentxml;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXmlList {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {
	}

	// ==================== Creation from Elements ====================

	@Test
	public void testCreateChildElements() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		XmlList<XmlElement> children = catalog.childElements();
		assertEquals(5, children.size());
	}

	@Test
	public void testCreateChildElementsFiltered() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		XmlList<XmlElement> books = catalog.childElements("book");
		assertEquals(5, books.size());
	}

	@Test
	public void testCreateEmptyChildElements() throws XmlException {
		Xml xml = Xml.fromString("<root/>");
		XmlElement root = xml.content();
		XmlList<XmlElement> children = root.childElements();
		assertEquals(0, children.size());
	}

	// ==================== Iteration ====================

	@Test
	public void testIterator() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		XmlList<XmlElement> books = catalog.childElements("book");
		int count = 0;
		for (XmlElement book : books) {
			count++;
			assertEquals("book", book.getName());
		}
		assertEquals(5, count);
	}

	// ==================== List access ====================

	@Test
	public void testSize() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		assertEquals(5, catalog.childElements("book").size());
	}

	@Test
	public void testList() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		List<XmlElement> list = catalog.childElements("book").list();
		assertEquals(5, list.size());
	}

	@Test
	public void testFindFirst() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		Optional<XmlElement> first = catalog.childElements("book").findFirst();
		assertTrue(first.isPresent());
		assertEquals("bk101", first.get().getAttributeValue("id").get());
	}

	@Test
	public void testFindFirstEmpty() {
		try {
			Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
			XmlElement catalog = xml.content();
			Optional<XmlElement> first = catalog.childElements("nonexistent").findFirst();
			assertFalse(first.isPresent());
		} catch (XmlException e) {
			fail("Unexpected: " + e.getMessage());
		}
	}

	// ==================== Stream ====================

	@Test
	public void testStream() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		long count = catalog.childElements("book").stream().count();
		assertEquals(5, count);
	}

	@Test
	public void testStreamMap() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement catalog = xml.content();
		List<String> ids = catalog.childElements("book").stream()
				.map(b -> b.getAttributeValue("id").get())
				.collect(Collectors.toList());
		assertEquals(5, ids.size());
		assertTrue(ids.contains("bk101"));
		assertTrue(ids.contains("bk105"));
	}

	// ==================== From XPath ====================

	@Test
	public void testFromXPathGetMany() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlList<XmlElement> books = xml.doXpath("//book").getMany(XmlElement.class);
		assertEquals(5, books.size());
	}

	@Test
	public void testFromXPathGetOne() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		XmlElement book = xml.doXpath("/catalog/book[1]").getOne(XmlElement.class);
		assertEquals("bk101", book.getAttributeValue("id").get());
	}

	@Test
	public void testFromXPathOptionalGet() throws XmlException {
		Xml xml = Xml.fromStream(TestXml.class.getResourceAsStream("/catalog.xml"));
		Optional<XmlElement> book = xml.doXpath("/catalog/book[1]").get(XmlElement.class);
		assertTrue(book.isPresent());
		assertEquals("bk101", book.get().getAttributeValue("id").get());

		Optional<XmlElement> missing = xml.doXpath("/catalog/missing").get(XmlElement.class);
		assertFalse(missing.isPresent());
	}

	// ==================== Empty List ====================

	@Test
	public void testEmptyList() throws XmlException {
		Xml xml = Xml.fromString("<root/>");
		XmlElement root = xml.content();
		XmlList<XmlElement> empty = root.childElements();
		assertEquals(0, empty.size());
		assertFalse(empty.findFirst().isPresent());
		assertEquals(0, empty.stream().count());
	}
}
