package ship;
import utils.Global;
import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.List;


public class Ship extends Agent implements ShipInterface {

    private List<AID> agentsAID = new ArrayList<>();
    private List<AID> active_agents = new ArrayList<>();
    private AID planetAID;
    private int tank = 0;
    private int max_tank = Global.SHIP_MAX_TANK;
    private String activity;
    private boolean onPlanet = false;
    private boolean stopSignal = false;
    private int max_time_onPlanet = Global.SHIP_MAX_TIME_ON_PLANET;

    protected void setup()
    {
        registerO2AInterface(ShipInterface.class, this);

        //Get other agents
        AMSAgentDescription[] agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults ((long) -1);
            agents = AMSService.search( this, new AMSAgentDescription (), c);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //Save agents and planet
        for (AMSAgentDescription agent : agents)
        {
            if (agent.getName().toString().contains(Global.AGENT_PREFIX))
                agentsAID.add(agent.getName());
            if (agent.getName().toString().contains(Global.PLANET_PREFIX))
                planetAID = agent.getName();
        }

        System.out.println("Ship "+getAID().getName()+" is ready.");

        //Receive message
        addBehaviour(new HandleMessage());

        //Check status
        addBehaviour(new CheckStatus());

        //Stop mining after a period of time
        addBehaviour(new TickerBehaviour(this, max_time_onPlanet) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onTick() {
                stopSignal();
            }
        });
    }

    private class HandleMessage extends CyclicBehaviour {
        @Override
        public void action()
        {
            MessageTemplate msgTemplate;
            ACLMessage msg;

            //PLANET LAND ACCEPT MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                    MessageTemplate.MatchConversationId("PLANET_LAND")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                activity = "DEPLOYING AGENTS";
                //deploy agents
                for (AID agentAID : agentsAID)
                {
                    ACLMessage deployMsg = new ACLMessage(ACLMessage.REQUEST);
                    deployMsg.addReceiver(agentAID);
                    deployMsg.setContent("MAYDAY DEPLOY IMMINENT");
                    deployMsg.setConversationId("PLANET_DEPLOY");
                    send(deployMsg);
                }
            }

            //AGENT DEPLOY INFORM SUCCESS
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                    MessageTemplate.MatchConversationId("PLANET_DEPLOY")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                if (!active_agents.contains(msg.getSender()))
                    active_agents.add(msg.getSender());
                if (active_agents.size() >= agentsAID.size()) activity = "MINING OPERATION";

            }

            //AGENT DROP REQUEST MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("AGENT_DROP")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                ACLMessage reply = msg.createReply();
                int quantity = Integer.parseInt(msg.getContent());
                tank += quantity;
                int remainder = 0;
                if (tank >= max_tank)
                {
                    remainder = tank - max_tank;
                    tank = max_tank;
                }
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(String.valueOf(remainder));
                send(reply);
            }

            //AGENT GET BACK INFORM
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("AGENT_BACK")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.AGREE);
                send(reply);
                if (active_agents.contains(msg.getSender()))
                    active_agents.remove(msg.getSender());
            }
        }
    }

    private class CheckStatus extends Behaviour {
        private boolean full_checked = false;
        private boolean done = false;
        @Override
        public void action()
        {
            //Capacity reached
            if (tank >= max_tank && !full_checked)
            {
                //message to everyone we get back here
                stopSignal();
                full_checked = true;
            }
            //All agents inside
            if (active_agents.size() == 0 && stopSignal)
            {
                activity = "TAKING-OFF! PFIOOU";
                done = true;
                try {
                    Thread.sleep(3000); //Wait a bite
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Tell the planet he leaves it
                ACLMessage leaveMsg = new ACLMessage(ACLMessage.INFORM);
                leaveMsg.addReceiver(planetAID);
                leaveMsg.setConversationId("LEAVE_PLANET");
                send(leaveMsg);
                onPlanet = false;
            }
        }
        @Override
        public boolean done() {
            return done;
        }
    }

    //Send message to all agents to get back in the ship
    private void stopSignal()
    {
        stopSignal = true;
        activity = "WAITING FOR TAKING OFF";
        for (AID agentAID : agentsAID)
        {
            ACLMessage deployMsg = new ACLMessage(ACLMessage.REQUEST);
            deployMsg.addReceiver(agentAID);
            deployMsg.setContent("MAYDAY MAYDAY GET BACK TO THE SHIP");
            deployMsg.setConversationId("MISSION_OVER");
            send(deployMsg);
        }
    }

    protected void takeDown()
    {
        System.out.println("Ship "+getAID().getName()+" terminating.");
    }

    @Override
    public void landOnPlanet()
    {
        //Send message to planet
        ACLMessage landMsg = new ACLMessage(ACLMessage.INFORM);
        landMsg.addReceiver(planetAID);
        landMsg.setContent("");
        landMsg.setConversationId("PLANET_LAND");
        send(landMsg);
        onPlanet = true;
    }

    @Override
    public boolean onPlanet()
    {
        return onPlanet;
    }

    @Override
    public String getInfo()
    {
        return "Tank : " + tank + " / " + max_tank + "\r\n"
                + "Nb active robots : " + active_agents.size() + "\r\n"
                + "Activity : " + activity;
    }
}