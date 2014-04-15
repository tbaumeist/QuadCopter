package com.tbaumeist.quadcontroller;

import java.security.InvalidParameterException;

import com.tbaumeist.quadcontroller.widget.JoystickMovedListener;
import com.tbaumeist.quadcontroller.widget.JoystickView;
import com.tbaumeist.quadcontroller.widget.Status;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class ControllerScreen extends Activity {

	private Application app;
	private Status status;
	private Switch switchStart, switchArm;
	private JoystickView stickLeft, stickRight;

	private CommunicationManager commManager;

	private TextView debug;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_controller_screen);

		app = (Application) getApplication();

		Button buttonSettings = (Button) findViewById(R.id.button_settings);
		buttonSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(ControllerScreen.this,
						SerialPortPreferences.class));
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

		status = (Status) findViewById(R.id.showStatus);
		status.setStatusNotReady();

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
		public void OnMessageReceived(byte[] buffer, int size) {
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
	};

	private void initQuadCopter() {
		try {
			commManager = new CommunicationManager(app.getSerialPort(),
					commListener);
			status.setStatusStartAlmostReady();
			commManager.initializeComm();

		} catch (SecurityException e) {
			switchArm.setChecked(false);
			DisplayError(R.string.error_security);
		} catch (InvalidParameterException e) {
			switchArm.setChecked(false);
			DisplayError(R.string.error_configuration);
		} catch (Exception e) {
			switchArm.setChecked(false);
			DisplayError(R.string.error_unknown);
		}
	}

	private void closeQuadCopter() {
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
}
