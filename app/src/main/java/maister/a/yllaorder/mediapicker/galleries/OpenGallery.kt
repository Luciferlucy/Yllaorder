package maister.a.yllaorder.com.coursion.freakycoder.mediapicker.galleries

import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import maister.a.yllaorder.R
import maister.a.yllaorder.com.coursion.freakycoder.mediapicker.adapters.MediaAdapter
import maister.a.yllaorder.com.coursion.freakycoder.mediapicker.fragments.ImageFragment
import maister.a.yllaorder.com.coursion.freakycoder.mediapicker.fragments.VideoFragment
import maister.a.yllaorder.com.coursion.freakycoder.mediapicker.helper.Util

open class OpenGallery : AppCompatActivity() {


    open lateinit var fab: FloatingActionButton
    open lateinit var toolbar: Toolbar
    open lateinit var recyclerView: RecyclerView

    companion object {
        var selected: MutableList<Boolean> = ArrayList()
        var imagesSelected = ArrayList<String>()
    }

    var parent: String? = null
    private var mAdapter: MediaAdapter? = null
    private val mediaList = ArrayList<String>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.multi_select_activity_open_gallery)
        fab = findViewById(R.id.fab)
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        setSupportActionBar(toolbar)
        val fab = findViewById<View>(R.id.fab) as FloatingActionButton
        val util = Util()
        util.setButtonTint(fab, ContextCompat.getColorStateList(applicationContext, R.color.colorPrimary)!!)
        fab.setOnClickListener { finish() }
        
        title = Gallery.title
        if (imagesSelected.size > 0) {
            title = imagesSelected.size.toString()
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
        parent = intent.extras!!.getString("FROM")
        mediaList.clear()
        selected.clear()
        if (parent == "Images") {
            mediaList.addAll(ImageFragment.imagesList)
            selected.addAll(ImageFragment.selected)
        } else {
            mediaList.addAll(VideoFragment.videosList)
            selected.addAll(VideoFragment.selected)
        }
        populateRecyclerView()
    }

    private fun populateRecyclerView() {
        for (i in selected.indices) {
            selected[i] = imagesSelected.contains(mediaList[i])
        }
        mAdapter = MediaAdapter(mediaList, selected, applicationContext)
        val mLayoutManager = androidx.recyclerview.widget.GridLayoutManager(applicationContext, 3)
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator?.changeDuration = 0
        recyclerView.adapter = mAdapter
        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this, recyclerView, object : ClickListener {
            override fun onClick(view: View, position: Int) {
                if (!selected[position] && imagesSelected.size < Gallery.maxSelection) {
                    imagesSelected.add(mediaList[position])
                    selected[position] = !selected[position]
                    mAdapter!!.notifyItemChanged(position)
                } else if (selected[position]) {
                    if (imagesSelected.indexOf(mediaList[position]) != -1) {
                        imagesSelected.removeAt(imagesSelected.indexOf(mediaList[position]))
                        selected[position] = !selected[position]
                        mAdapter!!.notifyItemChanged(position)
                    }
                }
                Gallery.selectionTitle = imagesSelected.size
                if (imagesSelected.size != 0) {
                    title = imagesSelected.size.toString()
                } else {
                    title = Gallery.title
                }
            }

            override fun onLongClick(view: View?, position: Int) {

            }

        }))
    }

    interface ClickListener {
        fun onClick(view: View, position: Int)

        fun onLongClick(view: View?, position: Int)
    }

    class RecyclerTouchListener(context: Context, recyclerView: RecyclerView, private val clickListener: ClickListener?) : RecyclerView.OnItemTouchListener {
        private val gestureDetector: GestureDetector

        init {
            gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    val child = recyclerView.findChildViewUnder(e.x, e.y)
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child))
                    }
                }
            })
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            val child = rv.findChildViewUnder(e.x, e.y)
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child))
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    }

}

