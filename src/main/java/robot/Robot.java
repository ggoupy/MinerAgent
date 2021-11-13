package robot;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.Global;
import java.util.*;
import java.util.stream.Collectors;

public class Robot extends Agent implements RobotInterface {
    private List<AID> agentsAID = new ArrayList<>();
    private AID shipAID;
    private AID planetAID;
    private boolean on = false;
    private boolean mining = false;
    private boolean exploring = false;
    private boolean mission_done = false;
    //Ship pos is 0,0
    private int localPosX = 0;
    //Memory of explored areas
    private List<String> memory = new ArrayList<>();
    private int localPosY = 0;
    private int tank = 0;
    private int max_tank = Global.AGENT_MAX_TANK;
    private ChooseDepositStrategy chooseDepositStrategy = ChooseDepositStrategy.BY_BIGGER;
    private int actionDelay = 1000; //Time between actions
    private int local_env = 0; //0 or x>0 if ores
    private Map<String, Integer> ore_deposits = new HashMap<>();
    private List<String> ore_deposits_done = new ArrayList<>();
    String[] MOVES = new String[]{"UP","DOWN","RIGHT","LEFT"};


    public enum ChooseDepositStrategy {
        BY_CLOSER,
        BY_BIGGER,
    }
    private class PositionComparator implements Comparator<Map.Entry<String,Integer>>
    {
        public int compare(Map.Entry<String,Integer> pos1, Map.Entry<String,Integer> pos2)
        {
            int x1 = Global.X(pos1.getKey());
            int y1 = Global.Y(pos1.getKey());
            int distance_1 = Math.abs(x1-localPosX) + Math.abs(y1-localPosY);
            int x2 = Global.X(pos2.getKey());
            int y2 = Global.Y(pos2.getKey());
            int distance_2 = Math.abs(x2-localPosX) + Math.abs(y2-localPosY);
            return Integer.compare(distance_2, distance_1);
        }
    }
    private String getActiveDepositPos(ChooseDepositStrategy strategy)
    {
        //Actives deposits sorted by value or position in descending order
        Iterator<Map.Entry<String, Integer>> actives = ore_deposits.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(strategy == ChooseDepositStrategy.BY_BIGGER ? Map.Entry.comparingByValue() : new PositionComparator())
                .collect(Collectors.toCollection(ArrayDeque::new))
                .descendingIterator();
        if (actives.hasNext())
        {
            return actives.next().getKey();
        }
        else return null;
    }

    private void updateLocalCoordinates(String move)
    {
        if (move.contains("UP")) localPosX++;
        if (move.contains("DOWN")) localPosX--;
        if (move.contains("LEFT")) localPosY--;
        if (move.contains("RIGHT")) localPosY++;
    }

    private String localPosStr()
    {
        return localPosX + "," + localPosY;
    }

    private String getMoveToTarget(String pos)
    {
        int x = Global.X(pos);
        int y = Global.Y(pos);
        if (localPosX > x) return "DOWN";
        if (localPosX < x) return "UP";
        if (localPosY > y) return "LEFT";
        else return "RIGHT";
    }

    private void waitingMode() { exploring = false; mining = false; }
    private void exploring() { exploring = true; mining = false; }
    private void mining() { exploring = false; mining = true; }

    protected void setup()
    {
        registerO2AInterface(RobotInterface.class, this);

        Object[] args = getArguments();
        max_tank = (int) args[0];
        actionDelay = (int) args[1];
        chooseDepositStrategy = (ChooseDepositStrategy) args[2];

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
            if (agent.getName().toString().contains(Global.AGENT_PREFIX) && !agent.getName().equals(this.getAID()))
                agentsAID.add(agent.getName());
            if (agent.getName().toString().contains(Global.SHIP_PREFIX))
                shipAID = agent.getName();
            if (agent.getName().toString().contains(Global.PLANET_PREFIX))
                planetAID = agent.getName();
        }

        System.out.println("Robot "+getAID().getName()+" is ready.");

        //Receive message
        addBehaviour(new HandleMessage());

        //Actions
        addBehaviour(new PlanetMining());

