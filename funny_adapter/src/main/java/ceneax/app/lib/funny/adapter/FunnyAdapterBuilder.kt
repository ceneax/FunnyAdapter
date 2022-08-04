@file:Suppress("UNCHECKED_CAST")

package ceneax.app.lib.funny.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun RecyclerView.requireFunnyAdapter() = adapter as FunnyAdapter

fun ViewPager2.requireFunnyAdapter() = adapter as FunnyAdapter

fun LifecycleOwner.FunnyAdapter(
    setup: FunnyAdapterBuilder.() -> Unit
) = FunnyAdapter(lifecycleScope, setup)

fun FunnyAdapter(
    coroutineScope: CoroutineScope,
    setup: FunnyAdapterBuilder.() -> Unit
) = FunnyAdapterBuilder(coroutineScope).apply(setup).build()

class FunnyAdapterBuilder(val coroutineScope: CoroutineScope) {
    val adapter = FunnyAdapter()

    inline fun <reified ITEM : Any, VIEW_BINDING : ViewBinding> setup(
        noinline bindingFactory: (LayoutInflater, ViewGroup, Boolean) -> VIEW_BINDING,
        setup: ItemProviderBuilder<ITEM, VIEW_BINDING>.() -> Unit = {}
    ) = adapter.addItemConfig(
        ITEM::class,
        ItemProviderBuilder<ITEM, VIEW_BINDING>(coroutineScope, bindingFactory).apply(setup).build()
    )

    fun build() = adapter
}

class ItemProviderBuilder<ITEM : Any, VIEW_BINDING : ViewBinding>(
    private val coroutineScope: CoroutineScope,
    private val bindingFactory: (LayoutInflater, ViewGroup, Boolean) -> VIEW_BINDING
) {
    private var initFunction: VIEW_BINDING.(FunnyAdapter.BindingHolder) -> Unit = {}
    private var bindFunction: suspend VIEW_BINDING.(CoroutineScope, holder: FunnyAdapter.BindingHolder, ITEM) -> Unit = { _, _, _ -> }
    private val diffUtilItemCallbackBuilder = DiffUtilItemCallbackBuilder<ITEM>()

    fun init(function: VIEW_BINDING.(FunnyAdapter.BindingHolder) -> Unit) {
        initFunction = function
    }

    fun bind(function: suspend VIEW_BINDING.(CoroutineScope, FunnyAdapter.BindingHolder, ITEM) -> Unit) {
        bindFunction = function
    }

    fun diffUtil(setup: DiffUtilItemCallbackBuilder<ITEM>.() -> Unit) {
        diffUtilItemCallbackBuilder.setup()
    }

    fun build() = object : FunnyAdapter.ItemConfig(
        bindingHolderFactory = { layoutInflater, container, attachToToot ->
            object : FunnyAdapter.BindingHolder(bindingFactory(layoutInflater, container, attachToToot)) {
                private var bindJob: kotlinx.coroutines.Job? = null

                init {
                    (binding as VIEW_BINDING).initFunction(this)
                }

                override fun bind(item: Any) {
                    val holder = this
                    bindJob = coroutineScope.launch {
                        (binding as VIEW_BINDING).bindFunction(
                            this,
                            holder,
                            item as ITEM
                        )
                    }
                }

                override fun recycle() {
                    bindJob?.cancel()
                    bindJob = null
                }
            }
        },
        diffUtilItemCallback = diffUtilItemCallbackBuilder.build()
    ) {}
}

class DiffUtilItemCallbackBuilder<ITEM : Any> {
    private var areItemsTheSameFunction: (oldItem: Any, newItem: Any) -> Boolean = { oldItem, newItem -> oldItem == newItem }
    private var areContentsTheSameFunction: (oldItem: Any, newItem: Any) -> Boolean = { oldItem, newItem -> oldItem == newItem }

    fun areItemsTheSame(function: (oldItem: ITEM, newItem: ITEM) -> Boolean) {
        areItemsTheSameFunction = function as (Any, Any) -> Boolean
    }

    fun areContentsTheSame(function: (oldItem: ITEM, newItem: ITEM) -> Boolean) {
        areContentsTheSameFunction = function as (Any, Any) -> Boolean
    }

    fun build() = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any) =
            oldItem::class == newItem::class && areItemsTheSameFunction(oldItem, newItem)

        override fun areContentsTheSame(oldItem: Any, newItem: Any) =
            oldItem::class == newItem::class && areContentsTheSameFunction(oldItem, newItem)
    }
}

fun <ITEM : Any, VIEW_BINDING : ViewBinding> ItemProviderBuilder<ITEM, VIEW_BINDING>.bind(
    function: suspend VIEW_BINDING.(FunnyAdapter.BindingHolder, ITEM) -> Unit
) = bind { _, bindingHolder, item ->
    function(bindingHolder, item)
}

fun <ITEM : Any, VIEW_BINDING : ViewBinding> ItemProviderBuilder<ITEM, VIEW_BINDING>.bind(
    function: suspend VIEW_BINDING.(ITEM) -> Unit
) = bind { _, _, item ->
    function(item)
}