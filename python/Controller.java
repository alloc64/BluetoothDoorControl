import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinDirection;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.trigger.GpioCallbackTrigger;
import com.pi4j.io.gpio.trigger.GpioPulseStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSetStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSyncStateTrigger;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.event.PinEventType;
import java.lang.*;
import java.io.*;
import java.nio.charset.*;
import javax.bluetooth.*;
import javax.microedition.io.*;

public class Controller
{
	private enum State
	{
		OPEN('1'),
		OPENING('2'),
		CLOSE('6'),
		CLOSING('7'),
		STOPPED('9');

		private final char value;
		private State(char value)
		{
			this.value = value;
		}

		public char getValue()
		{
			return this.value;
		}
	}

	private static final int MAX_ROTATIONS = 995;

	private int rotations = 0;
	private State state = State.CLOSE;

	private GpioPinDigitalOutput forwardGPIOPin;
	private GpioPinDigitalOutput backwardGPIOPin;
	private GpioPinDigitalInput hallGPIOPin;

	private OutputStream clientOutputStream;
	private boolean isRunning = true;

	private Controller()
	{
	}

	private void setState(State state)
	{
		this.state = state;
		dispatchState(state);
	}

	private void dispatchState(State state)
	{
		try
		{
			if(clientOutputStream != null)
			{
				clientOutputStream.write(state.getValue());
				clientOutputStream.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void open()
	{
		System.out.println("Opening doors...");
		setState(State.OPENING);
		this.backwardGPIOPin.low();
		this.forwardGPIOPin.high();
	}

	private void close()
	{
		System.out.println("Closing doors...");
		setState(State.CLOSING);
		this.backwardGPIOPin.high();
		this.forwardGPIOPin.low();
	}

	private void onOpened()
	{
		System.out.println("Doors opened...");
		this.setState(State.OPEN);
	}

	private void onClosed()
	{
		System.out.println("Doors closed...");
		this.setState(State.CLOSE);
	}

	private void onRotationChanged()
	{
		System.out.println("Rotation: " + rotations);

		if(state == State.CLOSING)
		{
			rotations++;

			if(rotations >= MAX_ROTATIONS)
			{
				enforceStop();
				onClosed();
			}
		}
		else if(state == State.OPENING)
		{
			rotations--;

			if(rotations < 1)
			{
				enforceStop();
				onOpened();
			}
		}
	}

	private void onMessageReceived(String msg)
	{
		if(msg == null)
			return;

		System.out.println("Received message:" + msg);

		switch(msg)
		{
			case "stat":
				dispatchState(state);
				break;

			case "open":
				open();
				break;

			case "close":
				close();
				break;

			case "stop":
				enforceStop();
				dispatchState(State.STOPPED);
				break;

			case "resume":
				if(state == State.OPENING)
					close();
				else if(state == State.CLOSING)
					open();
				else
					System.out.println("Invalid state " +  state + " while resume message received");
				break;
		}
	}

	private void enforceStop()
	{
		this.backwardGPIOPin.low();
		this.forwardGPIOPin.low();
	}

	public void run() throws Exception
	{
		GpioController gpio = GpioFactory.getInstance();
		System.out.println("Started!");

		this.forwardGPIOPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_24, "Forward direction", PinState.LOW); // 35 GPIO, puvodne 11 GPIO
		forwardGPIOPin.low();
		forwardGPIOPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);

		this.backwardGPIOPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_25, "Backwards direction", PinState.LOW); // 37 GPIO
		backwardGPIOPin.low();
		backwardGPIOPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);

		this.hallGPIOPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_27, "HALL", PinPullResistance.PULL_DOWN); // 36 GPIO

		hallGPIOPin.addListener(new GpioPinListenerDigital()
		{
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
			{
				onRotationChanged();
			}
		});

		System.out.println("Starting Bluetooth server...");

		LocalDevice local = LocalDevice.getLocalDevice();
		System.out.println("Device name: " + local.getFriendlyName());
		System.out.println("Bluetooth Address: " + local.getBluetoothAddress());

		boolean res = local.setDiscoverable(DiscoveryAgent.GIAC);
		System.out.println("Discoverability set: " + res);

		UUID uuid = new UUID("446118f08b1e11e29e960800200c9a66", false);

		StreamConnectionNotifier streamConnNotifier = (StreamConnectionNotifier)Connector.open("btspp://localhost:" + uuid +";name=SSPServer");

		char[] buffer = new char[8];

		while(isRunning)
		{
			StreamConnection connection = streamConnNotifier.acceptAndOpen();

			RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
			System.out.println("Remote device address: "+dev.getBluetoothAddress());
			System.out.println("Remote device name: "+dev.getFriendlyName(true));

			this.clientOutputStream = connection.openOutputStream();

			try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.openInputStream(), StandardCharsets.UTF_8)))
			{
				int size = 0;
				while((size = reader.read(buffer, 0, buffer.length)) != -1)
				{
					String msg = new String(buffer, 0, size);
					onMessageReceived(msg);
				}
			}
		}

		streamConnNotifier.close();
	}

	public static void main(String args[]) throws Exception
	{
		new Controller().run();
	}
}
