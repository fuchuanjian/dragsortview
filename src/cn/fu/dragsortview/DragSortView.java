package cn.fu.dragsortview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;

public class DragSortView extends ViewGroup
{
	private Context mContext;
	
	//各种拖拽view 之间的间隔 在values dimens中设置 drag_view_padding
	private int dragView_Padding;
	private static final int IDLE = 0;
	private static final int DRAGING = 1;
	private static final int UP = 2;
	private static final int NOT_MEASURE = -3;
	private static  int myMode = IDLE;
	private final int DIFF = 10;
	private int lastDownY = -1;
	private int targetOrder = -1;
	private int detalY =0;
	private int lastTagetPosY = 0;
	private int targetViewPosY = 0;
	private final int LONGPRESS_DELAY = 500;
	private int mCurScrollY = 0;
	private int dragRangeUpLine = 0;
	private int dragRangeBottomLine = 0; 
	private final int UP_SCROLL = -1;
	private final int STOP_SCROLL = 0;
	private final int DOWN_SCROLL = 1;
	private int mAutoScrolDirection = 0;
	private int mAutoScrollInterval = 2;				// 每次滚动距离的间隔
	private final int mAutoScrollInterval_time = 0;     // 每次滚动距离的时间间隔
	private final int DEFLAUT_VALUE = -1;
	private int getEventY = 0;
	private float scale = 1.0f;      //屏幕分辩率 
	private boolean isMessure = false;
	
	private int dragViewMarginSide = 4;
	private int totalMarginBottom;
	private LinearLayout targetView;
	private ViewInfo targetViewContainer;
	private int screenW; // 屏幕宽度
	private int screenH; // 屏幕高度
	private Scroller mScroller;
	private int sumDragViewHeight = 0;
	private ArrayList<ViewInfo> infoContainerList = new ArrayList<ViewInfo>();
	
//	private ForecastDragView mWeatherForecastView; // 天气预报区域
//	private TrendWeatherDragView mTrendWeatherDragView; // 天气趋势区域
//	private LifeInfoView mLifeInfoView; // 生活指数区域
//	private PressureDragView mPressureDragView; //气压风速栏
	
	private boolean isTargetAnimaFinished = true;
	private boolean isAdjustAnimaFinished = true;

	public DragSortView(Context context)
	{
		super(context);
		init(context);
	}
	private void init(Context context) {
		mScroller = new Scroller(context);
		mContext = context;
		screenW = getResources().getDisplayMetrics().widthPixels;
		screenH = getResources().getDisplayMetrics().heightPixels;
		//取dimen中dragview 之间预设的间隔
		scale = context.getResources().getDisplayMetrics().density;    
//		mAutoScrollInterval *= scale;
		dragView_Padding = context.getResources().getDimensionPixelOffset(R.dimen.drag_view_padding);              //上下padding
		dragViewMarginSide = context.getResources().getDimensionPixelOffset(R.dimen.drag_view_margin_side);        //自身左右margin
		
		//这是整个dragviewcontainer 预留底部的间隙
		totalMarginBottom = mContext.getResources().getDimensionPixelSize(R.dimen.dragview_container_bottom_margin);
		
		DragViewFrame myView1 = new DragViewFrame(context);
		DragViewFrame myView2 = new DragViewFrame(context);	
		DragViewFrame myView3 = new DragViewFrame(context);
		DragViewFrame myView4 = new DragViewFrame(context);	
		addWightViewItem(context, myView1, "view1", screenW);
		addWightViewItem(context, myView2, "view2", screenW);
		addWightViewItem(context, myView3, "view1", screenW);
		addWightViewItem(context, myView4, "view2", screenW);
		
		sortWightView();
	}
	private void addWightViewItem(Context context, View view ,String nickName, int screenW) {
		// 只是添加,没有布局
		addView(view);
		ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
				MarginLayoutParams.MATCH_PARENT,
				MarginLayoutParams.WRAP_CONTENT);
		
