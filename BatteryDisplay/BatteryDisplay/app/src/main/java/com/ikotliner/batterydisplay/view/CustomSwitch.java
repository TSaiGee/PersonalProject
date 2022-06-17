package com.ikotliner.batterydisplay.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.ikotliner.batterydisplay.R;
import com.ikotliner.batterydisplay.util.Common;

public class CustomSwitch extends ConstraintLayout implements View.OnClickListener {
    private ImageView imageView;
    private TextView textView;
    private ConstraintLayout layout;
    private boolean isChecked = false;
    private float defaultSize = 100;
    private int icon_width;
    private int icon_height;
    private Drawable icon;
    private Drawable background;
    private ClickListener mClickListener;

    public CustomSwitch(@NonNull Context context) {
        super(context);
    }

    public CustomSwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
        initView(context);
    }

    public CustomSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initView(context);
    }

    public CustomSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initAttrs(Context context, AttributeSet attributeSet) {
        LayoutInflater.from(context).inflate(R.layout.custom_swtich, this, true);
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CustomSwitch);
        icon_height = (int) typedArray.getDimension(R.styleable.CustomSwitch_icon_height, defaultSize);
        icon_width = (int) typedArray.getDimension(R.styleable.CustomSwitch_icon_width, defaultSize);
        icon = typedArray.getDrawable(R.styleable.CustomSwitch_custom_icon);
        background = typedArray.getDrawable(R.styleable.CustomSwitch_custom_background);
        layout = findViewById(R.id.switch_layout);
        textView = findViewById(R.id.switch_background);
        imageView = findViewById(R.id.switch_icon);
    }

    private void initView(Context context) {

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(icon_width, icon_height);
        params.width = icon_width;
        params.height = icon_height;
        params.topToTop = LayoutParams.PARENT_ID;
        params.startToStart = LayoutParams.PARENT_ID;
        params.endToEnd = LayoutParams.PARENT_ID;
        params.bottomToBottom = LayoutParams.PARENT_ID;

        textView.setBackground(background);
        imageView.setBackground(icon);
        imageView.setLayoutParams(params);
        textView.setBackground(background);
        imageView.setImageState(new int[]{android.R.attr.checked}, true);
        layout.setOnClickListener(this);
        imageView.setOnClickListener(this);
        textView.setOnClickListener(this);
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked() {
        isChecked = !isChecked;
        imageView.setSelected(isChecked);
        textView.setSelected(isChecked);
        if (mClickListener!=null){
            mClickListener.onChange(isChecked);
        }
    }

    public void updateStatus(boolean status,String flag){
        Log.e(Common.TAG, "update flag: " + flag + " status: " + status);
        isChecked = status;
        imageView.setSelected(status);
        textView.setSelected(status);
    }

    @Override
    public void onClick(View view) {
        setChecked();
    }

    public void addClickListener(ClickListener listener){
        this.mClickListener = listener;
    }

    public interface ClickListener{
        void onChange(boolean status);
    }
}
