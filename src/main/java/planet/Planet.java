package planet;
import utils.Global;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

public class Planet extends Agent implements PlanetInterface {

    //Constants
    public final static int PLANET_SIZE = Global.PLANET_SIZE;
    public final static int ORES_NB = Global.PLANET_ORES_NB;
    public final static int MAX_ORES = Global.PLANET_MAX_ORES;

    //Ground of the planet : 0 = free | X>0 : ore (X is the quantity)
    //Encoded in 1D but its a 2D array
    private int[] ground = new int[PLANET_SIZE * PLANET_SIZE];
    //Record of external objects positions (ship.Ship, agent)
    private Map<String, Integer> positions = new HashMap<>();
    private String shipName;

    private List<AID> agentsAID = new ArrayList<>();
    private AID shipAID;


    //Take 2D coordinates and map it to 1D
    private int c(int x, int y)
    {
        return PLANET_SIZE * x + y;
    }

    private boolean inBounds(int x, int y)
    {
        return c(x, y) >= 0 && c(x, y) < PLANET_SIZE * PLANET_SIZE;
    }

    private boolean in2DBounds(int x, int y)
    {
        return x >= 0 && x < PLANET_SIZE && y >= 0 && y < PLANET_SIZE;
    }
    
    public int middleFreePos()
    {
        int ind = c(PLANET_SIZE/2, PLANET_SIZE/2);
        if (ground[ind] == 0)
            return c(PLANET_SIZE/2, PLANET_SIZE/2);
        else
            while (ground[ind] == 0) ind++;
        return ground[ind];
    }

    //Not random for the moment
    private int randPosAround(int pos)
    {
        int pos_ship = positions.get(Global.SHIP_PREFIX);
        return pos_ship;
    }

    @Override
    public int containsOres(int x, int y)
    {
        return ground[c(x,y)];
    }

    @Override
    public Map<String, Integer> getExternalObjPositions()
    {
        return positions;
    }

    private int canMoveTo(int pos, String move)
    {
        int x = pos / PLANET_SIZE;
        int y = pos % PLANET_SIZE;
        if (move.contains("UP") && in2DBounds(x+1, y)) return c(x+1,y);
        if (move.contains("DOWN") && in2DBounds(x-1, y)) return c(x-1,y);
        if (move.contains("LEFT") && in2DBounds(x, y-1)) return c(x,y-1);
        if (move.contains("RIGHT") && in2DBounds(x, y+1)) return c(x,y+1);
        return -1; //Not possible
    }

    protected void setup()
    {
        registerO2AInterface(PlanetInterface.class, this);

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
        //Save agents and ship
        for (AMSAgentDescription agent : agents)
        {
            if (agent.getName().toString().contains(Global.AGENT_PREFIX))
                agentsAID.add(agent.getName());
            if (agent.getName().toString().contains(Global.SHIP_PREFIX))
                shipAID = agent.getName();
        }

        //Generates randomly the ores
        int randX, randY, nb;
        for (int i = 0; i < ORES_NB; ++i)
        {
            randX = Global.rand(0, PLANET_SIZE);
            randY = Global.rand(0, PLANET_SIZE);
            while (ground[c(randX,randY)] != 0)
            {
                randX = Global.rand(0, PLANET_SIZE);
                randY = Global.rand(0, PLANET_SIZE);
            }
            ground[c(randX,randY)] = Global.rand(1, MAX_ORES);
        }

        //Receive message
        addBehaviour(new HandleMessage());

        System.out.println("Planet "+getAID().getName()+" is ready.");
    }

    private class HandleMessage extends CyclicBehaviour {

        @Override
        public void action()
        {
            MessageTemplate msgTemplate;
            ACLMessage msg;

            //PLANET LAND MESSAGE
            msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("PLANET_LAND")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                shipName = msg.getSender().getLocalName();
                positions.put(shipName, middleFreePos());
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.AGREE);
                send(reply);
            }

            //AGENT DEPLOY MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("SHIP_AGENT_DEPLOY")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                int shipPos = positions.get(shipName);
                int agentPos = randPosAround(shipPos);
                positions.put(msg.getSender().getLocalName(), agentPos);
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.AGREE);
                send(reply);
            }

            //AGENT MOVE MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("AGENT_MOVE")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                int agentPos = positions.get(msg.getSender().getLocalName());
                String agentMove = msg.getContent();
                int new_pos = canMoveTo(agentPos, agentMove);
                if (new_pos != -1)
                {
                    positions.put(msg.getSender().getLocalName(), new_pos);
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.AGREE);
                    reply.setContent(agentMove+";"+ground[new_pos]);
                    send(reply);
                }
            }

            //AGENT MINE MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("AGENT_MINE")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                int agentPos = positions.get(msg.getSender().getLocalName());
                if (ground[agentPos] > 0)
                {
                    ground[agentPos]--; //We assume the robot mine 1 by 1 for the moment
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.AGREE);
                    reply.setContent("1;"+ground[agentPos]); //+1 ore
                    send(reply);
                }
                else
                {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("0"); //No more ore
                    send(reply);
                }
            }

            //LEAVE PLANET MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("LEAVE_PLANET")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                positions.remove(msg.getSender().getLocalName());
            }
        }
    }



    protected void takeDown() {
        System.out.println("Planet "+getAID().getName()+" destroying.");
    }
}
