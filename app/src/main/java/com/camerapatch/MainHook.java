package com.camerapatch;

import android.content.Intent;
import android.provider.MediaStore;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TARGET_PACKAGE = "com.target.app"; // CHANGE THIS
    private static final String FAKE_IMAGE_PATH = "/sdcard/DCIM/fake_photo.jpg";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        // Hook startActivityForResult to intercept camera intents
        XposedHelpers.findAndHookMethod(
            "android.app.Activity",
            lpparam.classLoader,
            "startActivityForResult",
            Intent.class, int.class, android.os.Bundle.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) param.args[0];
                    if (intent != null && MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction())) {
                        // Replace with gallery picker intent
                        Intent fakeIntent = new Intent(Intent.ACTION_PICK);
                        fakeIntent.setType("image/*");
                        param.args[0] = fakeIntent;
                    }
                }
            });

        // Hook onActivityResult to spoof image data
        XposedHelpers.findAndHookMethod(
            "android.app.Activity",
            lpparam.classLoader,
            "onActivityResult",
            int.class, int.class, Intent.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int resultCode = (int) param.args[1];
                    Intent data = (Intent) param.args[2];

                    if (resultCode == -1 && data != null) { // RESULT_OK
                        // Spoof EXIF metadata
                        ExifSpoofer.spoof(FAKE_IMAGE_PATH);
                        
                        // Replace the returned intent with fake image
                        data.setData(android.net.Uri.parse("file://" + FAKE_IMAGE_PATH));
                    }
                }
            });
    }
}