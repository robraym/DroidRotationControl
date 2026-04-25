package ray.droid.com.droidrotationcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class DroidMainActivity extends Activity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1;
    private static final int REQUEST_WRITE_SETTINGS_PERMISSION = 2;
    private boolean permissionDialogShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissionsAndStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsAndStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        if (permissionDialogShowing || isFinishing()) {
            return;
        }

        if (!canDrawOverlays()) {
            requestOverlayPermission();
            return;
        }

        if (!canWriteSettings()) {
            requestWriteSettingsPermission();
            return;
        }

        Intent intentService = new Intent(this, DroidHeadService.class);
        startService(intentService);
        finish();
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private boolean canWriteSettings() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this);
    }

    private void requestOverlayPermission() {
        permissionDialogShowing = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_overlay_title)
                .setMessage(R.string.permission_overlay_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .setOnDismissListener(dialog -> permissionDialogShowing = false)
                .show();
    }

    private void requestWriteSettingsPermission() {
        permissionDialogShowing = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_write_settings_title)
                .setMessage(R.string.permission_write_settings_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_WRITE_SETTINGS_PERMISSION);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                    finish();
                })
                .setOnDismissListener(dialog -> permissionDialogShowing = false)
                .show();
    }
}
