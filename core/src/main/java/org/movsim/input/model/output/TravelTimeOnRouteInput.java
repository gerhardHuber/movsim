/*
 * Copyright (C) 2010, 2011, 2012 by Arne Kesting, Martin Treiber, Ralph Germ, Martin Budden
 *                                   <movsim.org@gmail.com>
 * -----------------------------------------------------------------------------------------
 * 
 * This file is part of
 * 
 * MovSim - the multi-model open-source vehicular-traffic simulator.
 * 
 * MovSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MovSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MovSim. If not, see <http://www.gnu.org/licenses/>
 * or <http://www.movsim.org>.
 * 
 * -----------------------------------------------------------------------------------------
 */
package org.movsim.input.model.output;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TravelTimeOnRouteInput {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(TravelTimeOnRouteInput.class);

    private final double dt;
    private final String routeLabel;
    private final double tauEMA;
    
    public TravelTimeOnRouteInput(Element elem) {
        dt = Double.parseDouble(elem.getAttributeValue("dt"));
        routeLabel = elem.getAttributeValue("route");
        tauEMA = Double.parseDouble(elem.getAttributeValue("tauEMA"));
    }

    /**
     * Gets the dt.
     * 
     * @return the dt
     */
    public double getDt() {
        return dt;
    }

    /**
     * Gets the route label.
     *
     * @return the route label
     */
    public String getRouteLabel() {
        return routeLabel;
    }

     /**
     * Gets the tau for the exponential moving average
     *
     * @return the tau ema
     */
    public double getTauEMA() {
        return tauEMA;
    }
}
