# FluentXML

A fluent Java library for XML manipulation with XPath 2.0 support, XSLT transformation, JAXB binding, and CSS selectors. Also packaged as a Maven plugin for XML-to-Java code generation.

## Maven dependency

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>io.github.terminological</groupId>
    <artifactId>fluentxml</artifactId>
    <version>1.0</version>
</dependency>
```

## Quick Start

```java
import uk.co.terminological.fluentxml.*;

// Create XML from scratch
Xml xml = Xml.create();
XmlElement items = xml.withRoot("root")
    .withChildElement("items");
items.withChildElement("item")
    .withAttribute("id", "1")
    .withText("hello");

// Parse existing XML
Xml xml = Xml.fromFile(new File("data.xml"));
Xml xml = Xml.fromString("<root>text</root>");
Xml xml = Xml.fromHtmlStream(htmlInputStream);  // Auto-clean malformed HTML

// Query with XPath 2.0
XmlElement item = xml.doXpath("/root/item[1]").getOne(XmlElement.class);
XmlAttribute attr = xml.doXpath("/root/item/@id").getOne(XmlAttribute.class);

// Transform
String json = xml.doTransform(XmlTransforms.XML_TO_JSON)
    .withProperty("use-badgerfish", "true")
    .asString();

// Stream traversal
xml.content().stream().forEach(n -> System.out.println(n.getXPath()));
```

## API Overview

### Core Classes

| Class | Description |
|-------|-------------|
| `Xml` | Entry point - factory methods for creating/loading documents, XPath, transforms |
| `XmlNode` | Base class for all node types - common operations like tree walk, XPath gen |
| `XmlElement` | XML elements - building, querying children/attributes, streaming, JAXB |
| `XmlDocElement` | Document root - extends XmlElement with CSS selector support |
| `XmlAttribute` | Element attributes - get/set values, namespace info |
| `XmlText` | Text content and CDATA - value access |
| `XmlList<T>` | Typed, iterable collection with XPath support and Java Streams |
| `XmlXPath<T>` | XPath 2.0 query results - typed/untyped single and multiple accessors |
| `XmlXsl<T>` | XSLT transformation - fluent config for parameters and output format |
| `XmlTransforms` | Built-in transform enums (JSON, YAML, Markdown, CSV, etc.) |
| `XmlException` | Checked exception for parsing, XPath, and transform errors |

### Loading XML

```java
// From file
Xml xml = Xml.fromFile(new File("data.xml"));

// From string
Xml xml = Xml.fromString("<root><item>value</item></root>");

// From byte array
Xml xml = Xml.fromBytes(xmlBytes);

// From input stream (with base URI for resolution)
Xml xml = Xml.fromStream(inputStream, baseUri);

// From input stream with schema validation
Xml xml = Xml.fromFile(new File("data.xml"), new File("schema.xsd"));

// From malformed HTML (auto-cleaned)
Xml xml = Xml.fromHtmlStream(htmlInputStream);

// From JAXB object
MyObject obj = new MyObject();
Xml xml = Xml.fromJAXB(obj);
Xml xml = Xml.fromJAXB(obj, "http://example.com");  // with namespace

// From W3C DOM
Xml xml = Xml.fromDom(domDocument);

// Empty document
Xml xml = Xml.create();
```

### Building XML

```java
Xml xml = Xml.create();

// Set root element
XmlElement root = xml.withRoot("root");

// With namespace
XmlElement root = xml.withRoot("root", URI.create("http://example.com"));

// Fluent child building
XmlElement items = root.withChildElement("items");
items.withChildElement("item")
    .withAttribute("id", "1")
    .withText("value 1");

items.withChildElement("item")
    .withAttribute("id", "2")
    .withText("value 2");

// Multiple attributes
root.withChildElement("order")
    .withAttribute("customerId", "C001")
    .withAttribute("date", Xml.dateTime().toString())
    .withChildElement("item")
        .withAttribute("sku", "SKU-123")
        .withAttribute("qty", "3")
        .withText("Widget")
    .up()  // go back to order
    .withText("Complete");

// Append raw text to element
root.appendText(" raw text ");

