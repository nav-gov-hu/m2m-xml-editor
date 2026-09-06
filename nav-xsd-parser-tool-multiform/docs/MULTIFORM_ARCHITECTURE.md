# Multiform modul architektúra

A modul tudatosan önálló. A fő kód csak Java 17 szabványos XML API-kra épül.

## Függőségi irány

```text
nav-xsd-parser-tool-processing / web / külső Java program
                       |
                       v
          nav-xsd-parser-tool-multiform
                       |
                       v
                Java 17 JAXP/StAX
```

A `multiform` modul nem függ a `web`, `processing`, `xsd`, `core` vagy Spring moduloktól. Ez megakadályozza a körkörös függést és lehetővé teszi az önálló terjesztést.

Ha később a projekt meglévő általános XSD modelljeivel szeretnénk közös kódot használni, azt külön, stabil SPI/API rétegen érdemes megtenni; a merge/adapter/ZIP funkcionalitásnak továbbra is Spring-függetlennek kell maradnia.
