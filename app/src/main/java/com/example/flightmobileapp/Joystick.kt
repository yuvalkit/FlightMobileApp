package com.example.flightmobileapp

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Thread.sleep


class Joystick @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) :
    View(context, attrs), Runnable {

    // DRAWING
    private val mPaintCircleButton: Paint
    private val mPaintCircleBorder: Paint
    private val mPaintBackground: Paint
    private var mPaintBitmapButton: Paint? = null
    private var mButtonBitmap: Bitmap? = null
    private var reset = false
    private var down = false
    private var show = false

    /**
     * Ratio use to define the size of the button
     */
    private var mButtonSizeRatio: Float = 0f

    /**
     * Ratio use to define the size of the background
     */
    private var mBackgroundSizeRatio: Float = 0f

    // COORDINATE
    private var mPosX: Float = 0f
    private var mPosY: Float = 0f
    private var mCenterX: Float = 0f
    private var mCenterY: Float = 0f
    private var mFixedCenterX: Float = 0f
    private var mFixedCenterY: Float = 0f

    private var mFixedCenter: Boolean = false

    var isAutoReCenterButton: Boolean = false

    var isButtonStickToBorder: Boolean = false

    private var mEnabled: Boolean = false

    // SIZE
    private var mButtonRadius: Float = 0f
    private var mBorderRadius: Float = 0f

    private var mBorderAlpha: Float = 0f

    private var mBackgroundRadius: Float = 0f

    /**
     * Listener used to dispatch OnMove event
     */
    private var mCallback: OnMoveListener? = null
    private var mLoopInterval: Long = DEFAULT_LOOP_INTERVAL.toLong()
    private var mThread: Thread? = Thread(this)

    /**
     * Listener used to dispatch MultipleLongPress event
     */
    private var mOnMultipleLongPressListener: OnMultipleLongPressListener? = null
    private val mHandlerMultipleLongPress: Handler = Handler()
    private val mRunnableMultipleLongPress: Runnable
    private var mMoveTolerance: Float = 0f

    var buttonDirection: Double = 0.0

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Double
    ) : this(context, attrs) {
    }

    private fun initPosition() {
        // get the center of view to position circle
        mPosX = width.toFloat() / 2
        mCenterX = mPosX
        mFixedCenterX = mCenterX
        mPosY = width.toFloat() / 2
        mCenterY = mPosY
        mFixedCenterY = mCenterY
    }

    /**
     * Draw the background, the border and the button
     * @param canvas the canvas on which the shapes will be drawn
     */
    override fun onDraw(canvas: Canvas) {
        // Draw the background
        canvas.drawCircle(
            mFixedCenterX,
            mFixedCenterY,
            mBackgroundRadius,
            mPaintBackground
        )

        // Draw the circle border
        canvas.drawCircle(
            mFixedCenterX,
            mFixedCenterY,
            mBorderRadius,
            mPaintCircleBorder
        )

        // Draw the button from image
        if (mButtonBitmap != null) {
            canvas.drawBitmap(
                mButtonBitmap!!,
                (mPosX + mFixedCenterX) - mCenterX - mButtonRadius,
                (mPosY + mFixedCenterY) - mCenterY - mButtonRadius,
                mPaintBitmapButton
            )
        } else {
            canvas.drawCircle(
                mPosX + mFixedCenterX - mCenterX,
                mPosY + mFixedCenterY - mCenterY,
                mButtonRadius,
                mPaintCircleButton
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        initPosition()

        // radius based on smallest size : height OR width
        val d: Int = Math.min(w, h)
        mButtonRadius = (d / 2 * mButtonSizeRatio)
        mBorderRadius = (d / 2 * mBackgroundSizeRatio)
        mBackgroundRadius = mBorderRadius - (mPaintCircleBorder.strokeWidth / 2)
        if (mButtonBitmap != null) mButtonBitmap =
            Bitmap.createScaledBitmap(mButtonBitmap!!, (mButtonRadius * 2).toInt(), (mButtonRadius * 2).toInt(), true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // setting the measured values to resize the view to a certain width and height
        val d: Int = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec))
        setMeasuredDimension(d, d)
    }

    private fun measure(measureSpec: Int): Int {
        if (MeasureSpec.getMode(measureSpec) == MeasureSpec.UNSPECIFIED) {
            // if no bounds are specified return a default size (200)
            return DEFAULT_SIZE
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            return MeasureSpec.getSize(measureSpec)
        }
    }

    private fun getLength(x : Float, y : Float) : Float {
        return Math.sqrt((x * x).toDouble()+ (y * y).toDouble()).toFloat()
    }

    private fun getValues(x : Float, y : Float) {
        var length = getLength(x - mCenterX, y - mCenterY)
        var ratio = mBorderRadius / length
        if (length > mBorderRadius) {
            mPosY = mCenterY + ratio * (y - mCenterY)
            mPosX = mCenterX + ratio * (x - mCenterX)
        } else {
            mPosY =
                if (buttonDirection < 0) mCenterY else y // direction negative is horizontal axe
            mPosX =
                if (buttonDirection > 0) mCenterX else x // direction positive is vertical axe
        }
    }

    private fun inSmallRadius() : Boolean {
        if(mPosX > (mCenterX + mButtonRadius) || mPosX < (mCenterX - mButtonRadius)) {
            return false
        }
        if(mPosY > (mCenterY + mButtonRadius) || mPosY < (mCenterY - mButtonRadius)) {
            return false
        }
        return true
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        // if disabled we don't move the
        if (!mEnabled) {
            return true
        }

        if(!reset) {
            getValues(event.x, event.y)
        }

        if (event.action == MotionEvent.ACTION_UP) {

            // stop listener because the finger left the touch screen
            mThread!!.interrupt()

            // re-center the button or not (depending on settings)
            if (isAutoReCenterButton) {
                var times = 10
                var eachSleep = 20L
                if(down) {
                    down = false
                    show = false
                    reset = true
                    GlobalScope.launch {
                        resetButtonPosition(times, eachSleep)
                        // update now the last strength and angle which should be zero after resetButton
                        if (mCallback != null) mCallback!!.onMove(angle, strength)
                        reset = false
                    }
                }
            }
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            if(!reset) {
                if (mThread != null && mThread!!.isAlive) {
                    mThread!!.interrupt()
                }
                if(inSmallRadius()) {
                    down = true
                    show = true
                    mThread = Thread(this)
                    mThread!!.start()
                    if (mCallback != null) mCallback!!.onMove(angle, strength)
                } else {
                    down = false
                    show = false
                }
            }
        }
        when (event.actionMasked) {

            MotionEvent.ACTION_POINTER_DOWN -> {
                // when the second finger touch
                if (event.pointerCount == 2) {
                    mHandlerMultipleLongPress.postDelayed(
                        mRunnableMultipleLongPress,
                        ViewConfiguration.getLongPressTimeout() * 2.toLong()
                    )
                    mMoveTolerance = MOVE_TOLERANCE.toFloat()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                mMoveTolerance--
                if (mMoveTolerance == 0f) {
                    mHandlerMultipleLongPress.removeCallbacks(mRunnableMultipleLongPress)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {

                // when the last multiple touch is released
                if (event.pointerCount == 2) {
                    mHandlerMultipleLongPress.removeCallbacks(mRunnableMultipleLongPress)
                }
            }
        }
        val abs: Double = Math.sqrt(
            ((mPosX - mCenterX) * (mPosX - mCenterX)
                    + (mPosY - mCenterY) * (mPosY - mCenterY)).toDouble()
        )

        if(!reset) {
            // (abs > mBorderRadius) means button is too far therefore we limit to border
            // (buttonStickBorder && abs != 0) means wherever is the button we stick it to the border except when abs == 0
            if (abs > mBorderRadius || (isButtonStickToBorder && abs != 0.0)) {
                mPosX = ((mPosX - mCenterX) * mBorderRadius / abs + mCenterX).toFloat()
                mPosY = ((mPosY - mCenterY) * mBorderRadius / abs + mCenterY).toFloat()
            }
        }
        if (!isAutoReCenterButton && show) {
            // Now update the last strength and angle if not reset to center
            if (mCallback != null) mCallback!!.onMove(angle, strength)
        }

        if(!reset && show) {
            invalidate()
        }
        return true
    }

    private val angle: Int
        private get() {
            val angle: Int = Math.toDegrees(
                Math.atan2(
                    mCenterY - mPosY.toDouble(),
                    mPosX - mCenterX.toDouble()
                )
            ).toInt()
            return if (angle < 0) angle + 360 else angle // make it as a regular counter-clock protractor
        }

    private val strength: Int
        private get() = (100 * Math.sqrt(
            (mPosX - mCenterX)
                    * (mPosX - mCenterX) + (mPosY - mCenterY)
                    * (mPosY - mCenterY).toDouble()
        ) / mBorderRadius).toInt()

    private fun resetButtonPosition(times : Int, eachSleep : Long) {
        var extraX = (mCenterX - mPosX) / times
        var extraY = (mCenterY - mPosY) / times
        var x = mPosX
        var y = mPosY
        var i = 0
        while(i < times) {
            x += extraX
            y += extraY
            i++
            mPosX = x
            mPosY = y
            invalidate()
            sleep(eachSleep)
        }
        mPosX = mCenterX
        mPosY = mCenterY
    }

    override fun isEnabled(): Boolean {
        return mEnabled
    }

    var buttonSizeRatio: Float
        get() {
            return mButtonSizeRatio
        }
        set(newRatio) {
            if (newRatio > 0.0f && newRatio <= 1.0f) {
                mButtonSizeRatio = newRatio
            }
        }


    fun getmBackgroundSizeRatio(): Float {
        return mBackgroundSizeRatio
    }

    val normalizedX: Int
        get() {
            if (width == 0) {
                return 50
            }
            return Math.round((mPosX - mButtonRadius) * 100.0f / (width - mButtonRadius * 2))
        }

    val normalizedY: Int
        get() {
            if (height == 0) {
                return 50
            }
            return Math.round((mPosY - mButtonRadius) * 100.0f / (height - mButtonRadius * 2))
        }

    var borderAlpha: Int
        get() {
            return mBorderAlpha.toInt()
        }
        set(alpha) {
            mBorderAlpha = alpha.toFloat()
            mPaintCircleBorder.alpha = alpha
            invalidate()
        }

    fun setButtonDrawable(d: Drawable?) {
        if (d != null) {
            if (d is BitmapDrawable) {
                mButtonBitmap = d.bitmap
                if (mButtonRadius != 0f) {
                    mButtonBitmap = Bitmap.createScaledBitmap(
                        d.bitmap,
                        (mButtonRadius * 2).toInt(),
                        (mButtonRadius * 2).toInt(),
                        true
                    )
                }
                if (mPaintBitmapButton != null) mPaintBitmapButton = Paint()
            }
        }
    }

    fun setButtonColor(color: Int) {
        mPaintCircleButton.color = color
        invalidate()
    }

    fun setBorderColor(color: Int) {
        mPaintCircleBorder.color = color
        if (color != Color.TRANSPARENT) {
            mPaintCircleBorder.alpha = mBorderAlpha.toInt()
        }
        invalidate()
    }

    override fun setBackgroundColor(color: Int) {
        mPaintBackground.color = color
        invalidate()
    }

    fun setBorderWidth(width: Int) {
        mPaintCircleBorder.strokeWidth = width.toFloat()
        mBackgroundRadius = mBorderRadius - (width / 2.0f)
        invalidate()
    }

    fun setOnMoveListener(l: OnMoveListener?) {
        setOnMoveListener(l, DEFAULT_LOOP_INTERVAL)
    }

    fun setOnMoveListener(l: OnMoveListener?, loopInterval: Int) {
        mCallback = l
        mLoopInterval = loopInterval.toLong()
    }

    fun setOnMultiLongPressListener(l: OnMultipleLongPressListener?) {
        mOnMultipleLongPressListener = l
    }

    fun setFixedCenter(fixedCenter: Boolean) {
        // if we set to "fixed" we make sure to re-init position related to the width of the joystick
        if (fixedCenter) {
            initPosition()
        }
        mFixedCenter = fixedCenter
        invalidate()
    }

    override fun setEnabled(enabled: Boolean) {
        mEnabled = enabled
    }

    fun setBackgroundSizeRatio(newRatio: Float) {
        if (newRatio > 0.0f && newRatio <= 1.0f) {
            mBackgroundSizeRatio = newRatio
        }
    }

    override fun run() {
        while (!Thread.interrupted()) {
            post(object : Runnable {
                override fun run() {
                    if (mCallback != null) mCallback!!.onMove(angle, strength)
                }
            })
            try {
                Thread.sleep(mLoopInterval)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    companion object {

        private val DEFAULT_LOOP_INTERVAL: Int = 50 // in milliseconds

        private val MOVE_TOLERANCE: Int = 10

        private val DEFAULT_COLOR_BUTTON: Int = Color.BLACK

        private val DEFAULT_COLOR_BORDER: Int = Color.TRANSPARENT

        private val DEFAULT_ALPHA_BORDER: Int = 255

        private val DEFAULT_BACKGROUND_COLOR: Int = Color.TRANSPARENT

        private val DEFAULT_SIZE: Int = 200

        private val DEFAULT_WIDTH_BORDER: Int = 3

        private val DEFAULT_FIXED_CENTER: Boolean = true

        private val DEFAULT_AUTO_RECENTER_BUTTON: Boolean = true

        private val DEFAULT_BUTTON_STICK_TO_BORDER: Boolean = false

        var BUTTON_DIRECTION_BOTH: Int = 0
    }


    init {
        val styledAttributes: TypedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.JoystickView,
            0, 0
        )
        val buttonColor: Int
        val borderColor: Int
        val backgroundColor: Int
        val borderWidth: Int
        val buttonDrawable: Drawable?
        try {
            buttonColor = styledAttributes.getColor(
                R.styleable.JoystickView_JV_buttonColor,
                DEFAULT_COLOR_BUTTON
            )
            borderColor = styledAttributes.getColor(
                R.styleable.JoystickView_JV_borderColor,
                DEFAULT_COLOR_BORDER
            )
            mBorderAlpha = styledAttributes.getFloat(
                R.styleable.JoystickView_JV_borderAlpha,
                DEFAULT_ALPHA_BORDER.toFloat()
            )
            backgroundColor = styledAttributes.getColor(
                R.styleable.JoystickView_JV_backgroundColor,
                DEFAULT_BACKGROUND_COLOR
            )
            borderWidth = styledAttributes.getDimensionPixelSize(
                R.styleable.JoystickView_JV_borderWidth,
                DEFAULT_WIDTH_BORDER
            )
            mFixedCenter = styledAttributes.getBoolean(
                R.styleable.JoystickView_JV_fixedCenter,
                DEFAULT_FIXED_CENTER
            )
            isAutoReCenterButton = styledAttributes.getBoolean(
                R.styleable.JoystickView_JV_autoReCenterButton,
                DEFAULT_AUTO_RECENTER_BUTTON
            )
            isButtonStickToBorder = styledAttributes.getBoolean(
                R.styleable.JoystickView_JV_buttonStickToBorder,
                DEFAULT_BUTTON_STICK_TO_BORDER
            )
            buttonDrawable = styledAttributes.getDrawable(R.styleable.JoystickView_JV_buttonImage)
            mEnabled = styledAttributes.getBoolean(R.styleable.JoystickView_JV_enabled, true)
            mButtonSizeRatio = styledAttributes.getFraction(
                R.styleable.JoystickView_JV_buttonSizeRatio,
                1,
                1,
                0.25f
            )
            mBackgroundSizeRatio = styledAttributes.getFraction(
                R.styleable.JoystickView_JV_backgroundSizeRatio,
                1,
                1,
                0.75f
            )
            buttonDirection = styledAttributes.getFloat(
                R.styleable.JoystickView_JV_buttonDirection,
                BUTTON_DIRECTION_BOTH.toFloat()
            ).toDouble()
        } finally {
            styledAttributes.recycle()
        }

        // Initialize the drawing according to attributes
        mPaintCircleButton = Paint()
        mPaintCircleButton.isAntiAlias = true
        mPaintCircleButton.color = buttonColor
        mPaintCircleButton.style = Paint.Style.FILL
        if (buttonDrawable != null) {
            if (buttonDrawable is BitmapDrawable) {
                mButtonBitmap = buttonDrawable.bitmap
                mPaintBitmapButton = Paint()
            }
        }
        mPaintCircleBorder = Paint()
        mPaintCircleBorder.isAntiAlias = true
        mPaintCircleBorder.color = borderColor
        mPaintCircleBorder.style = Paint.Style.STROKE
        mPaintCircleBorder.strokeWidth = borderWidth.toFloat()
        if (borderColor != Color.TRANSPARENT) {
            mPaintCircleBorder.alpha = mBorderAlpha.toInt()
        }
        mPaintBackground = Paint()
        mPaintBackground.isAntiAlias = true
        mPaintBackground.color = backgroundColor
        mPaintBackground.style = Paint.Style.FILL


        // Init Runnable for MultiLongPress
        mRunnableMultipleLongPress = object : Runnable {
            override fun run() {
                if (mOnMultipleLongPressListener != null) mOnMultipleLongPressListener!!.onMultipleLongPress()
            }
        }
    }
}