package com.danleech.cordova.plugin.imagePicker.view;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

public class Button extends AppCompatButton {

  public Button(Context context) {
    super(context);
  }

  public Button(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public Button(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void setEnabled(boolean enabled) {
    setAlpha(enabled ? 1 : 0.5f);
    super.setEnabled(enabled);
  }
}
