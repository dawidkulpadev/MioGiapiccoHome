package pl.dawidkulpa.miogiapiccohome.activities;

import static pl.dawidkulpa.miogiapiccohome.API.User.ACTIVATION_SUCCESS;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.R;

public class AccountActivationActivity extends AppCompatActivity {

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_activation);

        user= getIntent().getParcelableExtra("UserAPI");
        if(user==null){
            Log.e("MainActivity", "User is null!");
            Intent intent= new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }

        TextInputEditText tiet= findViewById(R.id.activation_pin_input);
        tiet.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length()==6){
                    performActivation(s.toString());
                }
            }
        });
    }

    private void performActivation(String pin){
        user.activateAccount(pin, result -> {
            if(result==ACTIVATION_SUCCESS){
                onActivationSuccess();
            } else {
                // TODO: Say whats wrong
            }
        });
    }

    private void onActivationSuccess(){
        // TODO: Go to main activity
    }
}