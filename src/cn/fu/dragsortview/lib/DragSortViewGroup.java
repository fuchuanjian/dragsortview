package cn.fu.dragsortview.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cn.fu.dragsortview.R;
import cn.fu.dragsortview.R.dimen;
import cn.fu.dragsortview.R.id;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;

public class DragSortViewGroup extends ViewGroup
{
	// main Params
	private Context mContext;
	private ArrayList<ViewInfo> containerList = new ArrayList<ViewInfo>();
	private LinearLayout targetView;
	private ViewInfo targetViewContainer;
	private Scroller mScroller;
	private AccelerateInterpolator acInterpolator = new AccelerateInterpolator();
	private Handler mHanler = new Handler();
	private VelocityTracker mVelocityTracker; // scroll speed tracker

	private int startY = 0;

	// related with press or drag state
	private static final int IDLE = 0;
	private static final int DRAGING = 1;
	private static final int NOT_MEASURE = -3;
	private static int myMode = IDLE;

	// some final params
	private final int DIFF = 10;
	private final int LONGPRESS_DELAY = 10;
	private final int UP_SCROLL = -1;
	private final int STOP_SCROLL = 0;
	private final int DOWN_SCROLL = 1;
	private final int mAutoScrollInterval_time = 0;
	private final int DEFLAUT_VALUE = -1;

	// tmp params
	private int dragView_Padding;
	private int lastDownY = -1;
	private int targetOrder = -1;
	private int detalY = 0;
	private int lastTagetPosY = 0;
	private int targetViewPosY = 0;
	private int lastMotionY = 0;
	private int mCurScrollY = 0;
	private int dragRangeUpLine = 0;
	private int dragRangeBottomLine = 0;
	private int mAutoScrolDirection = 0;
	private int mAutoScrollInterval = 10;
	private int getEventY = 0;
	private int dragViewMarginSide = 4;
	private int totalMarginBottom;
	private int screenW;
	private int screenH;
	private int sumDragViewHeight = 0;
	private float scale = 1.0f;
	private int adjustAnimaCnt = 0; // equal to 0 means none of view is
									// animating

	public DragSortViewGroup(Context context, BaseView... bvs)
	{
		super(context);
		init(context, bvs);
	}

	private void init(Context context, BaseView... bvs)
	{
		this.setBackgroundColor(Color.DKGRAY);
		mScroller = new Scroller(context);
		mContext = context;
		screenW = getResources().getDisplayMetrics().widthPixels;
		screenH = getResources().getDisplayMetrics().heightPixels;

		scale = context.getResources().getDisplayMetrics().density;
		mAutoScrollInterval *= scale;
		dragView_Padding = context.getResources().getDimensionPixelOffset(R.dimen.drag_view_padding); 

		dragViewMarginSide = context.getResources().getDimensionPixelOffset(R.dimen.drag_view_margin_side); 
																											
		// total bottom margin
		totalMarginBottom = mContext.getResources().getDimensionPixelSize(R.dimen.dragview_container_bottom_margin);

		// using nickname to help sort these views
		for (int i = 0; i < bvs.length; i++)
		{
			addWightViewItem(context, bvs[i], "view" + i);
		}

		sortWightView();
	}

	// just init view layoutparams here, not sort them, sort them later
	private void addWightViewItem(Context context, BaseView view, String nickName)
	{
		addView(view);
		ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT);

		// if need left or right margin , init them when setting layoutparams
		lp.leftMargin = dragViewMarginSide;
		lp.rightMargin = dragViewMarginSide;
		lp.topMargin = NOT_MEASURE; // just a flg

