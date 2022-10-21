package ceneax.app.lib.funny.adapter

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass

class FunnyAdapter : RecyclerView.Adapter<FunnyAdapter.BindingHolder>() {
    private var mListItem = emptyList<Any>()
    private val mMapItemConfig = SparseArray<ItemConfig>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BindingHolder = getItemConfig(viewType).bindingHolderFactory(
        LayoutInflater.from(parent.context), parent, false
    )

    override fun onBindViewHolder(
        holder: BindingHolder,
        position: Int
    ) = holder.bind(mListItem[position])

    override fun onViewRecycled(holder: BindingHolder) = holder.recycle()

    override fun getItemCount(): Int = mListItem.size

    override fun getItemViewType(position: Int): Int = getItemViewType(mListItem[position])

    private fun getItemViewType(item: Any) = item::class.hashCode()

    private fun getItemConfig(viewType: Int) = mMapItemConfig[viewType]

    private fun getItemConfig(item: Any) = getItemConfig(getItemViewType(item))

    fun <I : Any> addItemConfig(itemClass: KClass<I>, itemConfig: ItemConfig) {
        mMapItemConfig[itemClass.hashCode()] = itemConfig
    }

    fun updateList(newList: List<Any>) {
        val diffResult = DiffUtil.calculateDiff(DiffCallback(mListItem, newList))
        mListItem = newList
        diffResult.dispatchUpdatesTo(this)
    }

    abstract class ItemConfig(
        val bindingHolderFactory: (LayoutInflater, ViewGroup, Boolean) -> BindingHolder,
        val diffUtilItemCallback: DiffUtil.ItemCallback<Any>
    )

    abstract class BindingHolder(protected val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(item: Any)
        abstract fun recycle()
    }

    private inner class DiffCallback(
        private val oldList: List<Any>,
        private val newList: List<Any>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ) = getItemConfig(newList[newItemPosition]).diffUtilItemCallback.areItemsTheSame(
            oldList[oldItemPosition],
            newList[newItemPosition]
        )

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ) = getItemConfig(newList[newItemPosition]).diffUtilItemCallback.areContentsTheSame(
            oldList[oldItemPosition],
            newList[newItemPosition]
        )

        override fun getChangePayload(
            oldItemPosition: Int,
            newItemPosition: Int
        ) = getItemConfig(newList[newItemPosition]).diffUtilItemCallback.getChangePayload(
            oldList[oldItemPosition],
            newList[newItemPosition]
        )

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size
    }
}