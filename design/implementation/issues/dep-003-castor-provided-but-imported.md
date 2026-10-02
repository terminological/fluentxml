---
target-version: 1.2
status: open
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

[IMPACTS](/src/main/java/uk/co/terminological/maven/CastorMojo.java)
[IMPACTS](/src/main/java/uk/co/terminological/maven/XmlToJavaMojo.java)
