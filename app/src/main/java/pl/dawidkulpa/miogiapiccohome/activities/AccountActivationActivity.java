package pl.dawidkulpa.miogiapiccohome.activities;

import static pl.dawidkulpa.miogiapiccohome.API.data.User.ACTIVATION_CODE_EXPIRED;
import static pl.dawidkulpa.miogiapiccohome.API.data.User.ACTIVATION_CODE_INCORRECT;
import static pl.dawidkulpa.miogiapiccohome.API.data.User.ACTIVATION_CONN_ERROR;
import static pl.dawidkulpa.miogiapiccohome.API.data.User.ACTIVATION_SUCCESS;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;

import android.text.TextWatcher;
import android.util.Log;

import android.view.View;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import pl.dawidkulpa.miogiapiccohome.API.data.User;
import pl.dawidkulpa.miogiapiccohome.R;

public class AccountActivationActivity extends AppCompatActivity {

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_activation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        findViewById(R.id.regenerate_pin_button).setOnClickListener(v -> onRegenerateActivationCode());
    }

    private void performActivation(String pin){
        findViewById(R.id.activate_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
        findViewById(R.id.activate_button).setEnabled(false);

        user.activateAccount(pin, result -> {
            findViewById(R.id.activate_button).setVisibility(View.VISIBLE);
            findViewById(R.id.progressbar).setVisibility(View.GONE);
            findViewById(R.id.activate_button).setEnabled(true);

            if(result==ACTIVATION_SUCCESS){
                onActivationSuccess();
            } else if(result==ACTIVATION_CODE_EXPIRED) {
                Snackbar.make(findViewById(R.id.contextView),
                        R.string.error_activation_code_expired,
                        BaseTransientBottomBar.LENGTH_SHORT).show();
            } else if(result==ACTIVATION_CODE_INCORRECT){
                Snackbar.make(findViewById(R.id.contextView),
                        R.string.error_incorrect_activation_code,
                        BaseTransientBottomBar.LENGTH_SHORT).show();
            } else if(result==ACTIVATION_CONN_ERROR){
                Snackbar.make(findViewById(R.id.contextView),
                        R.string.error_connection_error,
                        BaseTransientBottomBar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(R.id.contextView),
                        R.string.error_server_error,
                        BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        });
    }

    private void onActivationSuccess(){
        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra("UserAPI", this.user);
        startActivity(intent);
    }

    private void onRegenerateActivationCode(){
        user.regenerateActivationCode(this::onCodeRegenerated);
    }

    private void onCodeRegenerated(boolean success){
        if(success){
            Snackbar.make(findViewById(R.id.contextView), R.string.message_activation_code_regenerated, BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(findViewById(R.id.contextView), R.string.error_server_error, BaseTransientBottomBar.LENGTH_SHORT).show();
        }
    }
}