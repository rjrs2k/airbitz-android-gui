package com.airbitz.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.airbitz.R;
import com.airbitz.adapters.PasswordRecoveryAdapter;
import com.airbitz.api.CoreAPI;
import com.airbitz.api.SWIGTYPE_p_p_sABC_QuestionChoice;
import com.airbitz.api.SWIGTYPE_p_void;
import com.airbitz.api.core;
import com.airbitz.api.tABC_CC;
import com.airbitz.api.tABC_Error;
import com.airbitz.api.tABC_QuestionChoice;
import com.airbitz.api.tABC_QuestionChoices;
import com.airbitz.api.tABC_RequestResults;
import com.airbitz.utils.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2/10/14.
 * 6/11/14 twb: iPhone implements 6 questions, or skip this step. Two are String,
 * two are Numeric, and two are Address, in that order. See PasswordRecoveryViewcontroller.m.
 * Also, following questions in the same category don't repeat earlier questions.
 */
public class PasswordRecoveryActivity extends Activity {

    private Button mDoneSignUpButton;

    private ImageButton mHelpButton;
    private ImageButton mSkipStepButton;

    private TextView mTitleTextView;


    private String mUsername, mPassword;

    private View dummy;

    private Intent mIntent;

    private LinearLayout mLayoutRecovery;
    private LinearLayout mPasswordRecoveryListView;
    private List<View> mQuestionViews;
    private List<String> mQuestions;

    private FetchQuestionsTask mFetchQuestionsTask;
    private Map<String, Integer> mStringCategory = new HashMap<String, Integer>(); // Question, MinLength
    private List<String> currentStringCategory1;
    private List<String> currentStringCategory2;
    private List<String> mStringChosen = new ArrayList<String>();
    private Map<String, Integer> mNumericCategory = new HashMap<String, Integer>(); // Question, MinLength
    private List<String> currentNumericCategory1;
    private List<String> currentNumericCategory2;
    private List<String> mNumericChosen = new ArrayList<String>();
    private Map<String, Integer> mAddressCategory = new HashMap<String, Integer>(); // Question, MinLength
    private List<String> currentAddressCategory1;
    private List<String> currentAddressCategory2;
    private List<String> mAddressChosen = new ArrayList<String>();
    private View mQuestionViewString1;
    private View mQuestionViewString2;
    private View mQuestionViewString3;
    private View mQuestionViewString4;
    private View mQuestionViewString5;
    private View mQuestionViewString6;


    private SaveQuestionsTask mSaveQuestionsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsername = getIntent().getStringExtra(SignUpActivity.KEY_USERNAME);
        mPassword = getIntent().getStringExtra(SignUpActivity.KEY_PASSWORD);
        mUsername="junktest1"; // for testing
        mPassword="Aaaaaaaa1@"; // for testing
        String seed = "This is a 234-023-490 test seed string";
        CoreAPI.getApi().Initialize(this.getApplicationContext().getFilesDir().toString(), seed, seed.length());
        ;

        setContentView(R.layout.activity_password_recovery);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mIntent = new Intent(PasswordRecoveryActivity.this, NavigationActivity.class);

        mLayoutRecovery = (LinearLayout) findViewById(R.id.layout_pass_recovery);

        currentStringCategory1 = new ArrayList<String>();
        currentStringCategory2 = new ArrayList<String>();
        currentNumericCategory1 = new ArrayList<String>();
        currentNumericCategory2 = new ArrayList<String>();
        currentAddressCategory1 = new ArrayList<String>();
        currentAddressCategory2 = new ArrayList<String>();

        mHelpButton = (ImageButton) findViewById(R.id.button_help);

        mSkipStepButton = (ImageButton) findViewById(R.id.button_skip_step);
        mDoneSignUpButton = (Button) findViewById(R.id.button_complete_signup);
        mTitleTextView = (TextView) findViewById(R.id.textview_title);

        mTitleTextView.setTypeface(NavigationActivity.montserratBoldTypeFace);

