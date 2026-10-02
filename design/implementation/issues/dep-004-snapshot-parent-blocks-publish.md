---
target-version: 1.1
status: done
tags: build, publishing, blocker
---

# SNAPSHOT parent blocks publishing

The parent is `io.github.terminological:m2repo:0.0.5-SNAPSHOT`. A released artifact must not
inherit a SNAPSHOT parent, and every build already warns:

```
Could not transfer metadata io.github.terminological:m2repo:0.0.5-SNAPSHOT/maven-metadata.xml
from/to github (https://maven.pkg.github.com/terminological/m2repo): 401 Unauthorized
```

Fix by depending on a released parent, or by inlining what the parent contributes (deploy
config, distributionManagement). Confirm the parent is resolvable from Maven Central before
tagging 1.1.

[IMPACTS](/pom.xml)

## Resolution (2026-10-02)

`m2repo` 0.0.6 is on Central, the root pom references it, and 2.0.0 went out through
`release:prepare` and `release:perform`, tag `fluentxml-parent-2.0.0`. The publish did expose two
problems of its own: an scm inherited from a remote parent makes the release plugin append the
artifactId to the clone url, fixed by declaring `<scm>` in all three poms in `4aaf85f`, and the
artifact that came out was broken, see [dep-008](dep-008-jaxp-shading.md).
