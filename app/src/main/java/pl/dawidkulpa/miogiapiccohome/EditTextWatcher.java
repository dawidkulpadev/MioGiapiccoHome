package pl.dawidkulpa.miogiapiccohome;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputLayout;

public class EditTextWatcher implements TextWatcher {
    private final Button posButton;
    private final TextInputLayout inputLayout;
    private final String errorMsg;

    public EditTextWatcher(TextInputLayout inputLayout, Button b, String errorMsg) {
        this.inputLayout = inputLayout;
        posButton= b;
        this.errorMsg= errorMsg;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if(posButton!=null) {
            if (s.length() == 0) {
                posButton.setEnabled(false);
                inputLayout.setErrorEnabled(true);
                inputLayout.setError(errorMsg);
            } else {
                if (!posButton.isEnabled())
                    posButton.setEnabled(true);

                if (inputLayout.isErrorEnabled())
                    inputLayout.setErrorEnabled(false);
            }
        }
    }
}
