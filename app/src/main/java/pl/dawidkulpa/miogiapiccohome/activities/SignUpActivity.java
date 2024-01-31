package pl.dawidkulpa.miogiapiccohome.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.dawidkulpa.miogiapiccohome.R;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout emailTil;
    private TextInputLayout passTil;
    private TextInputLayout repassTil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailTil= findViewById(R.id.email_input_box);
        passTil= findViewById(R.id.password_input_box);
        repassTil= findViewById(R.id.repassword_input_box);
        repassTil.setEnabled(false);

        passTil.setErrorIconDrawable(null);
        repassTil.setErrorIconDrawable(null);

        findViewById(R.id.email_input).setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus){
                Pattern emailRegex= Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", Pattern.CASE_INSENSITIVE);
                Matcher matcher= emailRegex.matcher(((TextInputEditText)v).getEditableText().toString());
                if(!matcher.matches()){
                    emailTil.setErrorEnabled(true);
                    emailTil.setError(getString(R.string.error_email_invalid));
                }
            }
        });
        ((TextInputEditText)findViewById(R.id.email_input)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length()==0){
                    emailTil.setErrorEnabled(true);
                    emailTil.setError(getString(R.string.error_field_empty));
                } else {
                    Pattern emailRegex= Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", Pattern.CASE_INSENSITIVE);
                    Matcher matcher= emailRegex.matcher(s.toString());
                    if(matcher.matches()){
                        emailTil.setErrorEnabled(false);
                    }
                }

            }
        });

        findViewById(R.id.password_input).setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                passTil.setHelperText(getString(R.string.helper_password_rules));
                passTil.setHelperTextEnabled(true);
            } else {
                passTil.setHelperTextEnabled(false);
            }
        });

        ((TextInputEditText)findViewById(R.id.password_input)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String p= s.toString();
                if(p.length()>=8) {
                    Pattern strongPasswordRegex = Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$");
                    Matcher matcher = strongPasswordRegex.matcher(p);
                    if (!matcher.matches()) {
                        passTil.setErrorEnabled(true);
                        passTil.setError(getString(R.string.error_password_weak));
                        repassTil.setEnabled(false);
                    } else {
                        passTil.setErrorEnabled(false);
                        repassTil.setEnabled(true);
                    }
                } else {
                    passTil.setErrorEnabled(true);
                    passTil.setError(getString(R.string.error_password_short));
                    repassTil.setEnabled(false);
                }
            }
        });

        ((TextInputEditText)findViewById(R.id.repassword_input)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals(passTil.getEditText().getText().toString())){
                    repassTil.setErrorEnabled(false);
                } else {
                    repassTil.setErrorEnabled(true);
                    repassTil.setError(getString(R.string.error_password_mismatch));
                }
            }
        });
    }


}