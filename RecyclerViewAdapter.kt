package kim.uno.mock.util.recyclerview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.collection.ArrayMap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.safeCast

abstract class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder<Any>>() {

    companion object {
        private val NOT_SET = Any()
    }

    open lateinit var recyclerView: RecyclerView
    val inflater: LayoutInflater
        get() = LayoutInflater.from(recyclerView.context)
    val holders = ArrayList<ViewHolder<*>>()
    var items = ArrayList<Pair<Int, Any>>()
    private val diffCallback by lazy { DiffCallback() }

    var currentItem: Int
        get() = when (val layoutManager = recyclerView.layoutManager) {
            is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
            is StaggeredGridLayoutManager -> {
                val positions = IntArray(layoutManager.spanCount)
                layoutManager.findFirstVisibleItemPositions(positions)
                positions[0]
            }
            else -> 0
        }
        set(value) {
            recyclerView.smoothScrollToPosition(value)
        }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<Any> =
        onCreateHolder(viewType) as ViewHolder<Any>

    abstract fun onCreateHolder(viewType: Int): ViewHolder<*>

    override fun getItemCount() = items.size

    fun getItem(position: Int) = items[positionCalibrate(position)].second

    override fun onBindViewHolder(holder: ViewHolder<Any>, position: Int) {
        holder.onBindView(getItem(position), positionCalibrate(position))
    }

    override fun onBindViewHolder(
        holder: ViewHolder<Any>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val consumed = payloads
            .filterIsInstance<ArrayList<String>>()
            .firstOrNull()?.let {
                holder.onBindView(getItem(position), positionCalibrate(position), it)
            } ?: false
        if (!consumed) {
            super.onBindViewHolder(holder, positionCalibrate(position), payloads)
        }
    }

    override fun getItemViewType(position: Int): Int = items[positionCalibrate(position)].first

