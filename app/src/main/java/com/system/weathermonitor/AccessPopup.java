package com.system.weathermonitor;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.system.weathermonitor.databinding.DialogAccessBinding;

import java.util.Random;

/**
 * AccessPopup — the hidden access dialog.
 *
 * <h3>Appearance</h3>
 * A dark frosted popup titled "?" containing 7 letter buttons arranged in
 * two rows (4 + 3).  No branding, no hint of purpose.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li>All 7 visible letter buttons → close the entire app (finish + kill).</li>
 *   <li>Invisible {@code hiddenZone} view in the bottom-right corner →
 *       dismiss this popup and open {@link ControlPanelActivity}.</li>
 * </ul>
 *
 * <h3>Letter randomisation</h3>
 * Each time the popup is shown, 7 letters are drawn without replacement from
 * the full A–Z alphabet and shuffled, so the button labels are different on
 * every invocation.
 */
public class AccessPopup {

    // ── Letter pool ────────────────────────────────────────────────────────
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    // ── Callback ───────────────────────────────────────────────────────────
    /** Called when the hidden zone is tapped. */
    public interface OnHiddenZoneTapped {
        void onHiddenZoneTapped();
    }

    // ── Factory ────────────────────────────────────────────────────────────

    /**
     * Creates, configures, and returns a ready-to-show {@link Dialog}.
     *
     * @param context         host context (typically MainActivity)
     * @param onHiddenTapped  callback fired when the invisible bottom-right zone is tapped
     */
    public static Dialog create(Context context, OnHiddenZoneTapped onHiddenTapped) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        DialogAccessBinding binding = DialogAccessBinding.inflate(
                android.view.LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());

        // Transparent window chrome — only our bg_popup shape is visible
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Dim the weather screen behind the popup
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
        }

        // Assign random letters to the 7 buttons
        String[] letters = randomLetters();
        Button[] buttons = {
                binding.btnLetter0,
                binding.btnLetter1,
                binding.btnLetter2,
                binding.btnLetter3,
                binding.btnLetter4,
                binding.btnLetter5,
                binding.btnLetter6
        };
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setText(letters[i]);
            buttons[i].setOnClickListener(v -> closeApp(context));
        }

        // Hidden zone — bottom-right of the popup
        binding.hiddenZone.setOnClickListener(v -> {
            dialog.dismiss();
            onHiddenTapped.onHiddenZoneTapped();
        });

        // Tapping outside the popup does nothing (keeps it open)
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        return dialog;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Picks 7 unique letters at random from A–Z and returns them as strings.
     * Uses Fisher-Yates partial shuffle on a copy of the alphabet array.
     */
    private static String[] randomLetters() {
        // Copy alphabet so we don't mutate the constant
        char[] pool = ALPHABET.clone();
        Random rng  = new Random();
        String[] result = new String[7];

        for (int i = 0; i < 7; i++) {
            // Swap pool[i] with a random element from pool[i..25]
            int j = i + rng.nextInt(pool.length - i);
            char tmp  = pool[i];
            pool[i]   = pool[j];
            pool[j]   = tmp;
            result[i] = String.valueOf(pool[i]);
        }
        return result;
    }

    /**
     * Terminates the app immediately.
     * Calls {@link android.app.Activity#finishAffinity()} to clear the back stack,
     * then kills the process so the app cannot be resumed.
     */
    private static void closeApp(Context context) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finishAffinity();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
