package com.example.link.appfuzzer;

/**
 * Created by link on 12.03.17.
 */

/**
 * Provides some helper functions for XML safe strings.
 */
public class XMLTransformations {

    /**
     * Returns a XML safe string for the XML serializer.
     * @param cs The potentially unsafe char sequence.
     * @return A sanitized String.
     */
    public static String safeCharSeqToString(CharSequence cs) {
        if (cs == null)
            return "";
        else {
            return stripInvalidXMLChars(cs);
        }
    }

    /**
     * Strips invalid XML chars from a CharSequence.
     * @param cs The potentially unsafe char sequence.
     * @return A sanitized String.
     */
    private static String stripInvalidXMLChars(CharSequence cs) {
        StringBuilder ret = new StringBuilder();
        char ch;
        for (int i = 0; i < cs.length(); i++) {
            ch = cs.charAt(i);
            // code below from Html#withinStyle, this is a temporary workaround because XML
            // serializer does not support surrogates
            if (ch >= 0xD800 && ch <= 0xDFFF) {
                if (ch < 0xDC00 && i + 1 < cs.length()) {
                    char d = cs.charAt(i + 1);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        i++;
                        ret.append("?");
                    }
                }
            } else if (ch > 0x7E || ch < ' ') {
                ret.append("?");
            } else {
                ret.append(ch);
            }
        }
        return ret.toString();
    }
}
