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
package etpPointCase;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;


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
//import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Calculates evapotranspiration at hourly timestep using FAO Penman-Monteith equation")
@Author(name = "Giuseppe Formetta, Silvia Franceschi and Andrea Antonello", contact = "maryban@hotmail.it")
@Keywords("Evapotranspiration, Hydrology")
@Label("")
@Name("")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")

public class OmsFaoEtpHourly extends JGTModel {

    @Description("The net Radiation at the grass surface in W/m2 for the current hour.")
    @In
    @Unit("MJ m-2 hour-1")
    public HashMap<Integer, double[]> inNetradiation;

    @Description("The net Radiation default value in case of missing data.")
    @In
    @Unit("MJ m-2 hour-1")
    public double defaultNetradiation = 2.0;

    @Description("The average hourly wind speed.")
    @In
    @Unit("m s-1")
    public HashMap<Integer, double[]> inWind;

    @Description("The wind default value in case of missing data.")
    @In
    @Unit("m s-1")
    public double defaultWind = 2.0;

    @Description("The mean hourly air temperature.")
    @In
    @Unit("C")
    public HashMap<Integer, double[]> inTemp;

    @Description("The temperature default value in case of missing data.")
    @In
    @Unit("C")
    public double defaultTemp = 15.0;

    @Description("The average air hourly relative humidity.")
    @In
    @Unit("%")
    public HashMap<Integer, double[]> inRh;

    @Description("The humidity default value in case of missing data.")
    @In
    @Unit("%")
    public double defaultRh = 70.0;

    @Description("The atmospheric pressure in hPa.")
    @In
    @Unit("KPa")
    public HashMap<Integer, double[]> inPressure;

    @Description("The pressure default value in case of missing data.")
    @In
    @Unit("KPa")
    public double defaultPressure = 100.0;

    @Description("The reference evapotranspiration.")
    @Unit("mm hour-1")
    @Out
    public HashMap<Integer, double[]> outFaoEtp;

    @Execute
    public void process() throws Exception {
        outFaoEtp = new HashMap<Integer, double[]>();
        Set<Entry<Integer, double[]>> entrySet = inNetradiation.entrySet();
        for( Entry<Integer, double[]> entry : entrySet ) {
            Integer basinId = entry.getKey();

            double temperature = defaultTemp ;
            if ( inTemp!= null) {
                temperature = entry.getValue()[0];
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
            if (inPressure != null) rh=inRh.get(basinId)[0];
            if (isNovalue(rh)) {
                rh = defaultRh;
            }

            double etp = compute(netradiation, wind, temperature, rh, pressure);
            outFaoEtp.put(basinId, new double[]{etp});
        }

    }

    private double compute( double netradiation, double wind, double temperature, double rh, double pressure ) {

        // Computation of Delta [KPa °C-1]

        double denDelta = (temperature + 237.3) * (temperature + 237.3);
        double expDelta = (17.27 * temperature) / (temperature + 237.3);
        double numDelta = 4098 * (0.6108 * Math.exp(expDelta));
        double delta = numDelta / denDelta;
       // pm.message("delta = " + delta); //$NON-NLS-1$

        // End Computation of Delta

        // Computation of Psicrometric constant gamma[kPa °C-1]

        double gamma = 0.665 * 0.001 * pressure;
        //pm.message("gamma = " + gamma); //$NON-NLS-1$
        // End Computation of Psicrometric constant gamma

        // Computation of mean saturation vapour pressure e0_AirTem [kPa]

        double e0_AirTem = 0.6108 * Math.exp(expDelta);
        //pm.message("e0_AirTem = " + e0_AirTem); //$NON-NLS-1$

        // End of computation of mean saturation vapour pressure e0_AirTem

        // Computation of average hourly actual vapour pressure ea [kPa]

        double ea = e0_AirTem * rh / 100;

        //pm.message("ea = " + ea); //$NON-NLS-1$

        // End of computation average hourly actual vapour pressure ea

        // Computation of Soil heat flux [MJ m-2 day-1]
        boolean islight = true;
        double coeff_G;
        if (islight == true) {
            coeff_G = 0.1;
        } else {
            coeff_G = 0.5;
        }
        double G = coeff_G * netradiation;
        //pm.message("G = " + G); //$NON-NLS-1$

        // End of computation of Soil heat flux

        double num1 = 0.408 * delta * (netradiation - G);
        double num2 = (37 * gamma * wind * (e0_AirTem - ea)) / (temperature + 273);
        double den = delta + gamma * (1 + 0.34 * wind);
        double result = (num1 + num2) / den;
        return result;
    }
}
