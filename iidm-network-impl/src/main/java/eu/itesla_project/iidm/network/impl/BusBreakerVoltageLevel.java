/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.network.impl;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import eu.itesla_project.commons.ITeslaException;
import eu.itesla_project.commons.collect.Downcast;
import eu.itesla_project.graph.TraverseResult;
import eu.itesla_project.graph.Traverser;
import eu.itesla_project.graph.UndirectedGraphImpl;
import eu.itesla_project.graph.UndirectedGraphListener;
import eu.itesla_project.iidm.network.*;
import eu.itesla_project.iidm.network.util.Networks;
import eu.itesla_project.iidm.network.util.ShortIdDictionary;
import org.joda.time.DateTime;

import java.io.*;
import java.util.*;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class BusBreakerVoltageLevel extends AbstractVoltageLevel {

    private class SwitchAdderImpl extends IdentifiableAdderImpl<SwitchAdderImpl> implements BusBreakerView.SwitchAdder {

        private String busId1;

        private String busId2;

        private boolean open = false;

        private SwitchAdderImpl() {
        }

        @Override
        protected NetworkImpl getNetwork() {
            return BusBreakerVoltageLevel.this.getNetwork();
        }

        @Override
        protected String getTypeDescription() {
            return "Switch";
        }

        @Override
        public BusBreakerView.SwitchAdder setBus1(String bus1) {
            this.busId1 = bus1;
            return this;
        }

        @Override
        public BusBreakerView.SwitchAdder setBus2(String bus2) {
            this.busId2 = bus2;
            return this;
        }

        @Override
        public BusBreakerView.SwitchAdder setOpen(boolean open) {
            this.open = open;
            return this;
        }

        @Override
        public Switch add() {
            String id = checkAndGetUniqueId();
            if (busId1 == null) {
                throw new ValidationException(this, "first connection bus is not set");
            }
            if (busId2 == null) {
                throw new ValidationException(this, "second connection bus is not set");
            }

            SwitchImpl _switch = new SwitchImpl(getNetwork().getRef(),
                                                id, getName(), SwitchKind.BREAKER, open, true);
            addSwitch(_switch, busId1, busId2);
            getNetwork().getListeners().notifyCreation(_switch);
            return _switch;
        }

    }

    private final UndirectedGraphImpl<ConfiguredBus, SwitchImpl> graph = new UndirectedGraphImpl<>();

    /* buses indexed by vertex number */
    private final Map<String, Integer> buses = new HashMap<>();

    /* switches indexed by edge number */
    private final Map<String, Integer> switches = new HashMap<>();

    private Integer getVertex(String busId, boolean throwException) {
        Objects.requireNonNull(busId, "bus id is null");
        Integer v = buses.get(busId);
        if (throwException && v == null) {
            throw new ITeslaException("Bus " + busId
                    + " not found in substation voltage level "
                    + BusBreakerVoltageLevel.this.id);
        }
        return v;
    }

    ConfiguredBus getBus(String busId, boolean throwException) {
        Integer v = getVertex(busId, throwException);
        if (v != null) {
            ConfiguredBus bus = graph.getVertexObject(v);
            if (!bus.getId().equals(busId)) {
                throw new InternalError("Must not happened");
            }
            return bus;
        }
        return null;
    }

    private Integer getEdge(String switchId, boolean throwException) {
        Objects.requireNonNull(switchId, "switch id is null");
        Integer e = switches.get(switchId);
        if (throwException && e == null) {
            throw new ITeslaException("Switch " + switchId
                    + " not found in substation voltage level"
                    + BusBreakerVoltageLevel.this.id);
        }
        return e;
    }

    private SwitchImpl getSwitch(String switchId, boolean throwException) {
        Integer e = getEdge(switchId, throwException);
        if (e != null) {
            SwitchImpl _switch = graph.getEdgeObject(e);
            if (!_switch.getId().equals(switchId)) {
                throw new InternalError("Must not happened");
            }
            return _switch;
        }
        return null;
    }

    /**
     * Bus only topology cache
     */
    private static class BusCache {

        /* merged bus by id */
        private final Map<String, MergedBus> mergedBus;

        /* bus to merged bus mapping */
        private final Map<ConfiguredBus, MergedBus> mapping;

        private BusCache(Map<String, MergedBus> mergedBus, Map<ConfiguredBus, MergedBus> mapping) {
            this.mergedBus = mergedBus;
            this.mapping = mapping;
        }

        private Collection<MergedBus> getMergedBuses() {
            return mergedBus.values();
        }

        private MergedBus getMergedBus(String id) {
            return mergedBus.get(id);
        }

        private MergedBus getMergedBus(ConfiguredBus cfgBus) {
            return mapping.get(cfgBus);
        }

    }

    /**
     * Bus only topology calculated from bus/breaker topology
     */
    class CalculatedBusTopology {

        protected boolean isBusValid(Set<ConfiguredBus> busSet) {
            int feederCount = 0;
            int branchCount = 0;
            for (TerminalExt terminal : FluentIterable.from(busSet).transformAndConcat(ConfiguredBus::getConnectedTerminals)) {
                ConnectableImpl connectable = terminal.getConnectable();
                switch (connectable.getType()) {
                    case LINE:
                    case TWO_WINDINGS_TRANSFORMER:
                    case THREE_WINDINGS_TRANSFORMER:
                        branchCount++;
                        feederCount++;
                        break;

                    case DANGLING_LINE:
                    case LOAD:
                    case GENERATOR:
                    case SHUNT_COMPENSATOR:
                        feederCount++;
                        break;

                    case BUSBAR_SECTION: // must not happend in a bus/breaker topology
                    default:
                        throw new AssertionError();
                }
            }
            return Networks.isBusValid(feederCount, branchCount);
        }

        private void updateCache() {
            if (states.get().cache != null) {
                return;
            }

            Map<String, MergedBus> mergedBuses = new LinkedHashMap<>();

            // mapping between configured buses and merged buses
            Map<ConfiguredBus, MergedBus> mapping = new IdentityHashMap<>();

            boolean[] encountered = new boolean[graph.getMaxVertex()];
            Arrays.fill(encountered, false);
            int busNum = 0;
            for (int v : graph.getVertices()) {
                if (!encountered[v]) {
                    final Set<ConfiguredBus> busSet = new HashSet<>(1);
                    busSet.add(graph.getVertexObject(v));
                    graph.traverse(v, new Traverser<SwitchImpl>() {
                        @Override
                        public TraverseResult traverse(int v1, int e, int v2) {
                            SwitchImpl _switch = graph.getEdgeObject(e);
                            if (_switch.isOpen()) {
                                return TraverseResult.TERMINATE;
                            } else {
                                busSet.add(graph.getVertexObject(v2));
                                return TraverseResult.CONTINUE;
                            }
                        }
                    }, encountered);
                    if (isBusValid(busSet)) {
                        String mergedBusId = BusBreakerVoltageLevel.this.id + "_" + busNum++;
                        MergedBus mergedBus = new MergedBus(mergedBusId, busSet);
                        mergedBuses.put(mergedBus.getId(), mergedBus);
                        for (ConfiguredBus bus : busSet) {
                            mapping.put(bus, mergedBus);
                        }
                    }
                }
            }

            states.get().cache = new BusCache(mergedBuses, mapping);
        }

        private void invalidateCache() {
            // detach buses
            if (states.get().cache != null) {
                for (MergedBus bus : states.get().cache.getMergedBuses()) {
                    bus.invalidate();
                }
                states.get().cache = null;
            }
        }

        private Collection<MergedBus> getMergedBuses() {
            updateCache();
            return states.get().cache.getMergedBuses();
        }

        private MergedBus getMergedBus(String mergedBusId, boolean throwException) {
            updateCache();
            MergedBus bus = states.get().cache.getMergedBus(mergedBusId);
            if (throwException && bus == null) {
                throw new ITeslaException("Bus " + mergedBusId
                        + " not found in substation voltage level "
                        + BusBreakerVoltageLevel.this.id);
            }
            return bus;
        }

        MergedBus getMergedBus(ConfiguredBus bus) {
            Objects.requireNonNull(bus, "bus is null");
            updateCache();
            return states.get().cache.getMergedBus(bus);
        }

    }

    final CalculatedBusTopology calculatedBusTopology
            = new CalculatedBusTopology();

    private static class StateImpl implements State {

        private BusCache cache;

        private StateImpl() {
        }

        private StateImpl(StateImpl other) {
        }

        @Override
        public StateImpl copy() {
            return new StateImpl(this);
        }

    }

    protected final StateArray<StateImpl> states;

    BusBreakerVoltageLevel(String id, String name, SubstationImpl substation,
                           float nominalV, float lowVoltageLimit, float highVoltageLimit) {
        super(id, name, substation, nominalV, lowVoltageLimit, highVoltageLimit);
        states = new StateArray<>(substation.getNetwork().getRef(), () -> new StateImpl());
        graph.addListener(() -> {
            // invalidate topology and connected components
            invalidateCache();
        });
    }

    private void invalidateCache() {
        calculatedBusTopology.invalidateCache();
        getNetwork().getConnectedComponentsManager().invalidate();
    }

    @Override
    public Iterable<Terminal> getTerminals() {
        return FluentIterable.from(graph.getVerticesObj())
                             .transformAndConcat(ConfiguredBus::getTerminals)
                             .transform(Terminal.class::cast);
    }

    @Override
    public <C extends Connectable> FluentIterable<C> getConnectables(Class<C> clazz) {
        Iterable<Terminal> terminals = getTerminals();
        return FluentIterable.from(terminals)
                .transform(Terminal::getConnectable)
                .filter(clazz);
    }

    @Override
    public <C extends Connectable> int getConnectableCount(Class<C> clazz) {
        return getConnectables(clazz).size();
    }

    static ITeslaException createNotSupportedBusBreakerTopologyException() {
        return new ITeslaException("Not supported in a bus breaker topology");
    }

    private final NodeBreakerViewExt nodeBreakerView = new NodeBreakerViewExt() {

        @Override
        public int getNodeCount() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public NodeBreakerView setNodeCount(int count) {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public int getNode1(String switchId) {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public int getNode2(String switchId) {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public SwitchAdder newSwitch() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public InternalConnectionAdder newInternalConnection() { throw createNotSupportedBusBreakerTopologyException();}

        @Override
        public SwitchAdder newBreaker() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public SwitchAdder newDisconnector() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public NodeBreakerView openSwitch(String switchId) {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public NodeBreakerView closeSwitch(String switchId) {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public Switch getSwitch(String switchId) {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public Iterable<Switch> getSwitches() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public int getSwitchCount() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public BusbarSectionAdder newBusbarSection() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public Iterable<BusbarSection> getBusbarSections() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public int getBusbarSectionCount() {
            throw createNotSupportedBusBreakerTopologyException();
        }

        @Override
        public BusbarSection getBusbarSection(String id) {
            throw createNotSupportedBusBreakerTopologyException();
        }

    };

    @Override
    public NodeBreakerViewExt getNodeBreakerView() {
        return nodeBreakerView;
    }

    private final BusBreakerViewExt busBreakerView = new BusBreakerViewExt() {

        @Override
        public Iterable<Bus> getBuses() {
            return Iterables.unmodifiableIterable(Iterables.transform(graph.getVerticesObj(), new Downcast<>()));
        }

        @Override
        public ConfiguredBus getBus(String id) {
            return BusBreakerVoltageLevel.this.getBus(id, false);
        }

        @Override
        public BusAdder newBus() {
            return new BusAdderImpl(BusBreakerVoltageLevel.this);
        }

        @Override
        public void removeBus(String busId) {
            BusBreakerVoltageLevel.this.removeBus(busId);
        }

        @Override
        public void removeAllBuses() {
            BusBreakerVoltageLevel.this.removeAllBuses();
        }

        @Override
        public BusBreakerView openSwitch(String switchId) {
            SwitchImpl _switch = BusBreakerVoltageLevel.this.getSwitch(switchId, true);
            if (!_switch.isOpen()) {
                _switch.setOpen(true);
                invalidateCache();
            }
            return this;
        }

        @Override
        public BusBreakerView closeSwitch(String switchId) {
            SwitchImpl _switch = BusBreakerVoltageLevel.this.getSwitch(switchId, true);
            if (_switch.isOpen()) {
                _switch.setOpen(false);
                invalidateCache();
            }
            return this;
        }

        @Override
        public Iterable<Switch> getSwitches() {
            return Iterables.unmodifiableIterable(Iterables.transform(graph.getEdgesObject(), new Function<SwitchImpl, Switch>() {
                @Override
                public Switch apply(SwitchImpl sw) {
                    return sw;
                }
            }));
        }

        @Override
        public void removeSwitch(String switchId) {
            BusBreakerVoltageLevel.this.removeSwitch(switchId);
        }

        @Override
        public void removeAllSwitches() {
            BusBreakerVoltageLevel.this.removeAllSwitches();
        }

        @Override
        public ConfiguredBus getBus1(String switchId) {
            int e = getEdge(switchId, true);
            int v1 = graph.getEdgeVertex1(e);
            return graph.getVertexObject(v1);
        }

        @Override
        public ConfiguredBus getBus2(String switchId) {
            int e = getEdge(switchId, true);
            int v2 = graph.getEdgeVertex2(e);
            return graph.getVertexObject(v2);
        }

        @Override
        public SwitchImpl getSwitch(String switchId) {
            return BusBreakerVoltageLevel.this.getSwitch(switchId, false);
        }

        @Override
        public BusBreakerView.SwitchAdder newSwitch() {
            return new SwitchAdderImpl();
        }

    };

    @Override
    public BusBreakerViewExt getBusBreakerView() {
        return busBreakerView;
    }

    private final BusViewExt busView = new BusViewExt() {

        @Override
        public Iterable<Bus> getBuses() {
            return Collections.<Bus>unmodifiableCollection(calculatedBusTopology.getMergedBuses());
        }

        @Override
        public MergedBus getBus(String id) {
            return calculatedBusTopology.getMergedBus(id, false);
        }

    };

    @Override
    public BusViewExt getBusView() {
        return busView;
    }

    @Override
    public TopologyKind getTopologyKind() {
        return TopologyKind.BUS_BREAKER;
    }

    void addBus(ConfiguredBus bus) {
        getNetwork().getObjectStore().checkAndAdd(bus);
        int v = graph.addVertex();
        graph.setVertexObject(v, bus);
        buses.put(bus.getId(), v);
    }

    private void removeBus(String busId) {
        ConfiguredBus bus = getBus(busId, true);
        if (bus.getTerminalCount() > 0) {
            throw new ValidationException(this, "Cannot remove bus "
                    + bus.getId() + " because of connectable equipments");
        }
        // TODO improve check efficency
        for (Map.Entry<String, Integer> entry : switches.entrySet()) {
            String switchId = entry.getKey();
            int e = entry.getValue();
            int v1 = graph.getEdgeVertex1(e);
            int v2 = graph.getEdgeVertex2(e);
            ConfiguredBus b1 = graph.getVertexObject(v1);
            ConfiguredBus b2 = graph.getVertexObject(v2);
            if (bus == b1 || bus == b2) {
                throw new RuntimeException("Cannot remove bus '" + bus.getId()
                        + "' because switch '" + switchId + "' is connected to it");
            }
        }
        getNetwork().getObjectStore().remove(bus);
        int v = buses.remove(bus.getId());
        graph.removeVertex(v);
    }

    private void removeAllBuses() {
        if (graph.getEdgeCount() > 0) {
            throw new ValidationException(this, "Cannot remove all buses because there is still some switches");
        }
        for (ConfiguredBus bus : graph.getVerticesObj()) {
            if (bus.getTerminalCount() > 0) {
                throw new ValidationException(this, "Cannot remove bus "
                        + bus.getId() + " because of connected equipments");
            }
        }
        for (ConfiguredBus bus : graph.getVerticesObj()) {
            getNetwork().getObjectStore().remove(bus);
        }
        graph.removeAllVertices();
        buses.clear();
    }

    private void addSwitch(SwitchImpl _switch, String busId1, String busId2) {
        int v1 = getVertex(busId1, true);
        int v2 = getVertex(busId2, true);
        getNetwork().getObjectStore().checkAndAdd(_switch);
        int e = graph.addEdge(v1, v2, _switch);
        switches.put(_switch.getId(), e);
    }

    private void removeSwitch(String switchId) {
        Integer e = switches.remove(switchId);
        if (e == null) {
            throw new RuntimeException("Switch '" + switchId
                    + "' not found in substation voltage level '" + id + "'");
        }
        SwitchImpl _switch = graph.removeEdge(e);
        getNetwork().getObjectStore().remove(_switch);
    }

    private void removeAllSwitches() {
        for (SwitchImpl s : graph.getEdgesObject()) {
            getNetwork().getObjectStore().remove(s);
        }
        graph.removeAllEdges();
        switches.clear();
    }

    private void checkTerminal(TerminalExt terminal) {
        if (!(terminal instanceof BusTerminal)) {
            throw new ValidationException(terminal.getConnectable(),
                    "voltage level " + BusBreakerVoltageLevel.this.id + " has a bus/breaker topology"
                    + ", a bus connection should be specified instead of a node connection");
        }

        // check connectable buses exist
        String connectableBusId = ((BusTerminal) terminal).getConnectableBusId();
        if (connectableBusId != null) {
            getBus(connectableBusId, true);
        }
    }

    @Override
    public void attach(final TerminalExt terminal, boolean test) {
        checkTerminal(terminal);
        if (test) {
            return;
        }
        // create the link terminal -> voltage level
        terminal.setVoltageLevel(this);

        // create the link bus -> terminal
        String connectableBusId = ((BusTerminal) terminal).getConnectableBusId();

        final ConfiguredBus connectableBus = getBus(connectableBusId, true);

        getNetwork().getStateManager().forEachState(() -> {
            connectableBus.addTerminal((BusTerminal) terminal);

            // invalidate connected components
            invalidateCache();
        });
    }

    @Override
    public void detach(final TerminalExt terminal) {
        assert terminal instanceof BusTerminal;

        // remove the link terminal -> voltage level
        terminal.setVoltageLevel(null);

        // remove the link bus -> terminal
        String connectableBusId = ((BusTerminal) terminal).getConnectableBusId();

        final ConfiguredBus connectableBus = getBus(connectableBusId, true);

        getNetwork().getStateManager().forEachState(() -> {
            connectableBus.removeTerminal((BusTerminal) terminal);
            ((BusTerminal) terminal).setConnectableBusId(null);

            invalidateCache();
        });
    }

    @Override
    public void clean() {
        // nothing to do
    }

    @Override
    public void connect(TerminalExt terminal) {
        assert terminal instanceof BusTerminal;

        // already connected?
        if (((BusTerminal) terminal).isConnected()) {
            return;
        }

        ((BusTerminal) terminal).setConnected(true);

        // invalidate connected components
        invalidateCache();
    }

    @Override
    public void disconnect(TerminalExt terminal) {
        assert terminal instanceof BusTerminal;

        // already connected?
        if (!terminal.isConnected()) {
            return;
        }

        ((BusTerminal) terminal).setConnected(false);

        // invalidate connected components
        invalidateCache();
    }

    @Override
    public void extendStateArraySize(int initStateArraySize, int number, int sourceIndex) {
        states.push(number, () -> states.copy(sourceIndex));
    }

    @Override
    public void reduceStateArraySize(int number) {
        states.pop(number);
    }

    @Override
    public void deleteStateArrayElement(int index) {
        states.delete(index);
    }

    @Override
    public void allocateStateArrayElement(int[] indexes, final int sourceIndex) {
        states.allocate(indexes, () -> states.copy(sourceIndex));
    }

    @Override
    public void printTopology() {
        printTopology(System.out, null);
    }

    @Override
    public void printTopology(PrintStream out, ShortIdDictionary dict) {
        out.println("-------------------------------------------------------------");
        out.println("Topology of " + BusBreakerVoltageLevel.this.id);
        graph.print(out, bus -> {
            StringBuilder builder = new StringBuilder();
            builder.append(bus.getId())
                    .append(" [");
            for (Iterator<TerminalExt> it = bus.getConnectedTerminals().iterator(); it.hasNext(); ) {
                TerminalExt terminal = it.next();
                builder.append(dict != null ? dict.getShortId(terminal.getConnectable().getId()) : terminal.getConnectable().getId());
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append("]");
            return builder.toString();
        }, aSwitch -> {
            StringBuilder builder = new StringBuilder();
            builder.append("id=").append(aSwitch.getId())
                    .append(" status=").append(aSwitch.isOpen() ? "open" : "closed");
            return builder.toString();
        });
    }

    @Override
    public void exportTopology(String filename) throws IOException {
        try (OutputStream writer = new FileOutputStream(filename)) {
            exportTopology(writer);
        }
    }

    @Override
    public void exportTopology(OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.append("graph \"").append(BusBreakerVoltageLevel.this.id).append("\" {\n");
        for (ConfiguredBus bus : graph.getVerticesObj()) {
            String label = "BUS\\n" + bus.getId();
            writer.append("  ").append(bus.getId())
                        .append(" [label=\"").append(label).append("\"]\n");
            for (TerminalExt terminal : bus.getTerminals()) {
                ConnectableImpl connectable = terminal.getConnectable();
                label = connectable.getType().toString() + "\\n" + connectable.getId();
                writer.append("  ").append(connectable.getId())
                        .append(" [label=\"").append(label).append("\"]\n");
            }
        }
        for (ConfiguredBus bus : graph.getVerticesObj()) {
            for (TerminalExt terminal : bus.getTerminals()) {
                ConnectableImpl connectable = terminal.getConnectable();
                writer.append("  ").append(bus.getId())
                    .append(" -- ").append(connectable.getId())
                    .append(" [").append("style=\"").append(terminal.isConnected() ? "solid" : "dotted").append("\"")
                    .append("]\n");
            }
        }
        boolean drawSwitchId = false;
        for (int e = 0; e < graph.getEdgeCount(); e++) {
            int v1 = graph.getEdgeVertex1(e);
            int v2 = graph.getEdgeVertex2(e);
            SwitchImpl sw = graph.getEdgeObject(e);
            ConfiguredBus bus1 = graph.getVertexObject(v1);
            ConfiguredBus bus2 = graph.getVertexObject(v2);
            writer.append("  ").append(bus1.getId())
                    .append(" -- ").append(bus2.getId())
                    .append(" [");
            if (drawSwitchId) {
                writer.append("label=\"").append(sw.getId())
                        .append("\", fontsize=10");
            }
            writer.append("style=\"").append(sw.isOpen() ? "dotted" : "solid").append("\"");
            writer.append("]\n");
        }
        writer.append("}\n");
    }

}