		// measure view params ,low efficiency needs to be improved
		view.measure(MeasureSpec.makeMeasureSpec(screenW - dragViewMarginSide * 2, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		lp.width = screenW - dragViewMarginSide * 2;
		lp.height = view.getMeasuredHeight();
		view.setLayoutParams(lp);

		ViewInfo viewInfo = new ViewInfo();
		viewInfo.name = nickName;
		viewInfo.order = OrderSettingUtil.getIntPref(mContext, nickName, 0);
		viewInfo.view = view;
		viewInfo.height = view.getMeasuredHeight();
		viewInfo.topMargin = NOT_MEASURE;
		containerList.add(viewInfo);
	}

	public void updateChildView()
	{
		for (ViewInfo vi : containerList)
		{
			vi.view.updateDate();
		}
	}

	/**
	 * need to override onLayout method
	 * */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		int nextTopMargin = startY;
		sumDragViewHeight = 0;

		for (int i = 0; i < containerList.size(); i++)
		{
			ViewInfo container = containerList.get(i);
			View child = container.view;
			child.measure(MeasureSpec.makeMeasureSpec(screenW - dragViewMarginSide * 2, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			MarginLayoutParams lP = (MarginLayoutParams) child.getLayoutParams();

			if (isDragAnimationFinished())
			{
				// force sort views by theirs view info
				nextTopMargin += dragView_Padding;
				lP.topMargin = nextTopMargin;
				container.topMargin = nextTopMargin;
				container.height = child.getMeasuredHeight();
				child.layout(lP.leftMargin, lP.topMargin, lP.leftMargin + child.getMeasuredWidth(), lP.topMargin + container.height);
			} else
			{
				child.layout(lP.leftMargin, lP.topMargin, lP.leftMargin + child.getMeasuredWidth(), lP.topMargin + child.getMeasuredHeight());
			}

			nextTopMargin += child.getMeasuredHeight();
			sumDragViewHeight += child.getMeasuredHeight() + dragView_Padding;

		}
		// bottom margin blank
		dragRangeBottomLine = sumDragViewHeight + totalMarginBottom;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		handleMotionEventToDragView(event, getScrollY());
		return true;
	}

	// so boring here , needs to refactor
	public boolean handleMotionEventToDragView(MotionEvent event, int scrollY)
	{

		initVelocityTacker();
		mVelocityTracker.addMovement(event);

		mCurScrollY = scrollY;
		if (myMode == IDLE)
		{
			switch (event.getAction() & MotionEvent.ACTION_MASK)
			{
			case MotionEvent.ACTION_DOWN:
				lastDownY = -1;
				lastMotionY = (int) event.getY();
				int k = findHitItemOrder(event);
				if (k != -1)
				{
					lastDownY = (int) event.getY();
					targetViewContainer = containerList.get(k);
					targetView = targetViewContainer.view;
					bringChildToFront(targetView);
					mLongPressHandler.postDelayed(mLongPressRunnable, LONGPRESS_DELAY);
				}

			case MotionEvent.ACTION_MOVE:
				if (lastDownY > 0 && Math.abs(event.getY() - lastDownY) > 10)
				{
					mLongPressHandler.removeCallbacks(mLongPressRunnable);
				}
				int curY = (int) event.getY();
				int detalY = lastMotionY - curY;
				if (getScrollY() < 0 || getScrollY() > sumDragViewHeight - getHeight())
				{
					detalY /= 4;
				}
				scrollBy(0, detalY);
				lastMotionY = curY;
				return false;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mLongPressHandler.removeCallbacks(mLongPressRunnable);

				mVelocityTracker.computeCurrentVelocity(1000, 8000);
				int initialVelocity = (int) mVelocityTracker.getYVelocity(event.getPointerId(0));
				if ((Math.abs(initialVelocity) > 50))
				{
					fling(-initialVelocity);
				} else
				{
					// revertLayout();
				}

				if (getScrollY() < 0)
				{
					scrollTo(0, 0);
				}
				if (getScrollY() > sumDragViewHeight - getHeight())
				{
					scrollTo(0, sumDragViewHeight - getHeight());
				}
				recycleVelocityTracker();
				return false;
			}
		} else if (myMode == DRAGING)
		{
			switch (event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
				break;

			case MotionEvent.ACTION_MOVE:
				if (targetView != null && targetOrder != -1)
				{
					lastTagetPosY = targetViewPosY;

					constraintLayoutParams(event);
					targetView.requestLayout();
					adjustScroll(event);
					adjustAllItems();
				}
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				myMode = IDLE;
				((BaseView) targetView).changeModeToIDLE();
				setSrollDirction(STOP_SCROLL);
				mHanler.removeCallbacks(mScrollRunable);
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
				recycleVelocityTracker();
				break;
			}

		}
		return true;
	}

	public void fling(int velocityY)
	{
		mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, sumDragViewHeight - getHeight());
		invalidate();
	}

	/**
	 * compute scroll params. must override it ,otherwise, view can't fling
	 * */
	@Override
	public void computeScroll()
	{
		if (mScroller.computeScrollOffset())
		{
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
			awakenScrollBars();
		}
	}

	private void initVelocityTacker()
	{
		if (mVelocityTracker == null)
		{
			mVelocityTracker = VelocityTracker.obtain();
		}

	}

	private void recycleVelocityTracker()
	{
		if (mVelocityTracker != null)
		{
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private void adjustAllItems()
	{

		ViewInfo upViewContainer = null;
		ViewInfo downViewContainer = null;
		if (targetOrder != 0)
		{
			upViewContainer = containerList.get(targetOrder - 1);
		}
		if (targetOrder != containerList.size() - 1)
		{
			downViewContainer = containerList.get(targetOrder + 1);
		}
		boolean isCollideUp = checkIfCollideUp(copyViewInfo(targetViewContainer), copyViewInfo(upViewContainer));
		boolean isCollideDown = checkIfCollideDown(copyViewInfo(targetViewContainer), copyViewInfo(downViewContainer));

		// // 上移
		if (isCollideUp)
		{
			// 动画开始的标记
			adjustAnimaCnt++;

			// 跟上面的冲突了,上面的图往下移动
			final ViewInfo upInfo = copyViewInfo(upViewContainer);
			final ViewInfo targetInfo = copyViewInfo(targetViewContainer);
			TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, targetInfo.height + dragView_Padding);
			translateAnimation.setDuration((int) (targetInfo.height * 1.5 / scale));
			translateAnimation.setInterpolator(acInterpolator);
			final LinearLayout view = upInfo.view;
			MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
			lp.topMargin = upViewContainer.topMargin;
			view.setLayoutParams(lp);
			// final int startY = upViewContainer.topMargin;
			final int distanceY = targetInfo.height + upInfo.topMargin + dragView_Padding;
			final int scrollDownY = targetViewPosY - distanceY;
			final boolean isHelpScroll = false;
			view.startAnimation(translateAnimation);
			translateAnimation.setAnimationListener(new AnimationListener()
			{
				@Override
				public void onAnimationStart(Animation animation)
				{
				}

				@Override
				public void onAnimationRepeat(Animation animation)
				{
				}

				@Override
				public void onAnimationEnd(Animation animation)
				{
					// fix bug ,view twinkle when animation finished , so add an
					// empty animation for fixing bug
					view.clearAnimation();
					TranslateAnimation anim = new TranslateAnimation(0, 0, 0, 0);
					view.startAnimation(anim);
					MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
					lp.topMargin = distanceY;
					view.setLayoutParams(lp);
					view.requestLayout();
					// view.invalidate();
					// set flg when animation finished
					adjustAnimaCnt--;
				}
			});
			swapViewInfoContainerByOrder(targetOrder, upViewContainer.order);
		} else if (isCollideDown)
		{
			// set flg when animaiton start
			adjustAnimaCnt++;

			final ViewInfo downInfo = copyViewInfo(downViewContainer);
			final ViewInfo targetInfo = copyViewInfo(targetViewContainer);

			TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, -targetInfo.height - dragView_Padding);
			translateAnimation.setDuration((int) (targetInfo.height * 1.5 / scale));
			translateAnimation.setInterpolator(acInterpolator);
			final LinearLayout view = downInfo.view;
			MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
			lp.topMargin = downInfo.topMargin;
			view.setLayoutParams(lp);
			// final int startY = downViewContainer.topMargin;
			final int distanceY = targetInfo.topMargin;
			view.startAnimation(translateAnimation);
			final boolean isHelpScroll = false;
			translateAnimation.setAnimationListener(new AnimationListener()
			{
				@Override
				public void onAnimationStart(Animation animation)
				{
				}

				@Override
				public void onAnimationRepeat(Animation animation)
				{
				}

				@Override
				public void onAnimationEnd(Animation animation)
				{
					view.clearAnimation();
					TranslateAnimation anim = new TranslateAnimation(0, 0, 0, 0);
					view.startAnimation(anim);
					MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
					lp.topMargin = distanceY;
					view.setLayoutParams(lp);
					view.requestLayout();

					adjustAnimaCnt--;
				}
			});
			swapViewInfoContainerByOrder(targetOrder, downViewContainer.order);
		}

	}

	private boolean checkIfCollideUp(final ViewInfo targetInfo, final ViewInfo upInfo)
	{
		if (targetInfo == null || upInfo == null)
			return false;
		int upLine = upInfo.topMargin + upInfo.height * 2 / 3;

		if (targetViewPosY < upLine && lastTagetPosY >= upLine)
			return true;

		if ((targetViewPosY + targetInfo.height / 3 < upInfo.topMargin + upInfo.height && lastTagetPosY + targetInfo.height / 3 >= upInfo.topMargin + upInfo.height))
			return true;

		if (targetViewPosY <= upInfo.topMargin + DIFF && lastTagetPosY > upInfo.topMargin + DIFF)
			return true;
		return false;
	}

	private boolean checkIfCollideDown(final ViewInfo targetInfo, final ViewInfo downInfo)
	{
		if (targetInfo == null || downInfo == null)
			return false;

		int downLine = downInfo.topMargin + downInfo.height / 3;

		if (targetViewPosY + targetInfo.height > downLine && lastTagetPosY + targetInfo.height <= downLine)
			return true;

		if (targetViewPosY + targetInfo.height * 2 / 3 > downInfo.topMargin && lastTagetPosY + targetInfo.height * 2 / 3 <= downInfo.topMargin)
			return true;

		if (targetViewPosY + targetInfo.height > downInfo.topMargin + downInfo.height - DIFF && lastTagetPosY + targetInfo.height <= downInfo.topMargin + downInfo.height - DIFF)
			return true;
		return false;
	}

	private int findHitItemOrder(MotionEvent event)
	{

		int len = containerList.size();
		for (int i = 0; i < len; i++)
		{
			ViewInfo container = containerList.get(i);
			RelativeLayout tv = (RelativeLayout) container.view.findViewById(R.id.drag_handle);
			tv.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			if (container.topMargin <= (event.getY() + getScrollY() + DIFF / 10) && (container.topMargin + tv.getMeasuredHeight()) >= (event.getY() + getScrollY() - DIFF / 10))
			{
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

	private void swapViewInfoContainerByOrder(int targetFrom, int to)
	{
		BaseView tmp_View = containerList.get(targetFrom).view;
		containerList.get(targetFrom).view = containerList.get(to).view;
		containerList.get(to).view = tmp_View;

		String tmp_name = containerList.get(targetFrom).name;
		containerList.get(targetFrom).name = containerList.get(to).name;
		containerList.get(to).name = tmp_name;

		if (targetFrom < to)
		{
			containerList.get(to).topMargin = containerList.get(targetFrom).topMargin + containerList.get(to).height + dragView_Padding;

		} else
		{
			containerList.get(targetFrom).topMargin = containerList.get(to).topMargin + containerList.get(targetFrom).height + dragView_Padding;
		}

		int tmp_height = containerList.get(targetFrom).height;
		containerList.get(targetFrom).height = containerList.get(to).height;
		containerList.get(to).height = tmp_height;

		targetOrder = to;
		targetView = containerList.get(targetOrder).view;
		targetViewContainer = containerList.get(targetOrder);

		OrderSettingUtil.setIntPref(mContext, containerList.get(targetFrom).name, containerList.get(targetFrom).order);
		OrderSettingUtil.setIntPref(mContext, containerList.get(to).name, containerList.get(to).order);

	}

	/**
	 * magnetic Target view by its order when MotionEvent.ACTION_UP
	 * 
	 * @see #targetOrder
	 */
	private void magneticTargetIem(int targetOrder)
	{
		// 动画开始的标记
		adjustAnimaCnt++;

		// 用final类型变量计算，因为动画结束时有延迟的
		final ViewInfo targetInfo = copyViewInfo(targetViewContainer);
		// AnimationSet animationSet = new AnimationSet(true);
		TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, -targetViewPosY + targetInfo.topMargin);
		translateAnimation.setDuration((int) (Math.abs(-targetViewPosY + targetInfo.topMargin) * 1.5 / scale));
		translateAnimation.setInterpolator(acInterpolator);
		// animationSet.addAnimation(translateAnimation);
		targetInfo.view.startAnimation(translateAnimation);
		translateAnimation.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				targetInfo.view.clearAnimation();// fix bug of view twinkle
				TranslateAnimation anim = new TranslateAnimation(0, 0, 0, 0);
				targetInfo.view.startAnimation(anim);
				MarginLayoutParams lp = (MarginLayoutParams) targetInfo.view.getLayoutParams();
				lp.topMargin = targetInfo.topMargin;
				lastTagetPosY = targetViewPosY;
				targetViewPosY = lp.topMargin;
				targetInfo.view.setLayoutParams(lp);
				targetInfo.view.requestLayout();

				// 动画结束的标记
				adjustAnimaCnt--;
			}
		});

	}

