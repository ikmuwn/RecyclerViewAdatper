package kim.uno.mock.util.recyclerview

abstract class InfiniteRecyclerViewAdapter : RecyclerViewAdapter() {

    companion object {
        const val LOOP_INIT_POSITION = Int.MAX_VALUE / 2
    }

    private val loop: Boolean
        get() = items.size > 1

    private val loopOffset: Int
        get() = LOOP_INIT_POSITION % items.size

    val initPosition: Int
        get() = if (loop) LOOP_INIT_POSITION - loopOffset else 0

    var wasInitPosition = 0

    override fun getItemCount() = if (loop) Int.MAX_VALUE else super.getItemCount()

    fun getUniqueItemCount() = super.getItemCount()

    override fun positionCalibrate(position: Int): Int {
        return if (loop) position % items.size else position
    }

    class Builder : RecyclerViewAdapter.Builder() {

        override val adapter by lazy {
            object : InfiniteRecyclerViewAdapter() {

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