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
package com.epam.deltix.vtype.test;

import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.dt.DateTime;
import com.epam.deltix.vtype.annotations.ValueTypeSuppressWarnings;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NestedClassTest {
    private Decimal64 x;
    private static final Decimal64 xStatic = Decimal64.fromDouble(16);

    private static final class InnerClass {
        private Decimal64 x;
        private static final Decimal64 xStatic = Decimal64.fromDouble(4);

        private static final class InnerInnerClass {
            private Decimal64 x = Decimal64.fromDouble(1);

            public Decimal64 getValue() {
                return NestedClassTest.xStatic.add(InnerClass.xStatic).add(x);
            }

            @ValueTypeSuppressWarnings({"refCompare"})
            public boolean transformOk() {
                return getValue() == Decimal64.fromDouble(21);
            }
        }
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678).addNanos(1) == DateTime.create(0x12345678).addNanos(1));
        assertTrue(new InnerClass.InnerInnerClass().transformOk());
    }

    @Test
    public void testResult() {
        Decimal64 y = new InnerClass.InnerInnerClass().getValue();
        assertTrue(y.toDouble() == 21);
        assertTrue(y == Decimal64.fromDouble(21));
    }
}