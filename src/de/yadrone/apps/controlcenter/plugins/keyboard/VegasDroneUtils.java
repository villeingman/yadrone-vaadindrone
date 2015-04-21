package de.yadrone.apps.controlcenter.plugins.keyboard;

import de.yadrone.base.IARDrone;
import de.yadrone.base.command.FlightAnimation;
import de.yadrone.base.command.LEDAnimation;

public class VegasDroneUtils {

	public static void doLedsAnimation(IARDrone drone) {
		try {
			drone.getCommandManager().setLedsAnimation(LEDAnimation.BLINK_ORANGE, 10, 2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void doWaveAnimation(IARDrone drone) {
		try {
			drone.getCommandManager().animate(FlightAnimation.THETA_M30_DEG);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