		//在addview时修改leftMargin 和 rightMargin 但是好像rightMargin不起作用，所以在onlayout里边的view.layout()又再次设定了定值
		lp.leftMargin = dragViewMarginSide;
		lp.rightMargin = dragViewMarginSide;
		lp.topMargin = NOT_MEASURE;
		view.measure(MeasureSpec.makeMeasureSpec(screenW-dragViewMarginSide*2, MeasureSpec.AT_MOST),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		lp.width = screenW - dragViewMarginSide*2;
		lp.height = view.getMeasuredHeight();
		view.setLayoutParams(lp);

		ViewInfo viewInfo = new ViewInfo();
		viewInfo.name = nickName;
		viewInfo.order = OrderSettingUtil.getIntPref(mContext, nickName, 0);
		viewInfo.view = (LinearLayout) view;
		viewInfo.height = view.getMeasuredHeight();
		viewInfo.topMargin = NOT_MEASURE;
		infoContainerList.add(viewInfo);
	}
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{;
		int nextTopMargin = 0 ;
		sumDragViewHeight = 0;
		for (int i = 0; i < infoContainerList.size(); i++) {
			ViewInfo container = infoContainerList.get(i);
			View child = container.view;
			//dragViewMarginSide 是左右的间隔
			child.measure(
					MeasureSpec.makeMeasureSpec(screenW-dragViewMarginSide*2, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			MarginLayoutParams lP = (MarginLayoutParams) child.getLayoutParams();
			
			if (isDragAnimationFinished())
			{
				nextTopMargin += dragView_Padding;
				lP.topMargin = nextTopMargin;
				container.topMargin = nextTopMargin;
				container.height = child.getMeasuredHeight();
				child.layout(lP.leftMargin, lP.topMargin,
						lP.leftMargin + child.getMeasuredWidth(), lP.topMargin + container.height);	
			}else {			
//				container.height = child.getMeasuredHeight();
				child.layout(lP.leftMargin, lP.topMargin,
						lP.leftMargin + child.getMeasuredWidth(), lP.topMargin + child.getMeasuredHeight());			
			}
			nextTopMargin += child.getMeasuredHeight();
			sumDragViewHeight += child.getMeasuredHeight()+ dragView_Padding;

//			Trace.e("wzt", "---totalHeight:" + mTotalHeight);
		}
		//totalMarginBottom是底部预留的间隔
		dragRangeBottomLine = sumDragViewHeight + totalMarginBottom;
//		return dragRangeBottomLine > 0 ? dragRangeBottomLine: totalHeight;
	}
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		handleMotionEventToDragView(event,getScrollY());
		return true;
	}
	public boolean handleMotionEventToDragView(MotionEvent event, int scrollY) {
		mCurScrollY = scrollY;
		if (myMode == IDLE) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (isMessure == false)
				{
					isMessure = true;
//					mAutoScrollInterval = mWeatherView.mVisibleH / 60;
				}
				lastDownY = -1;
				int k = findHitItemOrder(event);
				if (k != -1) {
					lastDownY = (int) event.getY();
					targetViewContainer = infoContainerList.get(k);
					targetView = targetViewContainer.view;
					bringChildToFront(targetView);
					mLongPressHandler.postDelayed(mLongPressRunnable,
							LONGPRESS_DELAY);
				}
				return false;

			case MotionEvent.ACTION_MOVE:
				if (lastDownY > 0 && Math.abs(event.getY() - lastDownY) > 10) {
					mLongPressHandler.removeCallbacks(mLongPressRunnable);
				}
				return false;
			case MotionEvent.ACTION_UP:
				mLongPressHandler.removeCallbacks(mLongPressRunnable);
				return false;
				
			case MotionEvent.ACTION_CANCEL:
				mLongPressHandler.removeCallbacks(mLongPressRunnable);
				return false;
			}
		} else if (myMode == DRAGING) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;

