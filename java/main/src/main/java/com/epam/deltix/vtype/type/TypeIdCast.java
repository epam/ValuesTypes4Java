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
package com.epam.deltix.vtype.type;

/**
 * This class contains the logic to check the possiblity of converting a type (possibly a transformed VType) into another
 */

public abstract class TypeIdCast {
    // Same or fully compatible type
    public static final int SUCCESS            = 0;

    // Destination type is a VType
    public static final int HAS_VTYPE          = 1;

    // Need VType boxing operation. It is possible to return NEED_BOXING without setting HAS_VTYPE
    // Elements in this case. (????) TODO: Do I still need this behavior?
    public static final int NEED_BOXING        = 2;

    // Need VType unboxing. HAS_VTYPE is assumed
    public static final int NEED_UNBOXING      = 4;

    // Conversion is not possible. Type incompatibility encountered probably due to a logic error,
    // invalid bytecode or unsupported case
    public static final int FAILURE            = -0x80000000;

        // Need destination type initialization, for Value Type at least
    public static final int UNINITIALIZED_VTYPE= 8;
    public static final int DISCARD_VTYPE      = 0x10;      // Need to discard VType at destination
    public static final int NEED_SUBSTITUTION  = 0x20;
    public static final int NO_SUBSTITUTION    = 0x40;
    public static final int DEPTH_MASK         = 0xFF00;
    // TODO: Maybe: Add array type?

    public static boolean isFailure(int checkResult) {
        return checkResult < 0;
    }


    public static int checkCastBase(int from, int to) {

        assert(from != to);
        assert(!TypeId.isVtWildcard(to));
        if (TypeId.isVt(to)) {
            if (TypeId.isVt(from)) {

                if (!TypeId.isSameArrayDepth(from, to))
                    return FAILURE; // We are tracking array types and mismatched number of dimensions equals failure

                if (TypeId.isSameVtClass(from, to)) {
                    if (TypeId.isVtRef(from) && TypeId.isVtValue(to))
                        return NEED_UNBOXING | HAS_VTYPE;

                    if (TypeId.isVtValue(from) && TypeId.isVtRef(to))
                        return NEED_BOXING | HAS_VTYPE;

                    if (TypeId.isVtArray(from) && TypeId.isVtArray(to))
                        return HAS_VTYPE;
                }

                assert("This code must be unreachable" == null);
                return FAILURE;
            }

            // "from" is not ValueType, "to" is some ValueType

            if (TypeId.isVtValue(to)) {
                if (TypeId.isRefScalarDst(from)) {
                    return NEED_UNBOXING | HAS_VTYPE;
                }
            } else {
                if (TypeId.isVtArray(to)) {
                    // TODO: Uncomment
                    //if (!isSameArrayDepth(from, to))
                        //return FAILURE;

                    // TODO: Remove this
                    if (TypeId.isNonVtScalarRef(from))
                        return HAS_VTYPE;   // Assume cast from Object to VT Array

                    return NEED_UNBOXING | HAS_VTYPE;
                }

                // "to" is ValueType but neither Value or Array
                assert(!TypeId.isVtWildcard(to));
                // Destination is VT Ref, do nothing  (TODO: Can we even reach this line?)
                return SUCCESS;
            }

            return FAILURE;
        } else if (TypeId.isRefDst(to)) {
            assert(!TypeId.isVt(to));
            // 'to' is Non-VType reference (or array)

            if (TypeId.isNonVtScalarRef(to)) {
                if (TypeId.isVtValue(from))
                    return NEED_BOXING;

                // Assume any ref type can be assigned to a ref (we can't afford full inheritance checks,
                // and there's no point in checking inheritance partially)
                if (TypeId.isRefDst(from))
                    return SUCCESS;

                // Assuming from != to we should report failure
                return FAILURE;
            }

            assert(TypeId.isArray(to));
            if (TypeId.isVt(from)) {
                // Can turn VT array reference into Long array!!!
                //if (isArray(from) && isInt64Array(to) && isSameArrayDepth(from, to))
                    //return SUCCESS;
                if (TypeId.isSameArrayDepth(from, to))
                    return NEED_BOXING;

                // Can't cast any ValueType to array of different depth
                return FAILURE;
            }

            assert(TypeId.isNonVtArray(to) && !TypeId.isVt(from));
            // We are not tracking /java/lang/Object as a separate type, like null
            // so we are not responsible for actually checking if this scalar type can be cast to Java array
            return SUCCESS;
        }

        return FAILURE;
    }


    public static int check(int from, int to) {

        if (from == to) return TypeId.isVt(to) ? HAS_VTYPE | SUCCESS : SUCCESS;

        assert(!TypeId.isVtWildcard(from) && !TypeId.isVtWildcard(to));   // Check for misuse
        assert(TypeId.VOID != from && TypeId.VOID != to);   // Check for misuse / 2

        return checkCastBase(from, to);
    }


    // This check adds support for conversions from/to "uninitialized" stack frame cells
    public static int checkArg(int from, int to) {

        if (from == to) {
            return TypeId.isVt(to) ? (HAS_VTYPE | SUCCESS) : SUCCESS;
        }

        assert(!TypeId.isVtWildcard(from));                        // stack arg can never be wildcard
        assert(TypeId.VOID != from && TypeId.VOID != to);   // Args can't have uninitialized type
        if (TypeId.isVtWildcard(to)) {
            if (TypeId.isVt(from)) {
                if (!TypeId.isSameArrayDepth(from, to))
                    return FAILURE;

                if (TypeId.isVtValue(from) && TypeId.isVtValue(to))
                    return NEED_SUBSTITUTION | HAS_VTYPE;

                if (TypeId.isVtRef(from) && TypeId.isVtValue(to))
                    return NEED_SUBSTITUTION | NEED_UNBOXING | HAS_VTYPE;

                if (TypeId.isVtArray(from) && TypeId.isVtArray(to))
                    return NEED_SUBSTITUTION | HAS_VTYPE;

                assert("This code must be unreachable" == null);
                return FAILURE;
            } else {
                return NO_SUBSTITUTION;
            }
        }

        return checkCastBase(from, to);
    }

    // This check adds support for conversions from/to "uninitialized" stack frame cells
    // Only supposed to be called during stack frame conversion and with 64-bit <-> uninitialized case handled separately
    public static int checkCastFrame(int from, int to) {

        if (from == to) {
            return TypeId.isVt(to) ? (HAS_VTYPE | SUCCESS) : SUCCESS;
        }

        assert(!TypeId.isVtWildcard(from) && !TypeId.isVtWildcard(to));   // Check for misuse

        if (TypeId.VOID == from) {
            return TypeId.isVt(to) ? (HAS_VTYPE | UNINITIALIZED_VTYPE) : SUCCESS;
            //return isVt(to) ? (HAS_VTYPE | UNINITIALIZED_VTYPE) : (!isSrc64(to) ? SUCCESS : FAILURE);
        }

        if (TypeId.VOID == to) {
            return TypeId.isVtValue(from) ? (HAS_VTYPE | DISCARD_VTYPE) : SUCCESS;
        }

        return checkCastBase(from, to);
    }

}