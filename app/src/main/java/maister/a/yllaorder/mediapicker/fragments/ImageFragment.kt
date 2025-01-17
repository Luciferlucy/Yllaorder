package maister.a.yllaorder.com.coursion.freakycoder.mediapicker.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import maister.a.yllaorder.com.coursion.freakycoder.mediapicker.adapters.BucketsAdapter
import maister.a.yllaorder.com.coursion.freakycoder.mediapicker.galleries.OpenGallery
import maister.a.yllaorder.R
import java.io.File

class ImageFragment : androidx.fragment.app.Fragment() {

    companion object {
        var imagesList: MutableList<String> = ArrayList()
        var selected: MutableList<Boolean> = ArrayList()
    }

    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private var mAdapter: BucketsAdapter? = null
    private val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA)
    private val projection2 = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA)
    private var bucketNames: MutableList<String> = ArrayList()
    private val bitmapList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bucket names reloaded
        bitmapList.clear()
        imagesList.clear()
        bucketNames.clear()
        getPicBuckets()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.multi_select_fragment_image, container, false)
        recyclerView = v.findViewById(R.id.recyclerView)
        populateRecyclerView()
        return v
    }

    private fun populateRecyclerView() {
        val mLayoutManager = androidx.recyclerview.widget.GridLayoutManager(this.requireContext(), 3)
        recyclerView.layoutManager = mLayoutManager
        mAdapter = BucketsAdapter(bucketNames, bitmapList, this.requireContext())
        recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        recyclerView.adapter = mAdapter
        recyclerView.addOnItemTouchListener(RecyclerTouchListener(this.requireContext(), recyclerView, object : ClickListener {
            override fun onClick(view: View, position: Int) {
                getPictures(bucketNames[position])
                val intent = Intent(context, OpenGallery::class.java)
                intent.putExtra("FROM", "Images")
                startActivity(intent)
            }

            override fun onLongClick(view: View?, position: Int) {

            }
        }))
        mAdapter!!.notifyDataSetChanged()
    }

    private fun getPicBuckets() {
        val cursor = requireContext().contentResolver
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_ADDED)
        val bucketNamesTEMP = ArrayList<String>(cursor!!.count)
        val bitmapListTEMP = ArrayList<String>(cursor.count)
        val albumSet = HashSet<String>()
        var file: File
        if (cursor.moveToLast()) {
            do {
                if (Thread.interrupted()) {
                    return
                }
                val album = cursor.getString(cursor.getColumnIndex(projection[0]))
                val image = cursor.getString(cursor.getColumnIndex(projection[1]))
                file = File(image)
                if (file.exists() && !albumSet.contains(album)) {
                    bucketNamesTEMP.add(album)
                    bitmapListTEMP.add(image)
                    albumSet.add(album)
                }
            } while (cursor.moveToPrevious())
        }
        cursor.close()
        if (bucketNamesTEMP.isNotEmpty()) {
            bucketNames = ArrayList()
        }
        bucketNames.clear()
        bitmapList.clear()
        bucketNames.addAll(bucketNamesTEMP)
        bitmapList.addAll(bitmapListTEMP)
    }

    fun getPictures(bucket: String) {
        selected.clear()
        val cursor = requireContext().contentResolver
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection2,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " =?",
                        arrayOf(bucket),
                        MediaStore.Images.Media.DATE_ADDED)
        var imagesTEMP: ArrayList<String>? = ArrayList(cursor!!.count)
        val albumSet = HashSet<String>()
        var file: File
        if (cursor.moveToLast()) {
            do {
                if (Thread.interrupted()) {
                    return
                }
                val path = cursor.getString(cursor.getColumnIndex(projection2[1]))
                file = File(path)
                if (file.exists() && !albumSet.contains(path)) {
                    imagesTEMP!!.add(path)
                    albumSet.add(path)
                    selected.add(false)
                }
            } while (cursor.moveToPrevious())
        }
        cursor.close()
        if (imagesTEMP == null) {
            imagesTEMP = ArrayList()
        }
        imagesList.clear()
        imagesList.addAll(imagesTEMP)
    }

    interface ClickListener {
        fun onClick(view: View, position: Int)
        fun onLongClick(view: View?, position: Int)
    }

    class RecyclerTouchListener(context: Context, recyclerView: androidx.recyclerview.widget.RecyclerView,
                                private val clickListener: ClickListener?) :
            androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {
        private val gestureDetector: GestureDetector

        init {
            gestureDetector = GestureDetector(context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapUp(e: MotionEvent): Boolean {
                            return true
                        }

                        override fun onLongPress(e: MotionEvent) {
                            val child = recyclerView.findChildViewUnder(e.x, e.y)
                            if (child != null && clickListener != null) {
                                clickListener.onLongClick(child,
                                        recyclerView.getChildAdapterPosition(child))
                            }
                        }
                    })
        }

        override fun onInterceptTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent): Boolean {
            val child = rv.findChildViewUnder(e.x, e.y)
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child))
            }
            return false
        }

        override fun onTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent) {}

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

        }
    }
}



