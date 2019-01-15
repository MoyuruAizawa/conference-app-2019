package io.github.droidkaigi.confsched2019.session.ui.widget

import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.soywiz.klock.DateTime
import io.github.droidkaigi.confsched2019.model.Room
import kotlin.math.max
import kotlin.math.min

class TimeTableLayoutManager(
    private val columnWidth: Int,
    private val pxPerMinute: Int,
    private val sessionLookUp: (position: Int) -> SessionInfo
) : RecyclerView.LayoutManager() {

    class SessionInfo(startDateTime: DateTime, endDateTime: DateTime, val roomRoom: Room) {
        val startEpochMin = (startDateTime.unixMillisLong / 1000 / 60).toInt()
        val endEpochMin = (endDateTime.unixMillisLong / 1000 / 60).toInt()
        val durationMin = endEpochMin - startEpochMin
    }

    private val parentLeft get() = paddingLeft
    private val parentTop get() = paddingTop
    private val parentRight get() = width - paddingRight
    private val parentBottom get() = height - paddingBottom

    private val columnCount = 9
    private var endEpochMin = Int.MIN_VALUE
    private var startEpochMin = Int.MAX_VALUE
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

        startEpochMin = Int.MAX_VALUE
        endEpochMin = Int.MIN_VALUE
        (0 until itemCount).forEach {
            val sessionInfo = sessionLookUp(it)
            startEpochMin = min(sessionInfo.startEpochMin, startEpochMin)
            endEpochMin = max(sessionInfo.endEpochMin, endEpochMin)
            sessionList.put(it, sessionInfo)
        }

        for (position in 0 until itemCount) {
            val session = sessionList.get(position) ?: break

            val view = recycler.getViewForPosition(position)
            addView(view)
            measureChildWithMargins(view, session)
            val width = getDecoratedMeasuredWidth(view)
            val height = getDecoratedMeasuredHeight(view)
            val left = session.roomRoom.id * columnWidth
            val top = (session.startEpochMin - startEpochMin) * pxPerMinute
            val right = left + width
            val bottom = top + height
            layoutDecoratedWithMargins(view, left, top, right, bottom)
        }
    }

    override fun canScrollVertically() = true

    override fun canScrollHorizontally() = true

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (dy == 0) return 0

        val scrollAmount = if (dy > 0) {
            val bottom = (childCount - columnCount until childCount)
                .mapNotNull { getChildAt(it)?.let(this::getDecoratedBottom) }.min() ?: return 0
            if (bottom - dy < parentBottom) bottom - parentBottom else dy
        } else {
            val top = (0 until columnCount)
                .mapNotNull { getChildAt(it)?.let(this::getDecoratedBottom) }.max() ?: return 0
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

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        return (endEpochMin - startEpochMin) * pxPerMinute
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        return super.computeVerticalScrollOffset(state)
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
