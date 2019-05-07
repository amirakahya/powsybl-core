/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.contingency;

import com.powsybl.contingency.tasks.AbstractTrippingTask;
import com.powsybl.contingency.tasks.StaticVarCompensatorTripping;

import java.util.Objects;

/**
 * @author Teofil Calin BANC <teofil-calin.banc at rte-france.com>
 */
public class StaticVarCompensatorContingency implements ContingencyElement {

    private final String id;

    public StaticVarCompensatorContingency(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ContingencyElementType getType() {
        return ContingencyElementType.STATIC_VAR_COMPENSATOR;
    }

    @Override
    public AbstractTrippingTask toTask() {
        return new StaticVarCompensatorTripping(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StaticVarCompensatorContingency) {
            StaticVarCompensatorContingency other = (StaticVarCompensatorContingency) obj;
            return id.equals(other.id);
        }
        return false;
    }
}
