package robot;

//For the moment, only various attributes are changed
//Thus, there is no need to make a subclass
//If a strategy or a behaviour (exploring, mining) changes
//We will have to create a subclass
public class FastRobot {
	public static Object[] args()
	{
		Object[] fastRobot = new Object[3];
		fastRobot[0] = 5;
		fastRobot[1] = 700;
		fastRobot[2] = Robot.ChooseDepositStrategy.BY_CLOSER;
		return fastRobot;
	}
}