        dummy = findViewById(R.id.password_dummy);

        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Common.showHelpInfo(PasswordRecoveryActivity.this, "Info", "Business directory info");
            }
        });

        mSkipStepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowSkipQuestionsAlert();
            }
        });
        mDoneSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CompleteSignup();
            }
        });

        mPasswordRecoveryListView = (LinearLayout) findViewById(R.id.password_recovery_listview);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mFetchQuestionsTask = new FetchQuestionsTask(mUsername);
        mFetchQuestionsTask.execute((Void) null);
    }

    private void CompleteSignup() {
        //verify that all six questions have been selected
        boolean allQuestionsSelected = true;
        boolean allAnswersValid = true;
        String questions = "";
        String answers = "";

        int count = 0;
        for (View view : mQuestionViews) {
            QuestionView qaView = (QuestionView) view;
            if (qaView.getQuestion().equals(getString(R.string.activity_recovery_question_default))) {
                allQuestionsSelected = false;
                break;
            }
            //verify that all six answers have achieved their minimum character limit
            if (qaView.getText().length() < qaView.getMinimumCharacters()) {
                allAnswersValid = false;
            } else {
                //add question and answer to arrays
                if (count != 0) {
                    questions += "\n";
                    answers += "\n";
                }
                questions += qaView.getQuestion();
                answers += qaView.getText();
            }
            count++;
        }
        if (allQuestionsSelected) {
            if (allAnswersValid) {
                mSaveQuestionsTask = new SaveQuestionsTask(mUsername, mPassword, questions, answers);
                mSaveQuestionsTask.execute((Void) null);
            } else {
                ShowAnswerAllQuestionsAlert();
            }
        } else {
            ShowPickAllQuestionsAlert();
        }
    }

    private void InitializeQuestionViews() {
        mQuestionViews = new ArrayList<View>();
        mQuestionViewString1 = new QuestionView(this, currentStringCategory1,currentStringCategory2, "string");
        mQuestionViewString2 = new QuestionView(this, currentStringCategory2,currentStringCategory1, "string");
        mQuestionViewString3 = new QuestionView(this, currentNumericCategory1,currentNumericCategory2, "numeric");
        mQuestionViewString4 = new QuestionView(this, currentNumericCategory2,currentNumericCategory1, "numeric");
        mQuestionViewString5 = new QuestionView(this, currentAddressCategory1,currentAddressCategory2, "address");
        mQuestionViewString6 = new QuestionView(this, currentAddressCategory2,currentAddressCategory1, "address");
        mQuestionViews.add(mQuestionViewString1);
        mQuestionViews.add(mQuestionViewString2);
        mQuestionViews.add(mQuestionViewString3);
        mQuestionViews.add(mQuestionViewString4);
        mQuestionViews.add(mQuestionViewString5);
        mQuestionViews.add(mQuestionViewString6);
        populateQuestionViews();
    }

    private void populateQuestionViews() {
        mPasswordRecoveryListView.removeAllViews();
        for(View v: mQuestionViews) {
            mPasswordRecoveryListView.addView(v);
        }
        mPasswordRecoveryListView.invalidate();
    }

    public void ShowSkipQuestionsAlert(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getResources().getString(R.string.activity_recovery_prompt_title))
                .setMessage(getResources().getString(R.string.activity_recovery_prompt_skip))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.string_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SuccessfulSignup();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.string_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void ShowAnswerAllQuestionsAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.activity_recovery_answer_questions_alert))
                .setTitle(getResources().getString(R.string.activity_recovery_alert_title))
                .setCancelable(false)
                .setNeutralButton(getResources().getString(R.string.string_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void ShowPickAllQuestionsAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.activity_recovery_pick_questions_alert))
                .setTitle(getResources().getString(R.string.activity_recovery_alert_title))
                .setCancelable(false)
                .setNeutralButton(getResources().getString(R.string.string_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void ShowSaveFailAlert(String reason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(reason)
                .setTitle(getResources().getString(R.string.activity_recovery_alert_title))
                .setCancelable(false)
                .setNeutralButton(getResources().getString(R.string.string_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Represents an asynchronous question fetch task
     */
    public class FetchQuestionsTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        QuestionChoice[] mChoices;
        tABC_Error pError = new tABC_Error();
        QuestionResults pData = new QuestionResults();
        SWIGTYPE_p_void pVoid = core.requestResultsp_to_voidp(pData);

        FetchQuestionsTask(String username) {
            mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            tABC_CC result = core.ABC_GetQuestionChoices(mUsername, null, pVoid, pError);
            if(result == tABC_CC.ABC_CC_Ok) {
                QuestionChoices qcs = new QuestionChoices(pData.getPtrPtr());
                long num = qcs.getNumChoices();
                mChoices = qcs.getChoices();

                if(num>0) {
                    for(QuestionChoice choice : mChoices) {
                        if(choice.mCategory.equals("string")) {
                            mStringCategory.put(choice.getQuestion(), new Integer((int) choice.getMinLength()));
                            currentStringCategory1.add(choice.getQuestion());
                            currentStringCategory2.add(choice.getQuestion());
                        } else if(choice.mCategory.equals("numeric")) {
                            mNumericCategory.put(choice.getQuestion(), new Integer((int) choice.getMinLength()));
                            currentNumericCategory1.add(choice.getQuestion());
                            currentNumericCategory2.add(choice.getQuestion());
                        } else if(choice.mCategory.equals("address")) {
                            mAddressCategory.put(choice.getQuestion(), new Integer((int) choice.getMinLength()));
                            currentAddressCategory1.add(choice.getQuestion());
                            currentAddressCategory2.add(choice.getQuestion());
                        }
                    }
                    currentStringCategory1.add(getString(R.string.activity_recovery_question_default));
                    currentStringCategory2.add(getString(R.string.activity_recovery_question_default));
                    currentNumericCategory1.add(getString(R.string.activity_recovery_question_default));
                    currentNumericCategory2.add(getString(R.string.activity_recovery_question_default));
                    currentAddressCategory1.add(getString(R.string.activity_recovery_question_default));
                    currentAddressCategory2.add(getString(R.string.activity_recovery_question_default));
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mFetchQuestionsTask = null;

            if (success) {
                InitializeQuestionViews();
            }

            dummy.requestFocus();
        }

        @Override
        protected void onCancelled() {
            mFetchQuestionsTask = null;
        }

    }

    /**
     * Represents an asynchronous question save task
     */
    public class SaveQuestionsTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername, mPassword, mQuestions, mAnswers;
        tABC_Error pError = new tABC_Error();
        tABC_RequestResults pResults = new tABC_RequestResults();
        SWIGTYPE_p_void pVoid = core.requestResultsp_to_voidp(pResults);

        SaveQuestionsTask(String username, String password, String questions, String answers) {
            mUsername = username;
            mPassword = password;
            mQuestions = questions;
            mAnswers = answers;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            tABC_CC result = core.ABC_SetAccountRecoveryQuestions(mUsername, mPassword, mQuestions, mAnswers, null, pVoid, pError);
            return result == tABC_CC.ABC_CC_Ok;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSaveQuestionsTask = null;
            if (!success) {
                ShowSaveFailAlert(pError.getSzDescription());
            } else {
                SuccessfulSignup();
            }
        }

        @Override
        protected void onCancelled() {
            mSaveQuestionsTask = null;
        }
    }

    private void SuccessfulSignup() {
        startActivity(mIntent);
        PasswordRecoveryActivity.this.finish();
    }

    private class QuestionResults extends tABC_RequestResults {
        public long getPtrPtr() {
           QuestionChoices fake = new QuestionChoices(getCPtr(this)); // A fake to get *ptr
           return fake.getNumChoices();
        }
    }

    private class QuestionChoices extends tABC_QuestionChoices {
        long mNumChoices = 0;
        long mChoiceStart = 0;
        QuestionChoice[] choices;

        public QuestionChoices (long pv) {
           super(pv, false);
           if(pv!=0) {
                mNumChoices = super.getNumChoices();
            }
        }

        public long getNumChoices() { return mNumChoices; }

        public QuestionChoice[] getChoices() {
            choices = new QuestionChoice[(int) mNumChoices];
            SWIGTYPE_p_p_sABC_QuestionChoice start = super.getAChoices();
            for(int i=0; i<mNumChoices; i++) {
                QuestionChoices fake = new QuestionChoices(PPVoid.getPtr(start, i*4));
                mChoiceStart = fake.getNumChoices();
                choices[i] = new QuestionChoice(new PVOID(mChoiceStart));
            }
            return choices;
        }
    }

    private class QuestionChoice extends tABC_QuestionChoice {
        String mQuestion = null;
        String mCategory = null;
        long mMinLength = -1;

        public QuestionChoice(SWIGTYPE_p_void pv) {
            super(PVoidStatic.getPtr(pv), false);
            if(PVoidStatic.getPtr(pv)!=0) {
                mQuestion = super.getSzQuestion();
                mCategory = super.getSzCategory();
                mMinLength = super.getMinAnswerLength();
            }
        }

        public String getQuestion() { return mQuestion; }

        public long getMinLength() { return mMinLength; }

        public String getCategory() { return mCategory; }

    }

    private class PVOID extends SWIGTYPE_p_void {
        public PVOID(long p) {
            super(p, false);
        }
    }

    private static class PVoidStatic extends SWIGTYPE_p_void {
        public static long getPtr(SWIGTYPE_p_void p) { return getCPtr(p); }
    }

    private static class PPVoid extends SWIGTYPE_p_p_sABC_QuestionChoice {
        public static long getPtr(SWIGTYPE_p_p_sABC_QuestionChoice p) { return getCPtr(p); }
        public static long getPtr(SWIGTYPE_p_p_sABC_QuestionChoice p, long i) { return getCPtr(p)+i; }
    }

    private class QuestionView extends LinearLayout {
        Context mContext;
        private String mType;
        private String chosenQuestion = "";
        private int mCharLimit = 0;
        private Spinner mSpinner;
        private EditText mText;
        private PasswordRecoveryAdapter mAdapter;
        private List<String> currentQuestionList;
        private List<String> otherQuestionList;

        public QuestionView(Context context, List<String> questions,List<String> otherQuestions, String type) {
            super(context);
            mContext = context;
            this.mType = type;
            currentQuestionList = questions;
            otherQuestionList = otherQuestions;
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.item_password_recovery, this);
            mSpinner = (Spinner)findViewById(R.id.item_password_recovery_spinner);
            mAdapter = new PasswordRecoveryAdapter(context, currentQuestionList);
            mAdapter.setDropDownViewResource(R.layout.item_password_recovery_spinner_dropdown);
            mSpinner.setAdapter(mAdapter);
            mSpinner.setDropDownWidth((int)getResources().getDimension(R.dimen.spinner_width_password));
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if(mType.compareTo("string")==0){
                        for(String s: mStringCategory.keySet()){
                            if(s.compareTo((String)mSpinner.getSelectedItem())==0){
                                if(mSpinner.getSelectedItemPosition() != mAdapter.getCount()) {
                                    mCharLimit = mStringCategory.get(s).intValue();
                                    if(!chosenQuestion.isEmpty()) {
                                        otherQuestionList.add(chosenQuestion);
                                    }
                                    chosenQuestion = s;
                                    otherQuestionList.remove(s);
                                    List<String> tempList = new ArrayList<String>();
                                    for(String quest: mStringCategory.keySet()){
                                        if(otherQuestionList.contains(quest)){
                                            tempList.add(quest);
                                        }
                                    }
                                    tempList.add("Question");
                                    otherQuestionList.clear();
                                    otherQuestionList.addAll(tempList);
                                    //mAdapter.notifyDataSetChanged();
                                }else{
                                    mCharLimit = 0;
                                }
                            }
                        }
                    }else if(mType.compareTo("numeric")==0){
                        for(String s: mNumericCategory.keySet()){
                            if(s.compareTo((String)mSpinner.getSelectedItem())==0){
                                if(mSpinner.getSelectedItemPosition() != mAdapter.getCount()) {
                                    mCharLimit = mNumericCategory.get(s).intValue();
                                    if(!chosenQuestion.isEmpty()) {
                                        otherQuestionList.add(chosenQuestion);
                                    }
                                    chosenQuestion = s;
                                    otherQuestionList.remove(s);
                                    List<String> tempList = new ArrayList<String>();
                                    for(String quest: mNumericCategory.keySet()){
                                        if(otherQuestionList.contains(quest)){
                                            tempList.add(quest);
                                        }
                                    }
                                    tempList.add("Question");
                                    otherQuestionList.clear();
                                    otherQuestionList.addAll(tempList);
                                    //mAdapter.notifyDataSetChanged();
                                }else{
                                    mCharLimit = 0;
                                }
                            }
                        }
                    }else if(mType.compareTo("address")==0){
                        for(String s: mAddressCategory.keySet()){
                            if(s.compareTo((String)mSpinner.getSelectedItem())==0){
                                if(mSpinner.getSelectedItemPosition() != mAdapter.getCount()) {
                                    mCharLimit = mAddressCategory.get(s).intValue();
                                    if(!chosenQuestion.isEmpty()) {
                                        otherQuestionList.add(chosenQuestion);
                                    }
                                    chosenQuestion = s;
                                    otherQuestionList.remove(s);
                                    List<String> tempList = new ArrayList<String>();
                                    for(String quest: mAddressCategory.keySet()){
                                        if(otherQuestionList.contains(quest)){
                                            tempList.add(quest);
                                        }
                                    }
                                    tempList.add("Question");
                                    otherQuestionList.clear();
                                    otherQuestionList.addAll(tempList);
                                    //mAdapter.notifyDataSetChanged();
                                }else{
                                    mCharLimit = 0;
                                }
                            }
                        }
                    }
                    if(mSpinner.getSelectedItemPosition() != mAdapter.getCount()) {
                        mText.setFocusableInTouchMode(true);  //if this is not already set
                        mText.requestFocus();  //to move the cursor
                        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(mText, InputMethodManager.SHOW_FORCED);
                        mText.setCursorVisible(true);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            mText = (EditText) findViewById(R.id.item_password_recovery_answer);
            final View redRing = findViewById(R.id.red_ring);
            mText.setTypeface(NavigationActivity.montserratRegularTypeFace);
//        edittext.setOnKeyListener(new View.OnKeyListener() {
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                // If the event is a key-down event on the "enter" button
//                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
//                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
//                    // Perform action on key press
////                    View newQuestion = getQuestionView();
////                    mQuestionViews.add(newQuestion);
////                    populateQuestionViews();
////                    newQuestion.requestFocus();
//                    return true;
//                }
//                return false;
//            }
//        });

            mText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    try{
                        if(!mText.getText().toString().isEmpty() && mText.getText().toString().length() < mCharLimit){
                            redRing.setVisibility(View.VISIBLE);
                        }else{
                            redRing.setVisibility(View.GONE);
                        }
                    }catch(Exception e ){

                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
            mSpinner.setSelection(mAdapter.getCount());
            mText.clearFocus();
            mText.setCursorVisible(false);
        }

        public void setQuestions(List<String> questions) {

        }

        public String getQuestion() {
            return ((TextView) mSpinner.getSelectedView()).getText().toString();
        }

        public String getText() {
            return mText.getText().toString();
        }

        public int getMinimumCharacters() {
            return mCharLimit;
        }
    }

}

