package uk.co.terminological.fluentxml;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXsl {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
	}
	
	@Test
	public void testXsl() throws XmlException {
		
		{
			InputStream is = TestXPath.class.getResourceAsStream("/namespaced.xml");
			Xml xml = Xml.fromStream(is);
			String tmp = xml.doTransform(XmlTransforms.STRIP_NS).asXml();
			assertTrue(tmp.contains("<complexNode attribute=\"complexValue\">"));
		}
		
		{
			InputStream is = TestXPath.class.getResourceAsStream("/namespaced.xml");
			Xml xml = Xml.fromStream(is);
			String tmp2 = xml.doTransform(XmlTransforms.XML_TO_YAML).asString();
			//System.out.print(tmp2);
			assertTrue(tmp2.contains("\n   attribute: complexValue\n"));
		}
		
		{
			InputStream is = TestXPath.class.getResourceAsStream("/namespaced.xml");
			Xml xml = Xml.fromStream(is);
			String tmp2 = xml
					.doTransform(XmlTransforms.XML_TO_JSON)
					//.withProperty("use-rayfish", "true")
					.withProperty("use-badgerfish", "true")
					//.withProperty("use-rabbitfish", "true")
					.withProperty("use-namespaces", "false")
					.withProperty("skip-root", "true")
					.asString();
			//System.out.print(tmp2);
			assertTrue(tmp2.contains("\"@attribute\":\"basicValue\""));
		}
		
		{
			InputStream is = TestXPath.class.getResourceAsStream("/xhtmlExample.html");
			Xml xml = Xml.fromHtmlStream(is);
			String tmp2 = xml
					.doTransform(XmlTransforms.XHTML_TO_MARKDOWN)
					//.withProperty("unparseables", "strip")
					.asString();
			//System.out.print(tmp2);
			assertTrue(tmp2.contains("The registered content type for XHTML is application/xhtml+xml."));
		}
		
		{
			InputStream is = TestXPath.class.getResourceAsStream("/xhtmlExample.html");
			Xml xml = Xml.fromHtmlStream(is);
			String tmp2 = xml
					.doTransform(XmlTransforms.XHTML_TO_TEXT)
					//.withProperty("unparseables", "strip")
					.asString();
			//System.out.print(tmp2);
			assertTrue(tmp2.contains("The registered content type for XHTML is application/xhtml+xml."));
		}
		
		{
			InputStream is = TestXPath.class.getResourceAsStream("/xhtmlTable.html");
			Xml xml = Xml.fromHtmlStream(is);
			String tmp2 = xml
					.doTransform(XmlTransforms.XHTML_TABLE_TO_CSV)
					//.withProperty("unparseables", "strip")
					.asString();
			System.out.print(tmp2);
			assertTrue(tmp2.contains("\"\"\"Ernst\"\" Handel\",Roland Mendel,Austria"));
		}
	}
}
