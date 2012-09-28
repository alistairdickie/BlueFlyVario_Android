package com.bfv;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Created with IntelliJ IDEA.
 * User: Alistair
 * Date: 11/09/12
 * Time: 8:05 PM
 */
public class FilenameInputFilter implements InputFilter {

    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        String valid = "";
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            switch (c) {
                case '/':
                case '\\':
                case ':':
                    c = '-';
                    break;
                case '"':
                    c = '\'';
                    break;
                case '*':
                case '?':
                case '<':
                case '>':
                case '|':
                    c = '_';
            }
            valid = valid.concat(String.valueOf(c));
        }
        return valid;
    }
}