			case MotionEvent.ACTION_MOVE:
				// 有bug ，当左右切换移动时会出现问题.
				if (targetView != null && targetOrder!=-1)
				{
					//记录上一个位置
					lastTagetPosY = targetViewPosY;
					
					constraintLayoutParams(event);
					targetView.requestLayout();
//					targetView.invalidate();				
					adjustScroll(event);
					adjustAllItems();
				}
				break;
				
			case MotionEvent.ACTION_UP:
				//Log.i("fu", "draging UP目标tar"+targetOrder);
				myMode = IDLE;
				((BaseDragView)targetView).changeModeToIDLE();
				setSrollDirction(STOP_SCROLL);
				autoScrollHandler.removeCallbacks(scrollrRunnable);	
				mLongPressHandler.removeCallbacks(mLongPressRunnable);
				if (targetViewPosY < dragRangeUpLine)
				{
					targetViewPosY = dragRangeUpLine;
				}
				if (targetOrder != -1)
				{
					magneticTargetIem(targetOrder);
					targetOrder = -1;
				}
				break;
				
			case MotionEvent.ACTION_CANCEL:
				myMode = IDLE;
				((BaseDragView)targetView).changeModeToIDLE();
				//Log.i("fu",  "draging canel事件 cancel目标tar"+targetOrder);
				setSrollDirction(STOP_SCROLL);
				autoScrollHandler.removeCallbacks(scrollrRunnable);	
				mLongPressHandler.removeCallbacks(mLongPressRunnable);
			//	targetViewPosY = (int) event.getY() + mCurScrollY - detalY;
				if (targetViewPosY < dragRangeUpLine)
				{
					targetViewPosY = dragRangeUpLine;
				}
				if (targetOrder != -1)
				{
					magneticTargetIem(targetOrder);
					targetOrder = -1;
				}
				break;
			}

		} else if (myMode == UP) {

		}
		return true;
	}
	private void adjustAllItems() {
		
		ViewInfo upViewContainer = null;
		ViewInfo downViewContainer = null;
		if (targetOrder != 0) {
			upViewContainer = infoContainerList.get(targetOrder - 1);
		}
		if (targetOrder != infoContainerList.size() - 1) {
			downViewContainer = infoContainerList.get(targetOrder + 1);
		}
		boolean isCollideUp =  checkIfCollideUp(copyViewInfo(targetViewContainer),copyViewInfo(upViewContainer));
		boolean isCollideDown = checkIfCollideDown(copyViewInfo(targetViewContainer), copyViewInfo(downViewContainer));
		
//		// 上移
		if (isCollideUp)	
		{
			//动画开始的标记
			isAdjustAnimaFinished = false; 
			
			// 跟上面的冲突了,上面的图往下移动
			final ViewInfo upInfo = copyViewInfo(upViewContainer);
			final ViewInfo targetInfo = copyViewInfo(targetViewContainer);
			TranslateAnimation translateAnimation = new TranslateAnimation(0,
					0, 0, targetInfo.height + dragView_Padding);
			translateAnimation.setDuration((int)(targetInfo.height*1.5/scale));
			translateAnimation.setInterpolator(new AccelerateInterpolator());
			final LinearLayout view = upInfo.view;
			MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
			lp.topMargin = upViewContainer.topMargin;
			view.setLayoutParams(lp);
//			final int startY = upViewContainer.topMargin;
			final int distanceY = targetInfo.height
					+ upInfo.topMargin + dragView_Padding;
			final int scrollDownY = targetViewPosY - distanceY;
			final boolean isHelpScroll = false;
			view.startAnimation(translateAnimation);
			translateAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					if (isHelpScroll)
					{
						//自动调整，测试用
//						mWeatherView.getScroller().startScroll(0, mWeatherView.getScrollY(), 0, -200, 1000);
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					view.clearAnimation();
					TranslateAnimation anim = new TranslateAnimation(0, 0, 0, 0);
					view.startAnimation(anim);
					MarginLayoutParams lp = (MarginLayoutParams) view
							.getLayoutParams();
					lp.topMargin = distanceY;
					view.setLayoutParams(lp);
					view.requestLayout();
//					view.invalidate();
					//动画结束的标志
					isAdjustAnimaFinished = true;
				}
			});
			swapViewInfoContainerByOrder(targetOrder, upViewContainer.order);
		} else if (isCollideDown) {		
			//动画开始的标记
			isAdjustAnimaFinished = false;
			
			final ViewInfo downInfo = copyViewInfo(downViewContainer);
			final ViewInfo targetInfo = copyViewInfo(targetViewContainer);
			
			TranslateAnimation translateAnimation = new TranslateAnimation(0,
					0, 0, -targetInfo.height- dragView_Padding);
			translateAnimation.setDuration((int)(targetInfo.height*1.5/scale));
			translateAnimation.setInterpolator(new AccelerateInterpolator());
			final LinearLayout view = downInfo.view;
			MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
			lp.topMargin = downInfo.topMargin;
			view.setLayoutParams(lp);
//			final int startY = downViewContainer.topMargin;
			final int distanceY = targetInfo.topMargin;		
			view.startAnimation(translateAnimation);
			final boolean isHelpScroll = false;
			translateAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					if (isHelpScroll)
					{
						//自动调整，测试用
//						mWeatherView.getScroller().startScroll(0, mWeatherView.getScrollY(), 0, 200, 1000);
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					view.clearAnimation();
					TranslateAnimation anim = new TranslateAnimation(0, 0, 0, 0);
					view.startAnimation(anim);
					MarginLayoutParams lp = (MarginLayoutParams) view
							.getLayoutParams();
					lp.topMargin = distanceY;
					view.setLayoutParams(lp);
					view.requestLayout();
//					view.invalidate();
					
					//动画结束的标记
					isAdjustAnimaFinished = true;
				}
			});
			swapViewInfoContainerByOrder(targetOrder, downViewContainer.order);
		}

	}

	private boolean checkIfCollideUp(final ViewInfo targetInfo, final ViewInfo upInfo)
	{
		if (targetInfo == null || upInfo == null)
			return false;
		int upLine = upInfo.topMargin + upInfo.height *2/3;
		
		//拖拽view顶部 经过上面view的下半1/3线
		if (targetViewPosY < upLine && lastTagetPosY >= upLine)
			return true;
		//拖拽view自身1/3 经过上面view的底部
		if ((targetViewPosY + targetInfo.height/3 < upInfo.topMargin+upInfo.height && lastTagetPosY + targetInfo.height/3 >= upInfo.topMargin+upInfo.height))
			return true;
		//反复拖拽 自身顶部经过上一个图的顶部
		if (targetViewPosY <= upInfo.topMargin+DIFF && lastTagetPosY > upInfo.topMargin+DIFF)
			return true;
		return false;
	}
	private boolean checkIfCollideDown(final ViewInfo targetInfo, final ViewInfo downInfo)
	{		
		if (targetInfo == null || downInfo == null)
		return false;
		
		int downLine = downInfo.topMargin+ downInfo.height / 3;
		//自身底部经过下图的顶部1/3
//		Log.i("fu", "中线"+downLine +"   此时"+(targetViewPosY + targetInfo.height) +"   前"+(lastTagetPosY + targetInfo.height));
		if (targetViewPosY + targetInfo.height > downLine && lastTagetPosY + targetInfo.height <= downLine)
			return true;
		//自身底部1/3经过下图的顶部
		if (targetViewPosY + targetInfo.height*2/3 > downInfo.topMargin  && lastTagetPosY + targetInfo.height*2/3 <= downInfo.topMargin )
			return true;
		
		//反复上下拖时 自身底部 经过下图底部
		if (targetViewPosY + targetInfo.height > downInfo.topMargin + downInfo.height-DIFF && lastTagetPosY + targetInfo.height <= downInfo.topMargin + downInfo.height-DIFF)
			return true;
		return false;
	}

	private int findHitItemOrder(MotionEvent event) {

		int len = infoContainerList.size();
		for (int i = 0; i < len; i++) {
			ViewInfo container = infoContainerList.get(i);
			RelativeLayout tv = (RelativeLayout) container.view.findViewById(R.id.drag_handle);
			tv.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			if (container.topMargin <= (event.getY() + getScrollY() + DIFF/10)
					&& (container.topMargin + tv.getMeasuredHeight()) >= (event
							.getY() + getScrollY() - DIFF/10)) {
				targetOrder = i;
				detalY = (int) (event.getY() + getScrollY() - container.topMargin);
				lastTagetPosY = targetViewPosY;
				targetViewPosY = container.topMargin;
				getEventY = (int) event.getY() - detalY;
				return i;
			}
		}
		return -1;
	}
	
	private void swapViewInfoContainerByOrder(int targetFrom, int to) {
		LinearLayout tmp_RelativeLayout = infoContainerList.get(targetFrom).view;
		infoContainerList.get(targetFrom).view = infoContainerList.get(to).view;
		infoContainerList.get(to).view = tmp_RelativeLayout;

		String tmp_name = infoContainerList.get(targetFrom).name;
		infoContainerList.get(targetFrom).name = infoContainerList.get(to).name;
		infoContainerList.get(to).name = tmp_name;
		
		if (targetFrom < to) {
			infoContainerList.get(to).topMargin = infoContainerList
					.get(targetFrom).topMargin
					+ infoContainerList.get(to).height + dragView_Padding;
//			这里不能对LayoutParams操作 因为动画还没结束
//			MarginLayoutParams lp = (MarginLayoutParams) infoContainerList.get(to).view.getLayoutParams();
//			lp.topMargin = infoContainerList.get(to).topMargin;

		} else {
			infoContainerList.get(targetFrom).topMargin = infoContainerList
					.get(to).topMargin
					+ infoContainerList.get(targetFrom).height + dragView_Padding;
		}

		int tmp_height = infoContainerList.get(targetFrom).height;
		infoContainerList.get(targetFrom).height = infoContainerList.get(to).height;
		infoContainerList.get(to).height = tmp_height;
		
		targetOrder = to;
		targetView = infoContainerList.get(targetOrder).view;
		targetViewContainer = infoContainerList.get(targetOrder);
		
		OrderSettingUtil.setIntPref(mContext, infoContainerList.get(targetFrom).name, infoContainerList.get(targetFrom).order);
		OrderSettingUtil.setIntPref(mContext, infoContainerList.get(to).name, infoContainerList.get(to).order);
		
		
	}
	/**
	 * 拖拽view松手的磁性效果
	 * @param targetOrder
	 */
		private void magneticTargetIem(int targetOrder) {
			//动画开始的标记
			isTargetAnimaFinished = false;
			
			//用final类型变量计算，因为动画结束时有延迟的
			final ViewInfo targetInfo = copyViewInfo(targetViewContainer);
			// AnimationSet animationSet = new AnimationSet(true);
			TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0,
					-targetViewPosY + targetInfo.topMargin);
			translateAnimation.setDuration((int)(Math.abs(-targetViewPosY + targetInfo.topMargin)*1.5/scale)); 
			translateAnimation.setInterpolator(new AccelerateInterpolator());
			// animationSet.addAnimation(translateAnimation);
			targetInfo.view.startAnimation(translateAnimation);
			translateAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					targetInfo.view.clearAnimation();// 解决移动后闪烁现象 ①
					TranslateAnimation anim = new TranslateAnimation(0, 0, 0, 0);
					targetInfo.view.startAnimation(anim);
					MarginLayoutParams lp = (MarginLayoutParams) targetInfo.view
							.getLayoutParams();
					lp.topMargin = targetInfo.topMargin;
					lastTagetPosY = targetViewPosY;
					targetViewPosY = lp.topMargin;
					targetInfo.view.setLayoutParams(lp);
					targetInfo.view.requestLayout();
