package ceneax.app.lib.funny.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope

val RecyclerView.funnyAdapter get() = adapter as FunnyAdapter

inline fun <reified I : Any, reified VB : ViewBinding> RecyclerView.FunnyAdapter(
    nestedScrollingEnabled: Boolean = true,
    layoutManager: RecyclerView.LayoutManager? = null,
    builder: ItemProviderBuilder<I, VB>.() -> Unit
) = FunnyAdapter(nestedScrollingEnabled, layoutManager) {
    add(builder)
}

@JvmName("FunnyAdapterRecyclerView")
inline fun RecyclerView.FunnyAdapter(
    nestedScrollingEnabled: Boolean = true,
    layoutManager: RecyclerView.LayoutManager? = null,
    builder: FunnyAdapterBuilder.() -> Unit
): FunnyAdapter {
    isNestedScrollingEnabled = nestedScrollingEnabled
    if (this.layoutManager == null && layoutManager == null) {
        this.layoutManager = LinearLayoutManager(context)
    } else if (this.layoutManager == null) {
        this.layoutManager = layoutManager
    }
    adapter = (this.context as LifecycleOwner).FunnyAdapter(builder)
    return funnyAdapter
}

@JvmName("FunnyAdapterSingle")
inline fun <reified I : Any, reified VB : ViewBinding> LifecycleOwner.FunnyAdapter(
    builder: ItemProviderBuilder<I, VB>.() -> Unit
) = FunnyAdapter(lifecycleScope, builder)

@JvmName("FunnyAdapterSingle")
inline fun <reified I : Any, reified VB : ViewBinding> FunnyAdapter(
    scope: CoroutineScope,
    builder: ItemProviderBuilder<I, VB>.() -> Unit
) = FunnyAdapter(scope) {
    add(builder)
}

inline fun LifecycleOwner.FunnyAdapter(
    builder: FunnyAdapterBuilder.() -> Unit
) = FunnyAdapter(lifecycleScope, builder)

inline fun FunnyAdapter(
    scope: CoroutineScope,
    builder: FunnyAdapterBuilder.() -> Unit
) = FunnyAdapterBuilder(scope).apply(builder).build()

class FunnyAdapterBuilder(val scope: CoroutineScope) {
    val adapter = FunnyAdapter()

    fun build() = adapter

    inline fun <reified I : Any, reified VB : ViewBinding> add(
        block: ItemProviderBuilder<I, VB>.() -> Unit
    ) {
        val inflateMethod = VB::class.java.getDeclaredMethod(
            "inflate",
            LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
        )
        return adapter.addItemConfig(
            I::class,
            ItemProviderBuilder<I, VB>(scope, inflateMethod).apply(block).build()
        )
    }
}