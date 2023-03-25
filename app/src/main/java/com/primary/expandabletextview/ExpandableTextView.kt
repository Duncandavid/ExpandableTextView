package com.primary.expandabletextview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.*
import android.text.method.LinkMovementMethod
import androidx.appcompat.widget.AppCompatTextView
import kotlin.jvm.Volatile
import android.text.style.StyleSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import android.text.style.ClickableSpan
import android.text.style.AlignmentSpan
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

/**
 * Created by David at 2023/3/24
 */
class ExpandableTextView : AppCompatTextView {
    @Volatile
    var animating = false
    var isClosed = false
    private var mMaxLines = DEFAULT_MAX_LINE

    /** including paddingLeft and paddingRight  */
    private var initWidth = 0
    private lateinit var originalText: CharSequence
    private var openSpannableStr: SpannableStringBuilder? = null
    private var closeSpannableStr: SpannableStringBuilder? = null
    private var hasAnimation = false
    private var openAnim: Animation? = null
    private var closeAnim: Animation? = null
    private var openHeight = 0
    private var closeHeight = 0
    private var expandable = false
    private var closeInNewLine = false
    private var openSuffixSpan: SpannableString? = null
    private var closeSuffixSpan: SpannableString? = null
    private var openSuffixStr = ""
    private var closeSuffixStr = ""
    private var titleStr = ""
    private val space = " "
    private var openSuffixColor = 0
    private var closeSuffixColor = 0
    private var titleColor = 0
    private var onClickListener: OnClickListener? = null
    private var charSequenceToSpannableHandler: CharSequenceToSpannableHandler? = null

