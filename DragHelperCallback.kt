package kim.uno.mock.util.recyclerview

import android.graphics.Canvas
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragHelperCallback(private val adapter: DraggableRecyclerAdapter) :
    ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean {
        return adapter.isLongPressDragEnabled()
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return adapter.isItemViewSwipeEnabled()
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Set movement flags based on the layout manager
        return if (recyclerView.layoutManager is GridLayoutManager) {
            val dragFlags =
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END
            val swipeFlags = 0
            makeMovementFlags(dragFlags, swipeFlags)
        } else {
            if (recyclerView.layoutManager?.canScrollHorizontally() == true) {
                val dragFlags = ItemTouchHelper.START or ItemTouchHelper.END
                val swipeFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                makeMovementFlags(dragFlags, swipeFlags)
            } else {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
                makeMovementFlags(dragFlags, swipeFlags)
            }
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return adapter.onItemSwap(source, target)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        if (viewHolder is DraggableRecyclerAdapter.Swipeable) {
            if (!viewHolder.isSwipeEnabled() || viewHolder.onSwiped()) {
                return
            }
        }

        adapter.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (viewHolder is DraggableRecyclerAdapter.Swipeable) {
            if (viewHolder.onChildDraw(c, dX, dY, actionState)) {
                return
            } else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                if (viewHolder.isSwipeEnabled()) {
                    if (recyclerView.layoutManager?.canScrollHorizontally() == true) {
                        viewHolder.itemView.translationY = dY
                    } else {
                        viewHolder.itemView.translationX = dX
                    }
                }
                return
            }
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && viewHolder is DraggableRecyclerAdapter.Draggable) {
            viewHolder.onDragStateChanged(true)
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is DraggableRecyclerAdapter.Draggable) {
            viewHolder.onDragStateChanged(false)
        }
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return if (target is DraggableRecyclerAdapter.Draggable) target.isDragEnabled() else false
    }

}
