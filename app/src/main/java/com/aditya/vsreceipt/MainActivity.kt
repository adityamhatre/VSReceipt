package com.aditya.vsreceipt

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    val items = listOf("ac", "non_ac", "bun_3", "bun_5", "ac_dorm", "non_ac_dorm", "deluxe", "nivant", "room_6", "room_7", "adult", "child", "other1", "bhujing", "lollipop", "starter", "makingcost", "other2")

    val itemRates = ArrayList<Int>(items.map { i -> 0 })
    val itemCounts = ArrayList<Int>(items.map { i -> 0 })
    val itemTotals = ArrayList<Int>(items.map { i -> 0 })

    val receipt = UUID.randomUUID()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()

        findViewById<TextView>(R.id.share).setOnClickListener {

            hideViews()

            Handler().postDelayed({
                val sv = findViewById<ScrollView>(R.id.receipt);
                val totalHeight: Int = sv.getChildAt(0).height
                val totalWidth: Int = sv.getChildAt(0).width
                val bitmap = getBitmapFromView(sv, totalHeight, totalWidth)
                shareBitmap(bitmap)

                showViews()
            }, 500)


        }
    }


    private fun hideViews() {
        itemTotals.forEachIndexed { i, v ->
            if (v == 0) {
                (findViewById<View>(resources.getIdentifier(items[i], "id", packageName)).parent as View).visibility = View.GONE
            }
        }

        if (itemTotals[10] + itemTotals[11] + itemTotals[12] == 0) {
            findViewById<View>(R.id.food_title).visibility = View.GONE
        }

        if (itemTotals[13] + itemTotals[14] + itemTotals[15] + itemTotals[16] + itemTotals[17] == 0) {
            findViewById<View>(R.id.other_expenses_title).visibility = View.GONE
        }
    }

    private fun showViews() {
        itemTotals.forEachIndexed { i, v ->
            if (v == 0) {
                (findViewById<View>(resources.getIdentifier(items[i], "id", packageName)).parent as View).visibility = View.VISIBLE
            }
        }


        if (itemTotals[8] + itemTotals[9] + itemTotals[10] == 0) {
            findViewById<View>(R.id.food_title).visibility = View.VISIBLE
        }

        if (itemTotals[11] + itemTotals[12] + itemTotals[13] + itemTotals[14] + itemTotals[15] == 0) {
            findViewById<View>(R.id.other_expenses_title).visibility = View.VISIBLE
        }
    }


    private fun setupViews() {
        findViewById<EditText>(R.id.advance1).doAfterTextChanged {
            findViewById<EditText>(R.id.total_amount).setText((itemTotals.reduce { acc, i -> acc + i } - getDiscount()).toString())
        }
        findViewById<EditText>(R.id.discount).doAfterTextChanged {
            findViewById<EditText>(R.id.total_amount).setText((itemTotals.reduce { acc, i -> acc + i } - getDiscount()).toString())
        }
        findViewById<TextView>(R.id.receiptNumber).text = "Receipt no:\n ${receipt}"
        findViewById<TextView>(R.id.date).text = "Date\n" + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDateTime.now())

        items.forEachIndexed { i, item ->

            val rate = resources.getIdentifier(item + "_rate", "id", packageName)
            findViewById<EditText>(rate).doAfterTextChanged {
                val rateString = it?.toString()
                itemRates[i] = if (rateString.isNullOrBlank()) 0 else rateString.toString().toInt()

                setTotals(i, item)
            }

            val count = resources.getIdentifier(item, "id", packageName)
            findViewById<EditText>(count).doAfterTextChanged {
                val countString = it?.toString()
                val countValue = if (countString.isNullOrBlank()) 0 else countString.toString().toInt()
                itemCounts[i] = countValue

                setTotals(i, item)
            }

        }

    }

    private fun getDiscount(): Int {
        val advanceString = findViewById<EditText>(R.id.advance1).text.toString()
        val discountString = findViewById<EditText>(R.id.discount).text.toString()
        val a: Int = if (advanceString.isBlank()) 0 else advanceString.toInt()
        val d: Int = if (discountString.isBlank()) 0 else discountString.toInt()

        return a + d
    }

    private fun setTotals(i: Int, item: String) {
        val itemTotal = itemRates[i] * itemCounts[i]
        itemTotals[i] = itemTotal

        val totalView = resources.getIdentifier(item + "_total", "id", packageName)

        findViewById<EditText>(totalView).setText(itemTotal.toString())
        findViewById<EditText>(R.id.amount1).setText(itemTotals.reduce { acc, i -> acc + i }.toString())
        findViewById<EditText>(R.id.total_amount).setText((itemTotals.reduce { acc, i -> acc + i } - getDiscount()).toString())

    }

    private fun getBitmapFromView(view: View, totalHeight: Int, totalWidth: Int): Bitmap {
        val height = totalHeight.coerceAtMost(totalHeight)
        val percent = height / totalHeight.toFloat()
        val canvasBitmap = Bitmap.createBitmap(
            (totalWidth * percent).toInt(),
            (totalHeight * percent).toInt(), Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(canvasBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas) else canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.scale(percent, percent)
        view.draw(canvas)
        canvas.restore()
        return canvasBitmap
    }


    private fun shareBitmap(bitmap: Bitmap) {
        val cachePath = File(externalCacheDir, "shared_receipts/")
        cachePath.mkdirs()

        //create png file
        val file = File(cachePath, "${receipt}.png")
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        //---Share File---//
        //get file uri
        val myImageFileUri: Uri = FileProvider.getUriForFile(
            applicationContext,
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )

        //create a intent
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, myImageFileUri)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.type = "image/png"
        startActivity(Intent.createChooser(intent, "Share with"))
    }
}