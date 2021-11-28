package com.example.coo_eat

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.Dimension
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_join.*
import kotlinx.android.synthetic.main.scrap.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread

class ScrapActivity : AppCompatActivity() {
    val db = Firebase.firestore
    val TAG: String = "MainActivity : " //log출력을 위한 TAG

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStart() {
        super.onStart()
        setContentView(R.layout.scrap)

        val pref = getSharedPreferences("pref", Context.MODE_PRIVATE)
        val user_email = pref.getString("email", "no email")
        val user_scrap = db.collection("${user_email}").document("scrap")

        CoroutineScope(Dispatchers.Main).launch {
            val pref = getSharedPreferences("pref", Context.MODE_PRIVATE)
            val user_email = pref.getString("email", "no email")
            val user_scrap = db.collection("${user_email}").document("scrap")
            var items = mutableListOf<String>()
            var foodNames = mutableListOf<String>()
            var foodImages = mutableListOf<String>()
            var foodCategories = mutableListOf<String>()

            // 재료 배열로 받아옴
            user_scrap.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val item =
                            document["my"].toString().replace("[", "").replace("]", "").split(", ")
                        for (i in 0..item.size - 1) {
                            items.add(item[i])
                        }
                        Log.d(TAG, "스크랩 레시피 : ${items}")
                    }
                }

            // 사용자가 선택한 재료가 포함된 레시피 정보를 API에서 추출
            CoroutineScope(Dispatchers.IO).async {
                val key: String = "cf8505a99bb545f8882c"
                val url: String =
                    "http://openapi.foodsafetykorea.go.kr/api/" + key + "/COOKRCP01/xml/1/1000"
                val xml: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)

                xml.documentElement.normalize()
                println("Root element : " + xml.documentElement.nodeName)

                val list: NodeList = xml.getElementsByTagName("row")


                for (i in 0..list.length - 1) {
                    var n: Node = list.item(i)
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        val elem = n as Element
                        val map = mutableMapOf<String, String>()

                        for (j in 0..elem.attributes.length - 1) {
                            map.putIfAbsent(
                                elem.attributes.item(j).nodeName,
                                elem.attributes.item(j).nodeValue
                            )
                        }

//                        val ingredients = elem.getElementsByTagName("RCP_PARTS_DTLS").item(0).textContent
//                        val ingredientsArray = ingredients.split(" ")

                        val recipeName = elem.getElementsByTagName("RCP_NM").item(0).textContent

                        if (items.size > 1) {
                            if (recipeName in items) {
                                Log.d(
                                    TAG,
                                    "레시피 정보: ${
                                        elem.getElementsByTagName("RCP_NM").item(0).textContent
                                    }"
                                )
                                foodNames.add(
                                    elem.getElementsByTagName("RCP_NM").item(0).textContent
                                )
                                foodImages.add(
                                    elem.getElementsByTagName("ATT_FILE_NO_MAIN")
                                        .item(0).textContent
                                )
                                foodCategories.add(
                                    elem.getElementsByTagName("RCP_PAT2").item(0).textContent
                                )

                            }

                        }
                    }
                }
            }.await()

            val scrapMainLayout: LinearLayout = findViewById(R.id.scrap_recipe_main)

            for (i in 0..foodNames.size-1) {
                val scrapImage = ImageView(this@ScrapActivity)
                val newButton = TextView(this@ScrapActivity)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )

                val imageLayoutParams = LinearLayout.LayoutParams(
                    900,
                    350,
                )

                imageLayoutParams.gravity = Gravity.CENTER

                layoutParams.setMargins(80,5,0,0)

                scrapImage.layoutParams = imageLayoutParams
                newButton.layoutParams = layoutParams

                scrapImage.setScaleType(ImageView.ScaleType.FIT_XY)


                Glide.with(this@ScrapActivity).load(foodImages[i]).into(scrapImage)

                newButton.setText(foodNames[i])
                newButton.setTextSize(Dimension.SP,15.0f)
                newButton.setTypeface(newButton.typeface, Typeface.BOLD)
                newButton.setTextColor(Color.BLACK)

                scrapMainLayout.addView(scrapImage)
                scrapMainLayout.addView(newButton)
            }
        }



        // 뒤로가기 버튼 클릭
        btn_scrap_back.setOnClickListener {
            val intent = Intent(this, RecipeActivity::class.java)
            startActivity(intent)
        }

        // 추천 레시피 버튼 클릭
        btn_scrap_recipe.setOnClickListener {
            val intent = Intent(this, RecipeActivity::class.java)
            startActivity(intent)
        }
    }

}
