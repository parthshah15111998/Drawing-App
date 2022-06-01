package com.example.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.drawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imgBtnCurrentPaint:ImageButton? = null
    private var customProgressDialog:Dialog? = null

    private val requestPermission:ActivityResultLauncher<Array<String>> =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permission ->
                permission.entries.forEach{
                    val permission=it.key
                    val isGranted=it.value

                    if (isGranted){
                        Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG).show()
                        val pickIntent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)

                    }else{
                        if (permission == Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(this,"Permission Denied",Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

    private val openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
            if (result.resultCode == RESULT_OK && result.data != null){
                binding.imgBackground.setImageURI(result.data?.data)
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.drawingView.setSizeForBrush(20.toFloat())

        imgBtnCurrentPaint=binding.llPaintColor[1] as ImageButton
        imgBtnCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed))

        binding.imgBtnBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.imgBtnGallery.setOnClickListener {
            requestStoragePermission()
        }

        binding.imgBtnClear.setOnClickListener {
            binding.drawingView.clear()
        }
        binding.imgBtnSave.setOnClickListener {
            showProgressDialog()
            if (isReadStorageAllowed()){
                lifecycleScope.launch {
                    saveBitmapFile(getBitmapFromView(binding.flDrawingViewContainer))
                }
            }
        }

    }

    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Drawing App","Drawing App"+"needs to Access your External Storage")


        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }


    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size:")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.imgBtn_smallBrush)
        smallBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.imgBtn_mediumBrush)
        mediumBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val medium2Btn = brushDialog.findViewById<ImageButton>(R.id.imgBtn_medium2Brush)
        medium2Btn.setOnClickListener {
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.imgBtn_largeBrush)
        largeBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(40.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view:View){
        if (view !== imgBtnCurrentPaint){
            val imgButton=view as ImageButton
            val colorTag=imgButton.tag.toString()
            binding.drawingView.setColor(colorTag)

            imgButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))

            imgBtnCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_normal))

            imgBtnCurrentPaint =view
        }
    }

    private fun showRationaleDialog(title:String, Message:String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage("Message")
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View):Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes= ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.JPEG,90,bytes)

                   /* val f=File(externalCacheDir?.absoluteFile.toString()+
                    File.separator+"DrawingApp"+ System.currentTimeMillis()/1000+".jpeg")*/
                    val f=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() +
                                File.separator+"DrawingApp"+ System.currentTimeMillis()/1000+".jpeg")
                    val fo=FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File Saved Successfully:$result",Toast.LENGTH_LONG).show()
                            shareImage(f)
                        }else{
                            Toast.makeText(this@MainActivity,"Something went Wrong:$result",Toast.LENGTH_LONG).show()
                        }
                    }

                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custome_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if (customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: File){
        val shareIntent = Intent()
        shareIntent.action=Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Sharing Image")
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
            this,
            "com.example.drawingapp.provider",result))
        shareIntent.type="image/jpeg"
        startActivity(Intent.createChooser(shareIntent,"Share"))

    }

}