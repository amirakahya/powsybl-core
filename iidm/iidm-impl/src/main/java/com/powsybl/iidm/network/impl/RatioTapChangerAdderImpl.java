/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.impl;

import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.RatioTapChangerAdder;
import com.powsybl.iidm.network.Terminal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class RatioTapChangerAdderImpl implements RatioTapChangerAdder {

    private final RatioTapChangerParent parent;

    private int lowTapPosition = 0;

    private Integer tapPosition;

    private final List<RatioTapChangerStepImpl> steps = new ArrayList<>();

    private boolean loadTapChangingCapabilities = false;

    private boolean regulating = false;

    private double targetV = Double.NaN;

    private TerminalExt regulationTerminal;

    class StepAdderImpl implements StepAdder {

        private double ratio = Double.NaN;

        private double rdr = Double.NaN;

        private double rdx = Double.NaN;

        private double rdg = Double.NaN;

        private double rdb = Double.NaN;

        @Override
        public StepAdder setRatio(double ratio) {
            this.ratio = ratio;
            return this;
        }

        @Override
        public StepAdder setRdr(double rdr) {
            this.rdr = rdr;
            return this;
        }

        @Override
        public StepAdder setRdx(double rdx) {
            this.rdx = rdx;
            return this;
        }

        @Override
        public StepAdder setRdg(double rdg) {
            this.rdg = rdg;
            return this;
        }

        @Override
        public StepAdder setRdb(double rdb) {
            this.rdb = rdb;
            return this;
        }

        @Override
        public RatioTapChangerAdder endStep() {
            if (Double.isNaN(ratio)) {
                throw new ValidationException(parent, "step ratio is not set");
            }
            if (Double.isNaN(rdr)) {
                throw new ValidationException(parent, "step rdr is not set");
            }
            if (Double.isNaN(rdx)) {
                throw new ValidationException(parent, "step rdx is not set");
            }
            if (Double.isNaN(rdg)) {
                throw new ValidationException(parent, "step rdg is not set");
            }
            if (Double.isNaN(rdb)) {
                throw new ValidationException(parent, "step rdb is not set");
            }
            RatioTapChangerStepImpl step = new RatioTapChangerStepImpl(ratio, rdr, rdx, rdg, rdb);
            steps.add(step);
            return RatioTapChangerAdderImpl.this;
        }

    }

    RatioTapChangerAdderImpl(RatioTapChangerParent parent) {
        this.parent = parent;
    }

    NetworkImpl getNetwork() {
        return parent.getNetwork();
    }

    @Override
    public RatioTapChangerAdder setLowTapPosition(int lowTapPosition) {
        this.lowTapPosition = lowTapPosition;
        return this;
    }

    @Override
    public RatioTapChangerAdder setTapPosition(int tapPosition) {
        this.tapPosition = tapPosition;
        return this;
    }

    @Override
    public RatioTapChangerAdder setLoadTapChangingCapabilities(boolean loadTapChangingCapabilities) {
        this.loadTapChangingCapabilities = loadTapChangingCapabilities;
        return this;
    }

    @Override
    public RatioTapChangerAdder setRegulating(boolean regulating) {
        this.regulating = regulating;
        return this;
    }

    @Override
    public RatioTapChangerAdder setTargetV(double targetV) {
        this.targetV = targetV;
        return this;
    }

    @Override
    public RatioTapChangerAdder setRegulationTerminal(Terminal regulationTerminal) {
        this.regulationTerminal = (TerminalExt) regulationTerminal;
        return this;
    }

    @Override
    public StepAdder beginStep() {
        return new StepAdderImpl();
    }

    @Override
    public RatioTapChanger add() {
        if (tapPosition == null) {
            throw new ValidationException(parent, "tap position is not set");
        }
        if (steps.isEmpty()) {
            throw new ValidationException(parent, "ratio tap changer should have at least one step");
        }
        int highTapPosition = lowTapPosition + steps.size() - 1;
        if (tapPosition < lowTapPosition || tapPosition > highTapPosition) {
            throw new ValidationException(parent, "incorrect tap position "
                    + tapPosition + " [" + lowTapPosition + ", "
                    + highTapPosition + "]");
        }
        ValidationUtil.checkRatioTapChangerRegulation(parent, loadTapChangingCapabilities, regulating, regulationTerminal, targetV, getNetwork());
        RatioTapChangerImpl tapChanger
                = new RatioTapChangerImpl(parent, lowTapPosition, steps, regulationTerminal, loadTapChangingCapabilities,
                                          tapPosition, regulating, targetV);
        parent.setRatioTapChanger(tapChanger);
        return tapChanger;
    }

}
