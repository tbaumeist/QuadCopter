package com.tbaumeist.quadcontroller.widget;

import com.tbaumeist.quadcontroller.R;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Status extends LinearLayout {
	
	private TextView statusText;
	private ImageView statusImage;
	
	private int currentImageID = R.drawable.red;
	private boolean isFlashing = false;
	
	private final int DELAY = 500;
	private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
        	if(!isFlashing)
        		return;
        	toggleNotReadyAlmostReady();
            timerHandler.postDelayed(timerRunnable, DELAY);
        }
    };

	public Status(Context context) {
		super(context);
		if(!isInEditMode())
			init(context);
	}
	
	public Status(Context context, AttributeSet attrs) {
		super(context, attrs);
		if(!isInEditMode())
			init(context);
	}
	
	public Status(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if(!isInEditMode())
			init(context);
	}
	
	public void setStatusNotReady(){
		statusText.setText(R.string.status_notready);
		setCurrentImage(R.drawable.red);
	}
	
	public void setStatusStartAlmostReady(){
		isFlashing = true;
		statusText.setText(R.string.status_gettingready);
		setCurrentImage(R.drawable.red);
		timerHandler.postDelayed(timerRunnable, 0);
	}
	
	public void setStatusAlmostReady(){
		isFlashing = false;
		timerHandler.removeCallbacks(timerRunnable);

		statusText.setText(R.string.status_almostready);
		setCurrentImage(R.drawable.yellow);
	}
	
	public void setStatusReady(){
		statusText.setText(R.string.status_ready);
		setCurrentImage(R.drawable.green);
	}

	private void init(Context context){
		setOrientation(LinearLayout.HORIZONTAL);
	    setGravity(Gravity.CENTER_VERTICAL);

	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    inflater.inflate(R.layout.view_status, this, true);
	    
	    statusText = (TextView)findViewById(R.id.statusText);
		
		statusImage = (ImageView)findViewById(R.id.statusImage);
		setCurrentImage(R.drawable.red);
		
	}
	
	private void setCurrentImage(int id){
		currentImageID = id;
		statusImage.setImageResource(currentImageID);
	}
	
	private void toggleNotReadyAlmostReady(){
		if(currentImageID == R.drawable.yellow)
			setCurrentImage(R.drawable.red);
		else if(currentImageID == R.drawable.red)
			setCurrentImage(R.drawable.yellow);
	}
}
