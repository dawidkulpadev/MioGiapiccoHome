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
        findViewById(R.id.login_edit_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.pass_edit_layout).setVisibility(View.VISIBLE);
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
        Intent i= new Intent(this, SignUpActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
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

    public void onSignInResult(User user, int r){
        findViewById(R.id.signin_button).setVisibility(View.VISIBLE);
        findViewById(R.id.progressbar).setVisibility(View.GONE);

        if(r==User.SIGN_IN_RESULT_SUCCESS){
            registerPassword(user.getLogin(), user.getPassword());
            startMainActivity();
        } else if(r==User.SIGN_IN_RESULT_NOT_ACTIVATED) {
            startActivateActivity();
        } else if(r==User.SIGN_IN_RESULT_AUTH_ERROR) {
            Snackbar.make(findViewById(R.id.signin_button), R.string.info_signin_failed, BaseTransientBottomBar.LENGTH_SHORT).show();
        } else if(r==User.SIGN_IN_RESULT_SERVER_ERROR){
            Snackbar.make(findViewById(R.id.signin_button), R.string.error_server_error, BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(findViewById(R.id.signin_button), R.string.error_connection_error, BaseTransientBottomBar.LENGTH_SHORT).show();
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

    void startActivateActivity(){
        Intent intent= new Intent(this, AccountActivationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra("UserAPI", this.user);
        startActivity(intent);
    }

    public static void removeLastLoginData(Context context){
        SharedPreferences prefs= context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(USERNAME_KEY).remove(PASSWORD_KEY).apply();
    }
}