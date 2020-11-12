package com.kproduce.roundcorners.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.kproduce.roundcorners.R;

/**
 * <p>Author：     zenglq<p>
 * <p>Email：      380233376@qq.com<p>
 * <p>Date：       2020/11/11<p>
 * <p>Description：<p>
 */
public class CutHelper {
    private Context mContext;
    private View mView;

    private Paint mPaint;
    private RectF mRectF;
    private RectF mStrokeRectF;

    private Path mPath;
    private Path mTempPath;

    private Xfermode mXfermode;

    private float[] mRadii;
    private float[] mStrokeRadii;

    private int mWidth;
    private int mHeight;
    private int mStrokeColor;
    private float mStrokeWidth;

    private float mCutTopLeft;
    private float mCutTopRight;
    private float mCutBottomLeft;
    private float mCutBottomRight;

    private float cutMax;

    public void init(Context context, AttributeSet attrs, View view) {
        if (view instanceof ViewGroup && view.getBackground() == null) {
            view.setBackgroundColor(Color.parseColor("#00000000"));
        }
        mContext = context;
        mView = view;
        mRadii = new float[8];
        mStrokeRadii = new float[8];
        mPaint = new Paint();
        mRectF = new RectF();
        mStrokeRectF = new RectF();
        mPath = new Path();
        mTempPath = new Path();
        mXfermode = new PorterDuffXfermode(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PorterDuff.Mode.DST_OUT : PorterDuff.Mode.DST_IN);
        mStrokeColor = Color.WHITE;

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CutCorner);
        if (array == null) {
            return;
        }
        float radius = array.getDimension(R.styleable.CutCorner_rRadius, 0);
        float radiusLeft = array.getDimension(R.styleable.CutCorner_rLeftRadius, radius);
        float radiusRight = array.getDimension(R.styleable.CutCorner_rRightRadius, radius);
        float radiusTop = array.getDimension(R.styleable.CutCorner_rTopRadius, radius);
        float radiusBottom = array.getDimension(R.styleable.CutCorner_rBottomRadius, radius);

        //四个切角必须要一致，不一致取最大
        mCutTopLeft = array.getDimension(R.styleable.CutCorner_rTopLeftRadius, radiusTop > 0 ? radiusTop : radiusLeft);
        mCutTopRight = array.getDimension(R.styleable.CutCorner_rTopRightRadius, radiusTop > 0 ? radiusTop : radiusRight);
        mCutBottomLeft = array.getDimension(R.styleable.CutCorner_rBottomLeftRadius, radiusBottom > 0 ? radiusBottom : radiusLeft);
        mCutBottomRight = array.getDimension(R.styleable.CutCorner_rBottomRightRadius, radiusBottom > 0 ? radiusBottom : radiusRight);

        cutMax = Math.max(cutMax, mCutTopLeft);
        cutMax = Math.max(cutMax, mCutTopRight);
        cutMax = Math.max(cutMax, mCutBottomLeft);
        cutMax = Math.max(cutMax, mCutBottomRight);

        mStrokeWidth = array.getDimension(R.styleable.CutCorner_rStrokeWidth, 0);
        mStrokeColor = array.getColor(R.styleable.CutCorner_rStrokeColor, mStrokeColor);

