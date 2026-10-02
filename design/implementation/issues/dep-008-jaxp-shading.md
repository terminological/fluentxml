---
target-version: 2.1
status: done
tags: dependencies, shade, packaging
---

fluentxml 2.0.0: shaded fat-jar bundles JAXP service providers whose implementation classes were dropped by the shade — broken standalone,   
 hijacks downstream JVMs                                                                                                                      
                                                                                                                                              
 Severity: Critical for consumers. The artifact's own service files elect SAX/XSLT providers whose classes are not in the jar, so (a)         
 fluentxml-2.0.0 alone cannot parse or transform XML, and (b) every downstream project gets its JAXP providers silently elected to shaded,    
 partial copies.                                                                                                                              
                                                                                                                                              
 ### Reproduce (20 s, no build tool needed)                                                                                                   
                                                                                                                                              
 ```bash                                                                                                                                      
   curl -sO https://repo1.maven.org/maven2/io/github/terminological/fluentxml/2.0.0/fluentxml-2.0.0.jar                                       
   cat > FxSelf.java <<'EOF'                                                                                                                  
   import uk.co.terminological.fluentxml.Xml;                                                                                                 
   public class FxSelf {                                                                                                                      
     public static void main(String[] a) throws Exception {                                                                                   
       System.out.println(Xml.fromString("<root><b>hi</b></root>").content().outerXml());                                                     
     }                                                                                                                                        
   }                                                                                                                                          
   EOF                                                                                                                                        
   javac -cp fluentxml-2.0.0.jar FxSelf.java                                                                                                  
   java -cp fluentxml-2.0.0.jar:. FxSelf                                                                                                      
 ```                                                                                                                                          
                                                                                                                                              
 Actual:                                                                                                                                      
                                                                                                                                              
 ```                                                                                                                                          
   org.apache.xerces.parsers.ObjectFactory$ConfigurationError:                                                                                
   Provider org.apache.xerces.parsers.XIncludeAwareParserConfiguration not found                                                              
 ```                                                                                                                                          
                                                                                                                                              
 And the XSLT path directly (same thing XmlXsl does internally):                                                                              
                                                                                                                                              
 ```                                                                                                                                          
   TransformerFactoryConfigurationError: Provider for class javax.xml.transform.TransformerFactory cannot be created                          
   Caused by: IllegalArgumentException: Failed to instantiate catalog loader:                                                                 
       org.xmlresolver.loaders.XmlLoader: org.xmlresolver.loaders.XmlLoader                                                                   
 ```                                                                                                                                          
                                                                                                                                              
 ### Root cause                                                                                                                               
                                                                                                                                              
 The jar shades the JAXP ecosystem but the shade filter drops classes and the META-INF/services files survive:                                
                                                                                                                                              
 1. META-INF/services/javax.xml.transform.TransformerFactory → net.sf.saxon.TransformerFactoryImpl (shaded Saxon-12, 2570 classes). Saxon's   
    Configuration.newConfiguration() needs an xmlresolver catalog loader; the jar contains 523 org.xmlresolver.* classes but no               
    org.xmlresolver.loaders classes and no META-INF/services/org.xmlresolver.loaders.XmlLoader — provider elected, factory uninstantiable.    
 2. META-INF/services/org.xml.sax.driver → org.apache.xerces.parsers.SAXParser, but XIncludeAwareParserConfiguration (Xerces' own internal    
    class) is absent → parsing dies the same way.                                                                                             
 3. 1.1-SNAPSHOT had the previous generation of the identical bug: a 2006 partial Saxon-PE whose TransformerFactoryImpl wanted                
    net.sf.saxon.functions.Extensions (gone from HE since 9.x).                                                                               
                                                                                                                                              
 javax.xml.parsers.DocumentBuilderFactory, SAXParserFactory, DatatypeFactory, SchemaFactory, XMLEventFactory service files are all present    
 and pointing at shaded Xerces/Saxon — a library electing global JAXP providers for every consumer is a footgun even when the classes are     
 complete; these failures are the "when" arriving.                                                                                            
                                                                                                                                              
 ### Downstream symptom (why this looks like your build is broken)                                                                            
                                                                                                                                              
 FactoryFinder only ever instantiates the first provider found on the classpath. Any project with fluentxml on the classpath gets the broken  
 provider chosen first — e.g. bibliographic-api-client's Entrez outerXml() and every JATS transform threw                                     
 TransformerFactoryConfigurationError under Maven only (Eclipse classpath order accidentally masked it). Only discovered by bisecting service 
 files across the classpath.                                                                                                                  
                                                                                                                                              
 ### Suggested fix (any one, in order of preference)                                                                                          
                                                                                                                                              
 1. Stop shading the JAXP world. Depend on net.sf.saxon:Saxon-HE and (if Saxon 12) org.xmlresolver:xmlresolver as normal compile deps; don't  
    shade, and ship no META-INF/services/javax.xml.* files at all.                                                                            
 2. If shading is unavoidable: relocate the packages (net.sf.saxon → uk.co.terminological.shaded.saxon, same for xerces/xmlresolver) and drop 
    their service files, invoking the relocated factory explicitly inside fluentxml instead of TransformerFactory.newInstance().              
 3. Whatever you keep: add a standalone smoke test that runs Xml.fromString(...).outerXml() with only the published jar on the classpath —    
    both failures above would have been caught pre-release.                                                                                   
                                                                                                                                              
 ### Consumer workaround meanwhile (verified, bibliographic-api-client 330f304)                                                               
                                                                                                                                              
 Declare a full net.sf.saxon:Saxon-HE:10.9 before fluentxml in the pom: classpath and shade are first-wins, so the complete Saxon shadows the 
 partial bundle (its service entry is elected instead) and both XSLT and parsing work. Ordering is load-bearing; document it in the pom. 
