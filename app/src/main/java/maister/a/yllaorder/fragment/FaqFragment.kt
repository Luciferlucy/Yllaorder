package maister.a.yllaorder.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import maister.a.yllaorder.R
import maister.a.yllaorder.adapter.FaqAdapter
import maister.a.yllaorder.databinding.FragmentFaqBinding
import maister.a.yllaorder.helper.ApiConfig.Companion.requestToVolley
import maister.a.yllaorder.helper.Constant
import maister.a.yllaorder.helper.Session
import maister.a.yllaorder.helper.VolleyCallback
import maister.a.yllaorder.model.Faq

class FaqFragment : Fragment() {
    lateinit var binding: FragmentFaqBinding
    lateinit var root: View
    private lateinit var faqs: ArrayList<Faq?>
    private lateinit var faqAdapter: FaqAdapter
    lateinit var activity: Activity
    lateinit var session: Session
    var total = 0
    var offset = 0
    var isLoadMore = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_faq, container, false)

        binding = FragmentFaqBinding.inflate(inflater,container,false)

        activity = requireActivity()
        session = Session(activity)

        setHasOptionsMenu(true)
        faqData()
        binding.swipeLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeLayout.setOnRefreshListener {
            binding.swipeLayout.isRefreshing = false
            offset = 0
            faqData()
        }
        return binding.root
    }

    private fun faqData(){
        binding.recyclerView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
            faqs = ArrayList()
            val linearLayoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = linearLayoutManager
            val params: MutableMap<String, String> = HashMap()
            params[Constant.GET_FAQS] = Constant.GetVal
            params[Constant.OFFSET] = "" + offset
            params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT + 10
            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject = JSONObject(response)
                            if (!jsonObject.getBoolean(Constant.ERROR)) {
                                total = jsonObject.getString(Constant.TOTAL).toInt()
                                session.setData(Constant.TOTAL, total.toString())
                                val `object` = JSONObject(response)
                                val jsonArray = `object`.getJSONArray(Constant.DATA)

                                for (i in 0 until jsonArray.length()) {
                                    val jsonObject1 = jsonArray.getJSONObject(i)
                                    if (jsonObject1 != null) {
                                        val faq =
                                            Gson().fromJson(jsonObject1.toString(), Faq::class.java)
                                        faqs.add(faq)
                                    } else {
                                        break
                                    }
                                }
                                if (offset == 0) {
                                    faqAdapter = FaqAdapter(activity, faqs)
                                    faqAdapter.setHasStableIds(true)
                                    binding.recyclerView.adapter = faqAdapter
                                    binding.shimmerFrameLayout.stopShimmer()
                                    binding.shimmerFrameLayout.visibility = View.GONE
                                    binding.recyclerView.visibility = View.VISIBLE
                                    binding.scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                        // if (diff == 0) {
                                        if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                            val linearLayoutManager1 =
                                                    binding.recyclerView.layoutManager as LinearLayoutManager?
                                            if (faqs.size < total) {
                                                if (!isLoadMore) {
                                                    if (linearLayoutManager1 != null && linearLayoutManager1.findLastCompletelyVisibleItemPosition() == faqs.size - 1) {
                                                        //bottom of list!
                                                        offset += Constant.LOAD_ITEM_LIMIT
                                                        val params1: MutableMap<String, String> =
                                                            HashMap()
                                                        params1[Constant.GET_FAQS] = Constant.GetVal
                                                        params1[Constant.OFFSET] = "" + offset
                                                        params1[Constant.LIMIT] =
                                                            "" + Constant.LOAD_ITEM_LIMIT + 10
                                                        requestToVolley(
                                                            object : VolleyCallback {
                                                                override fun onSuccess(
                                                                    result: Boolean,
                                                                    response: String
                                                                ) {
                                                                    if (result) {
                                                                        try {

                                                                            val jsonObject1 =
                                                                                JSONObject(response)
                                                                            if (!jsonObject1.getBoolean(
                                                                                    Constant.ERROR
                                                                                )
                                                                            ) {
                                                                                session.setData(
                                                                                    Constant.TOTAL,
                                                                                    jsonObject1.getString(
                                                                                        Constant.TOTAL
                                                                                    )
                                                                                )

                                                                                val object1 =
                                                                                    JSONObject(
                                                                                        response
                                                                                    )
                                                                                val jsonArray1 =
                                                                                    object1.getJSONArray(
                                                                                        Constant.DATA
                                                                                    )
                                                                                val g1 = Gson()
                                                                                for (i in 0 until jsonArray1.length()) {
                                                                                    val jsonObject2 =
                                                                                        jsonArray1.getJSONObject(
                                                                                            i
                                                                                        )
                                                                                    if (jsonObject2 != null) {
                                                                                        val faq =
                                                                                            g1.fromJson(
                                                                                                jsonObject2.toString(),
                                                                                                Faq::class.java
                                                                                            )
                                                                                        faqs.add(faq)
                                                                                    } else {
                                                                                        break
                                                                                    }
                                                                                }
                                                                                faqAdapter.notifyDataSetChanged()
                                                                                faqAdapter.setLoaded()
                                                                                isLoadMore = false
                                                                            }
                                                                        } catch (e: JSONException) {
                                                                            e.printStackTrace()
                                                                            binding.shimmerFrameLayout.stopShimmer()
                                                                            binding.shimmerFrameLayout.visibility =
                                                                                View.GONE
                                                                            binding.recyclerView.visibility =
                                                                                View.VISIBLE
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            activity,
                                                            Constant.FAQ_URL,
                                                            params1,
                                                            false
                                                        )
                                                    }
                                                    isLoadMore = true
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                binding.recyclerView.visibility = View.GONE
                                binding.tvAlert.visibility = View.VISIBLE
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                        }
                    }
                }
            }, activity, Constant.FAQ_URL, params, false)
        }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.faq)
        activity.invalidateOptionsMenu()
        hideKeyboard()
    }

    fun hideKeyboard() {
        try {
            val inputMethodManager =
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            inputMethodManager.hideSoftInputFromWindow(root.applicationWindowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
    }
}