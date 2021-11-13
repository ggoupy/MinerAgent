package robot;

//For the moment, only various attributes are changed
//Thus, there is no need to make a subclass
//If a strategy or a behaviour (exploring, mining) changes
//We will have to create a subclass
public class BigRobot  {
	public static Object[] args()
	{
		Object[] bigRobot = new Object[3];
		bigRobot[0] = 20;
		bigRobot[1] = 1400;
		bigRobot[2] = Robot.ChooseDepositStrategy.BY_BIGGER;
		return bigRobot;
	}
}