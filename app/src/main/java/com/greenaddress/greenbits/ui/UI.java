package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.greenaddress.greenapi.model.Conversion;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class UI {
    private static final String TAG = UI.class.getSimpleName();

    public static String PACKAGE_NAME = "com.greenaddress.greenbits_android_wallet";

    static final int INVALID_RESOURCE_ID = 0;
    public static final String[] UNITS = {"BTC", "mBTC", "\u00B5BTC", "bits", "sats"};
    public static final String[] LIQUID_UNITS = {"L-BTC", "L-mBTC", "L-\u00B5BTC", "L-bits", "L-sats"};

    private static Function<String, String> toUnitKeyFunc = new Function<String, String>() {
        @NullableDecl
        @Override
        public String apply(@NullableDecl String input) {
            return input != null ? Conversion.toUnitKey(input) : null;
        }
    };
    public static final List<String> UNIT_KEYS_LIST = Lists.transform(Arrays.asList(UNITS), UI.toUnitKeyFunc);

    // Class to unify cancel and dismiss handling */
    private static class DialogCloseHandler implements DialogInterface.OnCancelListener,
                                                       DialogInterface.OnDismissListener {
        private final Runnable mCallback;
        private final boolean mCancelOnly;

        DialogCloseHandler(final Runnable callback, final boolean cancelOnly) {
            mCallback = callback;
            mCancelOnly = cancelOnly;
        }
        @Override
        public void onCancel(final DialogInterface d) { mCallback.run(); }
        @Override
        public void onDismiss(final DialogInterface d) { if (!mCancelOnly) mCallback.run(); }
    }

    private static void setDialogCloseHandler(final Dialog d, final Runnable callback, final boolean cancelOnly) {
        final DialogCloseHandler handler = new DialogCloseHandler(callback, cancelOnly);
        d.setOnCancelListener(handler);
        d.setOnDismissListener(handler);
    }

    public static void setDialogCloseHandler(final Dialog d, final Runnable callback) {
        setDialogCloseHandler(d, callback, false);
    }

    public static MaterialDialog dismiss(final Activity a, final Dialog d) {
        if (d != null)
            if (a != null)
                a.runOnUiThread(new Runnable() { public void run() { d.dismiss(); } });
            else
                d.dismiss();
        return null;

    }


    public static View inflateDialog(final Activity a, final int id) {
        return a.getLayoutInflater().inflate(id, null, false);
    }

    private static boolean isEnterKeyDown(final KeyEvent e) {
        return e != null && e.getAction() == KeyEvent.ACTION_DOWN &&
               e.getKeyCode() == KeyEvent.KEYCODE_ENTER;
    }

    public static TextView.OnEditorActionListener getListenerRunOnEnter(final Runnable r) {
        return (v, actionId, event) -> {
                   if (actionId == EditorInfo.IME_ACTION_DONE ||
                       actionId == EditorInfo.IME_ACTION_SEARCH ||
                       actionId == EditorInfo.IME_ACTION_SEND ||
                       isEnterKeyDown(event)) {
                       if (event == null || !event.isShiftPressed()) {
                           r.run(); // The user is done typing.
                           return true; // Consume.
                       }
                   }
                   return false; // Pass on to other listeners.
        };
    }

    public static MaterialDialog.Builder popup(final Activity a, final String title, final int pos, final int neg) {
        final MaterialDialog.Builder b;
        b = new MaterialDialog.Builder(a)
            .title(title)
            .titleColorRes(R.color.white)
            .positiveColor(ThemeUtils.resolveColorAccent(a))
            .negativeColor(ThemeUtils.resolveColorAccent(a))
            .contentColorRes(R.color.white)
            .backgroundColor(a.getResources().getColor(R.color.buttonJungleGreen))
            .theme(Theme.DARK);
        if (pos != INVALID_RESOURCE_ID)
            b.positiveText(pos);
        if (neg != INVALID_RESOURCE_ID)
            return b.negativeText(neg);
        return b;
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title, final int pos, final int neg) {
        return popup(a, a.getString(title), pos, neg);
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title, final int pos) {
        return popup(a, title, pos, INVALID_RESOURCE_ID);
    }

    public static MaterialDialog.Builder popup(final Activity a, final String title) {
        return popup(a, title, android.R.string.ok, android.R.string.cancel);
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title) {
        return popup(a, title, android.R.string.ok, android.R.string.cancel);
    }

    static MaterialDialog hideDialog(final MaterialDialog dialog) {
        if (dialog != null) {
            try {
                dialog.cancel();
            } catch (final IllegalArgumentException e) {}
        }
        return null;
    }

    public static Map<String, String> getTwoFactorLookup(final Resources res) {
        final List<String> localized = Arrays.asList(res.getStringArray(R.array.twoFactorChoices));
        final List<String> methods = Arrays.asList(res.getStringArray(R.array.twoFactorMethods));
        final Map<String, String> map = new HashMap<>();
        for (int i = 0; i < localized.size(); i++)
            map.put(methods.get(i), localized.get(i));
        return map;
    }

    static MaterialDialog popupWait(final Activity a, final int title) {
        final int id = INVALID_RESOURCE_ID;
        final MaterialDialog dialog = popup(a, title, id).progress(true, 0).build();
        dialog.show();
        return dialog;
    }

    public static void toast(final Activity activity, final String msg, final Button reenable, final int len) {
        activity.runOnUiThread(() -> {
            if (reenable != null)
                reenable.setEnabled(true);
            Log.d(TAG, "Toast: " + msg);
            final Resources res = activity.getResources();
            final String translated = i18n(res, msg);
            final Toast t = Toast.makeText(activity, translated, len);
            try{
                final View v = t.getView();
                if(v != null) {
                    v.setBackgroundColor(0xaf000000);
                    ((TextView) v.findViewById(android.R.id.message)).setTextColor(ThemeUtils.resolveColorAccent(activity));
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            t.show();
        });
    }

    public static void toast(final Activity activity, final int id, final Button reenable) {
        toast(activity, activity.getString(id), reenable);
    }

    public static void toast(final Activity activity, final String msg, final Button reenable) {
        toast(activity, msg, reenable, Toast.LENGTH_LONG);
    }

    public static void toast(final Activity activity, final Throwable t, final Button reenable) {
        t.printStackTrace();
        toast(activity, t.getMessage(), reenable, Toast.LENGTH_LONG);
    }

    public static void toast(final Activity activity, final int id, final int len) {
        toast(activity, activity.getString(id), null, len);
    }

    public static void toast(final Activity activity, final String s, final int len) {
        toast(activity, s, null, len);
    }

    // Dummy TextWatcher for simple overrides
    public static class TextWatcher implements android.text.TextWatcher {
        public TextWatcher() { super(); }
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) { }
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) { }
        @Override
        public void afterTextChanged(final Editable s) { }
    }

    public static < T extends View > T mapClick(final View parent, final int id, final View.OnClickListener fn) {
        final T v = find(parent, id);
        if (v != null)
            v.setOnClickListener(fn);
        return v;
    }

    public static < T extends View > T mapClick(final Activity activity, final int id, final View.OnClickListener fn) {
        final T v = find(activity, id);
        if (v != null)
            v.setOnClickListener(fn);
        return v;
    }

    public static void unmapClick(final View v) {
        if (v != null)
            v.setOnClickListener(null);
    }

    public static void mapEnterToPositive(final Dialog dialog, final int editId) {
        final TextView edit = UI.find(dialog, editId);
        edit.setOnEditorActionListener(getListenerRunOnEnter(() -> {
            final MaterialDialog md = (MaterialDialog) dialog;
            md.onClick(md.getActionButton(DialogAction.POSITIVE));
        }));
    }

    private final static Set<Integer> idsToNotReplace = new HashSet<>();
    static {
        idsToNotReplace.add(R.id.layoutCode);
        idsToNotReplace.add(R.id.textCode);
        idsToNotReplace.add(R.id.copyButton);
        idsToNotReplace.add(R.id.fastButton);
        idsToNotReplace.add(R.id.mediumButton);
        idsToNotReplace.add(R.id.slowButton);
        idsToNotReplace.add(R.id.customButton);
        idsToNotReplace.add(R.id.status_layout);

    }

    // Keyboard hiding taken from https://stackoverflow.com/a/11656129
    public static void attachHideKeyboardListener(final Activity activity, final View view) {
        if (idsToNotReplace.contains(view.getId()))
            return;
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText) &&
            !(view instanceof Button)) {
            view.setOnClickListener(v -> hideSoftKeyboard(activity));
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                final View innerView = ((ViewGroup) view).getChildAt(i);
                attachHideKeyboardListener(activity, innerView);
            }
        }
    }

    public static void hideSoftKeyboard(final Activity activity) {
        if (activity == null)
            return;
        final InputMethodManager inputMethodManager =
            (InputMethodManager) activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null || activity.getCurrentFocus() == null)
            return;
        inputMethodManager.hideSoftInputFromWindow(
            activity.getCurrentFocus().getWindowToken(), 0);
    }

    // Show/Hide controls
    static void showIf(final boolean condition, final View v, final int hiddenViewState) {
        if (v != null)
            v.setVisibility(condition ? View.VISIBLE : hiddenViewState);
    }

    public static void showIf(final boolean condition, final View v) {
        showIf(condition, v, View.GONE);
    }

    public static void show(final View v) { showIf(true, v); }

    public static void hideIf(final boolean condition, final View v) {
        showIf(!condition, v);
    }

    public static void hide(final View v) { showIf(false, v); }

    // Enable/Disable controls
    public static void enableIf(final boolean condition, final View v) {
        v.setEnabled(condition);
    }

    public static void enable(final View v) { enableIf(true, v); }

    public static void disableIf(final boolean condition, final View v) {
        enableIf(!condition, v);
    }

    public static void disable(final View v) { enableIf(false, v); }

    public static void setText(final Activity activity, final int id, final int msgId) {
        final TextView t = find(activity, id);
        t.setText(msgId);
    }

    public static String getText(final View v, final int id) {
        return getText(find(v, id));
    }

    public static String getText(final TextView text) {
        return text.getText().toString();
    }

    public static void clear(final TextView v) {
        v.setText(R.string.empty);
    }

    public static < T extends View > T find(final Activity activity, final int id) {
        return (T) activity.findViewById(id);
    }

    public static < T extends View > T find(final View v, final int id) {
        return (T) v.findViewById(id);
    }

    public static < T extends View > T find(final Dialog dialog, final int id) {
        return (T) dialog.findViewById(id);
    }

    public static void showDialog(final Dialog dialog) {
        // (FIXME not sure if there's any smaller subset of these 3 calls below which works too)
        dialog.getWindow().clearFlags(LayoutParams.FLAG_NOT_FOCUSABLE |
                                      LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public static String getFeeRateString(final long feePerKB) {
        final double feePerByte = feePerKB / 1000.0;
        return Conversion.getNumberFormat(2).format(feePerByte) + " satoshi / vbyte";
    }

    // Return the translated string represented by the identifier given
    public static String i18n(final Resources res, final String textOrIdentifier) {
        if (TextUtils.isEmpty(textOrIdentifier))
            return "";
        if (!textOrIdentifier.startsWith("id_"))
            return textOrIdentifier; // Not a string id
        try {
            int resId = res.getIdentifier(textOrIdentifier, "string", PACKAGE_NAME);
            return res.getString(resId);
        } catch (final Exception e) {
            return textOrIdentifier; // Unknown id
        }
    }

}
