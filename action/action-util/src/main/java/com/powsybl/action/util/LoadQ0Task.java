/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.tasks.ModificationTask;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Load;

import java.util.Objects;

/**
 * @author Olivier Bretteville <olivier.bretteville at rte-france.com>
 */
public class LoadQ0Task implements ModificationTask {

    private final String loadId;
    private final double q0;

    LoadQ0Task(String loadId, double q0) {
        this.loadId = Objects.requireNonNull(loadId);
        this.q0 = q0;
    }

    @Override
    public void modify(Network network, ComputationManager computationManager) {
        Load load = network.getLoad(loadId);
        if (load == null) {
            throw new PowsyblException("Load '" + loadId + "' not found");
        }
        load.setQ0(q0);
    }
}