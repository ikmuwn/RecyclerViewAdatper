package kim.uno.mock.util.recyclerview

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class RecyclerViewTouchInterceptorListener : RecyclerView.OnItemTouchListener {

    private lateinit var gestureDetector: GestureDetector

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!::gestureDetector.isInitialized) {
            gestureDetector = GestureDetector(rv.context, GestureListener(rv))
        }
        gestureDetector.onTouchEvent(e)
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    private class GestureListener(val recyclerView: RecyclerView) :
        GestureDetector.SimpleOnGestureListener() {

        private val touchSlop = ViewConfiguration.get(recyclerView.context).scaledTouchSlop

        override fun onDown(e: MotionEvent): Boolean {
            recyclerView.parent.requestDisallowInterceptTouchEvent(true)
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (abs(distanceX) < abs(distanceY)) {
                recyclerView.parent.requestDisallowInterceptTouchEvent(false)
            } else if (abs(distanceX) > touchSlop) {
                recyclerView.parent.requestDisallowInterceptTouchEvent(true)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }
}
