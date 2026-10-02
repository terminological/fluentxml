---
target-version: 2.0
status: done
tags: dependencies, maven-plugin, bug
---

# Castor is provided but imported by main code

Both mojos import `org.exolab.castor.xml.schema.Schema` and `XMLInstance2Schema`, castor is
declared `provided`, and `org/exolab/castor` contributes 0 classes to the shaded jar. So any
consumer that invokes the `castor` or `xmltojava` goals through the published artifact gets a
`NoClassDefFoundError`. Castor itself is dead upstream (1.4.1, 2016).

Two ways out: bundle castor in the plugin jar, or move the mojos to their own module so the
library artifact stops carrying them. Also check whether `castor-xml` is needed at all, only
the `castor-xml-schema` classes are imported.

[IMPACTS](/maven-plugin/src/main/java/uk/co/terminological/maven/CastorMojo.java)
[IMPACTS](/maven-plugin/src/main/java/uk/co/terminological/maven/XmlToJavaMojo.java)

## Resolution (2026-10-02)

Fixed by the module split on branch `split-maven-plugin`. The two mojos moved to the
`maven-plugin` module (artifact `fluentxml-maven-plugin`), where castor is declared at
**compile** scope instead of `provided`, so Maven resolves it when the plugin runs. Only
`castor-xml-schema` is declared, because that is the only artifact the mojos import
(`org.exolab.castor.xml.schema.*`); it brings `castor-xml` and `castor-core` transitively. The
library artifact no longer contains any of: mojos, `org/apache/maven`, castor, commons-io or
mojo-executor.

Proven rather than assumed: running `io.github.terminological:fluentxml-maven-plugin:1.1-SNAPSHOT:castor`
in a scratch project got inside castor's own serialiser, where it used to die with
`NoClassDefFoundError`. What it now hits is a different, older problem, tracked as
[dep-007](dep-007-castor-goals-broken-on-modern-jdks.md).
