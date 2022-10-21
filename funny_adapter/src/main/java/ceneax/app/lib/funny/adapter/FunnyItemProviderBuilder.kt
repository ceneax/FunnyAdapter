package ceneax.app.lib.funny.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.reflect.Method

private typealias OldNewItemFun = (oldItem: Any, newItem: Any) -> Boolean

class ItemProviderBuilder<I : Any, VB : ViewBinding>(
    private val coroutineScope: CoroutineScope,
    private val inflateMethod: Method
) {
    private var initFunction: VB.(FunnyAdapter.BindingHolder) -> Unit = {}
    private var bindFunction: suspend VB.(
        CoroutineScope,
        holder: FunnyAdapter.BindingHolder,
        I
    ) -> Unit = { _, _, _ -> }
    private val diffUtilItemCallbackBuilder = DiffUtilItemCallbackBuilder<I>()

    fun init(function: VB.(FunnyAdapter.BindingHolder) -> Unit) {
        initFunction = function
    }

    fun bind(function: suspend VB.(CoroutineScope, FunnyAdapter.BindingHolder, I) -> Unit) {
        bindFunction = function
    }

    fun diffUtil(setup: DiffUtilItemCallbackBuilder<I>.() -> Unit) {
        diffUtilItemCallbackBuilder.setup()
    }

    @Suppress("UNCHECKED_CAST")
    fun build() = object : FunnyAdapter.ItemConfig(
        bindingHolderFactory = { layoutInflater, container, attachToToot ->
            object : FunnyAdapter.BindingHolder(
                inflateMethod.invoke(null, layoutInflater, container, attachToToot) as VB
            ) {
                private var bindJob: kotlinx.coroutines.Job? = null

                init {
                    (binding as VB).initFunction(this)
                }

                override fun bind(item: Any) {
                    val holder = this
                    bindJob = coroutineScope.launch {
                        (binding as VB).bindFunction(this, holder, item as I)
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

@Suppress("UNCHECKED_CAST")
class DiffUtilItemCallbackBuilder<I : Any> {
    private var areItemsTheSameFunction: OldNewItemFun = { oldItem, newItem -> oldItem === newItem }
    private var areContentsTheSameFunction: OldNewItemFun = { oldItem, newItem -> oldItem == newItem }

    fun areItemsTheSame(function: (oldItem: I, newItem: I) -> Boolean) {
        areItemsTheSameFunction = function as OldNewItemFun
    }

    fun areContentsTheSame(function: (oldItem: I, newItem: I) -> Boolean) {
        areContentsTheSameFunction = function as OldNewItemFun
    }

    fun build() = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any) =
            oldItem::class == newItem::class && areItemsTheSameFunction(oldItem, newItem)

        override fun areContentsTheSame(oldItem: Any, newItem: Any) =
            oldItem::class == newItem::class && areContentsTheSameFunction(oldItem, newItem)
    }
}

fun <I : Any, VB : ViewBinding> ItemProviderBuilder<I, VB>.bind(
    function: suspend VB.(FunnyAdapter.BindingHolder, I) -> Unit
) = bind { _, bindingHolder, item ->
    function(bindingHolder, item)
}

fun <I : Any, VB : ViewBinding> ItemProviderBuilder<I, VB>.bind(
    function: suspend VB.(I) -> Unit
) = bind { _, _, item ->
    function(item)
}