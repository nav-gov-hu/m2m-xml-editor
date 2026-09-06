# Beillesztés a multi-module projektbe

A modul önálló marad; a függőségi irány kifelé mutat belőle.

## Gyökér POM

A meglévő projekt gyökér POM-jában elegendő modulként felvenni:

```xml
<module>nav-xsd-parser-tool-multiform</module>
```

Ha a projekt közös parent POM-ját kell használni, a modul saját POM-jának `groupId`/`version` része a parent deklarációra cserélhető. Ez build-integráció, nem funkcionális függőség.

## Feldolgozási modulból

Ha később a `nav-xsd-parser-tool-processing` akarja használni:

```xml
<dependency>
    <groupId>hu.gov.nav</groupId>
    <artifactId>nav-xsd-parser-tool-multiform</artifactId>
    <version>${project.version}</version>
</dependency>
```

A javasolt függőségi irány:

```text
processing ------> multiform
web -------------> processing (vagy közvetlenül multiform, ha indokolt)
külső program ---> multiform

multiform -X-> web
multiform -X-> processing
multiform -X-> Spring
```

Így a multiform JAR külön is kiadható külső Java-fejlesztőknek.
