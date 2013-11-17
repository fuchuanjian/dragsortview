package cn.fu.dragsortview;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		  this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		DragSortView dragSortView = new DragSortView(this);
		setContentView(dragSortView);
	}
}
