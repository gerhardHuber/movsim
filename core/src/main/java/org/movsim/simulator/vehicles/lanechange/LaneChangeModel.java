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
package org.movsim.simulator.vehicles.lanechange;

import org.movsim.input.model.vehicle.lanechange.LaneChangeInputData;
import org.movsim.simulator.roadnetwork.Lane;
import org.movsim.simulator.roadnetwork.LaneSegment;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class LaneChangeModel.
 */
public class LaneChangeModel {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(LaneChangeModel.class);

    // to avoid flips
    public static double LANECHANGE_TDELAY_S = 3.0; // delay after lane change
    public static double LANECHANGE_TDELAY_FRONT_S = 3.0; // delay after a cut-in ahead

    public enum LaneChangeDecision {

        NONE(Lane.NO_CHANGE), STAY_IN_LANE(Lane.NO_CHANGE), DISCRETIONARY_TO_LEFT(Lane.TO_LEFT), DISCRETIONARY_TO_RIGHT(
                Lane.TO_RIGHT), MANDATORY_TO_LEFT(Lane.TO_LEFT), MANDATORY_TO_RIGHT(Lane.TO_RIGHT), MANDATORY_STAY_IN_LANE(
                Lane.NO_CHANGE);

        private final int laneChangeDirection;

        private LaneChangeDecision(int laneChangeDirection) {
            this.laneChangeDirection = laneChangeDirection;
        }

        public boolean isMandatory() {
            return (this == MANDATORY_TO_LEFT || this == MANDATORY_TO_RIGHT || this == MANDATORY_STAY_IN_LANE);
        }

        public boolean isDiscretionary() {
            return !isMandatory();
        }

        public boolean noDecisionMade() {
            return this == NONE;
        }

        public int getDirection() {
            return laneChangeDirection;
        }

        @Override
        public String toString() {
            return name();
        }

    }

    private final boolean withEuropeanRules;

    /** critical speed for kicking in European rules (in m/s) */
    private final double vCritEur;

    private Vehicle me;

    private final boolean isInitialized;

    private MOBIL lcModelMOBIL;

    private final LaneChangeInputData lcInputData;

    // Exit Handling
    // distance at which driver should think about changing lanes for exit
    private static double distanceBeforeExitWantsToChangeLanes = 500.0;
    // distance at which driver must get into exit lane
    public static double distanceBeforeExitMustChangeLanes = 300.0;

    /**
     * Instantiates a new lane changing model.
     * 
     * @param lcInputData
     *            the lc input data
     */
    public LaneChangeModel(LaneChangeInputData lcInputData) {

        this.lcInputData = lcInputData;
        this.withEuropeanRules = lcInputData.isWithEuropeanRules();
        this.vCritEur = lcInputData.getCritSpeedEuroRules();

        // TODO valid lane change model only if configured by xml
        isInitialized = lcInputData.isInitializedMobilData();
    }

    public LaneChangeModel(Vehicle vehicle, MOBIL lcModelMOBIL) {
        this.lcModelMOBIL = lcModelMOBIL;
        this.me = vehicle;
        this.withEuropeanRules = true;
        this.vCritEur = 5.0;
        this.lcInputData = null;
        isInitialized = true;
    }

    /**
     * Initialize.
     * 
     * @param vehicle
     *            the vehicle
     */
    public void initialize(Vehicle vehicle) {
        this.me = vehicle;
        lcModelMOBIL = (isInitialized) ? new MOBIL(me, lcInputData.getLcMobilData()) : new MOBIL(me);
    }

