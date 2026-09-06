package hu.nav.m2m.submitter.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fájlok és bájttartalmak SHA-256 ellenőrzőösszegének hexadecimális előállítására szolgáló utility.
 */
public final class Sha256Util {
    /**
     * Létrehozza a(z) {@code Sha256Util} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private Sha256Util() {}

    /**
     * SHA-256 hash-t számít a megadott tartalomból, és kisbetűs hexadecimális szövegként adja vissza.
     *
     * @param inputStream a művelethez átadott {@code inputStream} érték
     * @return a kiszámított ellenőrzőösszeg
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public static String sha256Hex(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024 * 1024];
            try (DigestInputStream dis = new DigestInputStream(inputStream, digest)) {
                while (dis.read(buffer) != -1) {
                    // streaming read only
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algoritmus nem érhető el", e);
        }
    }
    /**
     * SHA-256 hash-t számít a megadott tartalomból, és kisbetűs hexadecimális szövegként adja vissza.
     *
     * @param bytes a feldolgozandó bájttömb
     * @return a kiszámított ellenőrzőösszeg
     */
    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algoritmus nem érhető el", e);
        }
    }
}
