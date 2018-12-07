/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.powsybl.iidm.network.ThreeWindingsTransformer.Side;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class TwtDataTest extends AbstractTwtDataTest {

    @Test
    public void test() {
        TwtData twtData = new TwtData(twt, 0, false);

        assertEquals(99.218431, twtData.getComputedP(Side.ONE), .3);
        assertEquals(3.304328, twtData.getComputedQ(Side.ONE), .3);
        assertEquals(-216.198190, twtData.getComputedP(Side.TWO), .3);
        assertEquals(-85.368180, twtData.getComputedQ(Side.TWO), .3);
        assertEquals(118, twtData.getComputedP(Side.THREE), .3);
        assertEquals(92.612077, twtData.getComputedQ(Side.THREE), .3);

        assertEquals(412.66853716385845, twtData.getStarU(), .0001);
        assertEquals(-7.353779246544198, Math.toDegrees(twtData.getStarTheta()), .0001);
    }

}
