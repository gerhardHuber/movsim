package org.movsim.consumption.offline;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.movsim.consumption.input.xml.batch.ColumnInput;
import org.movsim.consumption.input.xml.batch.ConversionInput;
import org.movsim.utilities.Units;

public class InputDataParser {

    private final String timeInputPattern; // = "HH:mm:ss"; // 10:23:21 AM
    private final double speedConversionFactor;
    private final double accelerationConversionFactor = 1;
    private final double slopeConversionFactor;

    private final int timeColumn; // = 3 - 1;
    private final int speedColumn; // = 12 - 1; // km/h
    private final int accelerationColum;// = 26 - 1;
    private final int gradeColumn;// = 24 - 1; // in [%]

    final static int MIN_COLUMNS = 4;

    private double speedPrevious;
    private double timePrevious;
   
    public InputDataParser(ColumnInput columnData, ConversionInput conversionInput) {
        // consider shift from column count to array index
        this.timeColumn = columnData.getTimeColumn() - 1;
        this.speedColumn = columnData.getSpeedColumn() - 1;
        this.accelerationColum = columnData.getAccelerationColumn() - 1;
        this.gradeColumn = columnData.getGradientColumn() - 1;

        this.timeInputPattern = conversionInput.getTimeFormat();
        this.speedConversionFactor = conversionInput.getSpeedConversionFactor();
        this.slopeConversionFactor = conversionInput.getGradientConversionFactor();
    }

    public ConsumptionDataRecord parse(String[] line) throws NumberFormatException {
        if (line.length <= MIN_COLUMNS) {
            throw new NumberFormatException();
        }

        // System.out.println("parse = " + Arrays.toString(line));
        double speed = speedConversionFactor * Double.parseDouble(line[speedColumn]);
        double time = convertToSeconds(line[timeColumn]);
        double acceleration = (accelerationColum<0) ? calcAcceleration(time, speed) : accelerationConversionFactor * Double.parseDouble(line[accelerationColum]);
        double grade = (gradeColumn<0) ? 0 : slopeConversionFactor * Double.parseDouble(line[gradeColumn]);
        speedPrevious = speed;
        timePrevious = time;
        return new ConsumptionDataRecord(time, speed, acceleration, grade);
    }

    private double calcAcceleration(double time, double speed) {
        double estimatedAcceleration = (speed-speedPrevious)/(time-timePrevious);
        if(Math.abs(estimatedAcceleration)>4){
            System.out.println("estimated acc="+estimatedAcceleration+ ", time="+time+", timePrev="+timePrevious+", speed*3.6="+speed*3.6+ ",prevSpeed="+speedPrevious*3.6);
        }
       return (timePrevious==0) ? 0 : Math.min(6, estimatedAcceleration);  // TODO better smoothed acceleration from diff
    }

    private double convertToSeconds(String time) throws NumberFormatException {
        if (timeInputPattern.isEmpty()) {
            return Double.parseDouble(time);
        }
        // System.out.println("timeInputPattern="+timeInputPattern+ ", time="+time);
        DateTime dateTime = DateTime.parse(time, DateTimeFormat.forPattern(timeInputPattern));
        // System.out.println(time + "--> dateTime=" + dateTime);
        return dateTime.getSecondOfDay();
    }

}
