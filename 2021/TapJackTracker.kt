package cz.csob.smartbanking.app.feature.tapjack

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.ActionMode
import android.view.KeyEvent.ACTION_DOWN
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cz.csob.smartbanking.R
import cz.csob.smartbanking.codebase.presentation.TapJackViewModel
import cz.csob.smartbanking.codebase.presentation.util.safeLet
import cz.eman.logger.logError
import cz.eman.logger.logInfo
import cz.eman.logger.logVerbose
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.lang.ref.WeakReference

/**
 * TapJack tracker creates an overlay over the app (on the level of
 * [WindowManager.LayoutParams.LAST_SUB_WINDOW]) and touches to the application to decide if the
 * application is obstructed or not. Checks flags of touch events directed to the application
 * (activity window) and when they have obstructed information in them it will consume them and
 * do not send to the application. It will also display a error dialog using [TapJackViewModel].
 * Else it dispatches them to the decor view of the activity.
 *
 * @author: eMan a.s.
 */
class TapJackTracker : LifecycleObserver, KoinComponent {

    private lateinit var activityRef: WeakReference<Activity>
    private val viewModel by inject<TapJackViewModel>()
    private var frameLayout: FrameLayout? = null
    private var activityLifecycle: Lifecycle? = null
        set(value) {
            logVerbose { "New lifecycle owner $value (previous: $field)" }
            if (field != null && field != value) {
                field?.removeObserver(this)
            }
            field = value
            field?.addObserver(this)
        }
    private var activeActionMode: ActionMode? = null

    /**
     * Registers activity as [WeakReference] and sets [Lifecycle] value. Activity is used to get
     * context and window information. Settings the lifecycle adds this as a [LifecycleObserver].
     * Before it it set it clears values using [clear]. After the activity is set it builds the
     * [frameLayout] to be added later to windowManager.
     *
     * @param activity to be registered
     */
    fun registerActivity(activity: Activity, lifecycle: Lifecycle) {
        logVerbose { "registerActivity()" }
        clear()
        activityRef = WeakReference(activity)
        activityLifecycle = lifecycle
        initFrameLayout()
    }

    /**
     * This should be called when Activity is attached to the window. At this point the windowToken
     * is set to the decor view and it can be loaded. Triggers [addOverlayView] to add overlay view
     * to window manager.
     *
     * Note: window token is set after [Lifecycle.Event.ON_RESUME].
     */
    fun onAttachedToWindow() {
        val windowToken = getActivitySafe()?.window?.decorView?.windowToken
        logVerbose { "onAttachedToWindow(token = $windowToken)" }
        addOverlayView()
    }

    /**
     * This should be called when Activity is detached from the window. It makes sure the overlay
     * is removed. View should already by removed at this point but this functions is still here to
     * make sure.
     *
     * Note: activity is detached from window after [Lifecycle.Event.ON_DESTROY]
     */
    fun onDetachedFromWindow() {
        logVerbose { "onDetachedFromWindow()" }
        removeOverlayView()
    }

    /**
     * Informs the tracker that action mode was started. Action mode is usually context menu for
     * text selection / editing or contextual menu. It can be floating but still be able to pass
     * clicks to the application. Thus if the mode is [ActionMode.TYPE_FLOATING] it will be saved
     * into [activeActionMode] to disable click consumption.
     *
     * @param mode to be checked and saved if it is a [ActionMode.TYPE_FLOATING]
     */
    fun onActionModeStarted(mode: ActionMode?) {
        logVerbose { "onActionModeStarted(mode = $mode)" }
        if (mode?.type == ActionMode.TYPE_FLOATING) {
            activeActionMode = mode
        }
    }

    /**
     * Informs the tracker that action mode finished. Action mode is usually context menu for text
     * selection / editing or contextual menu. It can be floating but still be able to pass clicks
     * to the application. If the mode is [ActionMode.TYPE_FLOATING] it will removed to enable the
     * tracker protection once more.
     *
     * @param mode to be checked and deleted if it is a [ActionMode.TYPE_FLOATING]
     */
    fun onActionModeFinished(mode: ActionMode?) {
        logVerbose { "onActionModeFinished(mode = $mode)" }
        if (mode?.type == ActionMode.TYPE_FLOATING) {
            activeActionMode = null
        }
    }