---

## Resolution (2026-10-05, branch `unshade-library`)

Reproduced against the artifact actually on Central, so the report is confirmed rather than
inferred. `java -cp fluentxml-2.0.0.jar:. FxSelf` on JDK 25 gives exactly
`org.apache.xerces.parsers.ObjectFactory$ConfigurationError: Provider
org.apache.xerces.parsers.XIncludeAwareParserConfiguration not found` at `Xml.fromStream(Xml.java:215)`.
Inside that jar: 2570 `net/sf/saxon` classes, 523 `org/xmlresolver` classes but no
`org/xmlresolver/loaders`, zero `XIncludeAwareParserConfiguration`, and eleven
`META-INF/services/javax.xml.*` and `org.xml.sax.driver` entries electing some of it.

Took option 1: the shade plugin is deleted from `library/pom.xml`. It was the whole `<build>`
section. Nothing else in the reactor referenced it, and no relocations were configured, so the jar
was a plain un-relocated concatenation of Saxon, Xerces, xmlresolver, moxy and jakarta.mail, which
is the worst of both worlds: partial, and hijacking JAXP provider selection in every consumer.

The library is now an ordinary 101913 byte jar that declares its dependencies, and the published pom
is `library/pom.xml` rather than a dependency-reduced one, so those dependencies are visible to
Maven and Gradle and arrive transitively.

Verified:

* `mvn clean install`: parent, library 209/209, plugin, all SUCCESS.
* New jar: 0 entries under `META-INF/services/javax.xml.*`, our 16 resource files (10 xslt, 6 schema)
  all present, `uk/co` classes and `META-INF/maven` the only other content. 14596486 bytes to 101913.
* Consumer simulation, thin jar plus the runtime classpath Maven computes from the published pom,
  JDK 25, `FxSelf`: `parse+outerXml` and `doTransform().fragment().asXml()` both correct. Both throw
  with the 2.0.0 jar on the same classpath, so this is the check that would have caught it.

2.0.0 stays on Central broken, releases cannot be withdrawn. Consumers have the ordering workaround
(Saxon-HE declared before fluentxml) or just move to the next version. Releasing as **2.1.0** rather
than 2.0.1 because the artifact changed shape: anyone assembling a classpath by hand now needs
Saxon, Xerces, xmlresolver and moxy alongside, which Maven users get for free and jar users do not.

The check to keep, and it is one line: `unzip -l library/target/fluentxml-*.jar | grep services/javax.xml`
must print nothing. Recorded in the release procedure. A repo test cannot stand in for it, surefire
runs against `target/classes` plus the individual dependency jars, which is precisely why 209 tests
passed while the shipped artifact could not parse a string.

## The provided-scope jaxp-api theory, checked and rejected

A second write-up of this bug, done from the consumer side against `1.1-SNAPSHOT`, blames
`javax.xml.stream:jaxp-api:1.0.1` arriving at scope `provided` through Saxon, fluentxml code
importing `com.sun.org.apache.xml.internal.*` directly, and shade therefore not bundling those
classes, with `--add-exports` in the surefire `argLine` as the fix. That does not describe this
repository, three checks:

* `mvn dependency:tree` for the library module has **no** provided-scope entries and no `jaxp-api`
  artifact anywhere. `org.xmlresolver:xmlresolver:5.3.3` arrives at `compile` through
  `net.sf.saxon:Saxon-HE:12.10`, so shade bundled it, partially.
* No source file imports `com.sun.org.apache.*`. It could not, because `maven.compiler.release=11`
  compiles against `ct.sym`, which has no entries for non-exported packages, so such an import would
  be a compile error rather than a runtime surprise.
* The classes that are missing from the jar belong to compile-scope artifacts that shade did
  include: `XIncludeAwareParserConfiguration` is Xerces' own class and `org.xmlresolver.loaders.*` is
  xmlresolver's own. Scope filtering cannot remove them, `minimizeJar`'s reachability analysis can,
  and the jar proves it did, 523 `org/xmlresolver` entries with two in `loaders` and none of the
  Xerces configuration class.

Where that analysis is right: on a modern JDK, **Saxon 10.9** does fail on the catalog loader path,
and `--add-exports java.xml/com.sun.org.apache.xml.internal.utils=ALL-UNNAMED` does suppress it. But
10.9 is the version the consumer pinned in front of fluentxml as the workaround for this bug, so the
add-exports need is a consequence of the workaround. fluentxml's own declared stack, Saxon-HE 12.10
with xmlresolver 5.3.3, ran the same XSLT path on JDK 25 with no add-exports flags at all.

Which gives the consumer the simpler path: drop the Saxon 10.9 ordering hack when moving to 2.1.0
and let 12.10 arrive transitively, rather than carrying `.mvn/jvm.config` forever. Restoring full
shading is the wrong direction, a complete 14.6 MB JAXP bundle still elects global providers in
every consumer JVM, and it recreates the gap between the IDE classpath and the published artifact
that kept this invisible across two release generations.

Not verified here: JDK 11 itself, this machine has 8, 21 and 25 installed.
