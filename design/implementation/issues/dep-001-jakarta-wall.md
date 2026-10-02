---
target-version: 1.2
status: open
tags: dependencies, jakarta, breaking-change
---

# javax to jakarta migration

The 2026-10 dependency refresh stopped at the last `javax.xml.bind` releases: JAXB 2.3.9,
Moxy 2.7.16, `jakarta.activation:jakarta.activation-api:1.2.2` (still the `javax.activation`
package). Everything newer is `jakarta.xml.bind`, which renames the packages that appear in
public signatures, so it is a breaking change for consumers rather than a version bump.

Decision needed before starting: does fluentxml 2.0 drop `javax`, or ship both?

[IMPACTS](/src/main/java/uk/co/terminological/fluentxml/Xml.java)
[IMPACTS](/src/main/java/uk/co/terminological/fluentxml/XmlElement.java)
