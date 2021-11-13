package planet;
import utils.Global;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.awt.*;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class PlanetGui extends JFrame {
    private AgentController planet;
    private static final Color groundColor = new Color(179, 60, 0);
    private static final Color oreColor = new Color(255, 255, 0);
    private static final Color agentColor = new Color(0, 61, 102);

    //Planet is displayed like a grid
    JPanel[][] cells = new JPanel[Planet.PLANET_SIZE][Planet.PLANET_SIZE];
    JLabel[][] groundImg = new JLabel[Planet.PLANET_SIZE][Planet.PLANET_SIZE];
    JTextField[][] ground = new JTextField[Planet.PLANET_SIZE][Planet.PLANET_SIZE];
    private JLabel shipCell = null;

    public PlanetGui(AgentController planet)
    {
        super("The planet");
        this.planet = planet;

        //Main panel containing the grid
        JPanel mainPanel = new JPanel(new GridLayout(Planet.PLANET_SIZE, Planet.PLANET_SIZE));
        mainPanel.setBackground(groundColor);

        // Init grid
        for (int i = 0; i < Planet.PLANET_SIZE; ++i)
        {
            for (int j = 0; j < Planet.PLANET_SIZE; ++j)
            {
                cells[i][j] = new JPanel();
                cells[i][j] = new JPanel(new GridLayout(1, 1, 0, 0));
                cells[i][j].setBackground(groundColor);
                mainPanel.add(cells[i][j]);
            }
        }
        for (int i = 0; i < Planet.PLANET_SIZE; ++i)
        {
            for (int j = 0; j < Planet.PLANET_SIZE; ++j)
            {
                //Label containing image (bg) and text (fg)
                groundImg[i][j] = new JLabel();
                groundImg[i][j].setLayout( new BorderLayout() );
                groundImg[i][j].setHorizontalAlignment(JLabel.CENTER);
                ground[i][j] = new JTextField();
                ground[i][j].setBorder(new LineBorder(oreColor, 0, false));
                ground[i][j].setHorizontalAlignment(JTextField.CENTER);
                ground[i][j].setFont(ground[i][j].getFont().deriveFont(Font.BOLD, 22));
                ground[i][j].setOpaque(false);
                ground[i][j].setEditable(false);
                groundImg[i][j].add(ground[i][j]);
                cells[i][j].add(groundImg[i][j]);
            }
        }

        //Add main panel to window
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        setResizable(false);
    }

    public void showGui()
    {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.height-50, screenSize.height-50);
        setVisible(true);
        setResizable(false);
    }

    public void update() throws StaleProxyException {
        PlanetInterface p = planet.getO2AInterface(PlanetInterface.class);

        //Ores
        for (int i = 0; i < Planet.PLANET_SIZE; ++i) {
            for (int j = 0; j < Planet.PLANET_SIZE; ++j) {
                int ore_nb = p.containsOres(i,j);
                if (ore_nb > 0)
                {
                    ground[i][j].setText(String.valueOf(ore_nb));
                    if (groundImg[i][j].getIcon() == null) groundImg[i][j].setIcon(imageIcon("ore.png"));
                    ground[i][j].setForeground(Color.BLACK);
                }
                else
                {
                    ground[i][j].setText("");
                    //Erase all except ship
                    if (groundImg[i][j].getIcon() != null && !groundImg[i][j].equals(shipCell)) groundImg[i][j].setIcon(null);
                }
            }
        }

        //Ship
        Integer ind_ship = p.getExternalObjPositions().get(Global.SHIP_PREFIX);
        if (ind_ship != null) {
            //1D to 2D
            int x = ind_ship / Planet.PLANET_SIZE;
            int y = ind_ship % Planet.PLANET_SIZE;
            if (groundImg[x][y].getIcon() == null) {
                groundImg[x][y].setIcon(imageIcon("ship.png"));
                shipCell = groundImg[x][y];
            }
        }
        //Previous icon to remove
        else if (shipCell != null) {
            shipCell.setIcon(null);
        }

        //Agents
        Map<String, Integer> positions = new HashMap<>(p.getExternalObjPositions());
        positions.remove(Global.SHIP_PREFIX);
        for (Map.Entry<String, Integer> pos : positions.entrySet()) {
            //1D to 2D
            int x = pos.getValue() / Planet.PLANET_SIZE;
            int y = pos.getValue() % Planet.PLANET_SIZE;
            int nb = Collections.frequency(positions.values(), pos.getValue());
            if (nb > 1)
                ground[x][y].setText(nb + " " + Global.AGENT_PREFIX);
            else ground[x][y].setText(Global.AGENT_PREFIX);
            ground[x][y].setForeground(agentColor);
        }
    }

    private ImageIcon imageIcon(String path)
    {
        URL url = getClass().getResource("/" + path);
        ImageIcon imageIcon = new ImageIcon(url); // load the image to a imageIcon
        Image image = imageIcon.getImage(); // transform it
        Image newimg = image.getScaledInstance(48, 48,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        return new ImageIcon(newimg);  // transform it back
    }
}