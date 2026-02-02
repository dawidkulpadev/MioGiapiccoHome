package pl.dawidkulpa.miogiapiccohome.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.dawidkulpa.miogiapiccohome.API.data.User;
import pl.dawidkulpa.miogiapiccohome.R;



public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout emailTil;
    private TextInputLayout passTil;
    private TextInputLayout repassTil;
    private Button signupButton;
    final public Pattern emailRegex= Pattern.compile("^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!" +
            "#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x" +
            "5d-\\x7f]|\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[" +
            "a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?" +
            "[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?" +
            ":[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\[\\x01-\\x09\\x0b\\x" +
            "0c\\x0e-\\x7f])+)])$", Pattern.CASE_INSENSITIVE);

    final public Pattern strongPasswordRegex= Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Find input elements layouts
        emailTil= findViewById(R.id.email_input_box);
        passTil= findViewById(R.id.password_input_box);
        repassTil= findViewById(R.id.repassword_input_box);
        repassTil.setEnabled(false);
        signupButton= findViewById(R.id.signup_button);
        signupButton.setEnabled(false);

        passTil.setErrorIconDrawable(null);
        repassTil.setErrorIconDrawable(null);

        // Create / Prepare email text watcher
        findViewById(R.id.email_input).setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus){

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
                    Matcher matcher= emailRegex.matcher(s.toString());
                    if(matcher.matches()){
                        emailTil.setErrorEnabled(false);
                    }
                }
            }
        });

        // Create / Prepare password text watcher
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

        // Create / Prepare re password
        ((TextInputEditText)findViewById(R.id.repassword_input)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(passTil.getEditText()!=null &&
                        s.toString().equals(passTil.getEditText().getText().toString())){
                    repassTil.setErrorEnabled(false);
                    signupButton.setEnabled(true);
                } else {
                    repassTil.setErrorEnabled(true);
                    repassTil.setError(getString(R.string.error_password_mismatch));
                }
            }
        });

        findViewById(R.id.signup_button).setOnClickListener(v -> performSignup());
    }

    private void performSignup(){
        if(emailTil.getEditText()!=null && passTil.getEditText()!=null) {
            findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
            findViewById(R.id.signup_button).setVisibility(View.INVISIBLE);
            findViewById(R.id.signup_button).setEnabled(false);

            String l = emailTil.getEditText().getText().toString();
            String p = passTil.getEditText().getText().toString();
            User user = new User(l, p);
            user.signUp(this::onSignUpResult);
        } else {
            Snackbar.make(findViewById(R.id.contextView), R.string.error_signup_form_error, BaseTransientBottomBar.LENGTH_SHORT).show();
        }
    }

    public void onSignUpResult(User u, int r){
        findViewById(R.id.progressbar).setVisibility(View.GONE);
        findViewById(R.id.signup_button).setVisibility(View.VISIBLE);
        findViewById(R.id.signup_button).setEnabled(true);

        if (r==User.SIGN_UP_RESULT_SUCCESS) {
            Intent i = new Intent(this, AccountActivationActivity.class);
            i.putExtra("UserAPI", u);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(i);
        } else if(r==User.SIGN_UP_RESULT_ACCOUNT_EXISTS) {
            Snackbar.make(findViewById(R.id.contextView),  R.string.error_signup_email_exists,
                    BaseTransientBottomBar.LENGTH_SHORT).show();
        } else if (r==User.SIGN_UP_RESULT_SERVER_ERROR) {
            Snackbar.make(findViewById(R.id.contextView),
                    R.string.error_server_error, BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(findViewById(R.id.contextView),
                    R.string.error_connection_error, BaseTransientBottomBar.LENGTH_SHORT).show();
        }
    }
}