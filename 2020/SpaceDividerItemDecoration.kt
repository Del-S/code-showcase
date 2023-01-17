package cz.sberbank.sbercoins.presentation.core.view.adapter.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * Combination of [DividerItemDecoration] and [SpaceItemDecoration] that displays divider in the middle of defined
 * space. SpaceItemDecoration is a [RecyclerView.ItemDecoration] that can be used as a divider between items.
 *
 * It supports to control add spaces. It supports all positions defined in [SpacePosition].
 *
 * @param spaceSize Space size in pixels
 *
 * @author eMan s.r.o. (info@eman.cz)
 */
open class SpaceDividerItemDecoration(
    private val divider: Drawable,
    @Px private val spaceSize: Int,
    private val position: ItemDecorationPosition = ItemDecorationPosition.BOTTOM,
    private val predicate: DecorationPredicate? = null
) : RecyclerView.ItemDecoration() {

    companion object {
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }

    private val bounds = Rect()

    constructor(
        context: Context,
        @DrawableRes dividerId: Int,
        @DimenRes spaceSizeId: Int,
        position: ItemDecorationPosition = ItemDecorationPosition.BOTTOM,
        predicate: DecorationPredicate? = null
    ) : this(
        divider = checkNotNull(context.getDrawable(dividerId)),
        spaceSize = context.resources.getDimensionPixelSize(spaceSizeId),
        position = position,
        predicate = predicate
    )

    constructor(
        context: Context,
        @DimenRes spaceSizeId: Int,
        itemDecorationPosition: ItemDecorationPosition = ItemDecorationPosition.BOTTOM,
        predicate: DecorationPredicate? = null
    ) : this(
        context.obtainStyledAttributes(ATTRS).let { a ->
            val divider = checkNotNull(a.getDrawable(0)) {
                "@android:attr/listDivider was not set in the theme used for this DividerItemDecoration."
            }
            a.recycle()

            divider
        },
        context.resources.getDimensionPixelSize(spaceSizeId),
        itemDecorationPosition,
        predicate
    )

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null) {
            return
        }

        when (position) {
            ItemDecorationPosition.TOP, ItemDecorationPosition.BOTTOM -> drawVertical(c, parent)
            else -> drawHorizontal(c, parent)
        }
    }

    /**
     * Draws a divider for horizontal list, positions that draw this divider are [ItemDecorationPosition.TOP] and
     * [ItemDecorationPosition.BOTTOM].
     *
     * @param canvas to draw on
     * @param parent [RecyclerView]
     */
    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int

        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(left, parent.paddingTop, right, parent.height - parent.paddingBottom)
        } else {
            left = 0
            right = parent.width
        }

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (predicate?.invoke(child, parent) != false) {
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val verticalBounds = getVerticalBounds(child)
                divider.setBounds(left, verticalBounds.first, right, verticalBounds.second)
                divider.draw(canvas)
            }
        }
        canvas.restore()
    }

    /**
     * Gets top and bottom bounds for the divider based on it's position. Top calls [getBoundsTopDivider] and bottom
     * calls [getBoundsBottomDivider].
     *
     * @param child view to get the bounds from
     * @return [Pair] with left first and right second
     */
    private fun getVerticalBounds(child: View) = if (position == ItemDecorationPosition.TOP) {
        getBoundsTopDivider(child)
    } else {
        getBoundsBottomDivider(child)
    }

    /**
     * Gets top and bottom bounds for top sided divider. Bounds are increased by [spaceSize] to make sure the divider
     * is in the middle of the empty space.
     *
     * @param child view to get the bounds from
     * @return [Pair] with top first and bottom second
     */
    private fun getBoundsTopDivider(child: View): Pair<Int, Int> {
        val top = bounds.top + child.translationY.roundToInt()
        val bottom = top + divider.intrinsicHeight
        return Pair(top + spaceSize, bottom + spaceSize)
    }

    /**
     * Gets top and bottom bounds for bottom sided divider. Bounds are increased by [spaceSize] to make sure the divider
     * is in the middle of the empty space.
     *
     * @param child view to get the bounds from
     * @return [Pair] with top first and bottom second
     */
    private fun getBoundsBottomDivider(child: View): Pair<Int, Int> {
        val bottom = bounds.bottom + child.translationY.roundToInt()
        val top = bottom - divider.intrinsicHeight
        return Pair(top - spaceSize, bottom - spaceSize)
    }

    /**
     * Draws a divider for horizontal list, positions that draw this divider are [ItemDecorationPosition.LEFT] and
     * [ItemDecorationPosition.RIGHT].
     *
     * @param canvas to draw on
     * @param parent [RecyclerView]
     */
    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int

        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(parent.paddingLeft, top, parent.width - parent.paddingRight, bottom)
        } else {
            top = 0
            bottom = parent.height
        }

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (predicate?.invoke(child, parent) != false) {
                parent.layoutManager!!.getDecoratedBoundsWithMargins(child, bounds)
                val horizontalBounds = getHorizontalBounds(child)
                divider.setBounds(horizontalBounds.first, top, horizontalBounds.second, bottom)
                divider.draw(canvas)
            }
        }
        canvas.restore()
    }

    /**
     * Gets left and right bounds for the divider based on it's position. Left calls [getBoundsLeftDivider] and right
     * calls [getBoundsRightDivider].
     *
     * @param child view to get the bounds from
     * @return [Pair] with left first and right second
     */
    private fun getHorizontalBounds(child: View) = if (position == ItemDecorationPosition.LEFT) {
        getBoundsLeftDivider(child)
    } else {
        getBoundsRightDivider(child)
    }

    /**
     * Gets left and right bounds for left sided divider. Bounds are increased by [spaceSize] to make sure the divider
     * is in the middle of the empty space.
     *
     * @param child view to get the bounds from
     * @return [Pair] with left first and right second
     */
    private fun getBoundsLeftDivider(child: View): Pair<Int, Int> {
        val left = bounds.left + child.translationX.roundToInt()
        val right = left + divider.intrinsicWidth
        return Pair(left + spaceSize, right + spaceSize)
    }

    /**
     * Gets left and right bounds for right sided divider. Bounds are increased by [spaceSize] to make sure the divider
     * is in the middle of the empty space.
     *
     * @param child view to get the bounds from
     * @return [Pair] with left first and right second
     */
    private fun getBoundsRightDivider(child: View): Pair<Int, Int> {
        val right = bounds.right + child.translationX.roundToInt()
        val left = right - divider.intrinsicWidth
        return Pair(left - spaceSize, right - spaceSize)
    }

    /**
     * Offset is calculated as divider width/height + 2 * space size since the space on both sides of the divider.
     */
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (predicate?.invoke(view, parent) != false) {
            when (position) {
                ItemDecorationPosition.LEFT -> outRect.set(divider.intrinsicWidth + (2 * spaceSize), 0, 0, 0)
                ItemDecorationPosition.TOP -> outRect.set(0, divider.intrinsicHeight + (2 * spaceSize), 0, 0)
                ItemDecorationPosition.RIGHT -> outRect.set(0, 0, divider.intrinsicWidth + (2 * spaceSize), 0)
                ItemDecorationPosition.BOTTOM -> outRect.set(0, 0, 0, divider.intrinsicHeight + (2 * spaceSize))
            }
        }
    }
}
