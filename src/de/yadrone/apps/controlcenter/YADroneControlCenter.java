package de.yadrone.apps.controlcenter;

import de.yadrone.base.ARDrone;

public class YADroneControlCenter {

	private ARDrone ardrone = null;

	public YADroneControlCenter() {
		initialize();
	}

	private void initialize() {
		try {
			ardrone = new ARDrone("192.168.1.1", null);
			ardrone.start();

			new CCFrame(ardrone);

		} catch (Exception exc) {
			exc.printStackTrace();

			if (ardrone != null)
				ardrone.stop();
			System.exit(-1);
		}
	}

	public static void main(String args[]) {
		new YADroneControlCenter();
	}
}