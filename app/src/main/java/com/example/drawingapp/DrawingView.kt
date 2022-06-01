package com.example.drawingapp


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context:Context,attrs:AttributeSet):View(context,attrs) {


    private var mDrawPath:CustomPath?=null
    private var mCanvasBitMap:Bitmap?=null
    private var mDrawPaint: Paint?=null
    private var mCanvasPaint:Paint?=null
    private var mBrushSize:Float=0.toFloat()
    private var color=Color.BLACK
    private var canvas: Canvas?=null
    private var mPath = ArrayList<CustomPath>()
    private val undonePaths = ArrayList<CustomPath>()

    init {
        setDrawing()
    }

    private fun setDrawing() {
        mDrawPaint= Paint()
        mDrawPath=CustomPath(color,mBrushSize)
        mDrawPaint!!.color=color
        mDrawPaint!!.style=Paint.Style.STROKE
        mDrawPaint!!.strokeCap=Paint.Cap.ROUND
        mCanvasPaint= Paint(Paint.DITHER_FLAG)
        //mBrushSize=20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitMap= Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitMap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitMap!!,0f,0f,mCanvasPaint)

        for (path in mPath){
            mDrawPaint!!.strokeWidth = path.brushThickNess
            mDrawPaint!!.color = path.color
            canvas.drawPath(path,mDrawPaint!!)
        }


        if (!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickNess
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!,mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN->{
                mDrawPath!!.color=color
                mDrawPath!!.brushThickNess = mBrushSize

                mDrawPath!!.reset()
                mDrawPath !!.moveTo(touchX!!,touchY!!)

            }
            MotionEvent.ACTION_MOVE->{
                mDrawPath!!.lineTo(touchX!!,touchY!!)
            }
            MotionEvent.ACTION_UP->{
                mPath.add(mDrawPath!!)
                mDrawPath=CustomPath(color,mBrushSize)
            }else->return false
        }

        invalidate()
        return true
    }

    fun clear(){
        /*mDrawPath!!.reset()
        canvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mPath.clear()
        invalidate()*/
        if (mPath.size > 0) {
            undonePaths.add(mPath.removeAt(mPath.size - 1))
            invalidate()
        } else {
            Log.d("UNDO_ERROR", "Something went wrong with UNDO action")
        }
    }

    /*fun redoCanvasDrawing() {
        if (undonePaths.size > 0) {
            paths.add(undonePaths.removeAt(undonePaths.size - 1))
            invalidate()
        } else {
            Log.d("REDO_ERROR", "Something went wrong with REDO action")
        }
    }*/
    fun setSizeForBrush(newSize:Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize,resources.displayMetrics)

        mDrawPaint!!.strokeWidth=mBrushSize
    }

    fun setColor(newColor:String){
        color=Color.parseColor(newColor)
        mDrawPaint!!.color=color
    }

    internal inner class CustomPath(var color:Int,var brushThickNess:Float):Path(){

    }

}