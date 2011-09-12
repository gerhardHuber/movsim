package org.movsim.facades;

import java.net.URL;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.movsim.App;
import org.movsim.input.impl.InputDataImpl;
import org.movsim.input.impl.ProjectMetaDataImpl;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.impl.SimulatorImpl;
import org.movsim.simulator.roadSection.RoadSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovsimViewerFacade {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(MovsimViewerFacade.class);

    public void initLocalizationAndLogger() {
        final URL log4jConfig = App.class.getResource("/sim/log4j.properties");
        PropertyConfigurator.configure(log4jConfig);
    }

    private Simulator model;

    private InputDataImpl inputData;

    private ProjectMetaDataImpl projectMetaDataImpl;

    public MovsimViewerFacade() {

        model = new SimulatorImpl();

        // init Logger
        initLocalizationAndLogger();

        inputData = (InputDataImpl) model.getSimInput();
        projectMetaDataImpl = inputData.getProjectMetaDataImpl();

        // TODO set project config
        projectMetaDataImpl.setInstantaneousFileOutput(false);
        projectMetaDataImpl.setXmlFromResources(true);
        // TODO extract path

        // reads from src/main/resources --> sim/
        projectMetaDataImpl.setProjectName("/sim/onramp_multilane.xml");

        initializeModel();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.ui.desktop.ControllerInterface#start()
     */
    public void start() {
        // threadSuspended = false;
        model.run();
    }

    public void continuesim() {
        // threadSuspended = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.ui.desktop.ControllerInterface#pause()
     */
    public void pause() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.ui.controller.Controller#initializeModel()
     */
    public void initializeModel() {
        model.initialize();
    }

    public void loadScenarioFromXml(String scenario) {
        String xmlFileName = "/sim/" + scenario + ".xml";

        inputData.setProjectName(xmlFileName);
        initializeModel();
    }

    public void resetWthParams() {
    }

    public void reset() {
    }

    public void resetToDefault() {
    }

    public void update() {
        model.update();
    }

    public Simulator getSimulatorCore() {
        return model;
    }

    public RoadSection getMainroad() {
        return model.getRoadSections().get(0); // hack
    }

    public RoadSection getOnramp() {
        return model.getRoadSections().get(1); // hack
    }
    
    public RoadSection findRoadById(int id) {
        return model.findRoadById(id);
    }
    
    public List<RoadSection> getRoadSections() {
        return model.getRoadSections();
    }

    public double getTimestep() {
        return model.timestep();
    }

    public long getIterationCount() {
        return model.iterationCount();
    }

    public double getSimulationTime() {
        return model.time();
    }

}