        array.recycle();
    }

    private void setRadius() {
        mRadii[0] = mRadii[1] = mCutTopLeft - mStrokeWidth;
        mRadii[2] = mRadii[3] = mCutTopRight - mStrokeWidth;
        mRadii[4] = mRadii[5] = mCutBottomRight - mStrokeWidth;
        mRadii[6] = mRadii[7] = mCutBottomLeft - mStrokeWidth;

        mStrokeRadii[0] = mStrokeRadii[1] = mCutTopLeft;
        mStrokeRadii[2] = mStrokeRadii[3] = mCutTopRight;
        mStrokeRadii[4] = mStrokeRadii[5] = mCutBottomRight;
        mStrokeRadii[6] = mStrokeRadii[7] = mCutBottomLeft;
    }

    public void onSizeChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        //控件大小
        if (mRectF != null) {
            mRectF.set(0, 0, width, height);
        }
        //边框大小
        if (mStrokeRectF != null) {
            mStrokeRectF.set((mStrokeWidth / 2), (mStrokeWidth / 2), width - mStrokeWidth / 2, height - mStrokeWidth / 2);
        }
    }

    public void preDraw(Canvas canvas) {
        canvas.saveLayer(mRectF, null, Canvas.ALL_SAVE_FLAG);
        if (mStrokeWidth > 0) {
            float sx = (mWidth - 2 * mStrokeWidth) / mWidth;
            float sy = (mHeight - 2 * mStrokeWidth) / mHeight;
            // 缩小画布，使图片内容不被borders覆盖
            canvas.scale(sx, sy, mWidth / 2.0f, mHeight / 2.0f);
        }
    }

    public void drawPath(Canvas canvas) {
        mPaint.reset();
        mPath.reset();

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setXfermode(mXfermode);

        buildCutPath(1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mTempPath.reset();
            mTempPath.addRect(mRectF, Path.Direction.CCW);
            mTempPath.op(mPath, Path.Op.DIFFERENCE);
            canvas.drawPath(mTempPath, mPaint);
        } else {
            canvas.drawPath(mPath, mPaint);
        }
        mPaint.setXfermode(null);

        // draw stroke
        if (mStrokeWidth > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeWidth);
            mPaint.setColor(mStrokeColor);

            mPath.reset();
            buildCutPath(mStrokeWidth / 2);
            canvas.drawPath(mPath, mPaint);
        }
    }

    private void buildCutPath(float strokeRadii) {
        //左上
        if(mCutTopLeft != 0) {
            mPath.moveTo(mRectF.left - strokeRadii, mRectF.top + cutMax - strokeRadii);
            mPath.lineTo(mRectF.left + cutMax - strokeRadii, mRectF.top - strokeRadii);
        } else {
            mPath.moveTo(mRectF.left - strokeRadii, mRectF.top - strokeRadii);
        }

        //右上
        if(mCutTopRight != 0) {
            mPath.lineTo(mRectF.right - cutMax + strokeRadii, mRectF.top - strokeRadii);
            mPath.lineTo(mRectF.right + strokeRadii, mRectF.top + cutMax - strokeRadii);
        } else {
            mPath.lineTo(mRectF.right + strokeRadii, mRectF.top - strokeRadii);
        }

        //右下
        if(mCutBottomRight != 0) {
            mPath.lineTo(mRectF.right + strokeRadii, mRectF.bottom - cutMax + strokeRadii);
            mPath.lineTo(mRectF.right - cutMax + strokeRadii, mRectF.bottom + strokeRadii);
        } else {
            mPath.lineTo(mRectF.right + strokeRadii, mRectF.bottom  + strokeRadii);
        }

        //左下
        if(mCutBottomLeft != 0) {
            mPath.lineTo(mRectF.left + cutMax - strokeRadii, mRectF.bottom + strokeRadii);
            mPath.lineTo(mRectF.left - strokeRadii, mRectF.bottom - cutMax + strokeRadii);
        } else {
            mPath.lineTo(mRectF.left - strokeRadii, mRectF.bottom + strokeRadii);
        }

        mPath.close();
    }

    public void setRadius(float radiusDp) {
        if (mContext == null) {
            return;
        }
        float radiusPx = DensityUtil.dip2px(mContext, radiusDp);
        mCutTopLeft = radiusPx;
        mCutTopRight = radiusPx;
        mCutBottomLeft = radiusPx;
        mCutBottomRight = radiusPx;
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadius(float radiusTopLeftDp, float radiusTopRightDp, float radiusBottomLeftDp, float radiusBottomRightDp) {
        if (mContext == null) {
            return;
        }
        mCutTopLeft = DensityUtil.dip2px(mContext, radiusTopLeftDp);
        mCutTopRight = DensityUtil.dip2px(mContext, radiusTopRightDp);
        mCutBottomLeft = DensityUtil.dip2px(mContext, radiusBottomLeftDp);
        mCutBottomRight = DensityUtil.dip2px(mContext, radiusBottomRightDp);
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusLeft(float radiusDp) {
        if (mContext == null) {
            return;
        }
        float radiusPx = DensityUtil.dip2px(mContext, radiusDp);
        mCutTopLeft = radiusPx;
        mCutBottomLeft = radiusPx;
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusRight(float radiusDp) {
        if (mContext == null) {
            return;
        }
        float radiusPx = DensityUtil.dip2px(mContext, radiusDp);
        mCutTopRight = radiusPx;
        mCutBottomRight = radiusPx;
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusTop(float radiusDp) {
        if (mContext == null) {
            return;
        }
        float radiusPx = DensityUtil.dip2px(mContext, radiusDp);
        mCutTopLeft = radiusPx;
        mCutTopRight = radiusPx;
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusBottom(float radiusDp) {
        if (mContext == null) {
            return;
        }
        float radiusPx = DensityUtil.dip2px(mContext, radiusDp);
        mCutBottomLeft = radiusPx;
        mCutBottomRight = radiusPx;
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusTopLeft(float radiusDp) {
        if (mContext == null) {
            return;
        }
        mCutTopLeft = DensityUtil.dip2px(mContext, radiusDp);
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusTopRight(float radiusDp) {
        if (mContext == null) {
            return;
        }
        mCutTopRight = DensityUtil.dip2px(mContext, radiusDp);
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusBottomLeft(float radiusDp) {
        if (mContext == null) {
            return;
        }
        mCutBottomLeft = DensityUtil.dip2px(mContext, radiusDp);
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setRadiusBottomRight(float radiusDp) {
        if (mContext == null) {
            return;
        }
        mCutBottomRight = DensityUtil.dip2px(mContext, radiusDp);
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setStrokeWidth(float widthDp) {
        if (mContext == null) {
            return;
        }
        mStrokeWidth = DensityUtil.dip2px(mContext, widthDp);
        if (mView != null) {
            setRadius();
            onSizeChanged(mWidth, mHeight);
            mView.invalidate();
        }
    }

    public void setStrokeColor(int color) {
        mStrokeColor = color;
        if (mView != null) {
            mView.invalidate();
        }
    }

    public void setStrokeWidthColor(float widthDp, int color) {
        if (mContext == null) {
            return;
        }
        mStrokeWidth = DensityUtil.dip2px(mContext, widthDp);
        mStrokeColor = color;
        if (mView != null) {
            setRadius();
            onSizeChanged(mWidth, mHeight);
            mView.invalidate();
        }
    }
}
