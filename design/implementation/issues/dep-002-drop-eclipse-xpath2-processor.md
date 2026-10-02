---
target-version: 1.2
status: done
tags: dependencies, xpath, deletion
---

# Drop the Eclipse XPath 2 processor

`com.rackspace.eclipse.webtools.sourceediting:org.eclipse.wst.xml.xpath2.processor:2.1.100`
was last published in 2013 and drags `com.ibm.icu:icu4j:4.6` (2011) into the shaded jar.
It is the XPath engine behind `doXpath`.

Saxon is now a real compile dependency (Saxon-HE 12.10) and does XPath 3.1, so the whole
dependency plus its XPathException plumbing can go. Check behaviour differences first:
the current engine is strict about XPath 2 sequences, Saxon is not identically strict.

[IMPACTS](/library/src/main/java/uk/co/terminological/fluentxml/XmlXPath.java)
[IMPACTS](/library/src/main/java/uk/co/terminological/fluentxml/XmlList.java)

## Resolution (2026-10-02)

Done on branch `upgrade-saxon-13`. `XmlXPath` now compiles through Saxon's JAXP façade
(`net.sf.saxon.xpath.XPathEvaluator`), so DOM in, DOM out, no tree copying, and the expression
language is XPath 3.1 rather than 2.0.

Two things worth knowing if you touch this:

* JAXP exposes no XPath 2 default element namespace, and Saxon does not route the empty prefix
  through the `NamespaceContext`. Unprefixed names therefore bind to the document's default
  namespace via `getStaticContext().setDefaultElementNamespace(...)`, which keeps the old
  behaviour for `namespaced.xml` and the XHTML examples.
* Untyped accessors ask for a return type up front, so `returnType()` reads the statically
  inferred primary type: node set, number, boolean, otherwise string.

The unknown-abbreviation retry survives as a recompile after `deepScanNs()`. Verified: 208/208
tests, `org/eclipse/wst` and `com/ibm/icu` classes are 0 in the shaded jar, jar 20.4 MB to 14.0 MB.
