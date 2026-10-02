---
target-version: 2.0
status: open
tags: dependencies, saxon, java
---

# Move to Saxon 13 when Java 17 becomes the floor

`net.sf.saxon:Saxon-HE` sits at 12.10, the newest release whose bytecode is old enough for our
Java 11 consumers (class major 52). 13.0 is class major 61, so it fails with
`UnsupportedClassVersionError` on Java 11, and its `xmlresolver` moves 5.3.3 to 6.0.23.

The swap itself is already proven: 13.0 was tested on branch `upgrade-saxon-13` and gave
208/208 passing tests plus a working shaded jar. So the only blocker is the floor. When the
floor moves, change the version, set `maven.compiler.release` to 17, and re-run the class file
version scan over the shaded jar to prove nothing else crept up.

[IMPACTS](/pom.xml)
[IMPACTS](/src/main/java/uk/co/terminological/fluentxml/XmlXsl.java)
