package hu.nav.m2m.submitter.domain;

/**
 * Az M2M beküldés alkalmazáson belüli életciklusállapotait definiáló felsorolás.
 */
public enum SubmissionStatus {
    CREATED,
    /** XML/beküldési csomag megjelölve beküldésre. Csak ilyen állapotból indítható beküldés. */
    MARKED_FOR_SUBMISSION,
    /** Beküldésre jelölés visszavonva. Új beküldéshez ismét meg kell jelölni. */
    SUBMISSION_MARK_WITHDRAWN,
    /** Fájl lokálisan feltöltve/létrehozva, NAV Common Filestore feltöltés még nem feltétlenül sikeres. */
    FILE_UPLOADED,
    /** NAV Common Filestore feltöltés sikeres. */
    UPLOAD_SUCCESS,
    VALIDATION_PENDING,
    VALIDATED,
    VALIDATION_FAILED,
    SUBMIT_PENDING,
    /** Beküldés éppen folyamatban van a helyi alkalmazásban. */
    SUBMITTING,
    /** NAV oldalon köztes állapotban van, státuszlekérdezéssel pollolni kell. */
    SUBMISSION_IN_PROGRESS,
    SUBMITTED,
    SUBMIT_FAILED,
    TECHNICAL_FAILED,
    /** Technikai hiba történt; nincs bizonyított NAV végállapot, újra megjelölhető. */
    SUBMISSION_TECHNICAL_FAILED,
    /** A beküldés NAV oldali visszajelzés alapján sikeres végállapot. */
    SUBMITTED_OK,
    /** A beküldés NAV oldali validációs/beküldési hibával végállapotba került. */
    SUBMITTED_WITH_ERROR
}
