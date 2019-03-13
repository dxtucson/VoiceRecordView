package com.example.voicerecordview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.MediaRecorder
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VoiceRecordView : View {

    private var mediaRecorder: MediaRecorder? = null

    private lateinit var dataList: LinkedList<Float>
    private lateinit var backgroundPaint: Paint
    private lateinit var textPaint: Paint
    private lateinit var circlePaint: Paint
    private lateinit var barsPaint: Paint
    private lateinit var linearGradient: LinearGradient
    private lateinit var rectF: RectF
    private lateinit var barsRectF: RectF
    private lateinit var textRect: Rect

    private var textBottom = 0f
    private var textLeft = 0f
    private var padding = 0f
    private var circleR = 0f
    private var widthOfBars = 0f

    private val minHeightOfBar = 0.05f
    private val numberOfBars = 57  // Instagram uses 57
    private var timeElapsed = 0L // to set time label
    private var barSizeScale = 1f // to animate bar drawing
    private val barInterval = 100L
    private val updateInterval = 25L
    private var pixelOffset = 0f

    private var cancelWhenRelease = false

    private var textStr: String = "00:00"
    private var drawableIconWhite: Drawable? = null
    private var drawableIconGray: Drawable? = null

    constructor(ctx: Context) : super(ctx) {
        init()
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        init()
    }

    fun startRecord() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(context.cacheDir.path + File.separator + "audio.mp3")
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("startRecord", e.message)
            }
        }
        updateAnimation()
    }

    fun stopRecord() {
        //handler.
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun updateAnimation() {
        if (mediaRecorder == null) {
            timeElapsed = 0
            textStr = "00:00"
            dataList.clear()
            cancelWhenRelease = false
        } else {
            if (timeElapsed % 1000 == 0L) {
                textStr = (SimpleDateFormat("mm:ss", Locale.getDefault())).format(Date(timeElapsed));
            }
            if (timeElapsed % barInterval == 0L) {
                dataList.add(convertVolume())
                barSizeScale = updateInterval.toFloat() / barInterval.toFloat()
                if (dataList.size > numberOfBars) {
                    pixelOffset = 0f
                    dataList.pop()
                }
            }
            handler.postDelayed({
                timeElapsed += updateInterval
                barSizeScale += updateInterval.toFloat() / barInterval.toFloat()
                if (dataList.size == numberOfBars) {
                    pixelOffset += widthOfBars * updateInterval / barInterval
                }
                updateAnimation()
            }, updateInterval)
        }
        invalidate()
    }

    private fun init() {
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rectF = RectF()
        textRect = Rect()
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint.color = Color.WHITE
        barsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        barsPaint.color = Color.WHITE

        drawableIconWhite = ContextCompat.getDrawable(context, R.drawable.delete_white)
        drawableIconGray = ContextCompat.getDrawable(context, R.drawable.delete_gray)

        barsRectF = RectF()
        dataList = LinkedList()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        rectF.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())

        padding = 0.1f * measuredHeight

        textPaint.textSize = (measuredHeight.toFloat() - padding) / 3

        textPaint.getTextBounds(textStr, 0, textStr.length, textRect)
        val textHeight = textRect.height().toFloat()

        textBottom = (measuredHeight + textHeight) / 2

        textLeft = measuredWidth - padding - textPaint.measureText(textStr)

        circleR = (measuredHeight - padding - padding) / 2

        drawableIconWhite?.setBounds(
            (padding).toInt(),
            (padding).toInt(),
            (2 * circleR + padding).toInt(),
            (measuredHeight - padding).toInt()
        )

        drawableIconGray?.setBounds(
            (2 * padding).toInt(),
            (2 * padding).toInt(),
            (2 * circleR).toInt(),
            (measuredHeight - 2 * padding).toInt()
        )
        widthOfBars = (measuredWidth - 4 * padding - textPaint.measureText(textStr) - 2 * circleR) / numberOfBars / 2
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        linearGradient = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            0f,
            Color.parseColor("#4f99e9"),
            Color.parseColor("#59c1f0"),
            Shader.TileMode.CLAMP
        )
        rectF.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        backgroundPaint.shader = linearGradient
        canvas?.drawRoundRect(rectF, measuredHeight / 2f, measuredHeight / 2f, backgroundPaint)
        canvas?.drawText(textStr, textLeft, textBottom, textPaint)
        drawAllBars(canvas)
        if (cancelWhenRelease) {
            circlePaint.color = Color.parseColor("#ed4956")
            canvas?.drawCircle(padding + circleR, measuredHeight / 2f, circleR + padding, circlePaint)
            canvas?.save()
            canvas?.rotate(-25f, padding + circleR, measuredHeight / 2f)
            drawableIconWhite?.draw(canvas!!)
            canvas?.restore()
        } else {
            circlePaint.color = Color.WHITE
            canvas?.drawCircle(padding + circleR, measuredHeight / 2f, circleR, circlePaint)
            drawableIconGray?.draw(canvas!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (mediaRecorder == null) {
            cancelWhenRelease = false
            return true
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val temp = inCircle(event)
                if (temp && !cancelWhenRelease) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                }
                cancelWhenRelease = temp
            }

            MotionEvent.ACTION_MOVE -> {
                val temp = inCircle(event)
                if (temp && !cancelWhenRelease) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                }
                cancelWhenRelease = temp
            }

            MotionEvent.ACTION_UP -> {
                if (cancelWhenRelease) stopRecord()
            }
        }
        return true
    }

    private fun inCircle(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        return (x - padding - circleR) * (x - padding - circleR) + (y - measuredHeight / 2f) * (y - measuredHeight / 2f) <= circleR * circleR
    }

    private fun drawAllBars(canvas: Canvas?) {
        for (i in 0 until dataList.size) {
            val barLeft = Math.max(
                padding + 2 * circleR + padding + 2 * widthOfBars * i - pixelOffset,
                padding + 2 * circleR + padding
            )
            val barRight = Math.max(
                padding + 2 * circleR + padding + 2 * widthOfBars * i + widthOfBars - pixelOffset,
                padding + 2 * circleR + padding
            )

            val dataHeight =
                Math.max(if (i == dataList.size - 1) dataList[i] * barSizeScale else dataList[i], minHeightOfBar)

            val distToEdge = (measuredHeight - 2 * padding) * (1 - dataHeight) * 0.5f
            val barTop = padding + distToEdge
            val barBottom = measuredHeight - distToEdge - padding
            barsRectF.set(barLeft, barTop, barRight, barBottom)
            canvas?.drawRoundRect(barsRectF, widthOfBars / 2, widthOfBars / 2, barsPaint)
        }
    }

    private fun convertVolume(): Float {
        val amp: Double = mediaRecorder?.maxAmplitude?.toDouble() ?: 0.0
        val amplitudePercentage = amp / 32767
        val powerDb = Math.log10(amplitudePercentage) * 20.0
        //Log.d("amp", amplitudePercentage.toString())
        //Log.d("convertVolume", powerDb.toString())
        return powerDb.toFloat() * 0.01234f + 1.03402f
    }
}