// Write output
xml.write(System.out);
System.out.println(xml);  // toString() returns formatted XML
```

### Querying with XPath 2.0

```java
// Simple XPath queries
XmlAttribute attr = xml.doXpath("/root/item/@id").getOne(XmlAttribute.class);
String val = (String) xml.doXpath("string(/root/item/@id)").getOne();

// Optional result
Optional<XmlElement> item = xml.doXpath("/root/item[1]").get(XmlElement.class);
Optional<Object> count = xml.doXpath("count(/root/item)").get();

// Multiple results
XmlList<XmlElement> items = xml.doXpath("//item").getMany(XmlElement.class);
Stream<XmlElement> stream = xml.doXpath("//item").getManyAsStream(XmlElement.class);

// Iteration
for (XmlElement item : items) {
    System.out.println(item.getName() + ": " + item.getValue());
}

// List operations
List<XmlElement> all = items.list();
Optional<XmlElement> first = items.findFirst();

// Namespace-aware queries
Xml xml = Xml.fromStream(is);
XmlAttribute attr = xml.doXpath("/documentNode/ex2:complexNode/@attribute", "ex2")
    .getOne(XmlAttribute.class);

// CSS selectors (document level only)
XmlDocElement doc = Xml.fromHtmlStream(htmlIs).content();
XmlList<XmlElement> links = doc.doCssSelection("a[href]").getMany(XmlElement.class);
XmlList<XmlElement> headings = doc.doCssSelection("h1, h2, h3").getMany(XmlElement.class);
```

### XSLT Transformation

```java
// Identity transform (pretty print)
String xml = xml.doTransform().asXml();

// Text output
String text = xml.doTransform().text().asString();

// Unformatted output
String compact = xml.doTransform().unformatted().asXml();

// Fragment (no XML declaration)
String fragment = xml.doTransform().fragment().asXml();

// Write to file
xml.doTransform().toFile(new File("output.xml"));

// Write to stream
xml.doTransform().write(System.out);

// Transform to new Document
Xml newDoc = xml.doTransform().toDocument();

// Custom XSLT file
String result = xml.doTransform(new File("transform.xsl"))
    .withProperty("param1", "value1")
    .asXml();
```

### Built-in Transforms

| Transform | Description | Key Parameters |
|-----------|-------------|----------------|
| `ELEMENTS_TO_LOWER_CASE` | Lowercase element names | - |
| `ELEMENTS_TO_UPPER_CASE` | Uppercase element names | - |
| `ATTRIB_TO_ELEMENTS` | Convert attributes to elements | - |
| `STRIP_NS` | Remove namespaces | - |
| `STRIP_COMMENTS` | Remove XML comments | - |
| `XML_TO_YAML` | XML → YAML | - |
| `XML_TO_JSON` | XML → JSON | `use-badgerfish`, `use-rabbitfish`, `use-rayfish`, `use-namespaces`, `skip-root`, `jsonp` |
| `XHTML_TO_MARKDOWN` | XHTML → Markdown | `h-style`, `a-style`, `img-style`, `table-style`, `unparseables` |
| `XHTML_TO_TEXT` | XHTML → Plain text | `unparseables` |
| `XHTML_TABLE_TO_CSV` | XHTML tables → CSV | `unparseables` |

Examples:

```java
// XML to JSON with BadgerFish convention
String json = xml.doTransform(XmlTransforms.XML_TO_JSON)
    .withProperty("use-badgerfish", "true")
    .withProperty("use-namespaces", "false")
    .withProperty("skip-root", "true")
    .asString();

// Strip namespaces
String plain = xml.doTransform(XmlTransforms.STRIP_NS).asXml();

// XHTML to Markdown
String md = xml.doTransform(XmlTransforms.XHTML_TO_MARKDOWN).asString();

// Convert table to CSV
String csv = xml.doTransform(XmlTransforms.XHTML_TABLE_TO_CSV).asString();
```

### JAXB Binding

```java
// From object to XML
MyObject obj = new MyObject();
Xml xml = Xml.fromJAXB(obj);
xml.write(System.out);

// From XML to object
MyObject parsed = xml.unmarshalAs(MyObject.class);

// With namespace
Xml xml = Xml.fromJAXB(obj, "http://example.com");

