/**
 * 
 */
package uk.co.terminological.fluentxml;

import java.io.File;
import java.net.URL;

/**
 * @author rchallen
 *
 */
public enum XmlTransforms {
	
	ELEMENTS_TO_LOWER_CASE ("xslt/elements-to-lower-case.xsl"),
	ELEMENTS_TO_UPPER_CASE ("xslt/elements-to-upper-case.xsl"),
	ATTRIB_TO_ELEMENTS ("xslt/attrib-to-elements.xsl"),
	STRIP_NS ("xslt/strip-namespace.xsl"),
	STRIP_COMMENTS ("xslt/strip-comments.xsl"),
	XML_TO_YAML ("xslt/xml-to-yaml.xsl"),
	XML_TO_JSON ("xslt/xml-to-json.xsl"),
	XHTML_TO_MARKDOWN ("xslt/xhtml-to-markdown.xsl"),
	XHTML_TO_TEXT ("xslt/xhtml-to-text.xsl"),
	XHTML_TABLE_TO_CSV ("xslt/xhtml-table-to-csv.xsl"),
	;
	
	File inFile;
	XmlTransforms(String filename) {
		URL resourceFile = XmlTransforms.class.getClassLoader().getResource(filename);
		inFile = new File(resourceFile.getFile());
	}
	
	public File getFile() {return inFile;}
}
