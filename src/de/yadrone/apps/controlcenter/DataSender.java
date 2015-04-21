package de.yadrone.apps.controlcenter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.GsonBuilder;

import de.yadrone.apps.controlcenter.plugins.keyboard.VegasDroneUtils;
import de.yadrone.base.IARDrone;
import de.yadrone.base.navdata.Altitude;
import de.yadrone.base.navdata.AltitudeListener;
import de.yadrone.base.navdata.AttitudeListener;
import de.yadrone.base.navdata.BatteryListener;
import de.yadrone.base.navdata.ControlState;
import de.yadrone.base.navdata.DroneState;
import de.yadrone.base.navdata.StateListener;

public class DataSender implements MqttCallback {

	// IBM Bluemix
//	private static final String IOT_BROKER_HOST = "kwxdxh.messaging.internetofthings.ibmcloud.com";
//	private static final Integer IOT_BROKER_HOST_PORT = 1883;
//	private static final String IOT_USERNAME = "use-token-auth";
//	private static final String IOT_PASSWORD = "6Wm+UFH4(QsZaSndQD";
//	private static final String IOT_ID = "d:" + "kwxdxh:" + "dronedatalaptop:"
//			+ "vaadin-villeingman-1";
//	private static final String IOT_URI = "tcp://" + IOT_BROKER_HOST + ":"
//			+ IOT_BROKER_HOST_PORT;
//	private static final String IOT_TOPIC_FLIGHT_DATA = "iot-2/evt/vaadindrone-data/fmt/json";
//	private static final String IOT_TOPIC_COMMANDS = "iot-2/cmd/cid/fmt/json";

	// Viritin
	private static final String IOT_USERNAME = "empty";
	private static final String IOT_PASSWORD = "empty";
	private static final String IOT_ID = "vaadin-drone";
	private static final String IOT_URI = "tcp://mqtt.virit.in:1883";
	private static final String IOT_TOPIC_FLIGHT_DATA = "vaadindrone/data";
	private static final String IOT_TOPIC_COMMANDS = "vaadindrone/cmd";

	private static final String IOT_COMMAND_WAVE = "wave";
	private static final String MY_JSON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";

	private IARDrone ardrone;
	protected int latestBatteryPercentage;
	protected int latestAltitude;
	protected float latestPitch;
	protected float latestRoll;
	protected DroneState latestDroneState;
	protected ControlState latestControlState;

	private MqttClient client;
	private MqttClient localClient;
	private MqttConnectOptions opts;
	private boolean active = false;
	private StateListener myStateListener = new MyStateListener();
	private BatteryListener myBatteryListener = new MyBatteryListener();
	private AltitudeListener myAltitudeListener = new MyAltitudeListener();
	private AttitudeListener myAttitudeListener = new MyAttitudeListener();

	public DataSender() {
		initCommunications();
	}

	private void initCommunications() {
		try {
			client = new MqttClient(IOT_URI, IOT_ID);
			opts = new MqttConnectOptions();
			opts.setUserName(IOT_USERNAME);
			opts.setPassword(IOT_PASSWORD.toCharArray());
			opts.setKeepAliveInterval(60);
			opts.setConnectionTimeout(60);
			client.setCallback(this);
			localClient = new MqttClient("tcp://127.0.0.1:1883", IOT_ID);
			localClient.setCallback(this);
		} catch (Exception e) {
			System.err.println("comms init failed");
			return;
		}
	}

