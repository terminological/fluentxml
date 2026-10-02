---
target-version: 1.1
status: open
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
