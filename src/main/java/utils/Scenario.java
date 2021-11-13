package utils;

public class Scenario {

	public static void lowScale()
	{
		Global.AGENT_NB = 5;
		Global.PLANET_SIZE = 13;
		Global.PLANET_ORES_NB = 20;
		Global.PLANET_MAX_ORES = 20;
		Global.SHIP_MAX_TANK = 30;
		Global.AGENT_MAX_TANK = 5;
		Global.SHIP_MAX_TIME_ON_PLANET = 120000;
	}

	public static void largeScale()
	{
		Global.AGENT_NB = 20;
		Global.PLANET_SIZE = 21;
		Global.PLANET_ORES_NB = 40;
		Global.PLANET_MAX_ORES = 40;
		Global.SHIP_MAX_TANK = 120;
		Global.AGENT_MAX_TANK = 5;
		Global.SHIP_MAX_TIME_ON_PLANET = 120000;
	}

	public static void fastTakeOff()
	{
		Global.AGENT_NB = 10;
		Global.PLANET_SIZE = 21;
		Global.PLANET_ORES_NB = 10;
		Global.PLANET_MAX_ORES = 30;
		Global.SHIP_MAX_TANK = 50;
		Global.AGENT_MAX_TANK = 5;
		Global.SHIP_MAX_TIME_ON_PLANET = 10000;
	}

	public static void fastRobots()
	{
		Global.AGENT_NB = 5;
		Global.PLANET_SIZE = 13;
		Global.PLANET_ORES_NB = 20;
		Global.PLANET_MAX_ORES = 20;
		Global.SHIP_MAX_TANK = 30;
		Global.AGENT_MAX_TANK = 5;
		Global.SHIP_MAX_TIME_ON_PLANET = 120000;
	}

	public static void bigRobots()
	{
		Global.AGENT_NB = 5;
		Global.PLANET_SIZE = 13;
		Global.PLANET_ORES_NB = 20;
		Global.PLANET_MAX_ORES = 20;
		Global.SHIP_MAX_TANK = 30;
		Global.AGENT_MAX_TANK = 5;
		Global.SHIP_MAX_TIME_ON_PLANET = 120000;
	}
}