    open fun positionCalibrate(position: Int) = position

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        if (this is InfiniteRecyclerViewAdapter) {
            (recyclerView.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(initPosition, 0)
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder<Any>) {
        super.onViewAttachedToWindow(holder)
        if (!holders.contains(holder)) holders.add(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder<Any>) {
        holder.onViewDetachedFromWindow()
        holders.remove(holder)
        super.onViewDetachedFromWindow(holder)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun notifyDataSetChange(
        animation: Boolean = true,
        detectMoves: Boolean = true,
        unit: (RecyclerViewAdapter) -> Unit
    ) {
        val transactionPairs = ArrayList(items)
        unit(this)

        if (!animation || transactionPairs.size == 0) {
            notifyDataSetChanged()
        } else {
            diffCallback.transaction = transactionPairs
            DiffUtil.calculateDiff(diffCallback, detectMoves).dispatchUpdatesTo(this)
        }

        if (this is InfiniteRecyclerViewAdapter && wasInitPosition != initPosition) {
            wasInitPosition = initPosition
            when (val layoutManager = recyclerView.layoutManager) {
                is LinearLayoutManager -> layoutManager.scrollToPositionWithOffset(initPosition, 0)
                is StaggeredGridLayoutManager -> layoutManager.scrollToPositionWithOffset(
                    initPosition,
                    0
                )
            }
        }
    }

    open fun add(index: Int = items.size, item: Any? = null, viewType: Int = 0) {
        items.add(index, viewType to (item ?: NOT_SET))
    }

    open fun add(index: Int = items.size, pair: Pair<Int, Any>) {
        items.add(index, pair)
    }

    /**
     * for java
     */
    fun addAll(items: List<*>?, viewType: Int = 0) {
        items?.forEachIndexed { i, item ->
            add(
                item = item,
                viewType = viewType
            )
        }
    }

    fun addAll(index: Int = this.items.size, items: List<*>?, viewType: Int = 0) {
        items?.forEachIndexed { i, item ->
            add(
                index = index + i,
                item = item,
                viewType = viewType
            )
        }
    }

    fun remove(item: Any): Boolean {
        return items.firstOrNull {
            it.second == item
        }?.let {
            items.remove(it)
        } ?: false
    }

    fun removeAll(items: List<*>?) {
        items?.forEach { it?.let { remove(it) } }
    }

    fun remove(viewType: Int): Boolean {
        return items.firstOrNull {
            it.first == viewType
        }?.let {
            items.remove(it)
        } ?: false
    }

    fun removeAll(viewType: Int): Boolean {
        return items.removeAll {
            it.first == viewType
        }
    }

    open fun clear() {
        items.clear()
    }

    open class ViewHolder<ITEM>(val adapter: RecyclerViewAdapter, itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        constructor(adapter: RecyclerViewAdapter, @LayoutRes resId: Int) : this(
            adapter,
            LayoutInflater.from(adapter.recyclerView.context)
                .inflate(resId, adapter.recyclerView, false)
        )

        internal val context = adapter.recyclerView.context

        open fun onBindView(item: ITEM, position: Int) {}

        /**
         * @return True if the view has consumed the bind, false otherwise.
         */
        open fun onBindView(item: ITEM, position: Int, payloads: ArrayList<String>) = false

        open fun onViewAttachedToWindow() {

        }

        open fun onViewDetachedFromWindow() {

        }

        fun removeRecyclerViewInsets() {
            val isVertically = when (val layoutManager = adapter.recyclerView.layoutManager) {
                is LinearLayoutManager -> layoutManager.orientation == LinearLayoutManager.VERTICAL
                is StaggeredGridLayoutManager -> layoutManager.orientation == StaggeredGridLayoutManager.VERTICAL
                else -> true
            }

            (itemView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                if (isVertically) {
                    leftMargin = -adapter.recyclerView.paddingLeft
                    rightMargin = -adapter.recyclerView.paddingLeft
                } else {
                    topMargin = -adapter.recyclerView.paddingTop
                    bottomMargin = -adapter.recyclerView.paddingBottom
                }
            }
        }
    }

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ItemDiff

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ContentsDiff

    internal inner class DiffCallback : DiffUtil.Callback() {

        lateinit var transaction: ArrayList<Pair<Int, Any>>

        override fun getOldListSize() = transaction.size
        override fun getNewListSize() = items.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areSame(oldItemPosition, newItemPosition, ItemDiff::class.java)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areSame(oldItemPosition, newItemPosition, ContentsDiff::class.java)
        }

        private fun areSame(
            oldItemPosition: Int,
            newItemPosition: Int,
            clazz: Class<out Annotation>
        ): Boolean {
            val before = transaction[oldItemPosition]
            val after = items[newItemPosition]
            if (before.second.javaClass.name == after.second.javaClass.name && before.first == after.first) {
                var isAnnotationPresent = false
                before.second.javaClass.declaredFields
                    .filter { it.isAnnotationPresent(clazz) }
                    .forEach {
                        isAnnotationPresent = true
                        val accessible = it.isAccessible
                        it.isAccessible = true
                        val beforeValue = it.get(before.second)
                        val afterValue = it.get(after.second)
                        it.isAccessible = accessible
                        if (beforeValue != afterValue) {
                            return false
                        }
                    }

                return isAnnotationPresent || before == NOT_SET || after == NOT_SET || before == after
            }

            return false
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val before = transaction[oldItemPosition]
            val after = items[newItemPosition]
            if (before.second.javaClass.name == after.second.javaClass.name) {
                val payload = ArrayList<String>()
                before.second.javaClass.declaredFields
                    .filter { it.isAnnotationPresent(ContentsDiff::class.java) }
                    .forEach {
                        val accessible = it.isAccessible
                        it.isAccessible = true
                        val beforeValue = it.get(before.second)
                        val afterValue = it.get(after.second)
                        it.isAccessible = accessible
                        if (beforeValue != afterValue) {
                            payload.add(it.name)
                        }
                    }

                if (payload.size > 0) {
                    return payload
                }
            }
            return null
        }

    }

    open class Builder {

        protected open val adapter by lazy {
            object : RecyclerViewAdapter() {

                override fun onCreateHolder(viewType: Int): ViewHolder<*> {
                    return this@Builder.onCreateHolder(viewType)
                }

                override fun onBindViewHolder(holder: ViewHolder<Any>, position: Int) {
                    super.onBindViewHolder(holder, position)
                    this@Builder.onBindViewHolder(holder, position)
                }

            }
        }

        protected fun onCreateHolder(viewType: Int): ViewHolder<*> {
            return holderConstructors[viewType]?.invoke()
                ?: throw Exception("Unknown view type: $viewType")
        }

        protected fun onBindViewHolder(holder: ViewHolder<Any>, position: Int) {
            binder?.invoke(holder, position, adapter.itemCount)
        }

        private var binder: ((holder: ViewHolder<Any>, position: Int, itemCount: Int) -> Unit)? =
            null

        private val holderConstructors by lazy {
            ArrayMap<Int, () -> ViewHolder<*>?>()
        }

        fun <ITEM, HOLDER : ViewHolder<ITEM>> addHolder(
            holder: KClass<HOLDER>,
            vararg args: Any?
        ) = addHolder(
            viewType = 0,
            holder = holder,
            args = args
        )

        fun <ITEM, HOLDER : ViewHolder<ITEM>> addHolder(
            viewType: Int,
            holder: KClass<HOLDER>,
            vararg args: Any?
        ): Builder {
            holderConstructors[viewType] = {
                var instance: HOLDER? = null
                holder.constructors.forEach {
                    try {
                        val arguments = args.mapIndexed { index, any ->
                            it.parameters[index + 1].type.jvmErasure.safeCast(any)
                        }.toTypedArray()
                        instance = it.call(adapter, *arguments)
                        return@forEach
                    } catch (e: Throwable) {

                    }
                }

                instance
            }
            return this
        }

        fun addBinder(binder: (holder: ViewHolder<Any>, position: Int, itemCount: Int) -> Unit): Builder {
            this.binder = binder
            return this
        }

        fun build() = adapter.apply { }

    }

}
