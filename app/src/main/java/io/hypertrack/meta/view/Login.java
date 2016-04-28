package io.hypertrack.meta.view;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.hypertrack.meta.MetaApplication;
import io.hypertrack.meta.R;
import io.hypertrack.meta.model.Country;
import io.hypertrack.meta.model.CountryMaster;
import io.hypertrack.meta.model.CountrySpinnerAdapter;
import io.hypertrack.meta.presenter.RegisterPresenter;
import io.hypertrack.meta.util.HTConstants;
import io.hypertrack.meta.util.PhoneUtils;
import io.hypertrack.meta.util.SharedPreferenceManager;

public class Login extends AppCompatActivity implements RegisterView {

    private static final String TAG = Login.class.getSimpleName();

    @Bind(R.id.phoneNumber)
    public EditText phoneNumberView;

    @Bind(R.id.spinner_countries)
    public Spinner spinner;

    private ProgressDialog mProgressDialog;
    private CountrySpinnerAdapter adapter;
    private String isoCode;
    private RegisterPresenter resgisterPresenter;
    private SharedPreferenceManager sharedPreferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Verify");

        ButterKnife.bind(this);

        sharedPreferenceManager = new SharedPreferenceManager(MetaApplication.getInstance());

        resgisterPresenter = new RegisterPresenter();
        resgisterPresenter.attachView(this);

        initCountryFlagSpinner();

        if(checkPermission()) {

        } else {
            requestPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resgisterPresenter.detachView();
    }

    private void initCountryFlagSpinner() {


        CountryMaster cm = CountryMaster.getInstance(this);
        final ArrayList<Country> countries = cm.getCountries();
        String countryIsoCode = cm.getDefaultCountryIso();
        //Country country = cm.getCountryByIso(countryIsoCode);

        adapter = new CountrySpinnerAdapter(this, R.layout.view_country_list_item, countries);
        spinner.setAdapter(adapter);

        String isoCountryCode = PhoneUtils.getCountryRegionFromPhone(this);
        Log.v(TAG, "Region ISO: " + isoCountryCode);

        if (!TextUtils.isEmpty(isoCountryCode)) {
            for (Country c : countries) {
                if (c.mCountryIso.equalsIgnoreCase(isoCountryCode)) {
                    spinner.setSelection(adapter.getPosition(c));
                }
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isoCode = countries.get(position).mCountryIso;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    private void  poulatePhoneNumberIfAvailable() {
        TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        String number = tm.getLine1Number();
        if(!TextUtils.isEmpty(number)) {
            phoneNumberView.setText(number);
        }
    }

    @OnClick(R.id.verify)
    public void verifyPhoneNumber() {

        String number = phoneNumberView.getText().toString();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("registering phone number");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        resgisterPresenter.attemptRegistration(number, isoCode);

    }

    private static final int PERMISSION_REQUEST_CODE = 1;

    private boolean checkPermission(){

        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;

    }

    private void requestPermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){

            Toast.makeText(this,"GPS permission allows us to access location data. Please allow in App Settings for additional functionality.",Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //Toast.makeText(this,"Permission Granted, Now you can access location data.",Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(this,"Permission Denied, You cannot access location data.",Toast.LENGTH_LONG).show();

                }
                break;
        }
    }

    @Override
    public void registrationFailed() {

        mProgressDialog.dismiss();
        Toast.makeText(Login.this, "Apologies, we could process your request at this moment.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void navigateToVerificationScreen() {

        mProgressDialog.dismiss();

        int userId = sharedPreferenceManager.getUserId();

        Intent intent = new Intent(Login.this, Verify.class);
        intent.putExtra(HTConstants.USER_ID, userId);
        startActivity(intent);

    }

    @Override
    public void showValidationError() {
        mProgressDialog.dismiss();
        phoneNumberView.setError("Please enter a valid number.");
    }
}