    /**
     * Checks if is initialized.
     * 
     * @return true, if is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean withEuropeanRules() {
        return withEuropeanRules;
    }

    public double vCritEurRules() {
        return vCritEur;
    }

    public LaneChangeDecision makeDecision(RoadSegment roadSegment) {
        LaneChangeDecision decision = LaneChangeDecision.NONE;

        // check for mandatory lane changes to reach exit lane
        decision = checkForMandatoryLaneChangeToExit(roadSegment);
        if (decision.isMandatory()) {
            return decision;
        }

        decision = checkForMandatoryLaneChangeAtEntrance(roadSegment);
        if (decision.isMandatory()) {
            return decision;
        }
        
        decision = checkForLaneChangeForEnteringVehicle(roadSegment);
        if(!decision.noDecisionMade()){
            return decision;
        }
        

        // check discretionary lane changes
        decision = determineDiscretionaryLaneChangeDirection(roadSegment);

        return decision;
    }

    private boolean isSafeLaneChange(LaneSegment laneSegment) {
        final Vehicle front = laneSegment.frontVehicle(me);
        final Vehicle back = laneSegment.rearVehicle(me);
        final boolean changeSafe = checkSafetyCriterion(front, back);
        return changeSafe;
    }

    private boolean checkSafetyCriterion(Vehicle frontVeh, Vehicle backVeh) {

        final double safeDeceleration = lcModelMOBIL.getSafeDeceleration();

        // check distance to front vehicle
        final double gapFront = me.getNetDistance(frontVeh);
        if (gapFront < lcModelMOBIL.getMinimumGap()) {
            logger.debug("gapFront={}", gapFront);
            return false;
        }

        // check distance to vehicle at behind
        if (backVeh != null) {
            final double gapBack = backVeh.getNetDistance(me);
            if (gapBack < lcModelMOBIL.getMinimumGap()) {
                logger.debug("gapBack={}", gapBack);
                return false;
            }
            // check acceleration of back vehicle
            final double backNewAcc = backVeh.getLongitudinalModel().calcAcc(backVeh, me);
            if (backNewAcc <= -safeDeceleration) {
                logger.debug("gapFront = {}, gapBack = {}", gapFront, gapBack);
                logger.debug("backNewAcc={}, bSafe={}", backNewAcc, safeDeceleration);
                return false;
            }
        }

        // check acceleration of vehicle ahead
        final double meNewAcc = me.getLongitudinalModel().calcAcc(me, frontVeh);
        if (meNewAcc >= -safeDeceleration) {
            logger.debug("meNewAcc={}, bSafe={}", meNewAcc, safeDeceleration);
            return true;
        }

        return false;
    }

    private LaneChangeDecision determineDiscretionaryLaneChangeDirection(RoadSegment roadSegment) {

        final int currentLane = me.getLane();
        // initialize with largest possible deceleration
        double accToLeft = -Double.MAX_VALUE;
        double accToRight = -Double.MAX_VALUE;
        // consider lane-changing to right-hand side lane (decreasing lane index)
        if (currentLane + Lane.TO_RIGHT >= Lane.MOST_RIGHT_LANE) {
            final LaneSegment newLaneSegment = roadSegment.laneSegment(currentLane + Lane.TO_RIGHT);
            if (newLaneSegment.type() == Lane.Type.TRAFFIC) {
                // only consider lane changes into traffic lanes, other lane changes are handled by mandatory lane
                // changing
                accToRight = lcModelMOBIL.calcAccelerationBalance(me, Lane.TO_RIGHT, roadSegment);
            }
        }

        // consider lane-changing to left-hand side lane (increasing the lane index)
        if (currentLane + Lane.TO_LEFT < roadSegment.laneCount()) {
            final LaneSegment newLaneSegment = roadSegment.laneSegment(currentLane + Lane.TO_LEFT);
            if (newLaneSegment.type() == Lane.Type.TRAFFIC) {
                // only consider lane changes into traffic lanes, other lane changes are handled by mandatory lane
                // changing
                accToLeft = lcModelMOBIL.calcAccelerationBalance(me, Lane.TO_LEFT, roadSegment);
            }
        }

        // decision
        if ((accToRight > 0) || (accToLeft > 0)) {
            logger.debug("accToRight={}, accToLeft={}", accToRight, accToLeft);
            logger.debug("currentLane={}", currentLane);
            if (accToRight > accToLeft) {
                return LaneChangeDecision.DISCRETIONARY_TO_RIGHT;
            }
            return LaneChangeDecision.DISCRETIONARY_TO_LEFT;
        }

        return LaneChangeDecision.STAY_IN_LANE;
    }

    private LaneChangeDecision checkForMandatoryLaneChangeAtEntrance(RoadSegment roadSegment) {
        final int currentLane = me.getLane();
        final LaneSegment currentLaneSegment = roadSegment.laneSegment(currentLane);

        if (currentLaneSegment.type() == Lane.Type.ENTRANCE) {
            int direction = (currentLane == Lane.MOST_RIGHT_LANE) ? Lane.TO_LEFT : Lane.TO_RIGHT;
            final LaneSegment newLaneSegment = roadSegment.laneSegment(currentLane + direction);
            if (newLaneSegment != null && isSafeLaneChange(newLaneSegment)) {
                double distanceToRoadSegmentEnd = me.getDistanceToRoadSegmentEnd();
                if (distanceToRoadSegmentEnd < 0) {
                    // just a hack. should not happen.
                    logger.info("check this: roadSegmentLength not set. Do mandatory lane change anyway.");
                    return (direction == Lane.TO_LEFT) ? LaneChangeDecision.MANDATORY_TO_LEFT
                            : LaneChangeDecision.MANDATORY_TO_RIGHT;
                }
                // evaluate additional motivation to leave entrance lane
                double accInCurrentLane = me.getLongitudinalModel().calcAcc(me, currentLaneSegment.frontVehicle(me));
                double accInNewLane = me.getLongitudinalModel().calcAcc(me, newLaneSegment.frontVehicle(me));
                double bias = biasForMandatoryChange(distanceToRoadSegmentEnd);
                if (accInNewLane + bias > accInCurrentLane) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String
                                .format("change lane: veh.id=%d, distanceToRoadSegmentEnd=%.2f, accInCurrentLane=%.2f, accInNewLane=%.2f, bias=%.2f",
                                        me.getId(), distanceToRoadSegmentEnd, accInCurrentLane, accInNewLane, bias));
                    }
                    return (direction == Lane.TO_LEFT) ? LaneChangeDecision.MANDATORY_TO_LEFT
                            : LaneChangeDecision.MANDATORY_TO_RIGHT;
                }
            }
            return LaneChangeDecision.MANDATORY_STAY_IN_LANE;
        }

        return LaneChangeDecision.NONE;
    }

    private double biasForMandatoryChange(double distanceToRoadSegmentEnd) {
        final double interactionDistance = 10;
        double bias = me.getMaxDeceleration() * interactionDistance / Math.max(distanceToRoadSegmentEnd, 10.0);
        return bias;
    }

    private LaneChangeDecision checkForMandatoryLaneChangeToExit(RoadSegment roadSegment) {
        final int currentLane = me.getLane();

        // consider mandatory lane-change to exit
        if (me.exitRoadSegmentId() == roadSegment.id()) {
            if (currentLane == Lane.LANE1) {
                // already in exit lane, so do not move out of it
                return LaneChangeDecision.MANDATORY_STAY_IN_LANE;
            } else if (currentLane > Lane.LANE1) {
                // evaluate situation on the right lane
                final LaneSegment newLaneSegment = roadSegment.laneSegment(currentLane + Lane.TO_RIGHT);
                if (isSafeLaneChange(newLaneSegment)) {

                    return LaneChangeDecision.MANDATORY_TO_RIGHT;
                }
                return LaneChangeDecision.MANDATORY_STAY_IN_LANE;
            }
        }

        // consider mandatory lane-change to exit on next road segment ahead
        final LaneSegment sinkLaneSegment = roadSegment.laneSegment(currentLane).sinkLaneSegment();
        if (sinkLaneSegment != null && me.exitRoadSegmentId() == sinkLaneSegment.roadSegment().id()) {
            // next road segment is the exit segment
            final double distanceToExit = roadSegment.roadLength() - me.getFrontPosition();
            if (distanceToExit < distanceBeforeExitMustChangeLanes) {
                if (currentLane == Lane.LANE1) {
                    // already in exit lane, so do not move out of it
                    return LaneChangeDecision.MANDATORY_STAY_IN_LANE;
                } else if (currentLane > Lane.LANE1) {
                    final LaneSegment newLaneSegment = roadSegment.laneSegment(currentLane + Lane.TO_RIGHT);
                    if (isSafeLaneChange(newLaneSegment)) {
                        return LaneChangeDecision.MANDATORY_TO_RIGHT;
                    }
                    return LaneChangeDecision.MANDATORY_STAY_IN_LANE;
                }
            }
        }
        return LaneChangeDecision.NONE;
    }

    

    // TODO first drop of cooperative lane-changing behavior
    private LaneChangeDecision checkForLaneChangeForEnteringVehicle(RoadSegment roadSegment) {
        final int currentLane = me.getLane();
        LaneSegment entryLaneSegment = roadSegment.laneSegment(Lane.LANE1);
        if(currentLane == Lane.LANE2 && entryLaneSegment.type() == Lane.Type.ENTRANCE){
            if(roadSegment.laneCount()<=2){
                // change to left not possible
                return LaneChangeDecision.NONE;
            }
            Vehicle frontVehicle = entryLaneSegment.frontVehicle(me);
            if(frontVehicle==null || frontVehicle.type() == Vehicle.Type.OBSTACLE){
                return LaneChangeDecision.NONE;
            }
            
            double accToFront = me.getLongitudinalModel().calcAcc(me, frontVehicle);
            if(accToFront < -lcModelMOBIL.getSafeDeceleration()){
                // check own disadvantage to change to left to decide to make room
                if (logger.isDebugEnabled()) {
                    logger.debug(String
                            .format("next to entrance lane: pos=%.2f, lane=%d, netGap=%.2f, ownSpeed=%.2f, dv=%.2f, calcAccToFront=%.2f",
                                    me.getFrontPosition(), currentLane, me.getNetDistance(frontVehicle), me.getSpeed(),
                                    me.getRelSpeed(frontVehicle), accToFront));
                }
                final int newLane = currentLane + Lane.TO_LEFT; 
                final LaneSegment newLaneSegment = roadSegment.laneSegment(newLane);

                if (newLaneSegment.type() == Lane.Type.ENTRANCE) {
                    // never change lane into an entrance lane
                    return LaneChangeDecision.NONE; 
                }
                final Vehicle newFront = newLaneSegment.frontVehicle(me);
                if (newFront != null) {
                    if (newFront.inProcessOfLaneChange()) {
                        return LaneChangeDecision.NONE;
                    }
                    final double gapFront = me.getNetDistance(newFront);
                    if (gapFront < lcModelMOBIL.getMinimumGap()) {
                        return LaneChangeDecision.NONE;
                    }
                }
                final Vehicle newBack = newLaneSegment.rearVehicle(me);
                if (newBack != null) {
                    if (newBack.inProcessOfLaneChange()) {
                        return LaneChangeDecision.NONE;
                    }
                    final double gapRear = newBack.getNetDistance(me);
                    if (gapRear < lcModelMOBIL.getMinimumGap()) {
                        return LaneChangeDecision.NONE;
                    }
                }
                me.setLane(newLane);
                final int index = newLaneSegment.addVehicleTemp(me);
                final double newBackNewAcc = newBack == null ? 0 : newBack.calcAccModel(newLaneSegment, null);
                final double meNewAcc = me.calcAccModel(newLaneSegment, null);
                newLaneSegment.removeVehicle(index);
                me.setLane(currentLane);

                if (lcModelMOBIL.safetyCheckAcceleration(newBackNewAcc) || lcModelMOBIL.safetyCheckAcceleration(meNewAcc)) {
                    return LaneChangeDecision.NONE;
                }
                logger.debug("finally change to left to make room for vehicle at entrance lane ...");
                return LaneChangeDecision.DISCRETIONARY_TO_LEFT;
            }
        }
        return LaneChangeDecision.NONE;
    }

}
