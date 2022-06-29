package kim.uno.mock.util.recyclerview

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.abs

abstract class DraggableRecyclerAdapter : RecyclerViewAdapter() {

    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        itemTouchHelper = ItemTouchHelper(DragHelperCallback(this))
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    fun startDrag(holder: ViewHolder<*>) {
        itemTouchHelper?.startDrag(holder)
        if (holder is DragHelperCallback.Draggable) {
            holder.onDragStateChanged(true)
        }
    }

    fun onItemDismiss(position: Int) {
        if (itemCount > position) items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun onItemSwap(source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (source is DragHelperCallback.Draggable && source.isDragEnabled()
            && target is DragHelperCallback.Draggable && target.isDragEnabled()
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
        return false
    }

    open fun isItemViewSwipeEnabled(): Boolean {
        return true
    }

    class Builder : RecyclerViewAdapter.Builder() {

        override val adapter by lazy {
            object : DraggableRecyclerAdapter() {

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