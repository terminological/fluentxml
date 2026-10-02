---
target-version: 2.0
status: open
tags: maven-plugin, castor, jdk
---

# Castor goals cannot run on modern JDKs

Found while verifying [dep-003](dep-003-castor-provided-but-imported.md). With the module split
in place the `castor` goal finally loads castor, then fails on JDK 9 and later:

```
Could not instantiate serializer com.sun.org.apache.xml.internal.serialize.XMLSerializer:
java.lang.IllegalAccessException: class org.exolab.castor.xml.XercesJDK5Serializer cannot access
class com.sun.org.apache.xml.internal.serialize.XMLSerializer (in module java.xml) because
module java.xml does not export com.sun.org.apache.xml.internal.serialize to unnamed module
```

Castor 1.3.1 reaches into a JDK internal package that is no longer exported. Tried and rejected:
`-Dorg.exolab.castor.serializer=org.exolab.castor.xml.XercesSerializer` with `xercesImpl` 2.12.2
on the plugin classpath, which castor ignored, still selecting the JDK internal serialiser. Castor
has been dead upstream since 1.4.1 (2016), so nothing newer fixes this.

Three ways out, in rising order of effort:

1. Document `.mvn/jvm.config` with `--add-exports java.xml/com.sun.org.apache.xml.internal.serialize=ALL-UNNAMED`
   as a requirement for users of these goals. Fragile, and pushes the problem to consumers.
2. Stop using castor's serialiser in `CastorMojo` and write the derived `Schema` ourselves.
3. Drop the castor dependency and the `castor` goal, or swap instance-to-schema derivation for a
   maintained tool.

Related rot in the same module: `XmlToJavaMojo` invokes `org.apache.cxf:cxf-xjc-plugin:2.6.0` and
`org.codehaus.mojo:build-helper-maven-plugin:1.9.1`, both from around 2012, so the `xmltojava`
goal probably has its own JDK problems waiting behind this one.

[IMPACTS](/maven-plugin/src/main/java/uk/co/terminological/maven/CastorMojo.java)
[IMPACTS](/maven-plugin/src/main/java/uk/co/terminological/maven/XmlToJavaMojo.java)
