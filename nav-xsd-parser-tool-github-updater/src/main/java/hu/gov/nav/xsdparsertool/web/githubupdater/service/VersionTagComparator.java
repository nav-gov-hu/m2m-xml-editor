package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A GitHub release-tagokat természetes verziósorrendben összehasonlító komparátor. A numerikus és szöveges tokeneket külön kezeli, ezért például a 10-es komponens nem kerül a 2-es elé lexikografikus okból.
 */
@Component
public class VersionTagComparator implements Comparator<String> {
    /**
     * Két release taget természetes verziósorrendben hasonlít össze. A normalizált tageket numerikus és szöveges tokenekre bontja; azonos tokenprefix után a további komponensek és végül a normalizált teljes szöveg dönt.
     *
     * @param left a bal oldali verziótag
     * @param right a jobb oldali verziótag
     * @return negatív, nulla vagy pozitív érték a rendezési sorrendnek megfelelően
     */
    @Override
    public int compare(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        List<Token> a = tokenize(normalize(left));
        List<Token> b = tokenize(normalize(right));
        int max = Math.max(a.size(), b.size());
        for (int i = 0; i < max; i++) {
            Token ta = i < a.size() ? a.get(i) : Token.numeric(BigInteger.ZERO);
            Token tb = i < b.size() ? b.get(i) : Token.numeric(BigInteger.ZERO);
            int cmp = ta.compareTo(tb);
            if (cmp != 0) {
                return cmp;
            }
        }
        return left.compareToIgnoreCase(right);
    }

    /**
     * A megadott értéket a modul belső összehasonlítási és elérési szabályainak megfelelő kanonikus formára alakítja.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String normalize(String value) {
        String result = value.trim().toLowerCase(Locale.ROOT);
        if (result.startsWith("refs/tags/")) {
            result = result.substring("refs/tags/".length());
        }
        if (result.startsWith("v") && result.length() > 1 && Character.isDigit(result.charAt(1))) {
            result = result.substring(1);
        }
        return result;
    }

    /**
     * A normalizált verziószöveget egymást követő numerikus és szöveges tokenekre bontja; az elválasztó karakterek tokenhatárként működnek.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private List<Token> tokenize(String value) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Boolean digit = null;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean isDigit = Character.isDigit(ch);
            if (!Character.isLetterOrDigit(ch)) {
                flush(tokens, current, digit);
                current.setLength(0);
                digit = null;
                continue;
            }
            if (digit != null && digit != isDigit) {
                flush(tokens, current, digit);
                current.setLength(0);
            }
            digit = isDigit;
            current.append(ch);
        }
        flush(tokens, current, digit);
        return tokens;
    }

    /**
     * Az aktuálisan gyűjtött tokenpuffert numerikus vagy szöveges {@code Token} objektummá alakítja, majd üríti a puffert.
     *
     * @param tokens a művelethez átadott {@code tokens} érték
     * @param current a művelethez átadott {@code current} érték
     * @param digit a művelethez átadott {@code digit} érték
     */
    private void flush(List<Token> tokens, StringBuilder current, Boolean digit) {
        if (current.isEmpty() || digit == null) {
            return;
        }
        if (digit) {
            tokens.add(Token.numeric(new BigInteger(current.toString())));
        } else {
            tokens.add(Token.text(current.toString()));
        }
    }

    /**
     * Egy release-verzió felbontott numerikus vagy szöveges komponensét reprezentáló belső értékobjektum.
     */
    private record Token(boolean numeric, BigInteger number, String text) implements Comparable<Token> {
        /**
         * Jelzi, hogy a verziótoken numerikus komponens-e; ezt a komparátor a számszerű és szöveges összehasonlítás közötti választáshoz használja.
         *
         * @param number a művelethez átadott {@code number} érték
         * @return a művelet eredménye
         */
        static Token numeric(BigInteger number) {
            return new Token(true, number, "");
        }

        /**
         * Visszaadja a token szöveges reprezentációját, amely a nem numerikus komponensek rendezésének alapja.
         *
         * @param text a művelethez átadott {@code text} érték
         * @return a művelet eredménye
         */
        static Token text(String text) {
            return new Token(false, BigInteger.ZERO, text);
        }

        /**
         * Két belső verziótokent hasonlít össze: numerikus tokennél számszerű, szöveges tokennél lexikografikus összevetést alkalmaz, és a token típusa is része a rendezésnek.
         *
         * @param other a művelethez átadott {@code other} érték
         * @return negatív, nulla vagy pozitív érték a tokenek sorrendjének megfelelően
         */
        @Override
        public int compareTo(Token other) {
            if (numeric && other.numeric) {
                return number.compareTo(other.number);
            }
            if (numeric != other.numeric) {
                return numeric ? 1 : -1;
            }
            return text.compareTo(other.text);
        }
    }
}
