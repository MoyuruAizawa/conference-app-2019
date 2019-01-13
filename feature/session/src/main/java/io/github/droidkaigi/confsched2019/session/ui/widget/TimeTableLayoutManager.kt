package io.github.droidkaigi.confsched2019.session.ui.widget

import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

class TimeTableLayoutManager(
    private val columnWidth: Int,
    private val pxPerMinute: Int,
    private val sessionLookUp: (position: Int) -> SessionInfo
) : RecyclerView.LayoutManager() {

    class SessionInfo(val startEpochMin: Int, val endEpochMin: Int) {
        val durationMin = endEpochMin - startEpochMin
    }

    private val columnCount = 9

    private val parentLeft get() = paddingLeft
    private val parentTop get() = paddingTop
    private val parentRight get() = width - paddingRight
    private val parentBottom get() = height - paddingBottom

    private val sessionList = SparseArray<SessionInfo>()

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)

        if (itemCount == 0) {
            sessionList.clear()
            return
        }

        var lastMin = 0
        (0 until itemCount).forEach {
            val sessionInfo = sessionLookUp(it)
            lastMin = max(lastMin, sessionInfo.endEpochMin)
            sessionList.put(it, sessionInfo)
        }

        var offset = parentLeft
        for (position in 0 until itemCount) {
            val columnNumber = position % columnCount
            val session = sessionList.get(position) ?: break

            val view = recycler.getViewForPosition(position)
            addView(view)
            measureChildWithMargins(view, session)
            val width = getDecoratedMeasuredWidth(view)
            val height = getDecoratedMeasuredHeight(view)
            val left = offset
            val top = session.startEpochMin * pxPerMinute
            val right = left + width
            val bottom = top + height
            layoutDecoratedWithMargins(view, left, top, right, bottom)

            offset = if (columnNumber == columnCount - 1) 0 else right
        }
    }

    override fun canScrollVertically() = true

    override fun canScrollHorizontally() = true

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (dy == 0) return 0

        val scrollAmount = if (dy > 0) {
            var bottom = 0
            (childCount - columnCount until childCount)
                .forEach { view -> getChildAt(view)?.let { bottom = max(bottom, getDecoratedBottom(it)) } }
            if (bottom - dy < parentBottom) bottom - parentBottom else dy
        } else {
            var top = 0
            (0 until columnCount).forEach { view -> getChildAt(view)?.let { top = min(top, getDecoratedTop(it)) } }
            if (top - dy > parentTop) top - parentTop else dy
        }

        offsetChildrenVertical(-scrollAmount)
        return scrollAmount
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (dx == 0) return 0

        val scrollAmount = if (dx > 0) {
            val right = getDecoratedRight(getChildAt(columnCount - 1) ?: return 0)
            if (right - dx < parentRight) right - parentRight else dx
        } else {
            val left = getDecoratedLeft(getChildAt(0) ?: return 0)
            if (left - dx > parentLeft) left - parentLeft else dx
        }

        offsetChildrenHorizontal(-scrollAmount)
        return scrollAmount
    }

    private fun measureChildWithMargins(view: View, session: SessionInfo) {
        val lp = view.layoutParams as RecyclerView.LayoutParams
        lp.width = columnWidth
        lp.height = session.durationMin * pxPerMinute

        val insets = Rect().apply { calculateItemDecorationsForChild(view, this) }
        val widthSpec = getChildMeasureSpec(
            width,
            widthMode,
            paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin + insets.left + insets.right,
            lp.width,
            true
        )
        val heightSpec = getChildMeasureSpec(
            height,
            heightMode,
            paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin + insets.top + insets.bottom,
            lp.height,
            true
        )
        view.measure(widthSpec, heightSpec)
    }
}
