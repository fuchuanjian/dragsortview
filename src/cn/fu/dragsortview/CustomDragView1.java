package cn.fu.dragsortview;

import cn.fu.dragsortview.lib.BaseView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * custom  view inherit from BaseView
 * Just need to add LayoutParams when view init
 * */ 
public class CustomDragView1 extends BaseView
{
	private TextView tv;
	public CustomDragView1(Context context)
	{
		super(context);
		init(context);
	}
	public CustomDragView1(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}
	private void init(Context context)
	{
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.sample_view1, null);
		tv = (TextView) view.findViewById(R.id.view_description_tv);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		//display width & height
		lp.width = mScreenW;
		lp.height = 120;
		int paddingLeft = mHeaderPadding - dragViewMarginSide;
		int paddingRight = mHeaderPadding + dragViewMarginSide;
		view.setPadding(paddingLeft, 0, paddingRight, 0);
		view.setLayoutParams(lp);
		setContentView(view);   //setContentView to baseview

	}
	public void setContentText( String text)
	{
		tv.setText(text);
		setTileBar(-1, text, -1);
	}
	
	@Override
	public void updateDate()
	{
		// TODO Auto-generated method stub
	}
	
}
