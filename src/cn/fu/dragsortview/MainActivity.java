package cn.fu.dragsortview;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import cn.fu.dragsortview.lib.DragSortViewGroup;

public class MainActivity extends Activity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Full screen
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		//Custom Views
		CustomDragView1 myView = new CustomDragView1(this);
		CustomDragView myView1 = new CustomDragView(this);
		CustomDragView myView2 = new CustomDragView(this);	
		CustomDragView myView3 = new CustomDragView(this);
		CustomDragView myView4 = new CustomDragView(this);
		CustomDragView myView5 = new CustomDragView(this);
		CustomDragView myView6 = new CustomDragView(this);
		CustomDragView myView7 = new CustomDragView(this);
		
		//init views
		myView.setContentText("Fruit List");
		myView1.setPicAndText(R.drawable.watermelon, "Hello Watermelon");
		myView2.setPicAndText(R.drawable.banana, "Hello Banana");
		myView3.setPicAndText(R.drawable.grape, "Hello Grape");
		myView4.setPicAndText(R.drawable.orange, "Hello Orange");
		myView5.setPicAndText(R.drawable.papaya, "Hello Papaya");
		myView6.setPicAndText(R.drawable.strawberry, "Hello Strawberry");
		myView7.setPicAndText(R.drawable.apple, "Hello Iphone");
	
		
		// new a dragSortView 
		DragSortViewGroup dragSortView = new DragSortViewGroup(this,myView, myView1, myView2, myView3, myView4, myView5, myView6, myView7);
		
		setContentView(dragSortView);
	}
}
