package cn.fu.dragsortview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BaseDragView extends LinearLayout {

	private TextView titleTextView; // 名称
	private ImageView handleImageView; // 可拖动标记
	private RelativeLayout titleLayout;
	private ImageView iconImg; // 可拖动标记
	
	
	private View contentView;
	
	private final int IDLE = 0;
	private final int DRAGING = 1;	
	private int mMode = IDLE;
	
	protected int mHeaderPadding; // Header的左右Padding
	protected int mScreenW; // 屏幕宽度
	protected int dragViewMarginSide = 0;
	
	public BaseDragView(Context context) {
		super(context);
		initBaseView(context);
	}

	public BaseDragView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBaseView(context);
	}

	private void initBaseView(Context context) {
		mHeaderPadding = getResources().getDimensionPixelSize(R.dimen.drag_view_margin_left);
		dragViewMarginSide = context.getResources().getDimensionPixelOffset(R.dimen.drag_view_margin_side);        //自身左右margin
		
		mScreenW = getResources().getDisplayMetrics().widthPixels;
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(lp);
		setOrientation(LinearLayout.VERTICAL);
		setBackgroundResource(R.drawable.weather_forcast_bg);
		// 添加title
		titleLayout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.drag_item_title, null);
		RelativeLayout.LayoutParams titlelLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		titlelLayoutParams.width = mScreenW;
		titlelLayoutParams.height = getResources().getDimensionPixelSize(R.dimen.drag_view_title_height);
		//因为leftmargin现在不等于0，保存左右对称
		titleLayout.setPadding(mHeaderPadding-dragViewMarginSide, 0, mHeaderPadding+dragViewMarginSide, 0);
		titleLayout.setLayoutParams(titlelLayoutParams);

		addView(titleLayout);
		
		titleTextView = (TextView) titleLayout.findViewById(R.id.title_text);
		handleImageView = (ImageView) titleLayout.findViewById(R.id.corner_img);
		iconImg = (ImageView) titleLayout.findViewById(R.id.dragview_icon_img);
		

	}
	
	public void setTileBar(int titleImgResId, String title, int cornerImgResId)
	{
		if (titleImgResId != -1)
		{
			iconImg.setImageResource(titleImgResId);
		}
		if (title != null)
		{
			titleTextView.setText(title);			
		}
		if (cornerImgResId != -1)
		{			
			handleImageView.setImageResource(cornerImgResId);
		}
	}
	public void setContentView(View view)
	{
//		RelativeLayout.LayoutParams contentParams =null;
//		 contentParams = (LayoutParams) view.getLayoutParams();
//		if (contentParams == null)
//		{
//			contentParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);			
//		}
//		contentParams.addRule(RelativeLayout.BELOW, titleLayout.getId());
//		contentParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
//		view.setLayoutParams(contentParams);
		if (contentView != null) {
			removeView(contentView);
		}
		contentView = view;
		int paddingLeft = mHeaderPadding-dragViewMarginSide;
		int paddingRight = mHeaderPadding+dragViewMarginSide;
		view.setPadding(paddingLeft, 0, paddingRight, 0);
		addView(view);

	}
	
	public void changeModeToDRAGING()
	{
		if (mMode != DRAGING)
		{
			mMode = DRAGING;
			setBackgroundResource(R.drawable.weather_forcast_bg_frame);
			
		}
	}
	
	public void changeModeToIDLE()
	{
		if (mMode != IDLE)
		{
			mMode = IDLE;
			setBackgroundResource(R.drawable.weather_forcast_bg);
		}
	}
	public void setBackground(int resId)
	{
		setBackgroundResource(resId);
	}
	
	
}
