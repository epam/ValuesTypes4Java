/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.dt;

import com.epam.deltix.vtype.annotations.ValueTypeCommutative;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is a simple test example ValueType class
 */

final public class Utils {
    private static final long NANOS_PER_MILLIS  = 1000000;
    private static final long NANOS_PER_SECOND  = 1000000000;
    private static final long NANOS_PER_MINUTE  = NANOS_PER_SECOND  * 60;
    private static final long NANOS_PER_HOUR    = NANOS_PER_MINUTE  * 60;
    private static final long NANOS_PER_DAY     = NANOS_PER_HOUR    * 24;

    public static final long NULL               = Long.MIN_VALUE;

    private Utils() {}

    // AdHoc DateTime parsing
    public static long fromString(String str) throws ParseException {
        long dt;
        int idx;
        SimpleDateFormat sdf;
        do {
            if ((idx = str.indexOf('-')) > 0) {
                sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                break;
            } else if ((idx = str.indexOf('/')) > 0) {
                sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
                break;
            }

            throw new ParseException(new StringBuilder("Unknown DateTime format: ").append(str).toString(), 0);
        } while(false);

        ParsePosition pos = new ParsePosition(0);
        Date result = sdf.parse(str, pos);
        if (pos.getIndex() == 0)
            throw new ParseException(String.format("Unparseable date: \"%s\"" , str), pos.getErrorIndex());

        dt = result.getTime() * NANOS_PER_MILLIS;
        if ((idx = str.indexOf('.', pos.getIndex())) > 0) {
            long m = 100_000_000L;
            char ch;
            boolean z = (char)10 < '9';
            while ((ch = (char)(str.charAt(++idx) - '0')) < (char)10) {
                dt += m * ch;
                if ((m /= 10) == 0)
                    break;
            }
        }
        return dt;
    }


    public static boolean isNull(long x) { return NULL == x; }

    @ValueTypeCommutative
    public static boolean equals(long dt1, long dt2) {
        return dt1 == dt2;
    }

    public static boolean equals(long dt1, Object o) {
        return null == o ? NULL == dt1 : o instanceof DateTime && dt1 == ((DateTime)o).dt;
    }

    public static boolean isIdentical(long dt1, long dt2) {
        return dt1 == dt2;
    }

    public static boolean isIdentical(long dt1, Object o) {
        return (null == o && NULL == dt1) || o instanceof DateTime && dt1 == ((DateTime)o).dt;
    }

    public static long[] identity(long[] x) {
        return x;
    }

    public static long identity(long x) {
        return x;
    }

    public static long[] copyArray(long[] src, int srcOffset, int length, long[] dst, int dstOffset) {

        int srcLength = src.length;
        int srcEndOffset = srcOffset + length;

        // NOTE: no bounds checks, just a sample
        for (int i = 0; i < length; ++i) {
            dst[dstOffset + i] = src[srcOffset + i];
        }

        return dst;
    }


    public static long createDefault() {
        return identity(0);
    }

    public static long now() {
        return System.currentTimeMillis() * NANOS_PER_MILLIS;
    }

    @ValueTypeCommutative
    public static long addNanos(long dt, long nanos) {
        return dt + nanos;
    }

    public static long addSeconds(long dt, long seconds) {
        return dt + seconds * NANOS_PER_MILLIS;
    }

    public static long addDays(long dt, long days) {
        return dt + days * NANOS_PER_DAY;
    }

    public static String toString(long dt) {
        if (NULL == dt)
            return "null";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.");
        StringBuffer sb = sdf.format(new Date(dt / NANOS_PER_MILLIS), new StringBuffer(), new FieldPosition(0));
        sb.append(String.format("%09d", dt % NANOS_PER_SECOND));
        return sb.toString();
    }


    /// region Array boxing/unboxing (array conversions from long[] / to long[])

    public static DateTime[] fromLongArray(long[] src, int srcOffset, DateTime[] dst, int dstOffset, int length) {

        int srcLength = src.length;
        int srcEndOffset = srcOffset + length;

        // NOTE: no bounds checks
        for (int i = 0; i < length; ++i) {
            dst[dstOffset + i] = DateTime.create(src[srcOffset + i]);
        }

        return dst;
    }


    public static long[] toLongArray(DateTime[] src, int srcOffset, long[] dst, int dstOffset, int length) {

        int srcLength = src.length;
        int srcEndOffset = srcOffset + length;

        // NOTE: no bounds checks
        for (int i = 0; i < length; ++i) {
            dst[dstOffset + i] = DateTime.getLong(src[srcOffset + i]);
        }

        return dst;
    }

    public static long[] toLongArray(DateTime[] src) {
        return null == src ? null : toLongArray(src, 0, new long[src.length], 0, src.length);
    }

    public static DateTime[] fromLongArray(long[] src) {
        return null == src ? null : fromLongArray(src, 0, new DateTime[src.length], 0, src.length);
    }

    public static long avgRenamed(long dt, long dt1) {
        return (dt + dt1) / 2;
    }
}