package me.peace.watcher.service

import android.accessibilityservice.AccessibilityService
import android.animation.RectEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 高亮焦点控件。
 */
class FocusFrameServiceDelegate : ServiceDelegate {
  private lateinit var manager: WindowManager
  /** 高亮 view 。 */
  private lateinit var view: View
  private lateinit var params: WindowManager.LayoutParams
  /** 动画执行过程中当前 [view] 的位置。 */
  private val animatorFocusRect = Rect()
  /** 当前 [view] 的目标位置。 */
  private val currentFocusRect = Rect()
  /** [view] 变化位置的动画。 */
  private val animator = ValueAnimator.ofObject(RectEvaluator(), Rect()).also {
    it.duration = 100
    it.addUpdateListener { animator ->
      val value = animator.animatedValue as Rect
      animatorFocusRect.set(value)
      params.height = value.height()
      params.width = value.width()
      params.x = value.left
      params.y = value.top
      view.visibility = if (value.isEmpty) View.GONE else View.VISIBLE
      manager.updateViewLayout(view, params)
    }
  }

  override fun isEnable(): Boolean = true

  override fun onServiceConnected(service: AccessibilityService) {
    super.onServiceConnected(service)
    val context = service.applicationContext
    // 创建高亮 view
    manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    view = View(context)
    view.setWillNotDraw(false)
    view.background = RainbowDrawable()
    params = WindowManager.LayoutParams()
    params.gravity = Gravity.TOP or Gravity.START
    params.format = PixelFormat.RGBA_8888
    params.x = 0
    params.y = 0
    params.width = 0
    params.height = 0
    params.type = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    // 添加将高亮 view
    manager.addView(view, params)
    updateLocation(service)
  }
  /**
   * 更新 [view] 的坐标位置。
   */
  private fun updateLocation(service: AccessibilityService) {
    if (!::view.isInitialized || !view.isAttachedToWindow ) return
    val rect = Rect()
    service.rootInActiveWindow.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.getBoundsInScreen(rect)
    if (rect == currentFocusRect) return
    currentFocusRect.set(rect)
    animator.cancel()
    animator.setObjectValues(animatorFocusRect, rect)
    animator.start()
  }

  override fun onAccessibilityEvent(service: AccessibilityService, event: AccessibilityEvent) {
    super.onAccessibilityEvent(service, event)

    updateLocation(service)
  }

  override fun onUnbind(service: AccessibilityService, intent: Intent?) {
    if (!::manager.isInitialized) return
    animator.cancel()
    manager.removeView(view)
  }

  class RainbowDrawable : Drawable() {
    private val paint = Paint().apply { alpha = 0x66 }
    private var linearGradient: LinearGradient? = null

    override fun setAlpha(alpha: Int) {
      paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
      paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onBoundsChange(bounds: Rect) {
      linearGradient = LinearGradient(0f, 0f, 0f, bounds.height().toFloat(),
        intArrayOf(0xffff0000.toInt(), 0xffffa500.toInt(), 0xffffff00.toInt(), 0xff008000.toInt(), 0xff00ffff.toInt(), 0xff0099ff.toInt(),  0xff9900ff.toInt()),
        floatArrayOf(0.142857f, 0.285714f, 0.4285714f, 0.5714286f, 0.714286f, 0.857143f, 1f),
        Shader.TileMode.CLAMP)
      paint.shader = linearGradient
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(bounds, paint)
    }
  }
}