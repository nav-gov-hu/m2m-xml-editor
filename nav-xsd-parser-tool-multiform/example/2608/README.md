# 2608 multiform példa

Ez a könyvtár a `nav-xsd-parser-tool-multiform` modul teljes, futtatható példája.

## Tartalom

```text
2608/
├── input.zip
├── input/
│   ├── 2608A.xml
│   ├── 2608M_000001.xml
│   ├── 2608M_000002.xml
│   └── 2608M_000003.xml
├── schema/
│   ├── NAV_2608.xsd
│   ├── common.xsd
│   └── az XSD által használt további sémák
└── expected/
    └── 2608-full.xml
```

Az `input.zip` pontosan 1 `Form_2608A` főlapot és 3 `Form_2608M` melléklapot tartalmaz.

A minták szándékosan minimálisak:

```xml
<Form_2608A/>
```

és:

```xml
<Form_2608M/>
```

A konkrét 2608-as séma ezeket érvényes részbizonylatként elfogadja, ezért a példa kizárólag az adapter-validáció és az összefűzés működésére koncentrál.

## Futtatás

A modul gyökeréből:

```bash
mvn clean package
```

Majd:

```bash
java -jar target/nav-xsd-parser-tool-multiform-1.0.0-SNAPSHOT.jar \
  validate-package \
  --xsd example/2608/schema/NAV_2608.xsd \
  --zip example/2608/input.zip
```

Összefűzés:

```bash
mkdir -p example/2608/generated

java -jar target/nav-xsd-parser-tool-multiform-1.0.0-SNAPSHOT.jar \
  merge \
  --xsd example/2608/schema/NAV_2608.xsd \
  --zip example/2608/input.zip \
  --output example/2608/generated/2608-full.xml
```

Az eredmény szerkezete:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<tns:Doc_2608 xmlns:tns="https://soap.api.nav.gov.hu/definitions/model/2.0/2608/5.2">
    <Form_2608A/>
    <Form_2608M/>
    <Form_2608M/>
    <Form_2608M/>
</tns:Doc_2608>
```

A tényleges generált formázás eltérhet, de az XML szerkezete és tartalma ennek megfelelő.
