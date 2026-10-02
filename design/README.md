---
target-version: 1.1
status: open
tags: index
---

# Design notes

Deliberately **not** a full continuous evolutionary design setup. This project uses a
small subset: `design/implementation/issues/` only. No `em` script, no `.agents/em`,
no `features/`, `prototypes/` or `test-scripts/` folders, no link checker.

## Conventions

* One markdown file per open issue, named `xxx-NNN-short-slug.md`, status `open` or `done`.
* YAML header with `target-version` and `status`, plus `tags`.
* `[IMPACTS](/path/to/File.java)` style links point at the code the issue touches.
* Anything bigger than a couple of hours of work gets split before it is started.

## Issues

| id | issue | status |
|---|---|---|
| [dep-001](implementation/issues/dep-001-jakarta-wall.md) | javax to jakarta migration is blocked on the Java 11 floor | open |
| [dep-002](implementation/issues/dep-002-drop-eclipse-xpath2-processor.md) | Replace the dead Eclipse XPath 2 engine with Saxon | done |
| [dep-003](implementation/issues/dep-003-castor-provided-but-imported.md) | Castor is `provided` but imported by main code, so the mojos are broken for consumers | done |
| [dep-004](implementation/issues/dep-004-snapshot-parent-blocks-publish.md) | Parent pom is a SNAPSHOT, so the artifact cannot be published | open |
| [dep-005](implementation/issues/dep-005-cssselector-to-xpath-jitpack-rc.md) | Vendor the jitpack cssSelector-to-xpath RC | done |
| [dep-006](implementation/issues/dep-006-saxon-13-blocked-on-java-11.md) | Move to Saxon 13 once Java 17 is the floor | open |
| [dep-007](implementation/issues/dep-007-castor-goals-broken-on-modern-jdks.md) | Castor goals cannot run on JDK 9 and later | open |
