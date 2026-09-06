package hu.nav.m2m.submitter.service.nav;

/** Felhasználóbarát NAV műveleti kivételeket képez anélkül, hogy transport- vagy proxy-hitelesítési adatot emelne át a hibaüzenetbe. */
public final class NavOperationExceptionFactory {
    /**
     * Létrehozza a(z) {@code NavOperationExceptionFactory} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private NavOperationExceptionFactory() {
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param resultCode a NAV eredménykód
     * @param resultMessage a művelethez átadott {@code resultMessage} érték
     * @return a művelet eredménye
     */
    public static IllegalStateException tokenFailure(String resultCode, String resultMessage) {
        return new IllegalStateException("NAV token kérés sikertelen. resultCode="
                + resultCode + ", resultMessage=" + resultMessage
                + ". A token request és response részletei a NAV_HTTP_TRACE eseményben láthatók.");
    }

    /**
     * A(z) {@code missingUploadedFileId} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param fileName a művelethez átadott {@code fileName} érték
     * @param resultCode a NAV eredménykód
     * @param virusScan a művelethez átadott {@code virusScan} érték
     * @return a művelet eredménye
     */
    public static IllegalStateException missingUploadedFileId(String operation, String fileName, String resultCode, String virusScan) {
        return new IllegalStateException(operation
                + " sikertelen: a NAV Common Filestore nem adott vissza fileId értéket. fileName="
                + fileName + ", resultCode=" + resultCode + ", virusScan=" + virusScan
                + ". A részletes NAV response a NAV_HTTP_TRACE esemény RESPONSE PAYLOAD blokkjában látható.");
    }

    /**
     * A(z) {@code nonceRedeemFailure} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param resultCode a NAV eredménykód
     * @param resultMessage a művelethez átadott {@code resultMessage} érték
     * @return a művelet eredménye
     */
    public static IllegalStateException nonceRedeemFailure(String resultCode, String resultMessage) {
        return new IllegalStateException("Nonce beváltás sikertelen vagy nem adott vissza signatureKeySecondPart értéket. resultCode="
                + resultCode + ", resultMessage=" + resultMessage);
    }
}