	/**
	 * 约束拖拽窗的位置，不要超过屏幕边界
	 * 
	 * @param event
	 */
	private void constraintLayoutParams(MotionEvent event)
	{
		int py = targetViewContainer.height * 2 > screenH ? targetViewContainer.height - screenH / 2 : targetViewContainer.height / 2;
		MarginLayoutParams lp = (MarginLayoutParams) targetView.getLayoutParams();
		lp.topMargin = (int) event.getY() + mCurScrollY - detalY;
		targetViewPosY = (int) event.getY() + mCurScrollY - detalY;
		getEventY = (int) event.getY() - detalY;
		if (event.getY() - detalY <= 0)
		{
			lp.topMargin = mCurScrollY;
			targetViewPosY = mCurScrollY;
		} else if (event.getY() - detalY >= screenH - lp.height / 2 && targetViewContainer.height < screenH / 2)
		{
			lp.topMargin = screenH - lp.height / 2 + mCurScrollY;
			targetViewPosY = screenH - lp.height / 2 + mCurScrollY;
		} else if (targetViewContainer.height >= screenH / 2 && event.getY() - detalY >= screenH - 0.1 * screenH)
		{
			lp.topMargin = (int) (mCurScrollY + screenH * 0.9);
			targetViewPosY = (int) (mCurScrollY + screenH * 0.9);
		}

		if (lp.topMargin <= dragRangeUpLine)
		{
			lp.topMargin = dragRangeUpLine;
			targetViewPosY = dragRangeUpLine;
		} else if (lp.topMargin >= (dragRangeBottomLine - py))
		{
			lp.topMargin = dragRangeBottomLine - py;
			targetViewPosY = dragRangeBottomLine - py;
		}
		targetView.setLayoutParams(lp);
	}

