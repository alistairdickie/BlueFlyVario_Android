/*
 BlueFlyVario flight instrument - http://www.alistairdickie.com/blueflyvario/
 Copyright (C) 2011-2012 Alistair Dickie

 BlueFlyVario is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 BlueFlyVario is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with BlueFlyVario.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bfv;

import android.text.InputFilter;
import android.text.Spanned;

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
