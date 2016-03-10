/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package etp;

import static java.lang.Math.exp;
import static java.lang.Math.pow;


import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Calculates evapotranspiration at daily timestep using FAO Penman-Monteith equation")
@Author(name = "Giuseppe Formetta, Silvia Franceschi and Andrea Antonello", contact = "maryban@hotmail.it")
@Keywords("Evapotranspiration, Hydrology")
@Label("")
@Name("")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class OmsFaoEtpDaily extends JGTModel {

    @Description("The net Radiation at the grass surface in W/m2 for the current day.")
    @In
    @Unit("MJ m-2 day-1")
    public HashMap<Integer, double[]> inNetradiation;

    @Description("The net Radiation default value in case of missing data.")
    @In
    @Unit("MJ m-2 day-1")
    public double defaultNetradiation = 2.0;

    @Description("The average daily wind speed.")
    @In
    @Unit("m s-1")
    public HashMap<Integer, double[]> inWind;

    @Description("The wind default value in case of missing data.")
    @In
    @Unit("m s-1")
    public double defaultWind = 2.0;

    @Description("The maximum daily air temperature.")
    @In
    @Unit("C")
    public HashMap<Integer, double[]> inMaxTemp;

    @Description("The minimum daily air temperature.")
    @In
    @Unit("C")
    public HashMap<Integer, double[]> inMinTemp;

    @Description("The max temperature default value in case of missing data.")
    @In
    @Unit("C")
    public double defaultMaxTemp = 15.0;

    @Description("The min temperature default value in case of missing data.")
    @In
    @Unit("C")
    public double defaultMinTemp = 0.0;

    @Description("The average air daily relative humidity.")
    @In
    @Unit("%")
    public HashMap<Integer, double[]> inRh;

    @Description("The humidity default value in case of missing data.")
    @In
    @Unit("%")
    public double defaultRh = 70.0;

    @Description("The average atmospheric daily air pressure in hPa.")
    @In
    @Unit("KPa")
    public HashMap<Integer, double[]> inPressure;

    @Description("The pressure default value in case of missing data.")
    @In
    @Unit("KPa")
    public double defaultPressure = 100.0;

    // TODO Add the elevation value in case of missing P data

    @Description("The reference evapotranspiration.")
    @Unit("mm day-1")
    @Out
    public HashMap<Integer, double[]> outFaoEtp;

    @Execute
    public void process() throws Exception {

        outFaoEtp = new HashMap<Integer, double[]>();

        Set<Entry<Integer, double[]>> entrySet = inMaxTemp.entrySet();
        for( Entry<Integer, double[]> entry : entrySet ) {
            Integer basinId = entry.getKey();

            double maxTemperature = defaultMaxTemp;
            if (inMaxTemp != null) {
                maxTemperature = entry.getValue()[0];
            }

            double minTemperature = defaultMinTemp;
            if (inMinTemp != null) {
                minTemperature = inMinTemp.get(basinId)[0];
            }

            double netradiation = defaultNetradiation;
            if (inNetradiation != null) {
                netradiation = inNetradiation.get(basinId)[0] * 3.6 / 1000.0;
            }

            double wind = defaultWind;
            if (inWind != null) {
                wind = inWind.get(basinId)[0];
            }

            double pressure = defaultPressure;
            if (inPressure != null) {
                pressure = inPressure.get(basinId)[0] / 10.0;
            }

            double rh = defaultRh;
            if (inRh != null) {
                rh = inRh.get(basinId)[0];
            }

            double etp = compute(netradiation, wind, maxTemperature, minTemperature, rh, pressure);
            outFaoEtp.put(basinId, new double[]{etp});
        }
    }

    private double compute( double netradiation, double wind, double maxtemperature, double mintemperature, double rh,
            double pressure ) {

        // Computation of Delta [KPa °C-1]
        double meanTemperature = (maxtemperature + mintemperature) / 2.0;
        double denDelta = pow(meanTemperature + 237.3, 2);
        double expDelta = (17.27 * meanTemperature) / (meanTemperature + 237.3);
        double expDeltaMax = (17.27 * maxtemperature) / (maxtemperature + 237.3);
        double expDeltaMin = (17.27 * mintemperature) / (mintemperature + 237.3);
        double numDelta = 4098 * (0.6108 * exp(expDelta));
        double delta = numDelta / denDelta;
        //pm.message("delta = " + delta); //$NON-NLS-1$

        // End Computation of Delta

        // Computation of Psicrometric constant gamma[kPa °C-1]

        double gamma = 0.665 * 0.001 * pressure;
        //pm.message("gamma = " + gamma); //$NON-NLS-1$
        // End Computation of Psicrometric constant gamma

        // Computation of mean saturation vapour pressure e0_AirTem [kPa]

        double e0_AirTemMax = 0.6108 * exp(expDeltaMax);
        double e0_AirTemMin = 0.6108 * exp(expDeltaMin);
        double es = (e0_AirTemMax + e0_AirTemMin) / 2.0;
        //pm.message("e0_AirTem = " + es); //$NON-NLS-1$

        // End of computation of mean saturation vapour pressure e0_AirTem

        // Computation of average hourly actual vapour pressure ea [kPa]

        double ea = rh / 100.0 * ((e0_AirTemMax + e0_AirTemMin) / 2.0);

        //pm.message("ea = " + ea); //$NON-NLS-1$

        // End of computation average hourly actual vapour pressure ea

        // compute the daily evapotranspiration in mm/day
        double num1 = 0.408 * delta * netradiation;
        double num2 = gamma * 900.0 / (meanTemperature + 273) * wind * (es - ea);
        double den = delta + gamma * (1 + 0.34 * wind);
        double result = (num1 + num2) / den;
        return result;
    }
}
