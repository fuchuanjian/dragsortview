package cn.fu.dragsortview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

public class DragViewFrame extends BaseDragView
{

	public DragViewFrame(Context context)
	{
		super(context);
		init(context);
	}
	public DragViewFrame(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}
	private void init(Context context)
	{
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.dragview_test, null);
		
		
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		lp.width = mScreenW;
		lp.height = 100;
		view.setLayoutParams(lp);
		setContentView(view);

	}
	
}
