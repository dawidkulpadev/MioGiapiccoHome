package pl.dawidkulpa.miogiapiccohome.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.R;

public class SignInActivity extends AppCompatActivity {

    private User user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String login= prefs.getString("login", "");
        String pass= prefs.getString("pass", "");
        if(login!=null && pass!=null && !login.isEmpty() && !pass.isEmpty()) {
            performSignIn(login, pass);
        } else {
            showSignInForm();
        }
    }

    public void showSignInForm(){
        findViewById(R.id.login_label).setVisibility(View.VISIBLE);
        findViewById(R.id.login_edit).setVisibility(View.VISIBLE);
        findViewById(R.id.pass_edit).setVisibility(View.VISIBLE);
        findViewById(R.id.signin_button).setVisibility(View.VISIBLE);
        findViewById(R.id.signup_button).setVisibility(View.VISIBLE);
        findViewById(R.id.progressbar).setVisibility(View.GONE);
    }

    public void onSignInClick(View v){
        String login= ((TextView)findViewById(R.id.login_edit)).getText().toString();
        String pass= ((TextView)findViewById(R.id.pass_edit)).getText().toString();

        performSignIn(login, pass);
    }



    public void onSignUpClick(View v){
        String login= ((TextView)findViewById(R.id.login_edit)).getText().toString();
        String pass= ((TextView)findViewById(R.id.pass_edit)).getText().toString();

        if(login.isEmpty() || pass.isEmpty()){
            Snackbar.make(findViewById(R.id.signin_button), R.string.info_login_pass_empty, BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            findViewById(R.id.signin_button).setVisibility(View.GONE);
            findViewById(R.id.progressbar).setVisibility(View.VISIBLE);

            user = new User(login, pass);
            user.signUp(this::onSignInResult);
        }
    }

    public void performSignIn(String login, String pass){
        if(login.isEmpty() || pass.isEmpty()){
            Snackbar.make(findViewById(R.id.signin_button), R.string.info_login_pass_empty, BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            findViewById(R.id.signin_button).setVisibility(View.GONE);
            findViewById(R.id.progressbar).setVisibility(View.VISIBLE);

            user = new User(login, pass);
            user.signIn(this::onSignInResult);
        }
    }

    public void onSignInResult(User user, boolean success){
        if(success){
            SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString("login", user.getLogin())
                        .putString("pass", user.getPassword()).apply();

            Intent intent= new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra("UserAPI", this.user);
            startActivity(intent);
            this.finish();
        } else {
            Snackbar.make(findViewById(R.id.signin_button), R.string.info_signin_failed, BaseTransientBottomBar.LENGTH_SHORT).show();
            findViewById(R.id.signin_button).setVisibility(View.VISIBLE);
            findViewById(R.id.progressbar).setVisibility(View.GONE);
        }
    }

}