	private Handler mLongPressHandler = new Handler();
	private Runnable mLongPressRunnable = new Runnable()
	{

		@Override
		public void run()
		{
			myMode = DRAGING;
			((BaseView) targetView).changeModeToDRAGING();
			vibrate();
		}
	};

	private void vibrate()
	{
		try
		{
			Vibrator vib = (Vibrator) mContext.getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
			vib.vibrate(100);
		} catch (Exception e)
		{
			// TODO: handle exception
		}

	}

	private boolean checkIsAllowUpScroll()
	{
		if (targetOrder != containerList.size() - 1)
			return true;

		if (mAutoScrolDirection != STOP_SCROLL)
			return true;

		if (targetOrder > 0 && targetViewPosY < containerList.get(targetOrder - 1).topMargin + containerList.get(targetOrder - 1).height * 2 / 3)
			return true;

		return false;
	}

	private boolean checkIsAllowDownScroll()
	{
		if (targetOrder != 0)
			return true;

		if (mAutoScrolDirection != STOP_SCROLL)
			return true;

		return false;
	}

	public void refreshAndSortDragView()
	{
		for (int i = 0; i < containerList.size(); i++)
		{
			ViewInfo viewInfo = containerList.get(i);
			viewInfo.order = OrderSettingUtil.getIntPref(mContext, viewInfo.name, 0);
		}
		sortWightView();
		for (int i = 0; i < containerList.size(); i++)
		{
			ViewInfo viewInfo = containerList.get(i);
			viewInfo.view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			viewInfo.view.requestLayout();
			// viewInfo.view.invalidate();
		}
		// mWeatherView.requestLayout();
		// mWeatherView.invalidate();

	}

