package kim.uno.mock.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

fun RecyclerView.addInitializationAnimator(animator: RecyclerViewAnimator) {
    viewTreeObserver.addOnPreDrawListener(
        object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (childCount == 0) return false
                viewTreeObserver.removeOnPreDrawListener(this)

                val animators = ArrayList<Animator>()
                children.forEachIndexed { index, view ->
                    animators.add(
                        animator.onAnimationCreate(
                            recyclerView = this@addInitializationAnimator,
                            view = view,
                            index = index
                        )
                    )
                }

                AnimatorSet().apply {
                    playTogether(animators)
                    doOnStart { animator.onAnimationStart() }
                    doOnEnd { animator.onAnimationEnd() }
                    start()
                }
                return true
            }
        })

    object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator?) {
            super.onAnimationStart(animation)
        }
    }
}

interface RecyclerViewAnimator {
    fun onAnimationCreate(recyclerView: RecyclerView, view: View, index: Int): Animator
    fun onAnimationStart()
    fun onAnimationEnd()
}

abstract class RecyclerViewAnimatorAdapter : RecyclerViewAnimator {
    abstract override fun onAnimationCreate(
        recyclerView: RecyclerView,
        view: View,
        index: Int
    ): Animator

    override fun onAnimationStart() {}
    override fun onAnimationEnd() {}
}

val RecyclerView.isVertically: Boolean
    get() = when (val layoutManager = layoutManager) {
        is LinearLayoutManager -> layoutManager.orientation == LinearLayoutManager.VERTICAL
        is StaggeredGridLayoutManager -> layoutManager.orientation == StaggeredGridLayoutManager.VERTICAL
        else -> true
    }

class DefaultRecyclerViewAnimator(
    private val translation: Float = 20f.toPixel(),
    private val duration: Long = 300L,
    private val gap: Long = 50L
) : RecyclerViewAnimatorAdapter() {
    override fun onAnimationCreate(recyclerView: RecyclerView, view: View, index: Int): Animator {
        view.alpha = 0f
        if (recyclerView.isVertically) {
            view.translationY = translation
        } else {
            view.translationX = translation
        }
        return AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, 0f),
            )
            duration = this@DefaultRecyclerViewAnimator.duration
            startDelay = this@DefaultRecyclerViewAnimator.gap * index
            interpolator = DecelerateInterpolator()
        }
    }
}
