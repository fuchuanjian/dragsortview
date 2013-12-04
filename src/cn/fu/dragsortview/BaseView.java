package cn.fu.dragsortview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**the base class of DragView 
 * 
 * this class defines some public functions 
 * 1. setTileBar (update or init view info by this method)
 * 2. setContentView (add display view here)
 * 3. changeModeToDRAGING changeModeToIDLE  (Override it if u need  when view's pressing state changed )
 * 4. setBackground
 * 
 * */
public class BaseView extends LinearLayout {

	private TextView titleTV; // title
	private ImageView handleIV; // flg
	private RelativeLayout titleLayout;
	private ImageView titleIV; // flg
	private View contentView;
	
	private final int IDLE = 0;
	private final int DRAGING = 1;	
	
	private int mMode = IDLE;
	
	protected int mScreenW; // screen width
	protected int mHeaderPadding = 15; // Header的左右Padding
	protected int dragViewMarginSide = 4;
	public BaseView(Context context) {
		super(context);
		initBaseView(context);
	}

	public BaseView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBaseView(context);
	}

	private void initBaseView(Context context) {
		
		mScreenW = getResources().getDisplayMetrics().widthPixels;
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(lp);
		setOrientation(LinearLayout.VERTICAL);
		setBackgroundResource(R.drawable.bg_normal);
		
		// add title
		titleLayout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.view_title_layout, null);
		RelativeLayout.LayoutParams titlelLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		titlelLayoutParams.width = mScreenW;
		// or u can resize it
		titlelLayoutParams.height = getResources().getDimensionPixelSize(R.dimen.drag_view_title_height);
		titleLayout.setLayoutParams(titlelLayoutParams);

		addView(titleLayout);
		
		titleTV = (TextView) titleLayout.findViewById(R.id.title_text);
		handleIV = (ImageView) titleLayout.findViewById(R.id.bg_handle_img);
		titleIV = (ImageView) titleLayout.findViewById(R.id.bg_icon_img);
		

	}
	/***  Drawable res Id  , -1 means use default resource
	 * @param titleImgResId   
	 * @param title
	 * @param handleImgResId
	 */
	public void setTileBar(int titleImgResId, String title, int handleImgResId)
	{
		if (titleImgResId != -1)
		{
			titleIV.setImageResource(titleImgResId);
		}
		if (title != null)
		{
			titleTV.setText(title);			
		}
		if (handleImgResId != -1)
		{			
			handleIV.setImageResource(handleImgResId);
		}
	}
	public void setContentView(View view)
	{
		if (contentView != null) {
			removeView(contentView);
		}
		contentView = view;
		addView(view);

	}
	
	public void changeModeToDRAGING()
	{
		if (mMode != DRAGING)
		{
			mMode = DRAGING;
			setBackgroundResource(R.drawable.bg_pressed);
			
		}
	}
	
	public void changeModeToIDLE()
	{
		if (mMode != IDLE)
		{
			mMode = IDLE;
			setBackgroundResource(R.drawable.bg_normal);
		}
	}
	public void setBackground(int resId)
	{
		setBackgroundResource(resId);
	}
	
	
}