        //Turn it on
        on = true;
    }

    private class HandleMessage extends CyclicBehaviour {
        @Override
        public void action()
        {
            if (!on) return;

            MessageTemplate msgTemplate;
            ACLMessage msg;

            //PLANET DEPLOY REQUEST MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("PLANET_DEPLOY")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                //Tell the ship he deployed
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                send(reply);
                //Tell the planet he is on it
                ACLMessage deployMsg = new ACLMessage(ACLMessage.INFORM);
                deployMsg.addReceiver(planetAID);
                deployMsg.setContent("I WALK ON YOU");
                deployMsg.setConversationId("SHIP_AGENT_DEPLOY");
                send(deployMsg);
            }

            //PLANET ACCEPT DEPLOY MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                    MessageTemplate.MatchConversationId("SHIP_AGENT_DEPLOY")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                memory.add(localPosStr()); //First pos
                exploring();
            }

            //PLANET ACCEPT MOVE MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                    MessageTemplate.MatchConversationId("AGENT_MOVE")
            );
            msg = myAgent.receive(msgTemplate);
            //If move, update pos and local env
            if (msg != null)
            {
                //content MOVE;env
                String[] content = msg.getContent().split(";");
                memory.add(content[0]);
                updateLocalCoordinates(content[0]);
                local_env = Integer.parseInt(content[1]);
                //To prevent wrong info
                checkEnv();
            }

            //AGENT DEPOSIT DISCOVERY
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("DEPOSIT_DISCOVERY")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                String[] content = msg.getContent().split(";");
                String agent_localPos = content[0];
                int agent_env = Integer.parseInt(content[1]);
                //If deposit already checked as done => wrong info from agent
                if (!ore_deposits_done.contains(agent_localPos))
                    ore_deposits.put(agent_localPos, agent_env);
                else ore_deposits.put(agent_localPos, 0); //To ensure
            }

            //SHIP DEPOSIT REPLY
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("AGENT_DROP")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                tank = Integer.parseInt(msg.getContent());
                exploring();
            }

            //PLANET MINE REPLY (AGREE)
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                    MessageTemplate.MatchConversationId("AGENT_MINE")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                String[] content = msg.getContent().split(";");
                int obtained = Integer.parseInt(content[0]);
                local_env = Integer.parseInt(content[1]);
                //No more ore (=> error from planet)
                if (local_env == 0) emptyDeposit(localPosStr());
                tank += obtained;
                if (tank > max_tank) tank = max_tank; //Remainder is lost
            }
            //PLANET MINE REPLY (REFUSE)
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                    MessageTemplate.MatchConversationId("AGENT_MINE")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                local_env = Integer.parseInt(msg.getContent());
                if (local_env == 0) emptyDeposit(localPosStr());
            }

            //AGENT EMPTY DEPOSIT
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("DEPOSIT_EMPTY")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                ore_deposits_done.add(msg.getContent());
                ore_deposits.put(msg.getContent(), 0);
                //If on deposit at this moment
                if (msg.getContent().equals(localPosStr())) local_env = 0;
            }

            //SHIP GET BACK MESSAGE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("MISSION_OVER")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                mission_done = true;
            }

            //SHIP ENTER INSIDE AGREE
            msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                    MessageTemplate.MatchConversationId("AGENT_BACK")
            );
            msg = myAgent.receive(msgTemplate);
            if (msg != null)
            {
                waitingMode();
                on = false;
                //Tell the planet he leaves it
                ACLMessage leaveMsg = new ACLMessage(ACLMessage.INFORM);
                leaveMsg.addReceiver(planetAID);
                leaveMsg.setConversationId("LEAVE_PLANET");
                send(leaveMsg);
            }
        }
    }

    private class PlanetMining extends CyclicBehaviour {
        @Override
        public void action()
        {
            //Not activated
            if (!on) return;

            //Has done signal from ship
            if (mission_done)
            {
                getBackToShip();
            }

            //Check local env for ores
            if (!mission_done && local_env > 0)
            {
                discoverDeposit(localPosStr(), local_env);
                mining(); //Activate mining mode
            }

            //// MODES OF THE ROBOT ////
            //Exploration mode
            if (!mission_done && exploring)
            {
                exploringMode();
            }
            //Mining mode
            else if (!mission_done && mining)
            {
                miningMode();
            }
            //// END MODES OF THE ROBOT ////

            //Pause between actions
            int u = 150;
            try {
                Thread.sleep(rand(actionDelay-u,actionDelay+u));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void exploringMode()
    {
        String move;
        String pos = getActiveDepositPos(chooseDepositStrategy);
        if (pos == null)
        {
            //Choose next move according to its memory
            //in order to explore the most areas of the planet
            move = exploringStrategyMove();
        }
        else move = getMoveToTarget(pos);
        //Send message to planet for move
        askMove(move);
        //Check local env to ensure no wrong information
        checkEnv();
    }

    private void miningMode()
    {
        //On deposit
        if (local_env > 0)
        {
            //Full
            if (tank >= max_tank)
            {
                //Go to ship
                String moveToShip = getMoveToTarget("0,0");
                askMove(moveToShip);
            }
            //Not full
            else
            {
                //Mine
                askMine();
            }
        }
        //Not on deposit
        else
        {
            //Full
            if (tank >= max_tank)
            {
                //On ship
                if (localPosStr().equals("0,0"))
                {
                    //Drop
                    askDrop(tank);
                    waitingMode();
                }
                //Not on ship
                else
                {
                    //Go to ship
                    String moveToShip = getMoveToTarget("0,0");
                    askMove(moveToShip);
                }
            }
            //Not full
            else
            {
                //Exploring
                exploring();
            }
        }
    }

    //Get back to the sip
    private void getBackToShip()
    {
        //On ship
        if (localPosStr().equals("0,0"))
        {
            askEnter();
        }
        else
        {
            //Go to ship
            String moveToShip = getMoveToTarget("0,0");
            askMove(moveToShip);
        }
    }

    //Choose next move according to its memory
    //in order to explore the planet
    private String exploringStrategyMove()
    {
        //Possible moves to make
        List<String> possibleMoves = new ArrayList<>();
        //Try all the moves
        for (String move : MOVES)
            if (!memory.contains(localPosStr(move))) possibleMoves.add(move);
        //Has explored all 4 directions : rand move
        if (possibleMoves.size() == 0) return MOVES[rand(0, 4)];
        //Else choose a random direction above the not explored
        else return possibleMoves.get(rand(0, possibleMoves.size()));
    }

    //Send message to planet for move
    private void askMove(String move)
    {
        ACLMessage moveMsg = new ACLMessage(ACLMessage.REQUEST);
        moveMsg.addReceiver(planetAID);
        moveMsg.setContent(move);
        moveMsg.setConversationId("AGENT_MOVE");
        send(moveMsg);
    }

    //Send message to planet for mining
    private void askMine()
    {
        ACLMessage mineMsg = new ACLMessage(ACLMessage.REQUEST);
        mineMsg.addReceiver(planetAID);
        mineMsg.setConversationId("AGENT_MINE");
        send(mineMsg);
    }

    //Send message to ship for drop
    private void askDrop(int quantity)
    {
        ACLMessage dropMsg = new ACLMessage(ACLMessage.REQUEST);
        dropMsg.addReceiver(shipAID);
        dropMsg.setContent(String.valueOf(quantity));
        dropMsg.setConversationId("AGENT_DROP");
        send(dropMsg);
    }

    //Send message to ship for entering inside
    private void askEnter()
    {
        ACLMessage enterMsg = new ACLMessage(ACLMessage.REQUEST);
        enterMsg.addReceiver(shipAID);
        enterMsg.setConversationId("AGENT_BACK");
        send(enterMsg);
    }

    //Send message to other agents to inform a new deposit
    private void discoverDeposit(String pos, int quantity)
    {
        for (AID agent : agentsAID)
        {
            ACLMessage mineMsg = new ACLMessage(ACLMessage.INFORM);
            mineMsg.addReceiver(agent);
            mineMsg.setContent(pos+";"+quantity);
            mineMsg.setConversationId("DEPOSIT_DISCOVERY");
            send(mineMsg);
        }
        ore_deposits.put(pos, quantity);
    }

    //Send message to other agents to inform an empty deposit
    private void emptyDeposit(String pos)
    {
        for (AID agent : agentsAID)
        {
            ACLMessage mineMsg = new ACLMessage(ACLMessage.INFORM);
            mineMsg.addReceiver(agent);
            mineMsg.setContent(pos);
            mineMsg.setConversationId("DEPOSIT_EMPTY");
            send(mineMsg);
        }
        ore_deposits.put(pos, 0);
        ore_deposits_done.add(pos);
    }

    //Check environment to prevent wrong (or not updated) deposit error
    private void checkEnv()
    {
        //Wrong information
        Integer saved_ore_at = ore_deposits.get(localPosStr());
        if (saved_ore_at != null && saved_ore_at > 0 && local_env == 0)
        {
            emptyDeposit(localPosStr());
        }
    }

    //Local position after a move
    private String localPosStr(String str)
    {
        if (str.contains("UP")) return (localPosX+1) + "," + localPosY;
        if (str.contains("DOWN")) return (localPosX-1) + "," + localPosY;
        if (str.contains("LEFT")) return localPosX + "," + (localPosY-1);
        if (str.contains("RIGHT")) return localPosX + "," + (localPosY+1);
        return localPosStr();
    }

    protected void takeDown() {
        System.out.println("Robot "+getAID().getName()+" terminating.");
    }

    @Override
    public String getInfo()
    {
        String status = on ? "ON" : "OFF";
        String mode = mining ? "MINING" : "";
        mode += exploring ? (mode.equals("") ? "EXPLORING" : " AND EXPLORING") : "";
        mode = mode.equals("") ? "NONE" : mode;
        return getLocalName() + "\r\n"
                + "Status : " + status + "\r\n"
                + "Mode : " + mode + "\r\n"
                + "Tank : " + tank + " / " + max_tank + "\r\n"
                + "Local position : " + localPosY + "," + localPosY;
    }


    public int rand(int minimum, int maximum)
    {
        return minimum + (new Random()).nextInt((maximum - minimum));
    }

}
