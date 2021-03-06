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
package org.movsim.simulator.roadnetwork;

import java.util.List;

import org.movsim.input.model.simulation.InflowDataPoint;
import org.movsim.utilities.Tables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class InflowTimeSeries.
 */
public class InflowTimeSeries {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(InflowTimeSeries.class);

    private double[] timeValues;

    private double[] flowValues;

    private double[] speedValues;

    private double constantFlowPerLane = -1;

    /** constant init speed = 80 km/h */
    private final double constantInitSpeed = 80 / 3.6;

    /**
     * Instantiates a new inflow time series.
     * 
     * @param inflowTimeSeries
     *            the inflow time series
     */
    public InflowTimeSeries(List<InflowDataPoint> inflowTimeSeries) {
        generateTimeSeriesData(inflowTimeSeries);
    }

    /**
     * Generate time series data.
     * 
     * @param inflowTimeSeries
     *            the inflow time series
     */
    private void generateTimeSeriesData(List<InflowDataPoint> inflowTimeSeries) {
        final int size = inflowTimeSeries.size();

        logger.info(" inflowDataPoint.size = {}", size);
        if (inflowTimeSeries.isEmpty()) {
            logger.info("no inflow data points --> no inflow.");
        }
        timeValues = new double[size];
        flowValues = new double[size];
        speedValues = new double[size];
        for (int i = 0; i < size; i++) {
            final InflowDataPoint inflowDataPoint = inflowTimeSeries.get(i);
            timeValues[i] = inflowDataPoint.getTime();
            flowValues[i] = inflowDataPoint.getFlow();
            speedValues[i] = inflowDataPoint.getSpeed();
            logger.debug("add data: flow={}, speed={}", flowValues[i], speedValues[i]);
        }
    }

    /**
     * Gets the flow.
     * 
     * @param time
     *            the time
     * @return the flow
     */
    public double getFlowPerLane(double time) {
        if (constantFlowPerLane >= 0) {
            return constantFlowPerLane;
        }
        return Tables.intpextp(timeValues, flowValues, time);
    }

    /**
     * Gets the speed.
     * 
     * @param time
     *            the time
     * @return the speed
     */
    public double getSpeed(double time) {
        if (constantFlowPerLane >= 0) {
            return constantInitSpeed;
        }
        return Tables.intpextp(timeValues, speedValues, time);
    }

    public void setConstantFlowPerLane(double newFlowPerLane) {
        logger.debug("set new flow per lane value={} per second", newFlowPerLane);
        assert newFlowPerLane >= 0;
        this.constantFlowPerLane = newFlowPerLane;
    }
}
