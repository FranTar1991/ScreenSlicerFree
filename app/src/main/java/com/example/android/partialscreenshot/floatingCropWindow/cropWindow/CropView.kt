package com.example.android.partialscreenshot.floatingCropWindow.cropWindow

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.MotionEvent.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.DrawableCompat
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.floatingCropWindow.CropViewFloatingWindowService.Companion.manager
import com.example.android.partialscreenshot.floatingCropWindow.optionsWindow.OptionsWindowView
import com.example.android.partialscreenshot.utils.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import androidx.appcompat.content.res.AppCompatResources







// endregion
// region: Inner class: Type
enum class Type {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, CENTER;

    companion object{
        fun getCorner(type: Type): Int{
          return  when(type){
                TOP_LEFT, LEFT -> 0
                TOP_RIGHT,TOP -> 1
                BOTTOM_LEFT, BOTTOM-> 2
                BOTTOM_RIGHT,  RIGHT -> 3
                else -> 5
            }
        }
    }

}
typealias styles = R.styleable
@SuppressLint("AppCompatCustomView")
class CropView @JvmOverloads constructor(context: Context,
                                         attrs: AttributeSet? = null,
                                         defStyleAttr: Int = 0): ImageView(context,attrs, defStyleAttr) {


    private var gradientBottom: Drawable? = null
    private var gradientTop: Drawable? = null
    private var mIsToClose: Boolean = false
    private var isMultiTouch: Boolean = false
    var thisOptionsView: OptionsWindowView? = null

    var showDrawable: Boolean = true
    set(value) {
        croppedImage = null
        invalidate()
        requestLayout()
        field = value
    }

    var croppedImage: Bitmap? = null
    set(value) {
        field = value
        invalidate()
        requestLayout()
    }
    private lateinit var coordinatesRect: Rect
    private lateinit var mainRect: Rect
    private var closeDrawableRight: Int = 0
    private var closeDrawableBottom: Int = 0
    private var closeDrawableTop: Int = 0
    private var closeDrawableLeft: Int = 0
    private var corner: Int = 0
    private val mTouchOffset: Point = Point()
    private var moveView: Boolean = false
    private var newMeasureSpecSizeForWidth: Int = 0
    private var newMeasureSpecSizeForHeight: Int = 0
    private var rectangleFullSize: Int = 0
    private var rectangleFullWIdth: Int = 0
    private var inMatchParentMode = false

    private var newX = 0
    private var newY = INITIAL_POINT
    private var xSide: Int = 0
    private var ySide: Int = 0
    private var heightConstraint: Int = 0
    var widthConstraint: Int = 0
    private lateinit var callBackForWindowManager: OnMoveCropWindowListener
    private lateinit var requestTakeScreenShotCallback: OnRequestTakeScreenShotListener
    private var isInitialized: Boolean = false
    private var paint = Paint()
    private var paintForBack = Paint()

    private var secondRectPoints = Array<Point>(4){Point()}
    private var mainRectPoints = Array<Point>(4){Point()}

    private var start = Point()
    private var minimumSideLength: Int = 0
    private var widthOfRect: Int = 0
    private var heightOfRect: Int = 0
    private var halfDrawableSize: Int = 0
    private var halfCloseDrawableSize: Int = 0
    private var edgeColor: Int = 0
    private var outsideColor: Int = 0
    private var fillColor: Int = 0


    private var resizeDrawable: Drawable? = null
    private var closeDrawable: Drawable? = null
    private var cropLeftUpDrawable: Drawable? = null
    private var cropLeftDownDrawable: Drawable? = null
    private var cropRightUpDrawable: Drawable? = null
    private var cropRightDownDrawable: Drawable? = null
    private var mActivePointerId: Int = 0

    private var displayMetrics: DisplayMetrics? = null
    private var touchRadius: Float = 0f

    private var moveType: Type? = null

    init {
    context.withStyledAttributes(attrs, styles.CropView){

        minimumSideLength = (getDimensionPixelSize(styles.CropView_minimumSide, 20))

        halfCloseDrawableSize = ((getDimensionPixelSize(styles.CropView_cornerSize2, 20))/2)
        outsideColor = getColor(styles.CropView_outsideColor, Color.BLACK)
        edgeColor = getColor(styles.CropView_edgeColor,Color.WHITE)
        fillColor = getColor(styles.CropView_fillColor,Color.BLACK)

        setInitialRects()

        resizeDrawable = getDrawable(styles.CropView_resizeCornerDrawable)
        closeDrawable = getDrawable(styles.CropView_closeDrawable)
        cropLeftUpDrawable = getDrawable(styles.CropView_crop_left_up_drawable)
        cropLeftDownDrawable = getDrawable(styles.CropView_crop_left_down_drawable)
        cropRightUpDrawable = getDrawable(styles.CropView_crop_right_up_drawable)
        cropRightDownDrawable = getDrawable(styles.CropView_crop_right_down_drawable)

        displayMetrics = Resources.getSystem().displayMetrics
        touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, displayMetrics)

        paint.apply {
            isAntiAlias = true
            strokeWidth = 8f
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
            color = edgeColor

        }

        paintForBack.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeJoin = Paint.Join.ROUND
            color = outsideColor
            strokeWidth = 8f
        }

        gradientTop = AppCompatResources.getDrawable(context, R.drawable.gradient_top)
        gradientBottom = AppCompatResources.getDrawable(context, R.drawable.gradient_bottom)
        isInitialized = true
    }

    }


    private fun setInitialRects() {

        widthOfRect = minimumSideLength
        heightOfRect = minimumSideLength
        xSide = minimumSideLength
        ySide = minimumSideLength
        mainRectPoints.setTheRect(0)
        secondRectPoints.setTheRect(INITIAL_POINT)


    }
    private fun Array<Point>.setTheRect(initialPoint: Int){
        this[0].x = 0
        this[0].y = initialPoint

        this[1].x = minimumSideLength
        this[1].y = initialPoint

        this[2].x = 0
        this[2].y = initialPoint+minimumSideLength

        this[3].x = minimumSideLength
        this[3].y = initialPoint+minimumSideLength
    }
    fun resetView(){
        setInitialRects()
        this.newX = 0
        this.newY = INITIAL_POINT
        manager.removeMyView(this, WRAP_CONTENT,0, INITIAL_POINT)

    }

    //region: Overrides
    override fun onDraw(canvas: Canvas?) {

       if(croppedImage?.isRecycled == true){
           setImageBitmap(null)
       }

        super.onDraw(canvas)

        if (isInitialized){

           canvas?.apply {

               paint.apply {
                   style = Paint.Style.STROKE
                   color = edgeColor
               }

               if (inMatchParentMode){
                   setImageBitmap(null)
                   drawMyBackground(this)
                   drawMyRect(this, secondRectPoints)
                   setCloseDrawable(canvas)

               } else{
                       croppedImage?.let {
                           setImageBitmap(it)
                       }
                   gradientTop?.draw(canvas)
                   gradientBottom?.draw(canvas)
                   drawMyRect(this, mainRectPoints)
                   setNewCropWindow(this)

               }

           }

        }

    }

    private fun setBackgroundShadowBounds() {

        gradientTop?. apply {
            setBounds(
                mainRectPoints[0].x ,
                mainRectPoints[0].y-100.dp,
                mainRectPoints[1].x,
                heightOfRect/6
            )
        }

        gradientBottom?.apply {
            setBounds(
                mainRectPoints[2].x,
                mainRectPoints[3].y - (heightOfRect/6),
                mainRectPoints[3].x,
                mainRectPoints[3].y+100.dp
            )

        }

    }

    /**
     * This method draws the crop window when the view is not in match parenting mode
     * draws the background and sets the drawable on top of it
     */
    private fun setNewCropWindow(canvas: Canvas) {
        if (showDrawable) {
            setImageBitmap(null)
            paint.apply {
                style = Paint.Style.FILL
                color = outsideColor
            }
            drawMyRect(canvas, mainRectPoints)
            paint.apply {
                style = Paint.Style.STROKE
                color = edgeColor
            }
            drawMyRect(canvas, mainRectPoints)

            setCropDrawables(canvas)

        }
    }
    private fun setCropDrawables(canvas: Canvas) {
        cropLeftUpDrawable?.apply {
            setBounds(
                mainRectPoints[0].x,
                mainRectPoints[0].y,
                (widthOfRect/4).dp,
                (heightOfRect/4).dp
            )
            this.draw(canvas)
        }

        cropRightDownDrawable?.apply {
            setBounds(
                mainRectPoints[3].x - (widthOfRect/4).dp,
                mainRectPoints[3].y - (heightOfRect/4).dp,
                mainRectPoints[3].x,
                mainRectPoints[3].y
            )
            this.draw(canvas)
        }

        cropRightUpDrawable?.apply {
            setBounds(
                mainRectPoints[1].x - (widthOfRect/4).dp,
                mainRectPoints[1].y,
                mainRectPoints[1].x,
                mainRectPoints[1].y + (heightOfRect/4).dp
            )
            this.draw(canvas)
        }

        cropLeftDownDrawable?.apply {
            setBounds(
                mainRectPoints[2].x,
                mainRectPoints[2].y - (heightOfRect/4).dp,
                (widthOfRect/4).dp,
                mainRectPoints[2].y
            )
            this.draw(canvas)
        }

    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = event?.actionMasked

        event?.let {
            if (event.pointerCount > 1){
                isMultiTouch = true
            }
            mActivePointerId = event.getPointerId(0)
        }

        when(action){
            ACTION_DOWN-> {
                croppedImage = null

                mainRect = Rect(mainRectPoints[0].x,mainRectPoints[0].y,mainRectPoints[3].x ,mainRectPoints[3].y )

                moveType = getMoveType(event, mainRect)

                calculateTouchOffset(event.x.toInt(), event.y.toInt(), mainRect)
                when (moveType){
                    Type.CENTER ->  {
                        callBackForWindowManager.onMove(event)
                        moveView = true
                    }
                    else ->    {
                        thisOptionsView?.destroyView()
                        showDrawable = false
                        changeWrapMode(MATCH_PARENT) }
                }

                start.x = secondRectPoints[corner].x
                start.y = secondRectPoints[corner].y

            }
            ACTION_UP -> {
                coordinatesRect =  Rect(secondRectPoints[0].x,secondRectPoints[0].y,secondRectPoints[3].x ,secondRectPoints[3].y )

                mIsToClose = isToClose(event)
                if(mIsToClose && inMatchParentMode ){
                    callBackForWindowManager.onClose()

                } else if(!mIsToClose && !moveView){
                    requestTakeScreenShotCallback.onRequestScreenShot(coordinatesRect)

                }
                changeWrapMode(WRAP_CONTENT, mIsToClose)
                moveView = false
                isMultiTouch = false
                setBackgroundShadowBounds()

            }
            ACTION_MOVE -> {

                mIsToClose = isToClose(event)

                event.actionIndex.also { pointerIndex ->
                    if(pointerIndex == event.findPointerIndex(mActivePointerId)){
                        if (isMultiTouch){
                            onMultiTouchMove(event, corner)

                        } else {
                            if (mIsToClose){
                                DrawableCompat.setTint(
                                    DrawableCompat.wrap(closeDrawable!!),
                                    ContextCompat.getColor(context, R.color.teal_200)
                                )
                            } else{
                                DrawableCompat.setTint(
                                    DrawableCompat.wrap(closeDrawable!!),
                                    ContextCompat.getColor(context, R.color.back_second)
                                )
                            }

                            when(moveType){
                                Type.BOTTOM_RIGHT -> {
                                    resizeXRight(event,corner)
                                    resizeYBottom(event,corner)
                                }
                                Type.TOP_RIGHT ->{
                                    resizeXRight(event,corner)
                                    resizeYTop(event,corner)
                                }
                                Type.TOP_LEFT -> {
                                    resizeXLeft(event, corner)
                                    resizeYTop(event, corner)
                                }
                                Type.BOTTOM_LEFT -> {
                                    resizeXLeft(event, corner)
                                    resizeYBottom(event,corner)
                                }
                                Type.LEFT -> {
                                    resizeXLeft(event,corner)
                                }
                                Type.TOP -> {
                                    resizeYTop(event,corner)
                                }
                                Type.RIGHT -> {
                                    resizeXRight(event,corner)
                                }
                                Type.BOTTOM -> {
                                    resizeYBottom(event, corner)
                                }
                                Type.CENTER -> callBackForWindowManager.onMove(event)
                                null -> {}
                            }
                        }
                    }
                }

                invalidate()
                requestLayout()
            }
            ACTION_POINTER_UP ->{
                isMultiTouch = false
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        widthConstraint = MeasureSpec.getSize(widthMeasureSpec)
        if (newMeasureSpecSizeForWidth == 0) newMeasureSpecSizeForWidth = widthConstraint


        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        heightConstraint = MeasureSpec.getSize(heightMeasureSpec)
        if (newMeasureSpecSizeForHeight == 0) newMeasureSpecSizeForHeight = heightConstraint

        rectangleFullWIdth = widthOfRect + 2 * halfDrawableSize

        val mLayoutWidth = getOnMeasureSpec(
            true,
            widthMode,
            widthConstraint,
            rectangleFullWIdth
        )

        rectangleFullSize = heightOfRect + 2 * halfDrawableSize

        val mLayoutHeight =  getOnMeasureSpec(
            false,
            heightMode,
            heightConstraint,
            rectangleFullSize
        )
        widthConstraint = newMeasureSpecSizeForWidth
        heightConstraint = newMeasureSpecSizeForHeight

        setMeasuredDimension(mLayoutWidth, mLayoutHeight)

    }

    //region: Helper to draw
    private fun drawMyRect(canvas: Canvas, rectToDraw: Array<Point>) {
        Log.i("drawingRect","drawing rect")
           canvas. drawRect(
               rectToDraw[0].x.plus(halfDrawableSize).toFloat(),
               rectToDraw[1].y.plus(halfDrawableSize).toFloat(),
               rectToDraw[3].x.plus(halfDrawableSize).toFloat(),
               rectToDraw[2].y.plus(halfDrawableSize).toFloat(),
                paint)


    }
    private fun drawMyBackground(canvas: Canvas) {
        //set paint to draw outside color, fill
        if (mIsToClose){
            paintForBack.color = ContextCompat.getColor(context, R.color.red_color_picker)
        } else{
            paintForBack.color = outsideColor
        }

        //top rectangle
        canvas.drawRect(0f, 0f, canvas.width.toFloat(),
            (secondRectPoints[0].y + halfDrawableSize).toFloat(), paintForBack)
        //left rectangle
        canvas.drawRect(0f,
            (secondRectPoints[0].y + halfDrawableSize).toFloat(),
            (secondRectPoints[0].x + halfDrawableSize).toFloat(), canvas.height.toFloat(), paintForBack)
        //right rectangle
        canvas.drawRect(
            (secondRectPoints[1].x + halfDrawableSize ).toFloat(),
            (secondRectPoints[0].y + halfDrawableSize).toFloat(),
            canvas.width.toFloat(),
            (secondRectPoints[3].y + halfDrawableSize).toFloat(),
            paintForBack)
        //bottom rectangle
        canvas.drawRect(
            (secondRectPoints[0].x + halfDrawableSize).toFloat(),
            (secondRectPoints[3].y + halfDrawableSize).toFloat(),
            canvas.width.toFloat(),
            canvas.height.toFloat(),
            paintForBack)
    }

    //region: Set callbacks
    fun setWindowManagerCallback(onVIewCropWindowListener: OnMoveCropWindowListener){
        this.callBackForWindowManager = onVIewCropWindowListener
    }
    fun setOnRequestTakeScreenShotListener(onRequestTakeScreenShotListener: OnRequestTakeScreenShotListener){
        this.requestTakeScreenShotCallback = onRequestTakeScreenShotListener
    }

    // region: Calculate offset and resize and get move type
    private fun calculateTouchOffset(touchX: Int, touchY: Int, rect: Rect) {
        var touchOffsetX = 0
        var touchOffsetY = 0

        when (moveType) {
            Type.TOP_LEFT -> {
                touchOffsetX = rect.left - touchX
                touchOffsetY = rect.top - touchY
            }
            Type.TOP_RIGHT -> {
                touchOffsetX = rect.right - touchX
                touchOffsetY = rect.top - touchY
            }
            Type.BOTTOM_LEFT -> {
                touchOffsetX = rect.left - touchX
                touchOffsetY = rect.bottom - touchY
            }
            Type.BOTTOM_RIGHT -> {
                touchOffsetX = rect.right - touchX
                touchOffsetY = rect.bottom - touchY

            }
            Type.LEFT -> {
                touchOffsetX = rect.left - touchX
                touchOffsetY = 0
            }
            Type.TOP -> {
                touchOffsetX = 0
                touchOffsetY = rect.top - touchY
            }
            Type.RIGHT -> {
                touchOffsetX = rect.right - touchX
                touchOffsetY = 0
            }
            Type.BOTTOM -> {
                touchOffsetX = 0
                touchOffsetY = rect.bottom - touchY
            }
            Type.CENTER -> {
                touchOffsetX = rect.centerX() - touchX
                touchOffsetY = rect.centerY() - touchY
            }
            else -> {
            }
        }
        mTouchOffset.x = touchOffsetX
        mTouchOffset.y = touchOffsetY
    }
    private fun resizeYBottom(event: MotionEvent, corner: Int): Int {

        //amount of pixels moved
        val cornerMovedInPixels: Double = floor(event.y.toDouble()- start.y + mTouchOffset.y)

        //If we go down the (heightOfRect + cornerMovedInPixels) will be more than minimumSideLength
        // if we go up the minimumSideLength will remain the same and at some point will be greater than
        //(heightOfRect + cornerMovedInPixels) because the pixels will be subtracted
        val restrictionOnSizeInY = max(
            minimumSideLength,
            (heightOfRect + cornerMovedInPixels).toInt()
        )



        //The space available in y is the distance between the upper more point and the height of the
        // view in the "y" position
        val spaceAvailableInY = heightConstraint - secondRectPoints[corner].y - 2 * halfDrawableSize

        //This is the current height of the rect + the available space in y
        // this gives us the maximum height we can set the rect to
        val maximumAllowedHeight = heightOfRect + spaceAvailableInY

        //we get the minimum of the 2 restrictions so at some point we´ll have 2 cases compared
        // 1: if we go down the restrictionOnSizeInY variable will be greater than
        // maximumAllowedHeight because the maximum allowed height will decrease as the rect grows
        //2: if we go up the maximumAllowedHeight will increase and the restrictionOnSizeInY will
        // be the minimum and at some point will reach the minimum length
        heightOfRect = min(restrictionOnSizeInY, maximumAllowedHeight)

        ySide = heightOfRect

            mainRectPoints[2].y = mainRectPoints[0].y + heightOfRect
            mainRectPoints[3].y = mainRectPoints[0].y + heightOfRect

            secondRectPoints[2].y = secondRectPoints[0].y + ySide
            secondRectPoints[3].y = secondRectPoints[0].y + ySide

            start.y = secondRectPoints[corner].y

        return start.y
    }
    private fun resizeYTop(event: MotionEvent, corner: Int): Int {

        //amount of pixels moved
        val cornerMovedInPixels = floor((event.y - start.y + mTouchOffset.y).toDouble())

        //If we go up the (heightOfRect - cornerMovedInPixels) will be more than minimumSideLength
        // if we go down the minimumSideLength will decrease and at some point will be greater than
        //(heightOfRect - cornerMovedInPixels)
        val restrictionOnSizeInY = max(
            minimumSideLength,
            (heightOfRect - cornerMovedInPixels).toInt()
        )

        //The space available in y is the distance between the upper more point and the point 0 y the
        //y position
        val spaceAvailableInY: Int = secondRectPoints[corner].y

        //This is the current height of the rect + the available space in y
        // this gives us the maximum height we can set the rect to
        val maximumAllowedHeight = heightOfRect + spaceAvailableInY

        //we get the minimum of the 2 restrictions so at some point we´ll have 2 cases compared
        // 1: if we go up the restrictionOnSizeInY variable will be greater than maximumAllowedHeight
        // because the latter will decrease
        //2: if we go down the maximumAllowedHeight will increase and the restrictionOnSizeInY will
        // be the minimum and at some point will reach the minimum length
        heightOfRect = min(restrictionOnSizeInY, maximumAllowedHeight)

        ySide = heightOfRect

        mainRectPoints[2].y = mainRectPoints[0].y + heightOfRect
        mainRectPoints[3].y = mainRectPoints[0].y + heightOfRect

        secondRectPoints[1].y = secondRectPoints[3].y - ySide
        secondRectPoints[0].y = secondRectPoints[2].y - ySide

        start.y = secondRectPoints[corner].y

        this.newY = secondRectPoints[corner].y

        return start.y
    }
    private fun resizeXRight(event: MotionEvent, corner: Int): Int {

        val pixelsThisCornerWasMovedInX: Double = floor(event.x.toDouble() - start.x + mTouchOffset.x)


        //This is used to know if the movement is greater than the minimum size of the rect
        val restrictionOnSizeInX = max(
            minimumSideLength,
            (widthOfRect + pixelsThisCornerWasMovedInX).toInt()
        )

        val spaceAvailableInX = widthConstraint - secondRectPoints[corner].x - 2 * halfDrawableSize

        //This is the current width of the rect + the available space in x
        val maximumAllowedWidth = widthOfRect + spaceAvailableInX

        //This is the new width of the rect based on the  horizontal restricitons
        // such as the minim width and the width of the screen
        widthOfRect = min(
            restrictionOnSizeInX,
            maximumAllowedWidth
        )

        xSide = widthOfRect

        mainRectPoints[1].x = mainRectPoints[0].x + widthOfRect
        mainRectPoints[3].x = mainRectPoints[0].x + widthOfRect

        secondRectPoints[1].x = secondRectPoints[0].x + xSide
        secondRectPoints[3].x = secondRectPoints[0].x + xSide

        start.x = secondRectPoints[corner].x

        return start.x
    }
    private fun resizeXLeft(event: MotionEvent, corner: Int): Int {
        //amount of pixels moved
        val cornerMovedInPixels = floor((event.x - start.x + mTouchOffset.x).toDouble())

        //If we go up the (heightOfRect - cornerMovedInPixels) will be more than minimumSideLength
        // if we go down the minimumSideLength will decrease and at some point will be greater than
        //(heightOfRect - cornerMovedInPixels)
        val restrictionOnSizeInX = max(
            minimumSideLength,
            (widthOfRect - cornerMovedInPixels).toInt()
        )

        //The space available in y is the distance between the upper more point and the point 0 y the
        //y position
        val spaceAvailableInX: Int = secondRectPoints[corner].x

        //This is the current height of the rect + the available space in y
        // this gives us the maximum height we can set the rect to
        val maximumAllowedHeight = widthOfRect + spaceAvailableInX

        //we get the minimum of the 2 restrictions so at some point we´ll have 2 cases compared
        // 1: if we go up the restrictionOnSizeInY variable will be greater than maximumAllowedHeight
        // because the latter will decrease
        //2: if we go down the maximumAllowedHeight will increase and the restrictionOnSizeInY will
        // be the minimum and at some point will reach the minimum length
        widthOfRect = min(restrictionOnSizeInX, maximumAllowedHeight)

        xSide = widthOfRect


        mainRectPoints[1].x = mainRectPoints[0].x + widthOfRect
        mainRectPoints[3].x = mainRectPoints[0].x + widthOfRect

        secondRectPoints[2].x = secondRectPoints[1].x - xSide
        secondRectPoints[0].x = secondRectPoints[1].x - xSide

        this.newX = secondRectPoints[corner].x

        start.x = secondRectPoints[corner].x
        return start.x
    }
    private fun getMoveType(event: MotionEvent, rect: Rect): Type {

        val (xPressed: Int, yPressed: Int) = event.x.toInt() to event.y.toInt()

        return  if (xPressed - touchRadius> rect.left  && xPressed + touchRadius < rect.right  && yPressed - touchRadius> rect.top  && yPressed + touchRadius< rect.bottom) {
            Type.CENTER
        } else if (abs(xPressed - rect.right) <= touchRadius && abs(yPressed - rect.bottom) <= touchRadius) {
            corner = Type.getCorner(Type.BOTTOM_RIGHT)
            Type.BOTTOM_RIGHT
        }
        else if (abs(xPressed - rect.left) <= touchRadius && abs(yPressed - rect.bottom) <= touchRadius) {
            corner = Type.getCorner(Type.BOTTOM_LEFT)
            Type.BOTTOM_LEFT
        }
        else if (abs(xPressed - rect.right) <= touchRadius && abs(yPressed - rect.top) <= touchRadius) {
            corner = Type.getCorner(Type.TOP_RIGHT)
            Type.TOP_RIGHT
        }
        else if (abs(xPressed - rect.left) <= touchRadius && abs(yPressed - rect.top) <= touchRadius) {
            corner = Type.getCorner(Type.TOP_LEFT)
            Type.TOP_LEFT
        }
        else if (xPressed > rect.left && xPressed < rect.right && abs(yPressed - rect.top) <= touchRadius) {
            corner = Type.getCorner(Type.TOP)
            Type.TOP
        }

        else if (xPressed > rect.left && xPressed < rect.right && abs(yPressed - rect.bottom) <= touchRadius) {
            corner = Type.getCorner(Type.BOTTOM)
            Type.BOTTOM
        }

        else if (abs(xPressed - rect.left) <= touchRadius && yPressed > rect.top && yPressed < rect.bottom){
            corner = Type.getCorner(Type.LEFT)
            Type.LEFT
        }
        else if (abs(xPressed - rect.right) <= touchRadius && yPressed > rect.top && yPressed < rect.bottom){
            corner = Type.getCorner(Type.RIGHT)
            Type.RIGHT
        } else {
            Type.CENTER
        }



    }
    private fun onMultiTouchMove(event: MotionEvent, corner: Int){

        secondRectPoints[0].x = max(secondRectPoints[0].x + min(floor(event.x - start.x + mTouchOffset.x), floor(width - secondRectPoints[0].x - 2 * halfDrawableSize - xSide.toFloat())).toInt(), 0)
        secondRectPoints[1].x = max(secondRectPoints[1].x + min(floor(event.x - start.x + mTouchOffset.x), floor(width - secondRectPoints[1].x - 2 * halfDrawableSize.toFloat())).toInt(), xSide)
        secondRectPoints[2].x = max(secondRectPoints[2].x + min(floor(event.x - start.x + mTouchOffset.x), floor(width - secondRectPoints[2].x - 2 * halfDrawableSize - xSide.toFloat())).toInt(), 0)
        secondRectPoints[3].x = max(secondRectPoints[3].x + min(floor(event.x - start.x + mTouchOffset.x), floor(width - secondRectPoints[3].x - 2 * halfDrawableSize.toFloat())).toInt(), xSide)

        secondRectPoints[0].y = max(secondRectPoints[0].y + min(floor(event.y - start.y + mTouchOffset.y), floor(height - secondRectPoints[0].y - 2 * halfDrawableSize - ySide.toFloat())).toInt(), 0)
        secondRectPoints[1].y = max(secondRectPoints[1].y + min(floor(event.y - start.y + mTouchOffset.y), floor(height - secondRectPoints[1].y - 2 * halfDrawableSize - ySide.toFloat())).toInt(), 0)
        secondRectPoints[2].y = max(secondRectPoints[2].y + min(floor(event.y - start.y + mTouchOffset.y), floor(height - secondRectPoints[2].y - 2 * halfDrawableSize.toFloat())).toInt(), ySide)
        secondRectPoints[3].y = max(secondRectPoints[3].y + min(floor(event.y - start.y + mTouchOffset.y), floor(height - secondRectPoints[3].y - 2 * halfDrawableSize.toFloat())).toInt(), ySide)


        this.newX = secondRectPoints[0].x
        this.newY = secondRectPoints[0].y

        start.x = secondRectPoints[corner].x
        start.y = secondRectPoints[corner].y

    }

    //region: Position the rectangle
    fun setNewPositionOfSecondRect(newX: Int, newY: Int) {

        this.newX =  max(0, newX)
        this.newY = max(0,newY)

        val zeroPosX = min(this.newX,widthConstraint - rectangleFullWIdth )
        val zeroPosY = min(this.newY, heightConstraint - rectangleFullSize)

        secondRectPoints[0].x = zeroPosX
        secondRectPoints[0].y = zeroPosY


        secondRectPoints[1].x = (zeroPosX + xSide)
        secondRectPoints[1].y = zeroPosY

        secondRectPoints[2].x = zeroPosX
        secondRectPoints[2].y = (zeroPosY  + ySide)

        secondRectPoints[3].x = (zeroPosX + xSide)
        secondRectPoints[3].y = (zeroPosY  + ySide)

    }
    private fun changeWrapMode(mode: Int, isToClose: Boolean = false){
        val lp = WindowManager.LayoutParams(
            mode,
            mode,
            layoutFlag,
            flags,
            PixelFormat.TRANSLUCENT
        )

            lp.x = this.newX
            lp.y = this.newY

        lp.gravity = Gravity.TOP or Gravity.START


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }


        if (mode== MATCH_PARENT && !moveView){

            manager.updateViewLayout(this, lp)
            inMatchParentMode = true
            invalidate()
            requestLayout()

        } else if(mode == WRAP_CONTENT && !moveView && !isToClose) {
            inMatchParentMode = false
            manager.removeMyView(this,
                mode,
                this.newX,
                this.newY)


        } else if (mode == WRAP_CONTENT && moveView && !isToClose){
            inMatchParentMode = false
            manager.updateViewLayout(this, lp)

        }


    }

    //region: close feature
    private fun isToClose(event: MotionEvent): Boolean {

        return event.x < closeDrawableRight && event.x > closeDrawableLeft
                && event.y < closeDrawableBottom && event.y > closeDrawableTop
    }
    private fun setCloseDrawable(canvas: Canvas) {

        closeDrawableLeft = secondRectPoints[1].x - (secondRectPoints[1].x - secondRectPoints[0].x)/2 - halfCloseDrawableSize
        closeDrawableTop = secondRectPoints[3].y - (secondRectPoints[3].y - secondRectPoints[1].y)/2 - halfCloseDrawableSize
        closeDrawableBottom = closeDrawableTop + 2 * halfCloseDrawableSize
        closeDrawableRight = closeDrawableLeft + 2 * halfCloseDrawableSize

        closeDrawable?.setBounds(
            closeDrawableLeft ,
            closeDrawableTop ,
            closeDrawableRight,
            closeDrawableBottom
        )

        closeDrawable?.draw(canvas)
    }

    private fun getOnMeasureSpec( isMeasuringWidth: Boolean, measureSpecMode: Int, measureSpecSize: Int, desiredSize: Int): Int {

        // Measure Width
        return when (measureSpecMode) {

             MeasureSpec.EXACTLY -> {
                 // Must be this size
                 if(isMeasuringWidth) {
                     newMeasureSpecSizeForWidth = measureSpecSize
                 } else {
                     newMeasureSpecSizeForHeight = measureSpecSize
                 }
                 measureSpecSize
             }
             MeasureSpec.AT_MOST -> {
                 // Can't be bigger than...; match_parent value
                if (isMeasuringWidth){
                    min(desiredSize, newMeasureSpecSizeForWidth)
                } else {
                    min(desiredSize, newMeasureSpecSizeForHeight)
                }


             }
             else -> {
                 // Be whatever you want; wrap_content
                 desiredSize

             }
         }
    }

}



