# nav-xsd-parser-tool-coverage

## A modul célja

A `nav-xsd-parser-tool-coverage` modul a projekt Java moduljainak összesített JaCoCo kódlefedettségi riportját állítja elő. Nem tartalmaz alkalmazási vagy üzleti logikát, és nem futtatható önálló programként.

A modul Maven aggregátor szerepet tölt be: függőségként felsorolja azokat a projektmodulokat, amelyek lefedettségi adatait egy közös jelentésben kell megjeleníteni.

## Belépési pont

A modulnak nincs Java `main` osztálya, REST végpontja vagy programozói szolgáltatási API-ja. A belépési pontja a Maven életciklus és a modul `pom.xml` fájljában konfigurált JaCoCo aggregáló riport.

A releváns végrehajtás:

```text
jacoco-maven-plugin:report-aggregate
```

A riport a `verify` Maven fázishoz van kötve.

## Mit aggregál?

A modul a projekt Java moduljait Maven-függőségként veszi fel, így a JaCoCo az egyes modulokban előállított végrehajtási és osztályinformációkat közös riportban tudja összesíteni.

Az aggregáció többek között a következő rétegeket fogja össze:

- közös domain és utility kód (`core`);
- XSD, UIModel és page-schema feldolgozás;
- schema registry és processing;
- nyomtatás;
- parancssori alkalmazások;
- GitHub integráció;
- M2M beküldő integráció;
- webalkalmazás.

## Kimenet

A konfigurált összesített riport célkönyvtára:

```text
nav-xsd-parser-tool-coverage/target/site/jacoco-aggregate
```

A HTML jelentés tipikus belépési pontja:

```text
nav-xsd-parser-tool-coverage/target/site/jacoco-aggregate/index.html
```

A modul önmagában nem gyűjt futási adatot. Az aggregált riport csak azokból a JaCoCo adatokból tud dolgozni, amelyeket a tesztfuttatások a részt vevő modulokban előállítottak.

## Használat

A coverage modul használata a projekt Maven buildfolyamatának része. A riport előállításához olyan Maven futás szükséges, amely végrehajtja a releváns teszteket és eléri a `verify` fázist.

## Felelősségi határ

A modul kizárólag riportaggregációért felel. Nem:

- tartalmaz üzleti logikát;
- módosítja a vizsgált modulokat;
- definiál teszteseteket a többi modul helyett;
- határozza meg önmagában az elfogadható lefedettségi küszöböket;
- helyettesíti a funkcionális, integrációs vagy biztonsági teszteket.

A lefedettségi százalék azt mutatja meg, hogy a tesztek milyen kódrészeket hajtottak végre; önmagában nem bizonyítja a tesztek minőségét vagy a funkciók helyességét.
