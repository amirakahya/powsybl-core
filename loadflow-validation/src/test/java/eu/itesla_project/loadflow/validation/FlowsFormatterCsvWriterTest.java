/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.loadflow.validation;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import org.junit.Test;

import eu.itesla_project.commons.io.table.CsvTableFormatterFactory;
import eu.itesla_project.commons.io.table.TableFormatterConfig;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class FlowsFormatterCsvWriterTest {

    private final String branchId = "branchId";
    private final float p1 = 39.5056f;
    private final float p1_calc = 39.5058f;
    private final float q1 = -3.72344f;
    private final float q1_calc = -3.72348f;
    private final float p2 = -39.5122f;
    private final float p2_calc = -39.5128f;
    private final float q2 = 3.7746f;
    private final float q2_calc = 3.7742f;
    private final double r = 0.04;
    private final double x = 0.423;
    private final double g1 = 0.0;
    private final double g2 = 0.0;
    private final double b1 = 0.0;
    private final double b2 = 0.0;
    private final double rho1 = 1;
    private final double rho2 = 11.249999728;
    private final double alpha1 = 0.0;
    private final double alpha2 = 0.0;
    private final double u1 = 236.80258178710938;
    private final double u2 = 21.04814910888672;
    private final double theta1 = 0.1257718437996544;
    private final double theta2 = 0.12547118123496284;
    private final double z = Math.hypot(r, x);
    private final double y = 1 / z;
    private final double ksi = Math.atan2(r, x);

    @Test
    public void test() throws Exception {
        String flowsContent = String.join(System.lineSeparator(),
                                          "test flow check",
                                          String.join(";","id","network_p1","expected_p1","network_q1","expected_q1","network_p2","expected_p2",
                                                      "network_q2","expected_q2"),
                                          String.join(";", branchId, 
                                                      String.format(Locale.getDefault(), "%g", p1), String.format(Locale.getDefault(), "%g", p1_calc), 
                                                      String.format(Locale.getDefault(), "%g", q1), String.format(Locale.getDefault(), "%g", q1_calc), 
                                                      String.format(Locale.getDefault(), "%g", p2), String.format(Locale.getDefault(), "%g", p2_calc), 
                                                      String.format(Locale.getDefault(), "%g", q2), String.format(Locale.getDefault(), "%g", q2_calc)));
        Writer writer = new StringWriter();
        TableFormatterConfig config = new TableFormatterConfig(Locale.getDefault(), ';', "inv", true, true);
        try (FlowsWriter flowsWriter = new FlowsFormatterCsvWriter("test", CsvTableFormatterFactory.class, config, writer, false)) {
            flowsWriter.write(branchId, p1, p1_calc, q1, q1_calc, p2, p2_calc, q2, q2_calc, r, x, g1, g2, b1, b2, rho1, rho2, 
                              alpha1, alpha2, u1, u2, theta1, theta2, z, y, ksi);
            assertEquals(flowsContent, writer.toString().trim());
        }
    }

    @Test
    public void testVerbose() throws Exception {
        String flowsContent = String.join(System.lineSeparator(),
                                          "test flow check",
                                          String.join(";","id","network_p1","expected_p1","network_q1","expected_q1","network_p2","expected_p2",
                                                      "network_q2","expected_q2", "r","x","g1","g2","b1","b2","rho1","rho2","alpha1","alpha2",
                                                      "u1","u2","theta1","theta2","z","y","ksi"),
                                          String.join(";", branchId, 
                                                      String.format(Locale.getDefault(), "%g", p1), String.format(Locale.getDefault(), "%g", p1_calc), 
                                                      String.format(Locale.getDefault(), "%g", q1), String.format(Locale.getDefault(), "%g", q1_calc), 
                                                      String.format(Locale.getDefault(), "%g", p2), String.format(Locale.getDefault(), "%g", p2_calc), 
                                                      String.format(Locale.getDefault(), "%g", q2), String.format(Locale.getDefault(), "%g", q2_calc),
                                                      String.format(Locale.getDefault(), "%g", r), String.format(Locale.getDefault(), "%g", x), 
                                                      String.format(Locale.getDefault(), "%g", g1), String.format(Locale.getDefault(), "%g", g2), 
                                                      String.format(Locale.getDefault(), "%g", b1), String.format(Locale.getDefault(), "%g", b2), 
                                                      String.format(Locale.getDefault(), "%g", rho1), String.format(Locale.getDefault(), "%g", rho2), 
                                                      String.format(Locale.getDefault(), "%g", alpha1), String.format(Locale.getDefault(), "%g", alpha2), 
                                                      String.format(Locale.getDefault(), "%g", u1), String.format(Locale.getDefault(), "%g", u2), 
                                                      String.format(Locale.getDefault(), "%g", theta1), String.format(Locale.getDefault(), "%g", theta2), 
                                                      String.format(Locale.getDefault(), "%g", z), String.format(Locale.getDefault(), "%g", y), 
                                                      String.format(Locale.getDefault(), "%g", ksi)));
        Writer writer = new StringWriter();
        TableFormatterConfig config = new TableFormatterConfig(Locale.getDefault(), ';', "inv", true, true);
        try (FlowsWriter flowsWriter = new FlowsFormatterCsvWriter("test", CsvTableFormatterFactory.class, config, writer, true)) {
            flowsWriter.write(branchId, p1, p1_calc, q1, q1_calc, p2, p2_calc, q2, q2_calc, r, x, g1, g2, b1, b2, rho1, rho2, 
                              alpha1, alpha2, u1, u2, theta1, theta2, z, y, ksi);
            assertEquals(flowsContent, writer.toString().trim());
        }
    }

}