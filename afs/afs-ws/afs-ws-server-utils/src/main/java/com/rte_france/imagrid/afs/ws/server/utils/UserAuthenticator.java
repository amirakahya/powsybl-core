/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.rte_france.imagrid.afs.ws.server.utils;

/**
 * @author Ali Tahanout <ali.tahanout at rte-france.com>
 */
public interface UserAuthenticator {

    void check(String login, String password);
}
