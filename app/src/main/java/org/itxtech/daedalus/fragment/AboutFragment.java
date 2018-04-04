package org.itxtech.daedalus.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.itxtech.daedalus.R;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class AboutFragment extends ToolbarFragment {

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        ((TextView) view.findViewById(R.id.website_link)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) view.findViewById(R.id.telegram_link)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) view.findViewById(R.id.email_link)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) view.findViewById(R.id.github_link)).setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    public void checkStatus() {
        menu.findItem(R.id.nav_about).setChecked(true);
        toolbar.setTitle(R.string.action_about);
    }
}