	private void adjustScroll(MotionEvent event)
	{

		// check if allow scroll
		boolean isAllowUpScroll = checkIsAllowUpScroll();
		boolean isAllowDownScroll = checkIsAllowDownScroll();

		// skip! skip ! skip this block, abnormal logic
		if (targetViewContainer.height > screenH / 2)
		{
			if (event.getY() - detalY < (screenH) * 0.1)
			{
				if (mAutoScrolDirection != DOWN_SCROLL)
				{
					mHanler.post(mScrollRunable);
				}
				setSrollDirction(DOWN_SCROLL);
			} else if (event.getY() - detalY > screenH * 0.8 && isAllowUpScroll)
			{
				if (mAutoScrolDirection != UP_SCROLL)
				{
					mHanler.post(mScrollRunable);
				}
				setSrollDirction(UP_SCROLL);
			} else
			{
				setSrollDirction(STOP_SCROLL);
				mHanler.removeCallbacks(mScrollRunable);
			}
			return;
		}

		// normal logic
		if (event.getY() - detalY < (screenH) * 0.1 && isAllowDownScroll)
		{
			if (mAutoScrolDirection != DOWN_SCROLL)
			{
				mHanler.post(mScrollRunable);
			}
			setSrollDirction(DOWN_SCROLL);
		} else if (event.getY() - detalY > screenH - targetViewContainer.height * 0.8 && getScrollY() < sumDragViewHeight - screenH && isAllowUpScroll)
		{
			if (mAutoScrolDirection != UP_SCROLL)
			{
				mHanler.post(mScrollRunable);
			}
			setSrollDirction(UP_SCROLL);
		} else
		{
			setSrollDirction(STOP_SCROLL);
			mHanler.removeCallbacks(mScrollRunable);
		}

	}

