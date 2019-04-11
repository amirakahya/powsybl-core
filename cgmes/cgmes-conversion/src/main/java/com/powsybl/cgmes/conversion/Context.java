/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.Conversion.Config;
import com.powsybl.cgmes.conversion.elements.ACLineSegmentConversion;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class Context {
    public Context(CgmesModel cgmes, Config config, Network network) {
        this.cgmes = Objects.requireNonNull(cgmes);
        this.config = Objects.requireNonNull(config);
        this.network = Objects.requireNonNull(network);

        // Even if the CGMES model is node-breaker,
        // we could decide to ignore the connectivity nodes and
        // create buses directly from topological nodes,
        // the configuration says if we are performing the conversion
        // based on existing node-breaker info
        nodeBreaker = cgmes.isNodeBreaker() && config.useNodeBreaker();

        namingStrategy = new NamingStrategy.Identity();
        boundary = new Boundary(cgmes);
        substationIdMapping = new SubstationIdMapping(this);
        terminalMapping = new TerminalMapping();
        tapChangerTransformers = new TapChangerTransformers();
        dcMapping = new DcMapping(this);
        currentLimitsMapping = new CurrentLimitsMapping(this);
        nodeMapping = new NodeMapping();

        ratioTapChangerTables = new HashMap<>();
        remoteRegulatingTerminals = new HashMap<>();
        reactiveCapabilityCurveData = new HashMap<>();
    }

    public CgmesModel cgmes() {
        return cgmes;
    }

    public Network network() {
        return network;
    }

    public Config config() {
        return config;
    }

    public boolean nodeBreaker() {
        return nodeBreaker;
    }

    public NamingStrategy namingStrategy() {
        return namingStrategy;
    }

    public TerminalMapping terminalMapping() {
        return terminalMapping;
    }

    public NodeMapping nodeMapping() {
        return nodeMapping;
    }

    public TapChangerTransformers tapChangerTransformers() {
        return tapChangerTransformers;
    }

    public SubstationIdMapping substationIdMapping() {
        return substationIdMapping;
    }

    public Boundary boundary() {
        return boundary;
    }

    public DcMapping dc() {
        return dcMapping;
    }

    public CurrentLimitsMapping currentLimitsMapping() {
        return currentLimitsMapping;
    }

    public static String boundaryVoltageLevelId(String nodeId) {
        Objects.requireNonNull(nodeId);
        return nodeId + "_VL";
    }

    public static String boundarySubstationId(String nodeId) {
        Objects.requireNonNull(nodeId);
        return nodeId + "_S";
    }

    public void loadReactiveCapabilityCurveData() {
        PropertyBags rccdata = cgmes.reactiveCapabilityCurveData();
        if (rccdata == null) {
            return;
        }
        rccdata.forEach(p -> {
            String curveId = p.getId("ReactiveCapabilityCurve");
            reactiveCapabilityCurveData.computeIfAbsent(curveId, cid -> new PropertyBags()).add(p);
        });
    }

    public PropertyBags reactiveCapabilityCurveData(String curveId) {
        return reactiveCapabilityCurveData.get(curveId);
    }

    public void loadRatioTapChangerTables() {
        PropertyBags rtcpoints = cgmes.ratioTapChangerTablesPoints();
        if (rtcpoints == null) {
            return;
        }
        rtcpoints.forEach(p -> {
            String tableId = p.getId("RatioTapChangerTable");
            ratioTapChangerTables.computeIfAbsent(tableId, tid -> new PropertyBags()).add(p);
        });
    }

    public PropertyBags ratioTapChangerTable(String tableId) {
        return ratioTapChangerTables.get(tableId);
    }

    public void putRemoteRegulatingTerminal(String idEq, String topologicalNode) {
        remoteRegulatingTerminals.put(idEq, topologicalNode);
    }

    public void setAllRemoteRegulatingTerminals() {
        remoteRegulatingTerminals.entrySet().removeIf(this::setRegulatingTerminal);
        remoteRegulatingTerminals.forEach((key, value) -> pending("Regulating terminal", String.format("The setting of the regulating terminal of the equipment %s is not handled.", key)));
    }

    private boolean setRegulatingTerminal(Map.Entry<String, String> entry) {
        Identifiable i = network.getIdentifiable(entry.getKey());
        if (i instanceof Generator) {
            Generator g = (Generator) i;
            Terminal regTerminal = terminalMapping.findFromTopologicalNode(entry.getValue());
            if (regTerminal == null) {
                missing(String.format("IIDM terminal for this CGMES topological node: %s", entry.getValue()));
            } else {
                g.setRegulatingTerminal(regTerminal);
                return true;
            }
        }
        // TODO add cases for ratioTapChangers and phaseTapChangers
        return false;
    }

    public void startLinesConversion() {
        countLines = 0;
        countLinesWithSvPowerFlowsAtEnds = 0;
    }

    public void anotherLineConversion(ACLineSegmentConversion c) {
        Objects.requireNonNull(c);
        countLines++;
        if (c.terminalPowerFlow(1).defined() && c.terminalPowerFlow(2).defined()) {
            countLinesWithSvPowerFlowsAtEnds++;
        }
    }

    public void endLinesConversion() {
        String enough = countLinesWithSvPowerFlowsAtEnds < countLines ? "FEW" : "ENOUGH";
        LOG.info("{} lines with SvPowerFlow values at ends: {} / {}",
                enough,
                countLinesWithSvPowerFlowsAtEnds,
                countLines);
    }

    public void invalid(String what, String reason) {
        LOG.warn("Invalid {}. Reason: {}", what, reason);
    }

    public void ignored(String what, String reason) {
        LOG.warn("Ignored {}. Reason: {}", what, reason);
    }

    public void pending(String what, String reason) {
        LOG.info("PENDING {}. Reason: {}", what, reason);
    }

    public void fixed(String what, String reason) {
        LOG.warn("Fixed {}. Reason: {}", what, reason);
    }

    public void fixed(String what, String reason, double wrong, double fixed) {
        LOG.warn("Fixed {}. Reason: {}. Wrong {}, fixed {}", what, reason, wrong, fixed);
    }

    public void missing(String what) {
        LOG.warn("Missing {}", what);
    }

    public void missing(String what, double defaultValue) {
        LOG.warn("Missing {}. Used default value {}", what, defaultValue);
    }

    private final CgmesModel cgmes;
    private final Network network;
    private final Config config;
    private final boolean nodeBreaker;
    private final NamingStrategy namingStrategy;
    private final SubstationIdMapping substationIdMapping;
    private final Boundary boundary;
    private final TerminalMapping terminalMapping;
    private final NodeMapping nodeMapping;
    private final TapChangerTransformers tapChangerTransformers;
    private final DcMapping dcMapping;
    private final CurrentLimitsMapping currentLimitsMapping;

    private final Map<String, PropertyBags> ratioTapChangerTables;
    private final Map<String, String> remoteRegulatingTerminals;
    private final Map<String, PropertyBags> reactiveCapabilityCurveData;

    private int countLines;
    private int countLinesWithSvPowerFlowsAtEnds;

    private static final Logger LOG = LoggerFactory.getLogger(Context.class);
}
