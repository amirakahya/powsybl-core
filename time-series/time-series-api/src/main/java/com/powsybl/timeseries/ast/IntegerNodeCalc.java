/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.timeseries.ast;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.timeseries.TimeSeriesException;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IntegerNodeCalc implements LiteralNodeCalc {

    static final String NAME = "integer";

    private final int value;

    public IntegerNodeCalc(int value) {
        this.value = value;
    }

    @Override
    public double toDouble() {
        return value;
    }

    @Override
    public <R, A> R accept(NodeCalcVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public int getValue() {
        return value;
    }

    @Override
    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeNumberField(NAME, value);
    }

    static NodeCalc parseJson(JsonParser parser) throws IOException {
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            if (token == JsonToken.VALUE_NUMBER_INT) {
                return new IntegerNodeCalc(parser.getIntValue());
            } else {
                throw NodeCalc.createUnexpectedToken(token);
            }
        }
        throw new TimeSeriesException("Invalid integer node calc JSON");
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerNodeCalc) {
            return ((IntegerNodeCalc) obj).value == value;
        }
        return false;
    }
}
