package cn.fu.dragsortview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * custom your view inherit from BaseView
 * Just need to add LayoutParams when view init
 * */ 
public class CustomDragView extends BaseView
{
	
	public CustomDragView(Context context)
	{
		super(context);
		init(context);
	}
	public CustomDragView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}
	private void init(Context context)
	{
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.view_content_layout, null);
		
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		//display width & height
		lp.width = mScreenW;
		lp.height = 300;
//		int paddingLeft = mHeaderPadding - dragViewMarginSide;
//		int paddingRight = mHeaderPadding + dragViewMarginSide;
//		view.setPadding(paddingLeft, 0, paddingRight, 0);
		view.setLayoutParams(lp);
		setContentView(view);   //setContentView to baseview

	}
	@Override
	public void updateDate()
	{
		// TODO Auto-generated method stub
	}
	
}
