/**
 * Copyright (c) 2014, Airbitz Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms are permitted provided that
 * the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Redistribution or use of modified source code requires the express written
 *    permission of Airbitz Inc.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the Airbitz Project.
 */

package com.airbitz.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.airbitz.AirbitzApplication;
import com.airbitz.R;
import com.airbitz.api.WalletWrapper;

import co.airbitz.core.Account;
import co.airbitz.core.Utils;
import co.airbitz.core.Wallet;

import java.util.List;

public class WalletOtherAdapter extends ArrayAdapter {

    private Context mContext;
    private List<WalletWrapper> mWalletList;
    private Typeface mBitcoinTypeface;
    private Account mAccount;

    public WalletOtherAdapter(Context context, List<WalletWrapper> walletList) {
        super(context, R.layout.item_request_wallet_spinner, walletList);
        mContext = context;
        mWalletList = walletList;
        mAccount = AirbitzApplication.getAccount();
        mBitcoinTypeface = Typeface.createFromAsset(context.getAssets(), "font/Lato-Regular.ttf");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        WalletWrapper wallet = mWalletList.get(position);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.item_request_wallet_spinner_dropdown, parent, false);
        TextView textView = (TextView) convertView.findViewById(R.id.item_request_wallet_spinner_dropdown_textview);
        textView.setTypeface(mBitcoinTypeface);
        textView.setText(wallet.name(mContext) + " (" + Utils.formatSatoshi(mAccount, wallet.wallet().balance(), true) + ")");
        textView.setBackground(mContext.getResources().getDrawable(R.drawable.dropdown_item_selector));

        return convertView;
    }
}