	public void setSrollDirction(int dir)
	{
		mAutoScrolDirection = dir;
	}

	private Runnable mScrollRunable = new Runnable()
	{
		@Override
		public void run()
		{
			if (targetView == null)
			{
				setSrollDirction(STOP_SCROLL);
				mHanler.removeCallbacks(mScrollRunable);
				return;
			}
			MarginLayoutParams lp = (MarginLayoutParams) targetView.getLayoutParams();
			switch (mAutoScrolDirection)
			{
			case UP_SCROLL:
				if (getScrollY() >= sumDragViewHeight - screenH && targetViewContainer.height < screenH / 2)
				{
					scrollTo(0, sumDragViewHeight - screenH);
					setSrollDirction(STOP_SCROLL);
					mHanler.removeCallbacks(mScrollRunable);
					return;
				}
				// if targetview's height is higher than half of screen then
				// enters first block , Otherwise enters second block
				int py = targetViewContainer.height * 2 > screenH ? targetViewContainer.height - screenH / 2 : lp.height / 2;
				if (lp.topMargin >= dragRangeBottomLine - py)
				{
					// skip this bock abnomal logic
					lp.topMargin = dragRangeBottomLine - py;
					lastTagetPosY = targetViewPosY;
					targetViewPosY = dragRangeBottomLine - py;
					targetView.setLayoutParams(lp);
					setSrollDirction(STOP_SCROLL);
				} else
				{
					// abnomal logic
					scrollBy(0, mAutoScrollInterval);
					lp.topMargin += mAutoScrollInterval;
					lastTagetPosY = targetViewPosY;
					targetViewPosY = lp.topMargin;
					targetView.setLayoutParams(lp);
					targetView.requestLayout();
					adjustAllItems();
				}
				break;
			case STOP_SCROLL:
				mHanler.removeCallbacks(mScrollRunable);
				break;
			case DOWN_SCROLL:
				if (lp.topMargin <= dragRangeUpLine)
				{
					lp.topMargin = dragRangeUpLine;
					lastTagetPosY = targetViewPosY;
					targetViewPosY = lp.topMargin;
					targetView.setLayoutParams(lp);
					setSrollDirction(STOP_SCROLL);
				} else
				{
					mScroller.startScroll(0, getScrollY(), 0, -mAutoScrollInterval, mAutoScrollInterval_time);
					scrollBy(0, -mAutoScrollInterval);
					lp.topMargin -= mAutoScrollInterval;
					lastTagetPosY = targetViewPosY;
					targetViewPosY = lp.topMargin;
					targetView.setLayoutParams(lp);
					targetView.requestLayout();
					adjustAllItems();
				}
				break;
			}
			mHanler.post(mScrollRunable);
		}
	};