//					targetViewContainer.view.invalidate();
					
					//动画结束的标记
					isTargetAnimaFinished = true;
				}
			});

		}
		/**
		 * 约束拖拽窗的位置，不要超过屏幕边界
		 * @param event
		 */
		private void constraintLayoutParams(MotionEvent event)
		{
			int py = targetViewContainer.height*2 > screenH? targetViewContainer.height -screenH/2 : targetViewContainer.height/2;
			MarginLayoutParams lp = (MarginLayoutParams) targetView.getLayoutParams();
			lp.topMargin = (int) event.getY() + mCurScrollY - detalY;
			targetViewPosY = (int) event.getY() + mCurScrollY - detalY;
			getEventY = (int) event.getY() - detalY;
			if (event.getY()-detalY<= 0)
			{
				lp.topMargin =  mCurScrollY ;
				targetViewPosY =   mCurScrollY;
			}else if (event.getY()- detalY>= screenH-lp.height/2 && targetViewContainer.height< screenH/2 ) {
				lp.topMargin =  screenH-lp.height/2 +mCurScrollY;
				targetViewPosY =  screenH-lp.height/2 + mCurScrollY;
			}else if (targetViewContainer.height >=screenH/2 && event.getY()- detalY >= screenH -0.1* screenH ) {
				lp.topMargin =  (int) (mCurScrollY + screenH*0.9);
				targetViewPosY =  (int) (mCurScrollY + screenH*0.9);
			}
				
			if (lp.topMargin <= dragRangeUpLine)
			{
				lp.topMargin = dragRangeUpLine;
				targetViewPosY = dragRangeUpLine;
			}else if (lp.topMargin >= (dragRangeBottomLine-py))  {
				lp.topMargin = dragRangeBottomLine-py;
				targetViewPosY = dragRangeBottomLine-py;
			}
			targetView.setLayoutParams(lp);
		}	
	private Handler mLongPressHandler = new Handler();
	private Runnable mLongPressRunnable = new Runnable() {

		@Override
		public void run() {
			myMode = DRAGING;
			((BaseDragView)targetView).changeModeToDRAGING();
			vibrate();
		}
	};
	private void vibrate() {
		try {
			Vibrator vib = (Vibrator) mContext
					.getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
			vib.vibrate(100);			
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
	/**
	 * 判断是否允许自动上移的标记，发生在当最后一个拖拽view在最下位置的时候
	 * 1.最后一个view 且 2.处于stop_sroll状态 且 3.他的顶点坐标低于上图的底部坐标
	 */
	private boolean checkIsAllowUpScroll()
	{
		if (targetOrder != infoContainerList.size()-1)
			return true;

		if (mAutoScrolDirection != STOP_SCROLL)	
			return true;

		if (targetOrder >0 && targetViewPosY < infoContainerList.get(targetOrder-1).topMargin +  infoContainerList.get(targetOrder-1).height*2/3)
			return true;
		
		return false;
	}
	
	public void refreshAndSortDragView()
	{
		for (int i=0; i<infoContainerList.size(); i++)
		{
			ViewInfo viewInfo = infoContainerList.get(i);
			viewInfo.order = OrderSettingUtil.getIntPref(mContext, viewInfo.name, 0);	
		}
		sortWightView();
		for (int i=0; i<infoContainerList.size(); i++)
		{
			ViewInfo viewInfo = infoContainerList.get(i);
			viewInfo.view.measure(
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			viewInfo.view.requestLayout();
//			viewInfo.view.invalidate();
		}
//		mWeatherView.requestLayout();
//		mWeatherView.invalidate();
		
	}
	private void adjustScroll(MotionEvent event) {
		
		//判断是否允许自动上移的标记，发生在当最后一个拖拽view在最下位置的时候
		boolean isAllowUpScroll = checkIsAllowUpScroll();
		
		
		//如果view的高度太高，例如生活指数，就走这里
		if (targetViewContainer.height > screenH/2)
		{
			if (event.getY()-detalY < (screenH)*0.1)
			{
				if (mAutoScrolDirection != DOWN_SCROLL)
				{			
					autoScrollHandler.post(scrollrRunnable);
				}
				setSrollDirction(DOWN_SCROLL);
			}else if (event.getY() -detalY >  screenH*0.8 && isAllowUpScroll) 
				{
					if (mAutoScrolDirection != UP_SCROLL)
					{
						autoScrollHandler.post(scrollrRunnable);				
					}
					setSrollDirction(UP_SCROLL);
				}else {
					setSrollDirction(STOP_SCROLL);
					autoScrollHandler.removeCallbacks(scrollrRunnable);
				}
		 return;
		}
		Log.i("fu", (event.getY() -detalY) +" " +  (screenH- targetViewContainer.height*0.8) );
		//正常逻辑
		if (event.getY()-detalY < (screenH)*0.1)
		{
			Log.i("fu", "DOWN_SCROLL "+ isAllowUpScroll );
			if (mAutoScrolDirection != DOWN_SCROLL)
			{			
				autoScrollHandler.post(scrollrRunnable);
			}
			setSrollDirction(DOWN_SCROLL);
		}else if (event.getY() -detalY >  screenH- targetViewContainer.height*0.8 && getScrollY() < sumDragViewHeight -screenH && isAllowUpScroll) {
			Log.i("fu", "UP_SCROLL");
			if (mAutoScrolDirection != UP_SCROLL)
			{
				autoScrollHandler.post(scrollrRunnable);				
			}
			setSrollDirction(UP_SCROLL);
		}else {
			setSrollDirction(STOP_SCROLL);
			autoScrollHandler.removeCallbacks(scrollrRunnable);
		}
		
	}
	public void setSrollDirction(int dir)
	{
		mAutoScrolDirection = dir;
	}
	private Runnable scrollrRunnable = new Runnable() {
		@Override
		public void run() {
			Message msg = autoScrollHandler.obtainMessage();
			msg.sendToTarget();
			autoScrollHandler.postDelayed(scrollrRunnable, mAutoScrollInterval_time);	
		}
		
	};

/**
 * 屏幕自动滚动的handler
 */
	private Handler autoScrollHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg) 
		{
			if (targetView == null)
			{
				setSrollDirction(STOP_SCROLL);
				autoScrollHandler.removeCallbacks(scrollrRunnable);
				return;
			}
			MarginLayoutParams lp = (MarginLayoutParams) targetView.getLayoutParams();
			switch (mAutoScrolDirection)
			{
				case UP_SCROLL:
				if (getScrollY() >= sumDragViewHeight -screenH && targetViewContainer.height< screenH/2)
					{
						scrollTo(0, sumDragViewHeight - screenH);
						setSrollDirction(STOP_SCROLL);
						autoScrollHandler.removeCallbacks(scrollrRunnable);
						return;
					}
				//两种情况，一种是当targetview 高度大于半个屏幕，另一种是高度低于半个屏幕
				int py = targetViewContainer.height*2 > screenH ? targetViewContainer.height -screenH/2 : lp.height/2;
				if (lp.topMargin >= dragRangeBottomLine- py)  {
						lp.topMargin = dragRangeBottomLine-py;
						lastTagetPosY = targetViewPosY;
						targetViewPosY = dragRangeBottomLine-py;
						targetView.setLayoutParams(lp);
						setSrollDirction(STOP_SCROLL);
						Log.i("fu", "up11 getScrollY =  "+ getScrollY());
					}else {
						
						scrollBy(0, mAutoScrollInterval);
//						getScroller().startScroll(0, getScrollY(), 0, mAutoScrollInterval, mAutoScrollInterval_time);
//						mWeatherView.scrollBy(0, mAutoScrollInterval);
						lp.topMargin += mAutoScrollInterval;
						lastTagetPosY = targetViewPosY;
						targetViewPosY = lp.topMargin;
						targetView.setLayoutParams(lp);
						targetView.requestLayout();
//						targetView.invalidate();
						adjustAllItems();		
						Log.i("fu", "up getScrollY =  "+ getScrollY());
					}
					break;
				case STOP_SCROLL:
					autoScrollHandler.removeCallbacks(scrollrRunnable);
					break;
				case DOWN_SCROLL:
					if (lp.topMargin <= dragRangeUpLine)
					{
						lp.topMargin = dragRangeUpLine;
						lastTagetPosY = targetViewPosY;
						targetViewPosY = lp.topMargin;
						targetView.setLayoutParams(lp);
						setSrollDirction(STOP_SCROLL);
						Log.i("fu", "down11 getScrollY =  "+ getScrollY());
					}else {
						getScroller().startScroll(0, getScrollY(), 0, -mAutoScrollInterval, mAutoScrollInterval_time);
						scrollBy(0, -mAutoScrollInterval);
//						mWeatherView.scrollBy(0, -mAutoScrollInterval);
						lp.topMargin -= mAutoScrollInterval;
						lastTagetPosY = targetViewPosY;
						targetViewPosY = lp.topMargin;
						targetView.setLayoutParams(lp);
						targetView.requestLayout();
//						targetView.invalidate();
						adjustAllItems();	
						Log.i("fu", "down getScrollY =  "+ getScrollY());
					}
					break;
			}
		}
	};
	private void sortWightView() {

		Comparator<ViewInfo> comparator = new ViewOrderComparator();
		Collections.sort(infoContainerList, comparator);
		for (int i = 0; i<infoContainerList.size() ;i++)
		{
			infoContainerList.get(i).order = i;
			//重置 infoContainerList的数据
			infoContainerList.get(i).topMargin = NOT_MEASURE;
//			mWeatherView.bringChildToFront(infoContainerList.get(i).view);
			bringChildToFront(infoContainerList.get(i).view);
			OrderSettingUtil.setIntPref(mContext, infoContainerList.get(i).name, infoContainerList.get(i).order);
		}
		
	}
	private class ViewOrderComparator implements Comparator<ViewInfo>
	{
		@Override
		public int compare(ViewInfo leftObj, ViewInfo rightObj)
		{
			int leftValue = leftObj.order;
			int rightValue = rightObj.order;
			
			if (leftValue < rightValue)
			{				
				return -1;
			}
			else if (leftValue > rightValue){				
				return 1;
			}else {
				return 0;
			}
		}
	}
	public static class OrderSettingUtil
	{
		public static final String VIEW_ORDER_SETTING = "drag_view_order";

		public static int getIntPref(Context context, String name, int def) {
			SharedPreferences prefs = context.getSharedPreferences(
					VIEW_ORDER_SETTING, Context.MODE_PRIVATE);
			return prefs.getInt(name, def);
		}

		public static void setIntPref(Context context, String name, int value) {
			SharedPreferences.Editor editPrefs = context.getSharedPreferences(
					VIEW_ORDER_SETTING, Context.MODE_PRIVATE).edit();
			editPrefs.putInt(name, value);
			editPrefs.commit();
		}
	}
	public static class ViewInfo {
		String name;
		int order;
		LinearLayout view;
		int height;
		int topMargin;
	}
	private ViewInfo copyViewInfo(ViewInfo src)
	{
		if (src == null)
			return null;
		ViewInfo des = new ViewInfo();
		des.name = src.name;
		des.order = src.order;
		des.view = src.view;
		des.height = src.height;
		des.topMargin = src.topMargin;
		return des;
	}

	/**
	 * 判断拖拽动作的动画是否完全结束
	 */
	private boolean isDragAnimationFinished()
	{
		if (myMode == IDLE && isAdjustAnimaFinished == true && isTargetAnimaFinished == true)
		{
			return true;
		}
		
		return false;
	}
	//清除内存
	public void release()
	{
		for (ViewInfo vi : infoContainerList)
		{
			vi.view = null;
		}
		infoContainerList.clear();
	}
	private Scroller getScroller()
	{
		return mScroller;
	}
}
