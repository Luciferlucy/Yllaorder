package maister.a.yllaorder.adapter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import maister.a.yllaorder.R
import maister.a.yllaorder.activity.MainActivity
import maister.a.yllaorder.fragment.FullScreenViewFragment
import maister.a.yllaorder.fragment.ProductDetailFragment
import maister.a.yllaorder.fragment.SubCategoryFragment
import maister.a.yllaorder.helper.Constant
import maister.a.yllaorder.model.Slider

class SliderAdapter(
    private val dataList: ArrayList<Slider>,
    val activity: Activity,
    val layout: Int,
    val from: String
) : PagerAdapter() {
    override fun instantiateItem(view: ViewGroup, position: Int): Any {
        val imageLayout = LayoutInflater.from(activity).inflate(layout, view, false)
        val imgSlider = imageLayout.findViewById<ImageView>(R.id.imgSlider)
        val lytMain: CardView = imageLayout.findViewById(R.id.lytMain)
        val singleItem = dataList[position]
        Glide.with(activity)
            .load(if (singleItem.image == "") "-" else singleItem.image)
            .centerInside()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(imgSlider)
        view.addView(imageLayout, 0)
        lytMain.setOnClickListener {
            if (from.equals("detail", ignoreCase = true)) {
                val fragment: Fragment = FullScreenViewFragment()
                val bundle = Bundle()
                bundle.putInt("pos", position)
                bundle.putSerializable("images", dataList)
                fragment.arguments = bundle
                MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            } else {
                when (singleItem.type) {
                    "category" -> {
                        val fragment: Fragment = SubCategoryFragment()
                        val bundle = Bundle()
                        bundle.putString(Constant.ID, singleItem.type_id)
                        bundle.putString(Constant.NAME, singleItem.name)
                        bundle.putString(Constant.FROM, "category")
                        fragment.arguments = bundle
                        MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                            .addToBackStack(null).commit()
                    }
                    "product" -> {
                        val fragment: Fragment = ProductDetailFragment()
                        val bundle = Bundle()
                        bundle.putString(Constant.ID, singleItem.type_id)
                        bundle.putString(Constant.FROM, "slider")
                        bundle.putInt("variantsPosition", 0)
                        fragment.arguments = bundle
                        MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                            .addToBackStack(null).commit()
                    }
                    "slider_url" -> {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(singleItem.slider_url))
                        activity.startActivity(browserIntent)
                    }
                }
            }
        }
        return imageLayout
    }

    override fun getCount(): Int {
        return dataList.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, jsonObject: Any) {
        container.removeView(jsonObject as View)
    }

    override fun isViewFromObject(view: View, jsonObject: Any): Boolean {
        return view == jsonObject
    }

}