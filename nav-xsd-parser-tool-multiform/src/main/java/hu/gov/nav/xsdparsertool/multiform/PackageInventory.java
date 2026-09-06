package hu.gov.nav.xsdparsertool.multiform;

import java.util.List;

/** Classified contents of a multiform ZIP package. */
public record PackageInventory(PackageEntry mainEntry, List<PackageEntry> repeatingEntries) {
    public PackageInventory {
        repeatingEntries = List.copyOf(repeatingEntries);
    }
}
