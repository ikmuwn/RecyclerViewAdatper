package kim.uno.mock.util.recyclerview

import android.graphics.Canvas
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.abs

abstract class DraggableRecyclerAdapter(
    private val longPressDragEnabled: Boolean = false
) : RecyclerViewAdapter() {

    private var itemTouchHelper: ItemTouchHelper? = null
    private var swipeable = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<Any> {
        return super.onCreateViewHolder(parent, viewType).also {
            swipeable = swipeable || it is Swipeable
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        itemTouchHelper = ItemTouchHelper(DragHelperCallback(this))
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    fun startDrag(holder: ViewHolder<*>) {
        if (holder is Draggable) {
            itemTouchHelper?.startDrag(holder)
            holder.onDragStateChanged(true)
        } else {
            throw Exception("You must implement the Draggable interface.")
        }
    }

    fun onItemDismiss(position: Int) {
        if (itemCount > position) items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun onItemSwap(source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (source is Draggable && source.isDragEnabled()
            && target is Draggable && target.isDragEnabled()
        ) {
            val sourcePosition = source.adapterPosition
            val targetPosition = target.adapterPosition

            if (sourcePosition != targetPosition) {

                val swapCount = abs(sourcePosition - targetPosition)
                var mountain = sourcePosition
                val direction = if (sourcePosition > targetPosition) -1 else 1

                for (i in 0 until swapCount) {
                    Collections.swap(items, mountain, mountain + 1 * direction)
                    mountain += 1 * direction
                }

                notifyItemMoved(sourcePosition, targetPosition)
                return true
            }
        }

        return false
    }

    open fun isLongPressDragEnabled(): Boolean {
        return longPressDragEnabled
    }

    open fun isItemViewSwipeEnabled(): Boolean {
        return swipeable
    }

    interface Draggable {
        fun onDragStateChanged(isSelected: Boolean) {}
        fun isDragEnabled() = true
    }

    interface Swipeable {
        fun isSwipeEnabled() = true
        fun onChildDraw(c: Canvas, dX: Float, dY: Float, actionState: Int) = false
        fun onSwiped() = false
    }

    class Builder(
        private val longPressDragEnabled: Boolean = false
    ) : RecyclerViewAdapter.Builder() {

        override val adapter by lazy {
            object : DraggableRecyclerAdapter(longPressDragEnabled = longPressDragEnabled) {

                override fun onCreateHolder(viewType: Int): ViewHolder<*> {
                    return this@Builder.onCreateHolder(viewType)
                }

                override fun onBindViewHolder(holder: ViewHolder<Any>, position: Int) {
                    super.onBindViewHolder(holder, position)
                    this@Builder.onBindViewHolder(holder, position)
                }

            }
        }

    }

}