	/**
	 * sort item views by their order {@link #ViewOrderComparator}
	 * */
	private void sortWightView()
	{

		Comparator<ViewInfo> comparator = new ViewOrderComparator();
		Collections.sort(containerList, comparator);
		for (int i = 0; i < containerList.size(); i++)
		{
			containerList.get(i).order = i;
			// reset not_measure flg
			containerList.get(i).topMargin = NOT_MEASURE;
			// bringChildToFront(containerList.get(i).view);
			OrderSettingUtil.setIntPref(mContext, containerList.get(i).name, containerList.get(i).order);
		}

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
	 * check drag animation is finished
	 * 
	 * @see #adjustAnimaCnt
	 */
	private boolean isDragAnimationFinished()
	{
		if (myMode == IDLE && adjustAnimaCnt == 0)
		{
			return true;
		}

		return false;
	}

	/**
	 * clear up memory
	 * */
	public void release()
	{
		for (ViewInfo vi : containerList)
		{
			vi.view = null;
		}
		containerList.clear();
	}

	/**
	 * Static Inner Class
	 * */

	/**
	 * an comparator for sortting the index of dragview
	 * */
	private static class ViewOrderComparator implements Comparator<ViewInfo>
	{
		@Override
		public int compare(ViewInfo leftObj, ViewInfo rightObj)
		{
			int leftValue = leftObj.order;
			int rightValue = rightObj.order;

			if (leftValue < rightValue)
			{
				return -1;
			} else if (leftValue > rightValue)
			{
				return 1;
			} else
			{
				return 0;
			}
		}
	}

	/**
	 * A SharedPref Uitl Class for record index
	 * */
	public static class OrderSettingUtil
	{
		public static final String VIEW_ORDER_SETTING = "drag_view_order";

		public static int getIntPref(Context context, String name, int def)
		{
			SharedPreferences prefs = context.getSharedPreferences(VIEW_ORDER_SETTING, Context.MODE_PRIVATE);
			return prefs.getInt(name, def);
		}

		public static void setIntPref(Context context, String name, int value)
		{
			SharedPreferences.Editor editPrefs = context.getSharedPreferences(VIEW_ORDER_SETTING, Context.MODE_PRIVATE).edit();
			editPrefs.putInt(name, value);
			editPrefs.commit();
		}
	}

	/**
	 * Package Class
	 * */
	public static class ViewInfo
	{
		String name;
		int order;
		BaseView view;
		int height;
		int topMargin;
	}
}