	public void begin(IARDrone drone) {

		latestBatteryPercentage = 0;
		latestAltitude = 0;
		latestPitch = 0;
		latestRoll = 0;

		this.active = true;
		this.ardrone = drone;
		addDroneListeners();

		try {
			Thread.sleep(500);
		} catch (Exception e) {
			// ignored
		}

		// Sending of data
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (active) {
					try {
						MqttMessage message = createMessageForSending();
						sendMessage(message);
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// ignored
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private void openConnections() {
		try {
			client.connect(opts);
			client.subscribe(IOT_TOPIC_COMMANDS);
		} catch (Exception e) {
			System.err.println("comms open failed");
		}
		try {
			localClient.connect();
			localClient.subscribe("command");
		} catch (Exception e) {
			System.err.println("local comms open failed");
		}
	}

	private MqttMessage createMessageForSending() {
		DroneDataSample d = new DroneDataSample();
		d.setAltitude(latestAltitude);
		d.setBatteryPercentage(latestBatteryPercentage);
		d.setPitch(latestPitch);
		d.setRoll(latestRoll);
		String json = new GsonBuilder().setDateFormat(MY_JSON_DATE_FORMAT)
				.create().toJson(d);
		// System.out.println(json);
		MqttMessage message = new MqttMessage(json.getBytes());
		return message;
	}

	@Override
	public void connectionLost(Throwable arg0) {
		System.err.println("Connection lost to MQTT server");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
	}

	@Override
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		if (topic.trim().equalsIgnoreCase(IOT_TOPIC_COMMANDS) || topic.trim().equalsIgnoreCase("command")) {
			if (message.toString().equalsIgnoreCase(IOT_COMMAND_WAVE)) {
				System.out.println("New wave: " + message.toString());
				VegasDroneUtils.doLedsAnimation(this.ardrone);
				VegasDroneUtils.doWaveAnimation(this.ardrone);
			} else {
				System.out.println("New blinker: " + message.toString());
				VegasDroneUtils.doLedsAnimation(this.ardrone);
			}
		}
	}

	private void sendMessage(MqttMessage message) {
		if (isCommunicationsOK()) {
			try {
				message.setQos(0);
				client.publish(IOT_TOPIC_FLIGHT_DATA, message);
			} catch (Exception e) {
				System.err.println("message publish failed");
			}
		}
		try {
			if (!localClient.isConnected()) {
				localClient.connect();
			}
			localClient.publish("data", message);
		} catch (Exception e) {
			System.err.println("local publish failed");
		}
	}

	private boolean isCommunicationsOK() {
		if (this.client.isConnected()) {
			return true;
		} else {
			initCommunications();
			openConnections();
		}
		return this.client.isConnected();
	}

	public void stop() {
		this.active = false;
		removeDroneListeners();
	}

	private void addDroneListeners() {
		ardrone.getNavDataManager().addStateListener(myStateListener);
		ardrone.getNavDataManager().addBatteryListener(myBatteryListener);
		ardrone.getNavDataManager().addAltitudeListener(myAltitudeListener);
		ardrone.getNavDataManager().addAttitudeListener(myAttitudeListener);
	}

	private void removeDroneListeners() {
		ardrone.getNavDataManager().removeStateListener(myStateListener);
		ardrone.getNavDataManager().removeBatteryListener(myBatteryListener);
		ardrone.getNavDataManager().removeAltitudeListener(myAltitudeListener);
		ardrone.getNavDataManager().removeAttitudeListener(myAttitudeListener);
	}

	private final class MyAttitudeListener implements AttitudeListener {
		@Override
		public void windCompensation(float pitch, float roll) {
		}

		@Override
		public void attitudeUpdated(float pitch, float roll) {
		}

		@Override
		public void attitudeUpdated(float pitch, float roll, float yaw) {
			latestPitch = pitch;
			latestRoll = roll;
		}
	}

	private final class MyAltitudeListener implements AltitudeListener {
		@Override
		public void receivedExtendedAltitude(Altitude d) {
		}

		@Override
		public void receivedAltitude(int altitude) {
			latestAltitude = altitude;
		}
	}

	private final class MyBatteryListener implements BatteryListener {
		@Override
		public void voltageChanged(int vbat_raw) {
		}

		@Override
		public void batteryLevelChanged(int percentage) {
			latestBatteryPercentage = percentage;
		}
	}

	private final class MyStateListener implements StateListener {
		@Override
		public void stateChanged(DroneState state) {
			latestDroneState = state;
		}

		@Override
		public void controlStateChanged(ControlState state) {
			latestControlState = state;
		}
	}


	// private void closeCommunications() {
	// try {
	// this.client.close();
	// } catch (Exception e) {
	// // ignored, can't do much
	// }
	// }
}
