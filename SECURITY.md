# Biztonsági előírások

Bár a forráskódot a felhasználó "AS IS" kapja, igyekszünk a annak biztonságosságát is szem előtt tartani, annak minőségére kapacitást fordítani.
A bejelentett probléma kezelése nem kapcsolódok SLA-hoz vagy egyéb szolgáltatási szinthez, annak javítását a forkolt forráskódon is el lehet végezni.

Kérjük, amennyiben esetlegesen valamilyen security problémát detektál az alábbiakat vegye figyelembe:

## Támogatott verziók

Az M2M XML Editor esetében kizárólag a legfrissebb hivatalosan kiadott verzió támogatott biztonsági szempontból.

| Version | Supported          |
| ------- | ------------------ |
| Legfrissebb kiadás | :white_check_mark: |
| Régebbi kiadás | :x: |

## Sérülékenység jelentése

Kérjük, hogy biztonsági sérülékenységet ne nyilvános GitHub Issue formájában jelentsenek.

A biztonsági problémák bejelentéséhez használja az alábbi elérhetőséget:

- E-mail: m2m-xml-editor@nav.gov.hu
- Tárgy: `Sérülékenységi jelentés - M2M XML Editor`

A bejelentés lehetőség szerint tartalmazza:

- a sérülékenység rövid leírását;
- a reprodukálás lépéseit;
- az érintett verziószámot;
- a várható és a tényleges működést;
- esetleges proof-of-concept mintát vagy képernyőképet;
- a potenciális hatás leírását.

## Kezelési folyamat

A bejelentés beérkezését követően:

1. Visszaigazoljuk a bejelentés fogadását.
2. Megvizsgáljuk a jelentett problémát.
3. Szükség esetén további információt kérünk.
4. Javítás esetén a hibát egy következő kiadásban kezeljük.
5. A javítás publikálását követően tájékoztatjuk a bejelentőt.

## Szkóp

A biztonsági jelentések különösen az alábbi területekre vonatkozhatnak:

- XML feldolgozás és validáció;
- XSD alapú modellkezelés;
- fájlkezelés;
- konfigurációs beállítások;
- hálózati kommunikáció;
- függőségekben található ismert sérülékenységek.

## Felelős nyilvánosságra hozatal

Kérjük, hogy a sérülékenység részleteit a javítás közzétételéig ne hozza nyilvánosságra.

Köszönjük a felelős hibabejelentést és a projekt biztonságának növeléséhez nyújtott segítséget.
