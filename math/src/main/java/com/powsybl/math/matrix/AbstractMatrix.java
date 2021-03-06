/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.math.matrix;

import java.util.Objects;

/**
 * Abstract class for matrix that provides an implementation for common methods.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractMatrix implements Matrix {

    /**
     * Get an estimation of non zero value count.
     *
     * @return an estimation of non zero value count
     */
    protected abstract int getEstimatedNonZeroValueCount();

    /**
     * {@inheritDoc}
     */
    @Override
    public Matrix copy(MatrixFactory factory) {
        Objects.requireNonNull(factory);
        Matrix matrix = factory.create(getRowCount(), getColumnCount(), getEstimatedNonZeroValueCount());
        iterateNonZeroValue(matrix::set);
        return matrix;
    }
}