// Round-trip
String s = Xml.fromJAXB(obj).toString();
MyObject roundTrip = Xml.fromString(s).unmarshalAs(MyObject.class);
```

### Tree Walking and Streaming

```java
// Walk entire tree (all node types)
xml.content().walkTree().forEach(n -> System.out.println(n.getXPath()));

// Filter by type
xml.content().walkTree(XmlElement.class).forEach(e -> System.out.println(e.getName()));
xml.content().walkTree(XmlText.class).forEach(t -> System.out.println(t.getValue()));

// Stream API
xml.content().stream()
    .map(e -> e.getXPath())
    .forEach(System.out::println);

// Deep stream from element
xml.content().stream()
    .flatMap(e -> e.streamChildElements())
    .filter(e -> e.getName().equals("item"))
    .forEach(e -> System.out.println(e.getAttributeValue("id")));

// Navigate up
XmlElement parent = child.up();
```

### Type Safety

```java
XmlNode node = ...;

// Type checking
if (node.is(XmlElement.class)) { ... }
if (node.is(XmlText.class)) { ... }

// Checked cast (throws XmlException)
XmlElement el = node.cast(XmlElement.class);

// Unchecked cast (throws RuntimeException)
XmlElement el = node.as(XmlElement.class);

// Get raw DOM
Element raw = element.getAsElement();
Attr rawAttr = attr.getAsAttribute();
Text rawText = text.getAsTextNode();
Document doc = xml.asDocument();
Node rawNode = node.getAsNode();
```

## Maven Plugin

### xmltojava - Generate JAXB from XML

Generates JAXB Java classes from sample XML files by first deriving an XSD schema:

```xml
<plugin>
    <groupId>io.github.terminological</groupId>
    <artifactId>fluentxml</artifactId>
    <version>1.0</version>
    <executions>
        <execution>
            <id>xmltojava</id>
            <phase>generate-sources</phase>
            <goals><goal>xmltojava</goal></goals>
            <configuration>
                <xmlJavaExecutions>
                    <xmlJavaExecution>
                        <inputFile>src/main/resources/sample.xml</inputFile>
                        <outputDirectory>${basedir}/target/generated-sources</outputDirectory>
                        <packageName>com.example.generated</packageName>
                        <forceUpdate>true</forceUpdate>
                    </xmlJavaExecution>
                </xmlJavaExecutions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### castor - Generate XSD from XML

Derives a W3C XML Schema from a sample XML file:

```xml
<plugin>
    <groupId>io.github.terminological</groupId>
    <artifactId>fluentxml</artifactId>
    <version>1.0</version>
    <executions>
        <execution>
            <id>xmltoxsd</id>
            <phase>generate-sources</phase>
            <goals><goal>castor</goal></goals>
            <configuration>
                <inputFile>src/main/resources/sample.xml</inputFile>
                <outputFile>${basedir}/target/sample.xsd</outputFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Project Structure

```
src/main/java/uk/co/terminological/fluentxml/
  Xml.java           - Entry point: factories, parsing, XPath, transforms, JAXB
  XmlNode.java       - Base class: tree walk, type checking, XPath generation
  XmlElement.java    - Elements: building, querying, streaming, JAXB unmarshal
  XmlDocElement.java - Document root: CSS selector support
  XmlAttribute.java  - Attributes: get/set, namespace
  XmlText.java       - Text/CDATA: value access
  XmlList.java       - Typed collection: iteration, XPath, streams
  XmlXPath.java      - XPath 2.0 results: typed/untyped access
  XmlXsl.java        - XSLT: fluent config and execution
  XmlTransforms.java - Built-in transforms enum
  XmlException.java  - Exception class

src/main/java/uk/co/terminological/maven/
  XmlToJavaMojo.java    - Maven plugin: XML → JAXB
  CastorMojo.java       - Maven plugin: XML → XSD
  XmlJavaExecution.java - Plugin configuration object

src/main/resources/xslt/
  elements-to-lower-case.xsl
  elements-to-upper-case.xsl
  attrib-to-elements.xsl
  strip-namespace.xsl
  strip-comments.xsl
  xml-to-yaml.xsl
  xml-to-json.xsl
  xhtml-to-markdown.xsl
  xhtml-to-text.xsl
  xhtml-table-to-csv.xsl
```
