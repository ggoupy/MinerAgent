package utils;

import java.util.Random;

public class Global {

	//SIMULATION PARAMETERS
	public static int AGENT_NB = 10;
	public static int PLANET_SIZE = 15;
	public static int PLANET_ORES_NB = 30;
	public static int PLANET_MAX_ORES = 20;
	public static int SHIP_MAX_TANK = 30;
	public static int AGENT_MAX_TANK = 5;
	public static int SHIP_MAX_TIME_ON_PLANET = 120000; //ms


	//FOR AGENT NAMES
	public final static String SHIP_PREFIX = "Ship";
	public final static String AGENT_PREFIX = "R";
	public final static String PLANET_PREFIX = "Planet";


	//RANDOM FUNCTION
	public static int rand(int minimum, int maximum)
	{
		return minimum + (new Random()).nextInt((maximum - minimum));
	}

	//Positions are sent with string using the format X,Y
	//To get integer positions from the String
	public static int X(String pos) { return Integer.parseInt(pos.split(",")[0]); }
	public static int Y(String pos) { return Integer.parseInt(pos.split(",")[1]); }
}
