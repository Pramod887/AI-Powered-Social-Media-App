package com.example.socialapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.core.content.ContextCompat;

public class FloatingHeartView extends View {
    
    private Paint paint;
    private Drawable heartDrawable;
    private float heartSize = 40f;
    private float currentY = 0f;
    private float currentX = 0f;
    private float currentScale = 1f;
    private float currentAlpha = 1f;
    private float currentRotation = 0f;
    
    private boolean isAnimating = false;
    private OnAnimationEndListener animationEndListener;
    
    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }
    
    public FloatingHeartView(Context context) {
        super(context);
        init();
    }
    
    public FloatingHeartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public FloatingHeartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        try {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.parseColor("#E91E63")); // Pink color
            
            heartDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_heart_filled);
            if (heartDrawable != null) {
                heartDrawable.setTint(Color.parseColor("#E91E63"));
            }
        } catch (Exception e) {
            // Handle initialization errors
        }
    }
    
    public void startFloatingAnimation(float startX, float startY, OnAnimationEndListener listener) {
        try {
            this.animationEndListener = listener;
            this.currentX = startX;
            this.currentY = startY;
            this.currentScale = 0.5f;
            this.currentAlpha = 1f;
            this.currentRotation = 0f;
            this.isAnimating = true;
            
            // Create the floating animation
            ObjectAnimator translateY = ObjectAnimator.ofFloat(this, "currentY", startY, startY - 200);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "currentScale", 0.5f, 1.2f, 0.8f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(this, "currentAlpha", 1f, 0f);
            ObjectAnimator rotation = ObjectAnimator.ofFloat(this, "currentRotation", 0f, 45f);
            
            // Set up the animation set
            translateY.setDuration(800);
            scaleX.setDuration(800);
            alpha.setDuration(800);
            rotation.setDuration(800);
            
            // Set interpolators
            translateY.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleX.setInterpolator(new OvershootInterpolator());
            
            // Add listeners
            translateY.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    isAnimating = false;
                    if (animationEndListener != null) {
                        animationEndListener.onAnimationEnd();
                    }
                }
            });
            
            // Start all animations together
            translateY.start();
            scaleX.start();
            alpha.start();
            rotation.start();
            
            // Trigger redraw
            invalidate();
        } catch (Exception e) {
            // Handle animation errors
            if (animationEndListener != null) {
                animationEndListener.onAnimationEnd();
            }
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
            
            if (isAnimating && heartDrawable != null) {
                // Save canvas state
                canvas.save();
                
                // Apply transformations
                canvas.translate(currentX, currentY);
                canvas.scale(currentScale, currentScale);
                canvas.rotate(currentRotation);
                
                // Set alpha
                paint.setAlpha((int) (currentAlpha * 255));
                
                // Draw the heart
                int left = (int) (-heartSize / 2);
                int top = (int) (-heartSize / 2);
                int right = (int) (heartSize / 2);
                int bottom = (int) (heartSize / 2);
                
                heartDrawable.setBounds(left, top, right, bottom);
                heartDrawable.setAlpha((int) (currentAlpha * 255));
                heartDrawable.draw(canvas);
                
                // Restore canvas state
                canvas.restore();
            }
        } catch (Exception e) {
            // Handle drawing errors
        }
    }
    
    // Getters and setters for animation properties
    public float getCurrentY() { return currentY; }
    public void setCurrentY(float currentY) { 
        this.currentY = currentY; 
        try {
            invalidate();
        } catch (Exception e) {
            // Handle invalidate errors
        }
    }
    
    public float getCurrentScale() { return currentScale; }
    public void setCurrentScale(float currentScale) { 
        this.currentScale = currentScale; 
        try {
            invalidate();
        } catch (Exception e) {
            // Handle invalidate errors
        }
    }
    
    public float getCurrentAlpha() { return currentAlpha; }
    public void setCurrentAlpha(float currentAlpha) { 
        this.currentAlpha = currentAlpha; 
        try {
            invalidate();
        } catch (Exception e) {
            // Handle invalidate errors
        }
    }
    
    public float getCurrentRotation() { return currentRotation; }
    public void setCurrentRotation(float currentRotation) { 
        this.currentRotation = currentRotation; 
        try {
            invalidate();
        } catch (Exception e) {
            // Handle invalidate errors
        }
    }
}
