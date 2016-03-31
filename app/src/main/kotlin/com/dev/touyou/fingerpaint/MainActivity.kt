package com.dev.touyou.fingerpaint

import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat

class MainActivity : AppCompatActivity(), View.OnTouchListener {

    var mCanvas: Canvas? = null
    var mPaint: Paint? = null
    var mPath: Path? = null
    var mBitmap: Bitmap? = null
    var mImageView: ImageView? = null
    var mAlertBuilder: AlertDialog.Builder? = null

    var x1: Float? = null
    var y1: Float? = null
    var width: Int? = null
    var height: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mImageView = findViewById(R.id.imageView) as ImageView
        mImageView?.setOnTouchListener(this)
        mPaint = Paint()
        mPaint?.strokeWidth = 5.0f
        mPaint?.style = Paint.Style.STROKE
        mPaint?.strokeJoin = Paint.Join.ROUND
        mPaint?.strokeCap = Paint.Cap.ROUND
        mPath = Path()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (mBitmap == null) {
            width = mImageView?.width
            height = mImageView?.height
            mBitmap = Bitmap.createBitmap(width!!, height!!, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mBitmap)
            mCanvas?.drawColor(Color.WHITE)
            mImageView?.setImageBitmap(mBitmap)
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val x: Float = event?.x!!
        val y: Float = event?.y!!
        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mPath?.reset()
                mPath?.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                mPath?.quadTo(x1!!, y1!!, x, y)
                mCanvas?.drawPath(mPath, mPaint)
            }
        }
        x1 = x
        y1 = y
        mImageView?.setImageBitmap(mBitmap)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val mi = menuInflater
        mi.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_save -> {
                val prefs = getSharedPreferences("FingerPaintPreferences", MODE_PRIVATE)
                var imageNumber = prefs.getInt("ImageNumber", 1)
                var file: File? = null
                val form = DecimalFormat("0000")
                val dirPath = Environment.getExternalStorageDirectory().toString() + "/FingerPaint/"
                val outDir = File(dirPath)
                if (!outDir.exists()) {
                    outDir.mkdir()
                    do {
                        file = File(dirPath+"img"+form.format(imageNumber)+".png")
                        imageNumber++
                    } while(file?.exists()!!)
                    if (writeImage(file!!)) {
                        scanMedia(file?.path!!)
                        val editor = prefs.edit()
                        editor.putInt("imageNumer",imageNumber+1)
                        editor.commit()
                    }
                }
            }
            R.id.menu_open -> {
                val intent = Intent()
                intent.setType("image/*")
                intent.action = Intent.ACTION_PICK
                startActivityForResult(intent, 0)
            }
            R.id.menu_color_change -> {
                val items = resources.getStringArray(R.array.ColorNames)
                val colors = resources.getIntArray(R.array.ColorValues)
                mAlertBuilder = AlertDialog.Builder(this)
                mAlertBuilder?.setTitle(R.string.menu_color_change)
                mAlertBuilder?.setItems(items, DialogInterface.OnClickListener { dialog, item ->
                    mPaint?.color = colors[item]
                })
                mAlertBuilder?.show()
            }
            R.id.menu_new -> {
                mAlertBuilder = AlertDialog.Builder(this)
                mAlertBuilder?.setTitle(R.string.menu_new)
                mAlertBuilder?.setMessage("作業内容が破棄されます。よろしいですか？")
                mAlertBuilder?.setPositiveButton("OK",
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            mCanvas?.drawColor(Color.WHITE)
                            mImageView?.setImageBitmap(mBitmap)
                        })
                mAlertBuilder?.setNegativeButton("キャンセル",
                        DialogInterface.OnClickListener { dialogInterface, i ->
                        })
                mAlertBuilder?.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data.getData()
        try {
            mBitmap = loadImage(uri)
        } catch(e: IOException) {
            e.printStackTrace()
        }
        mCanvas = Canvas(mBitmap)
        mImageView?.setImageBitmap(mBitmap)
    }

    fun loadImage(uri: Uri): Bitmap? {
        var landscape: Boolean = false
        var bm: Bitmap? = null
        val options: BitmapFactory.Options = BitmapFactory.Options()
        val Is = contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(Is, null, options)
        Is.close()
        var oh: Int = options.outHeight
        var ow: Int = options.outWidth
        if (ow > oh) {
            landscape = true
            oh = options.outWidth
            ow = options.outHeight
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = Math.max(ow / width!!, oh / height!!)
        val is2 = contentResolver.openInputStream(uri)
        bm = BitmapFactory.decodeStream(is2, null, options)
        is2.close()
        if (landscape) {
            val matrix = Matrix()
            matrix.setRotate(90.0f)
            bm = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, false)
        }
        bm = Bitmap.createScaledBitmap(bm, width!!, width!! * ((oh as Double) / (ow as Double)) as Int, false)
        val offBitmap = Bitmap.createBitmap(width!!, height!!, Bitmap.Config.ARGB_8888)
        val offCanvas = Canvas(offBitmap)
        offCanvas.drawBitmap(bm, 0.0f, (height!! - bm.height) / 2 as Float, null)
        bm = offBitmap
        return bm
    }

    fun writeImage(file: File): Boolean {
        try {
            val fo = FileOutputStream(file)
            mBitmap?.compress(Bitmap.CompressFormat.PNG, 100, fo)
            fo.flush()
            fo.close()
        } catch(e: Exception) {
            print(e.message)
            return false
        }
        return true
    }
    var mc: MediaScannerConnection? = null
    fun scanMedia(fp: String) {
        val client: MediaScannerConnectionClient = MediaScannerConnectionClient() {
            override fun onScanCompleted(path: String, uri: Uri) {
                mc?.disconnect()
            }
            override fun onMediaScannerConnected() {
                mc?.scanFile(fp, "image/*")
            }
        }
        mc = MediaScannerConnection(this, client)
        mc?.connect()
    }

}
