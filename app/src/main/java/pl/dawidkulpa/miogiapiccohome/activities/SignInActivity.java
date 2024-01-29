package pl.dawidkulpa.miogiapiccohome.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.R;

public class SignInActivity extends AppCompatActivity {
    private User user;

    private static final String SHARED_PREFS_NAME = "def-prefs";
    private static final String USERNAME_KEY = "username_key";
    private static final String PASSWORD_KEY = "password_key";

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        prefs= getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String lastUsername= prefs.getString(USERNAME_KEY, "");
        String lastPassword= prefs.getString(PASSWORD_KEY, "");

        if(!lastUsername.isEmpty() && !lastPassword.isEmpty()){
            performSignIn(lastUsername, lastPassword);
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
        /*String login= ((TextView)findViewById(R.id.login_edit)).getText().toString();
        String pass= ((TextView)findViewById(R.id.pass_edit)).getText().toString();

        if(login.isEmpty() || pass.isEmpty()){
            Snackbar.make(findViewById(R.id.signin_button), R.string.info_login_pass_empty, BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            findViewById(R.id.signin_button).setVisibility(View.GONE);
            findViewById(R.id.progressbar).setVisibility(View.VISIBLE);

            user = new User(login, pass);
            user.signUp(this::onSignInResult);
        }*/

        Intent i= new Intent(this, SignUpActivity.class);

        startActivity(i);
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
            registerPassword(user.getLogin(), user.getPassword());
            startMainActivity();
        } else {
            Snackbar.make(findViewById(R.id.signin_button), R.string.info_signin_failed, BaseTransientBottomBar.LENGTH_SHORT).show();
            findViewById(R.id.signin_button).setVisibility(View.VISIBLE);
            findViewById(R.id.progressbar).setVisibility(View.GONE);
        }
    }

    void registerPassword(String username, String password) {
        prefs.edit().putString(USERNAME_KEY, username).putString(PASSWORD_KEY, password).apply();
    }

    void startMainActivity(){
        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra("UserAPI", this.user);
        startActivity(intent);
    }

    public static void removeLastLoginData(Context context){
        SharedPreferences prefs= context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(USERNAME_KEY).remove(PASSWORD_KEY).apply();
    }
}