    /**
     * Adds an overlay view over the application. Overlay is added only when window token for
     * [activityRef] and [frameLayout] is set. It has window params build using [buildWindowParams].
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun addOverlayView() {
        safeLet(getActivitySafe()?.window?.decorView?.windowToken, frameLayout) { token, layout ->
            logVerbose { "addOverlayView()" }
            val params = buildWindowParams(token)
            getActivitySafe()?.windowManager?.addView(layout, params)
        } ?: logInfo { "addOverlayView() - not added" }
    }

    /**
     * Removes overlay view by removing the view from windowManager. This is done in
     * [Lifecycle.Event.ON_PAUSE] since we do not need to check clicks to the application when it is
     * paused.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun removeOverlayView() {
        frameLayout?.let {
            logVerbose { "removeOverlayView() - ${getActivitySafe()}" }
            try {
                getActivitySafe()?.windowManager?.removeViewImmediate(it)
                activeActionMode = null
            } catch (ex: IllegalArgumentException) {
                logVerbose { "removeOverlayView() - no view to remove" }
            }
        } ?: logInfo { "removeOverlayView() - layout is null" }
    }

    /**
     * Clears variables to makes sure they can be recreated later. Calls [removeOverlayView] and
     * clears [frameLayout].
     */
    private fun clear() {
        removeOverlayView()
        frameLayout = null
    }

    /**
     * Initializes frame layout using [activityRef] (used to get activity context). After it is
     * created it will have [touchListener] added to listen for touches.
     */
    private fun initFrameLayout() {
        getActivitySafe()?.let {
            logVerbose { "initFrameLayout()" }
            frameLayout = FrameLayout(it).apply {
                setOnTouchListener(touchListener)
            }
        } ?: logVerbose { "initFrameLayout() - activity ref is empty" }
    }

    /**
     * Touch listener used to decide if the application is obscured or not. Displays error dialog
     * for now if the motion event was obscured (and consumes the event). Else it dispatches the
     * event to the activity window decor view to pass it to the view being clicked.
     */
    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { _, event ->
        if (event.isObscured() && activeActionMode?.type != ActionMode.TYPE_FLOATING) {
            if (event.action == ACTION_DOWN) {
                logVerbose { "onTouch(event = $event) - obscured" }
                viewModel.displayErrorDialog(R.string.app_obstructed)
            }
            true
        } else {
            val dispatched = getActivitySafe()?.window?.decorView?.dispatchTouchEvent(event)
            if (event.action == ACTION_DOWN) {
                logVerbose { "onTouch(event = $event) - dispatched to application $dispatched" }
            }
            false
        }
    }

    /**
     * Builds [WindowManager.LayoutParams] for the view added to the windowManager. It has
     * [WindowManager.LayoutParams.MATCH_PARENT] width and height to cover the whole screen. Type is
     * set to [WindowManager.LayoutParams.LAST_SUB_WINDOW] this only works with a specific window
     * token which is provided [token]. It is basically window with the highest Z index for the
     * application (not system). But system based overlay would not work as intended. Flags are set
     * to [WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE] or
     * [WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS] which make sure the view (window) is not
     * focusable but it can receive touch events and is over status bar. [PixelFormat.TRANSLUCENT]
     * supports translucent colors for the view (not really needed). Last information added to the
     * params is the window token to which the new overlay view will be associated to (required due
     * to LAST_SUB_WINDOW flag).
     *
     * @return [WindowManager.LayoutParams] for the overlay view
     */
    private fun buildWindowParams(token: IBinder) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.LAST_SUB_WINDOW,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        this.token = token
    }

    /**
     * Gets [Activity] from reference safely. Catches [UninitializedPropertyAccessException] and
     * returns null in that case.
     *
     * @return [Activity] if initialized and not cleared, else null
     */
    private fun getActivitySafe(): Activity? {
        return try {
            activityRef.get()
        } catch (ex: UninitializedPropertyAccessException) {
            logError("getActivityRefSafely() - not initialized", ex)
            null
        }
    }

    /**
     * Checks if the motion event is obscured. It is obscured when flags contain
     * [MotionEvent.FLAG_WINDOW_IS_OBSCURED].
     *
     * @return true when obscured else false
     */
    fun MotionEvent.isObscured() = (this.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0
}
