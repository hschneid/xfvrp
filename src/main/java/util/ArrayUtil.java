package util;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class ArrayUtil {

    public static void add(float[] a, float[] b, float[] result) {
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = a[i] + b[i];
        }
    }
}
