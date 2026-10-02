---
target-version: 1.2
status: done
tags: dependencies, vendoring
---

# Vendor the cssSelector to XPath converter

`com.github.sam-rosenthal:java-cssSelector-to-xpath:V1.0.0RC1` comes from jitpack, is a
release candidate that never became a release, and is used at exactly one call site. It is a
small selector to XPath translation.

Either copy the handful of classes in under our own package, or move selector support to
`javax.xml.xpath` plus a documented limitation list. Deleting the jitpack repository entry
from the pom is part of this issue.

[IMPACTS](/library/src/main/java/uk/co/terminological/fluentxml/XmlDocElement.java)

## Resolution (2026-10-02)

Done on branch `upgrade-saxon-13`. The 9 model and utility classes (714 lines, only `java.util`
and `java.util.regex` imports) are vendored into
[src/main/java/uk/co/terminological/fluentxml/css](/library/src/main/java/uk/co/terminological/fluentxml/css),
with the original MIT attribution in a header on each file. Two source files were cp1252 encoded
with CRLF endings; they were re-encoded to UTF-8 with LF so the build's UTF-8 source encoding
holds. The wicket UI classes in that artifact were not needed and were left out, which is also why
the shaded jar lost its `org.apache.wicket` and jetty service entries.

The jitpack `<repository>` is gone from the pom, so the build no longer reaches jitpack.io. The
vendored converter is reachable: `XmlDocElement.doCssSelection` was commented out by an earlier
over-enthusiastic edit and has been restored, with `testCssSelection` in `TestXmlDocElement`
covering `a[href]` (10 anchors in `xhtmlExample.html`) and `body`.
