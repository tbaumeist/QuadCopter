package com.tbaumeist.quadcontroller;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;

import com.hoho.android.usbserial.driver.TISerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.tbaumeist.quadcontroller.widget.JoystickMovedListener;
import com.tbaumeist.quadcontroller.widget.JoystickView;
import com.tbaumeist.quadcontroller.widget.Status;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class ControllerScreen extends Activity {

	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	private Application app;
	private UsbManager usbManager;
	private UsbSerialDriver usbDriver;
	private Status status;
	private Switch switchStart, switchArm;
	private JoystickView stickLeft, stickRight;
	private Button buttonSettings;

	private CommunicationManager commManager;
	private PendingIntent mPermissionIntent;

	private TextView debug;

	private final BroadcastReceiver m_usbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (device != null
							&& intent.getBooleanExtra(
									UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						List<UsbSerialDriver> drivers = UsbSerialProber
								.probeSingleDevice(usbManager, device);
						if (drivers.isEmpty())
							return;
						Toast.makeText(app.getBaseContext(),
								"Connecting device", Toast.LENGTH_SHORT);
						usbDriver = drivers.get(0);
						status.setStatusNotReady();
						buttonSettings.setEnabled(false);
						switchArm.setEnabled(true);
					}
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_controller_screen);
		app = (Application) getApplication();

		status = (Status) findViewById(R.id.showStatus);
		status.setStatusNoGo();

		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		this.registerReceiver(m_usbReceiver, filter);

		buttonSettings = (Button) findViewById(R.id.button_settings);
		buttonSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					Toast.makeText(
							app.getBaseContext(),
							"Found " + usbManager.getDeviceList().size()
									+ " devices", Toast.LENGTH_SHORT).show();

					for (UsbDevice d : usbManager.getDeviceList().values()) {
						if (!UsbSerialProber.testIfSupported(d,
								TISerialDriver.getSupportedDevices()))
							continue;
						// get permission
						usbManager.requestPermission(d, mPermissionIntent);
						return;
					}
					
					Toast.makeText(app.getBaseContext(),
							"No matching devices found", Toast.LENGTH_SHORT)
							.show();
					// startActivity(new Intent(ControllerScreen.this,
					// SerialPortPreferences.class));
				} catch (Exception ex) {
					DisplayError(ex.getMessage());
				}
			}
		});

		switchArm = (Switch) findViewById(R.id.switchArm);
		switchArm.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked)
					initQuadCopter();
				else
					closeQuadCopter();
			}
		});
		switchArm.setEnabled(false);

		switchStart = (Switch) findViewById(R.id.switchStart);
		switchStart.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked)
					startQuadCopterRotors();
				else
					stopQuadCopterRotors();
			}
		});
		switchStart.setEnabled(false);
		debug = (TextView) findViewById(R.id.textViewDebug);

		stickLeft = (JoystickView) findViewById(R.id.joystickLeft);
		stickLeft.setOnJostickMovedListener(leftStickListener);
		stickLeft.setEnabled(false);
		stickLeft.setMovementConstraint(JoystickView.CONSTRAIN_CIRCLE);
		stickLeft.setIsThrottle(true);

		stickRight = (JoystickView) findViewById(R.id.joystickRight);
		stickRight.setOnJostickMovedListener(rightStickListener);
		stickRight.setEnabled(false);
		stickRight.setMovementConstraint(JoystickView.CONSTRAIN_CIRCLE);
	}

	private JoystickMovedListener leftStickListener = new JoystickMovedListener() {
		@Override
		public void OnMoved(int pan, int tilt) {
			if (commManager != null)
				commManager.setLeft(pan, tilt);
		}

		@Override
		public void OnReleased() {
		}

		public void OnReturnedToCenter() {
		};
	};

	private JoystickMovedListener rightStickListener = new JoystickMovedListener() {
		@Override
		public void OnMoved(int pan, int tilt) {
			if (commManager != null)
				commManager.setRight(pan, tilt);
		}

		@Override
		public void OnReleased() {
		}

		public void OnReturnedToCenter() {
		};
	};

	private CommunicationManagerListener commListener = new CommunicationManagerListener() {
		@Override
		public void OnMessageReceived(final byte[] buffer) {
			runOnUiThread(new Runnable() {
				public void run() {
					debug.append(new String(buffer));
				}
			});
		}

		@Override
		public void OnInitializedFinished() {
			runOnUiThread(new Runnable() {
				public void run() {
					switchStart.setEnabled(true);
					status.setStatusAlmostReady();
				}
			});
		}

		@Override
		public void OnError(String error) {
			DisplayError(error);
		}
	};

	private void initQuadCopter() {
		try {
			if (usbDriver == null)
				throw new IOException("Unable to find the usb device.");

			commManager = new CommunicationManager(usbDriver, commListener);
			status.setStatusStartAlmostReady();
			commManager.initializeComm();

		} catch (SecurityException e) {
			switchArm.setChecked(false);
			DisplayError(R.string.error_security);
		} catch (InvalidParameterException e) {
			switchArm.setChecked(false);
			DisplayError(R.string.error_configuration);
		} catch (IOException e) {
			switchArm.setChecked(false);
			DisplayError(e.getMessage());
		} catch (Exception e) {
			switchArm.setChecked(false);
			DisplayError(R.string.error_unknown);
		}
	}

	private void closeQuadCopter() {
		try {
			usbDriver.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		usbDriver = null;
		commManager = null;
		app.closeSerialPort();

		status.setStatusNotReady();
		switchStart.setEnabled(false);
	}

	private void startQuadCopterRotors() {
		commManager.startComm();

		status.setStatusReady();
		switchArm.setEnabled(false);
		stickLeft.setEnabled(true);
		stickRight.setEnabled(true);
	}

	private void stopQuadCopterRotors() {
		commManager.stopComm();

		status.setStatusAlmostReady();
		switchArm.setEnabled(true);
		stickLeft.setEnabled(false);
		stickRight.setEnabled(false);
		stickRight.setToStartPositions();
		stickLeft.setToStartPositions();
	}

	private void DisplayError(int resourceId) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Error");
		b.setMessage(resourceId);
		b.setPositiveButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		b.show();
	}

	private void DisplayError(String error) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Error");
		b.setMessage(error);
		b.setPositiveButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		b.show();
	}

}
