package org.movsim.output.route;

import org.movsim.input.model.output.ConsumptionOnRouteInput;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.Route;
import org.movsim.utilities.ExponentialMovingAverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumptionOnRoute extends OutputOnRouteBase {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(ConsumptionOnRoute.class);

    private final double tauEMA;

    private final double beta;

    private final FileConsumptionOnRoute fileWriter;

    private double instantaneousConsumptionRate;

    private double instConsumptionEMA;

    private double totalConsumption; // TODO unit liters or more general approach

    private int numberOfVehicles;

    public ConsumptionOnRoute(double simulationTimestep, ConsumptionOnRouteInput input, RoadNetwork roadNetwork,
            Route route, boolean writeOutput) {
        super(roadNetwork, route);
        this.tauEMA = input.getTauEMA();
        this.beta = Math.exp(-simulationTimestep / tauEMA);
        fileWriter = (writeOutput) ? new FileConsumptionOnRoute(input.getDt(), route) : null;
        totalConsumption = 0;
    }

    @Override
    public void timeStep(double dt, double simulationTime, long iterationCount) {

        instantaneousConsumptionRate = roadNetwork.instantaneousFuelUsedLiters(route);
        totalConsumption += instantaneousConsumptionRate * dt;

        numberOfVehicles = roadNetwork.vehicleCount(route);

        instConsumptionEMA = (simulationTime == 0) ? instantaneousConsumptionRate : ExponentialMovingAverage.calc(
                instantaneousConsumptionRate, instConsumptionEMA, beta);

        if (fileWriter != null) {
            fileWriter.write(simulationTime, this);
        }

    }

    public double getInstantaneousConsumptionRate() {
        return instantaneousConsumptionRate;
    }

    public double getInstantaneousConsumptionEMA() {
        return instConsumptionEMA;
    }

    public double getTotalConsumption() {
        return totalConsumption;
    }

    public int getNumberOfVehicles() {
        return numberOfVehicles;
    }

}
