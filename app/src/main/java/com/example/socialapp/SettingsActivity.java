package com.example.socialapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private static final String PREF_THEME = "theme_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_theme) {
            showThemeDialog();
            return true;
        } else if (id == R.id.menu_contact_us) {
            showContactUsDialog();
            return true;
        } else if (id == R.id.menu_privacy_policy) {
            showPrivacyPolicyDialog();
            return true;
        } else if (id == R.id.settings_menu_logout) {
            showLogoutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showThemeDialog() {
        String[] themes = {"Light Theme", "Dark Theme", "System Default"};
        int currentTheme = sharedPreferences.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        int selectedIndex = 0;
        switch (currentTheme) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                selectedIndex = 0;
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                selectedIndex = 1;
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                selectedIndex = 2;
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                    int themeMode;
                    switch (which) {
                        case 0:
                            themeMode = AppCompatDelegate.MODE_NIGHT_NO;
                            break;
                        case 1:
                            themeMode = AppCompatDelegate.MODE_NIGHT_YES;
                            break;
                        default:
                            themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                            break;
                    }
                    
                    // Save theme preference
                    sharedPreferences.edit().putInt(PREF_THEME, themeMode).apply();
                    
                    // Apply theme
                    AppCompatDelegate.setDefaultNightMode(themeMode);
                    
                    dialog.dismiss();
                    Toast.makeText(this, "Theme applied! Restart app for full effect.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showContactUsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Contact Us")
                .setMessage("Get in touch with us:\n\n" +
                        "📧 Email: pr75090@gmail.com\n" +
                        "📞 Phone: +91 8105021332\n\n" +
                        "We're here to help!")
                .setPositiveButton("Send Email", (dialog, which) -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:pr75090@gmail.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Social App Support");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello, I need help with...");
                    
                    if (emailIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(emailIntent);
                    } else {
                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Call", (dialog, which) -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:+918105021332"));
                    startActivity(callIntent);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage("Privacy Policy for Social App\n\n" +
                        "Last updated: " + new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()) + "\n\n" +
                        "1. Information We Collect\n" +
                        "• Profile information (username, profile picture)\n" +
                        "• Posts and comments you create\n" +
                        "• Usage data and preferences\n\n" +
                        "2. How We Use Your Information\n" +
                        "• To provide and maintain our service\n" +
                        "• To notify you about changes to our service\n" +
                        "• To provide customer support\n\n" +
                        "3. Data Security\n" +
                        "• We implement appropriate security measures\n" +
                        "• Your data is stored securely on Firebase servers\n\n" +
                        "4. Contact Information\n" +
                        "For privacy concerns, contact us at:\n" +
                        "pr75090@gmail.com\n\n" +
                        "By using this app, you agree to this privacy policy.")
                .setPositiveButton("I Understand", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
