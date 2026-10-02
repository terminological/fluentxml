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

[IMPACTS](/library/src/main/java/uk/co/terminological/fluentxml/Xml.java)
[IMPACTS](/library/src/main/java/uk/co/terminological/fluentxml/XmlElement.java)

## Attempt (2026-10-02)

Tried on branch `jakarta-migration`, from the checkpoint commit on `upgrade-saxon-13`, and it
works: `jakarta.xml.bind-api` 4.0.5, `org.glassfish.jaxb:jaxb-runtime` 4.0.9 (the old
`com.sun.xml.bind:jaxb-impl` is redundant in 4.x and was dropped), moxy 4.0.9, and
`jakarta.activation-api` 2.1.4. Only the imports changed, in `Xml`, `XmlElement` and `TestXml`.
All four artifacts are Java 11 bytecode or older, so the floor survives, and 209/209 tests pass.
`org.eclipse.persistence.oxm.NamespacePrefixMapper` still exists in EclipseLink 4.0.9 core, and
because both call sites use moxy's own `JAXBContextFactory.createContext` with
`JAXBContextProperties.NAMESPACE_PREFIX_MAPPER`, provider choice never depends on service file
merging in the shaded jar.

Costs before this can merge:

* Source breaking for consumers: annotated classes need `jakarta.xml.bind.annotation.*`, and any
  `javax.xml.bind` type in a signature becomes `jakarta.xml.bind`. That means 2.0, plus a README
  migration note.
* Jar grows 14.03 MB to 14.94 MB: jaxb-runtime 4 pulls angus activation and mail service entries
  that the 2.3 line did not have.
* Not attempted here: moxy 5.0.2, which is Java 17.
