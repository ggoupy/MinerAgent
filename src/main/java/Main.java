import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.*;
import planet.PlanetGui;
import robot.BigRobot;
import robot.FastRobot;
import robot.Robot;
import ship.Ship;
import ship.ShipGui;
import ship.ShipInterface;
import utils.Global;
import utils.Scenario;

public class Main {

    public static void main(String[] args)
    {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();

        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");
        ContainerController containerController = runtime.createMainContainer(profile);

        try {
            //Load Scenario
            Scenario.largeScale();

            AgentController[] agents = new AgentController[Global.AGENT_NB];
            AgentController ship, planet;

            //Init agents
            //Robots
            for (int i = 0; i < Global.AGENT_NB; ++i)
            {
                Object[] agentArgs = i < Global.AGENT_NB/2 ? FastRobot.args() : BigRobot.args();
                agents[i] = containerController.createNewAgent(Global.AGENT_PREFIX+i, "robot.Robot", agentArgs);
            }
            //Ship
            ship = containerController.createNewAgent(Global.SHIP_PREFIX, "ship.Ship", null);
            //Planet
            planet = containerController.createNewAgent(Global.PLANET_PREFIX, "planet.Planet", null);

            //Start
            planet.start();
            ship.start();
            for (AgentController a : agents)
                a.start();

            //GUIs
            PlanetGui planetGui = new PlanetGui(planet);
            planetGui.update(); planetGui.showGui();
            ShipGui shipGui = new ShipGui(ship, agents);
            shipGui.update(); shipGui.showGui();

            //Wait a bit
            Thread.sleep(3000);

            //The ship lands on the planet
            ShipInterface shipInterface = ship.getO2AInterface(ShipInterface.class);
            shipInterface.landOnPlanet();

            //Update display
            while(shipInterface.onPlanet())
            {
                planetGui.update();
                shipGui.update();
                Thread.sleep(200);
            }

            Thread.sleep(2000);
            //Turning off everything
            planet.kill();
            ship.kill();
            for (AgentController a : agents) a.kill();
            planetGui.dispose();
            shipGui.dispose();
            containerController.kill();
            runtime.shutDown();
        }
        catch (StaleProxyException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}