    constructor(context: Context) : super(context) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(context)
    }

    private fun initialize(context: Context) {
        closeSuffixColor = Color.parseColor("#0064F0")
        openSuffixColor = closeSuffixColor
        openSuffixStr = context.getString(R.string.expand)
        closeSuffixStr = context.getString(R.string.close)
        movementMethod = LinkMovementMethod.getInstance()
        includeFontPadding = false
        updateOpenSuffixSpan()
        updateCloseSuffixSpan()
    }

    override fun hasOverlappingRendering(): Boolean {
        return false
    }

    fun setOriginalText(original: CharSequence) {
        if (TextUtils.isEmpty(original)) {
            return
        }
        this.originalText = Utils.trim(original)
        expandable = false
        closeSpannableStr = SpannableStringBuilder()
        val maxLines = if (originalText.toString().endsWith("\n")) mMaxLines + 1 else mMaxLines
        val tempText = charSequenceToSpannable(originalText)
        openSpannableStr = charSequenceToSpannable(originalText)
        if (maxLines != -1) {
            val layout = createStaticLayout(tempText)
            expandable = layout.lineCount > maxLines
            if (expandable) {
                if (closeInNewLine) {
                    openSpannableStr?.append("\n")
                }
                if (closeSuffixSpan != null) {
                    openSpannableStr?.append(closeSuffixSpan)
                }
                //calculate the right position
                val endPos = layout.getLineEnd(maxLines - 1)
                closeSpannableStr = if (originalText.length <= endPos) {
                    charSequenceToSpannable(originalText)
                } else {
                    charSequenceToSpannable(originalText.subSequence(0, endPos))
                }
                var tempText2 = charSequenceToSpannable(closeSpannableStr!!).append(ELLIPSIS_STRING)
                if (openSuffixSpan != null) {
                    tempText2.append(openSuffixSpan)
                }
                var tempLayout = createStaticLayout(tempText2)
                while (tempLayout.lineCount > maxLines) {
                    val lastSpace = closeSpannableStr!!.length - 1
                    if (lastSpace == -1) {
                        break
                    }
                    closeSpannableStr = if (originalText.length <= lastSpace) {
                        charSequenceToSpannable(originalText)
                    } else {
                        charSequenceToSpannable(originalText.subSequence(0, lastSpace))
                    }
                    tempText2 = charSequenceToSpannable(closeSpannableStr!!).append(ELLIPSIS_STRING)
                    if (openSuffixSpan != null) {
                        tempText2.append(openSuffixSpan)
                    }
                    tempLayout = createStaticLayout(tempText2)
                }
                var lastSpace = closeSpannableStr!!.length
                if (lastSpace >= 0 && originalText.length > lastSpace) {
                    val redundantChar = originalText.subSequence(lastSpace, lastSpace + openSuffixSpan!!.length)
                    val offset = hasEnCharCount(redundantChar) - hasEnCharCount(openSuffixSpan) + 1
                    lastSpace = if (offset <= 0) lastSpace else lastSpace - offset
                    closeSpannableStr = charSequenceToSpannable(originalText.subSequence(0, lastSpace))
                }
                closeHeight = tempLayout.height + paddingTop + paddingBottom
                closeSpannableStr?.append(ELLIPSIS_STRING)
                if (openSuffixSpan != null) {
                    // open suffix text always display on the right
                    val index = maxLines - 1
                    val emptyWidth = initWidth - tempLayout.getLineWidth(index) - paint.measureText(openSuffixStr)
                    if (emptyWidth > 0) {
                        val measureText = paint.measureText(space)
                        var count = 0
                        while (measureText * count < emptyWidth) {
                            count++
                        }
                        for (i in 0 until count) {
                            closeSpannableStr!!.append(space)
                        }
                    }
                    closeSpannableStr!!.append(openSuffixSpan)
                }
            }
        }
        if (!TextUtils.isEmpty(titleStr) && !TextUtils.isEmpty(closeSpannableStr)) {
            openSpannableStr?.setSpan(StyleSpan(Typeface.BOLD), 0, titleStr.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            openSpannableStr?.setSpan(ForegroundColorSpan(titleColor), 0, titleStr.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            closeSpannableStr?.setSpan(StyleSpan(Typeface.BOLD), 0, titleStr.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            closeSpannableStr?.setSpan(ForegroundColorSpan(titleColor), 0, titleStr.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        } else if (!TextUtils.isEmpty(titleStr) && TextUtils.isEmpty(closeSpannableStr)) {
            openSpannableStr?.setSpan(StyleSpan(Typeface.BOLD), 0, titleStr.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            openSpannableStr?.setSpan(ForegroundColorSpan(titleColor), 0, titleStr.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        isClosed = expandable
        if (expandable) {
            text = closeSpannableStr
            super.setOnClickListener { }
        } else {
            text = openSpannableStr
        }
    }

    private fun hasEnCharCount(str: CharSequence?): Int {
        var count = 0
        if (!TextUtils.isEmpty(str)) {
            for (element in str!!) {
                if (element in ' '..'~') {
                    count++
                }
            }
        }
        return count
    }

    private fun switchOpenClose() {
        if (expandable) {
            isClosed = !isClosed
            if (isClosed) {
                close()
            } else {
                open()
            }
        }
    }

    /**
     * set if need animation
     *
     * @param hasAnimation
     */
    fun setHasAnimation(hasAnimation: Boolean) {
        this.hasAnimation = hasAnimation
    }

    private fun open() {
        if (hasAnimation) {
            val layout = createStaticLayout(openSpannableStr)
            openHeight = layout.height + paddingTop + paddingBottom
            executeOpenAnim()
        } else {
            super@ExpandableTextView.setMaxLines(Int.MAX_VALUE)
            text = openSpannableStr
            if (openCloseCallback != null) {
                openCloseCallback!!.onOpen()
            }
        }
    }

    private fun close() {
        if (hasAnimation) {
            executeCloseAnim()
        } else {
            super@ExpandableTextView.setMaxLines(mMaxLines)
            text = closeSpannableStr
            if (openCloseCallback != null) {
                openCloseCallback!!.onClose()
            }
        }
    }

    private fun executeOpenAnim() {
        if (openAnim == null) {
            openAnim = ExpandCollapseAnimation(this, closeHeight, openHeight)
            (openAnim as ExpandCollapseAnimation).fillAfter = true
            (openAnim as ExpandCollapseAnimation).setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    super@ExpandableTextView.setMaxLines(Int.MAX_VALUE)
                    text = openSpannableStr
                }

                override fun onAnimationEnd(animation: Animation) {
                    layoutParams.height = openHeight
                    requestLayout()
                    animating = false
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        }
        if (animating) {
            return
        }
        animating = true
        clearAnimation()
        startAnimation(openAnim)
    }

    private fun executeCloseAnim() {
        if (closeAnim == null) {
            closeAnim = ExpandCollapseAnimation(this, openHeight, closeHeight)
            (closeAnim as ExpandCollapseAnimation).fillAfter = true
            (closeAnim as ExpandCollapseAnimation).setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    animating = false
                    super@ExpandableTextView.setMaxLines(mMaxLines)
                    text = closeSpannableStr
                    layoutParams.height = closeHeight
                    requestLayout()
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        }
        if (animating) {
            return
        }
        animating = true
        clearAnimation()
        startAnimation(closeAnim)
    }

    /**
     * @param spannable
     *
     * @return
     */
    private fun createStaticLayout(spannable: SpannableStringBuilder?): Layout {
        val contentWidth = initWidth - paddingLeft - paddingRight
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = StaticLayout.Builder.obtain(spannable!!, 0, spannable.length, paint, contentWidth)
            builder.setAlignment(Layout.Alignment.ALIGN_NORMAL)
            builder.setIncludePad(includeFontPadding)
            builder.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            builder.build()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            StaticLayout(spannable, paint, contentWidth, Layout.Alignment.ALIGN_NORMAL,
                    lineSpacingMultiplier, lineSpacingExtra, includeFontPadding)
        } else {
            StaticLayout(spannable, paint, contentWidth, Layout.Alignment.ALIGN_NORMAL,
                    getFloatField("mSpacingMult", 1f), getFloatField("mSpacingAdd", 0f), includeFontPadding)
        }
    }

    private fun getFloatField(fieldName: String, defaultValue: Float): Float {
        var value = defaultValue
        if (TextUtils.isEmpty(fieldName)) {
            return value
        }
        try {
            val fields = this.javaClass.declaredFields
            for (field in fields) {
                if (TextUtils.equals(fieldName, field.name)) {
                    value = field.getFloat(this)
                    break
                }
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return value
    }

    /**
     * @param charSequence
     *
     * @return
     */
    private fun charSequenceToSpannable(charSequence: CharSequence): SpannableStringBuilder {
        var spannableStringBuilder: SpannableStringBuilder? = null
        if (charSequenceToSpannableHandler != null) {
            spannableStringBuilder = charSequenceToSpannableHandler!!.charSequenceToSpannable(charSequence)
        }
        if (spannableStringBuilder == null) {
            spannableStringBuilder = SpannableStringBuilder(charSequence)
        }
        return spannableStringBuilder
    }

    /**
     * init TextView width
     *
     * @param width
     */
    fun initWidth(width: Int) {
        initWidth = width
    }

    override fun setMaxLines(maxLines: Int) {
        mMaxLines = maxLines
        super.setMaxLines(maxLines)
    }

    /**
     * set open text suffix
     *
     * @param openSuffix
     */
    fun setOpenSuffix(openSuffix: String) {
        openSuffixStr = openSuffix
        updateOpenSuffixSpan()
    }

    /**
     * set open text suffix color
     *
     * @param openSuffixColor
     */
    fun setOpenSuffixColor(@ColorInt openSuffixColor: Int) {
        this.openSuffixColor = openSuffixColor
        updateOpenSuffixSpan()
    }

    /**
     * set close text suffix
     *
     * @param closeSuffix
     */
    fun setCloseSuffix(closeSuffix: String) {
        closeSuffixStr = closeSuffix
        updateCloseSuffixSpan()
    }

    /**
     * set close text suffix color
     *
     * @param closeSuffixColor
     */
    fun setCloseSuffixColor(@ColorInt closeSuffixColor: Int) {
        this.closeSuffixColor = closeSuffixColor
        updateCloseSuffixSpan()
    }

    /**
     * set Title text
     *
     * @param titleSuffix
     */
    fun setTitleSuffix(titleSuffix: String) {
        titleStr = titleSuffix
    }

    /**
     * set Title color
     *
     * @param color
     */
    fun setTitleColor(@ColorInt color: Int) {
        titleColor = color
    }

    /**
     * if need new line to show close text
     *
     * @param closeInNewLine
     */
    fun setCloseInNewLine(closeInNewLine: Boolean) {
        this.closeInNewLine = closeInNewLine
        updateCloseSuffixSpan()
    }

    private fun updateOpenSuffixSpan() {
        if (TextUtils.isEmpty(openSuffixStr)) {
            openSuffixSpan = null
            return
        }
        openSuffixSpan = SpannableString(openSuffixStr)
        openSuffixSpan?.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                switchOpenClose()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = openSuffixColor
                ds.isUnderlineText = false
            }
        }, 0, openSuffixStr.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }

    private fun updateCloseSuffixSpan() {
        if (TextUtils.isEmpty(closeSuffixStr)) {
            closeSuffixSpan = null
            return
        }
        closeSuffixSpan = SpannableString(closeSuffixStr)
        if (closeInNewLine) {
            val alignmentSpan: AlignmentSpan = AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE)
            closeSuffixSpan?.setSpan(alignmentSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        closeSuffixSpan?.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                switchOpenClose()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = closeSuffixColor
                ds.isUnderlineText = false
            }
        }, 0, closeSuffixStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    override fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    var openCloseCallback: OpenAndCloseCallback? = null
    fun setOpenAndCloseCallback(callback: OpenAndCloseCallback?) {
        openCloseCallback = callback
    }

    interface OpenAndCloseCallback {
        fun onOpen()
        fun onClose()
    }

    /**
     * set handle text
     *
     * @param handler
     */
    fun setCharSequenceToSpannableHandler(handler: CharSequenceToSpannableHandler?) {
        charSequenceToSpannableHandler = handler
    }

    interface CharSequenceToSpannableHandler {
        fun charSequenceToSpannable(charSequence: CharSequence?): SpannableStringBuilder
    }

    internal inner class ExpandCollapseAnimation(private val mTargetView: View, private val mStartHeight: Int, private val mEndHeight: Int) : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            mTargetView.scrollY = 0
            mTargetView.layoutParams.height = ((mEndHeight - mStartHeight) * interpolatedTime + mStartHeight).toInt()
            mTargetView.requestLayout()
        }

        init {
            duration = 400
        }
    }

    companion object {
        private val TAG = ExpandableTextView::class.java.simpleName
        val ELLIPSIS_STRING = String(charArrayOf('\u2026'))
        private const val DEFAULT_MAX_LINE = 3
    }
}