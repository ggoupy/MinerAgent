package ship;

import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import robot.RobotInterface;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ShipGui extends JFrame {
	private AgentController ship;
	private AgentController[] agents;
	JTextArea info = new JTextArea();

	public ShipGui(AgentController ship, AgentController[] agents)
	{
		super("The ship");
		this.ship = ship;
		this.agents = agents;
		//Main panel containing the grid
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
		info.setFont(info.getFont().deriveFont(Font.BOLD, 20));
		info.setBackground(new Color(179, 217, 255));
		info.setEditable(false);
		JScrollPane scroll = new JScrollPane(info);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(scroll);
		//Add main panel to window
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
		setResizable(true);
	}

	public void showGui()
	{
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.height-50, 0);
		setSize(500, screenSize.height);
		setVisible(true);
	}

	public void update() throws StaleProxyException {
		String info = "";
		info += "----------------------------- \r\n";
		info += "SHIP INFORMATION : \r\n";
		info += "----------------------------- \r\n";
		info += ship.getO2AInterface(ShipInterface.class).getInfo() + "\r\n\r\n\r\n";
		info += "-------------------------------- \r\n";
		info += "AGENTS INFORMATION : \r\n";
		info += "-------------------------------- \r\n";
		for (AgentController a : agents)
		{
			info += a.getO2AInterface(RobotInterface.class).getInfo();
			info += "\r\n\r\n";
		}
		this.info.setText(info);
	}
}
