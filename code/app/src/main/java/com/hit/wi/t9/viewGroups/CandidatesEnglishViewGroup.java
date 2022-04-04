package com.hit.wi.t9.viewGroups;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hit.wi.bean.JsonRootBean;
import com.hit.wi.jni.Kernel;
import com.hit.wi.t9.functions.QKEmojiManager;
import com.hit.wi.t9.values.Global;
import com.hit.wi.t9.view.QuickButton;
import com.hit.wi.util.DisplayUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.RequiresApi;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 全键
 * 候选词管理器
 * <p>
 * author 郭高扬
 * author purebluesong
 */
public class CandidatesEnglishViewGroup extends ScrolledViewGroup {

    private final float SCROLL_LARGE_HEIGHT_RATE = (float) 1.5;
    private final float TEXTSIZE_RATE = (float) 0.4;
    private final int TEXT_LENGTH_FACTOR = 3;
    private final int WORD_MAX_NUM = 300;
    private final int WORD_NUM_LAZY_LOAD = 6;

    private static final String YOUDAO_URL = "https://openapi.youdao.com/api";

    private static final String APP_KEY = "291aec2ceea5fef0";

    private static final String APP_SECRET = "ioydYqIcO5SM2kl9Fwkigr1efjoGoGyX";


    /**
     * Maximum number of displaying candidates par one line (full view mode)
     */
    private final int CAND_DIV_NUM = 6;
    /**
     * min value of the layer show num
     */
    private final int MIN_LAYER_SHOWNUM = 10;
    /**
     * general information about a display
     */
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    /**
     * EmojiUtil of QK
     */
    private QKEmojiManager mQKEmojiManager;
    /**
     * EmojiUtil of T9
     */

    private int mTextColor;
    private int mBackColor;

    private String mQKOrEmoji = Global.QUANPIN;

    private int standardButtonWidth;
    private int standardButtonHeight;

    private List<LinearLayout> layerList;
    private LinearLayout.LayoutParams layerParams;
    private int layerPointer;

    public CandidatesEnglishViewGroup() {
        mMetrics.setToDefaults();
        layerList = new ArrayList<>();
        layerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void create(Context context) {
        super.create(vertical, context);
        mTextColor = softKeyboard.skinInfoManager.skinData.textcolor_candidate_t9;
        mBackColor = softKeyboard.skinInfoManager.skinData.backcolor_candidate_t9;
        mQKEmojiManager = new QKEmojiManager(softKeyboard);
        standardButtonHeight = 0;
        standardButtonWidth = 0;
        scrollView.setOnTouchListener(scrollOnTouchListener);
//        SharedPreferences sp = SharedPreferenceManager.getDefaultSharedPreferences(context);
    }

    // state machine
    public void refreshState(boolean hide, String type) throws Exception, InterruptedException {
        if (hide) {
            Kernel.cleanKernel();
            if (scrollView.isShown()) {
                hide();
            }
        } else {
            if (!scrollView.isShown()) {
                show();
            }
            if (Kernel.getWordsNumber() > 0) {
                displayCandidates(type);
            }
            scrollView.fullScroll(ScrollView.FOCUS_UP);
        }
    }

    @Override
    public void show() {
        for (LinearLayout layout : layerList) {
            layout.setVisibility(View.VISIBLE);
        }
        clearAnimation();
        setVisibility(View.VISIBLE);
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        standardButtonWidth = width / CAND_DIV_NUM;
        standardButtonHeight = height;
        resetButtonWidthAndResetLayerHeight();
    }

    public void resetButtonWidthAndResetLayerHeight() {
        for (QuickButton button : buttonList) {
            button.itsLayoutParams.width = measureButtonLengthByText(button.getText());
            button.itsLayoutParams.height = standardButtonHeight;
        }
        layerParams.height = standardButtonHeight;
        updateViewLayout();
    }

    public void displayCandidates() throws Exception, InterruptedException {
        if (mQKOrEmoji.equals(Global.QUANPIN) || mQKOrEmoji.equals(Global.EMOJI)) {
            displayCandidates(mQKOrEmoji, WORD_MAX_NUM);
        } else {
            displayCandidates(mQKOrEmoji, symbols, WORD_MAX_NUM);
        }
    }

    public void displayCandidates(String type) throws Exception, InterruptedException {
        displayCandidates(type, WORD_NUM_LAZY_LOAD);
    }

    public void displayCandidates(String type, int show_num) throws Exception, InterruptedException {
        displayCandidates(type, Collections.EMPTY_LIST, show_num);
    }

    List<String> symbols;

    public void displayCandidates(String type, List<String> strings, int show_num) throws Exception, InterruptedException {
        mQKOrEmoji = type;
        int i = 0;
        List<String> words = new ArrayList<>();
        symbols = strings;
        if (!strings.equals(Collections.EMPTY_LIST)) {
            words = strings.subList(0, Math.min(show_num, strings.size() - 1));
        } else {
            int total = Math.min(show_num, Kernel.getWordsNumber() - 1);
            while (i < total) {
                String text = Kernel.getWordByIndex(i);
                words.add(mQKEmojiManager.getShowString(text));
                i++;
            }
        }

        setCandidates(words);
        scrollView.fullScroll(View.FOCUS_UP);//go to top
    }

    private LinearLayout getWorkingLayer(int position) {
        if (layerList.size() <= position) {
            return addLayer();
        } else {
            return layerList.get(position);
        }
    }

    private int getLayerRemainLength(LinearLayout layer) {
        int remainLength = standardButtonWidth * CAND_DIV_NUM;
        for (int i = layer.getChildCount() - 1; i >= 0; i--) {
            QuickButton button = (QuickButton) layer.getChildAt(i);
            remainLength -= button.itsLayoutParams.width;
        }
        return remainLength;
    }

    private LinearLayout addLayer() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setVisibility(View.VISIBLE);
        layoutforWrapButtons.addView(layout, layerParams);
        layerList.add(layout);
        return layout;
    }


    private QuickButton initNewButton(String text) {
        QuickButton button = super.addButton(mTextColor, mBackColor, text);
        button.setVisibility(View.VISIBLE);
        button.setPressed(false);
        button.setOnTouchListener(mCandidateOnTouch);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                measureButtonLengthByText(text), ViewGroup.LayoutParams.MATCH_PARENT
        );
        button.itsLayoutParams = params;
        button.setLayoutParams(params);

        button.setTextSize(DisplayUtil.px2sp(context, Global.textsizeFactor * standardButtonHeight * TEXTSIZE_RATE));
        return button;
    }

    /**
     * Add a candidate into the list
     */
    private QuickButton addButtonQ(String text, QuickButton button) {
        LinearLayout layer = getWorkingLayer(layerPointer);
        int remainLength = getLayerRemainLength(layer);
        button.setText(text);
        button.itsLayoutParams.width = measureButtonLengthByText(text);
        if (button.itsLayoutParams.width > remainLength) {
            if (remainLength > 20) {
                ((QuickButton) layer.getChildAt(layer.getChildCount() - 1)).itsLayoutParams.width += remainLength;
            }
            layerPointer++;
            layer = getWorkingLayer(layerPointer);
        }
        layer.addView(button, button.itsLayoutParams);
        button.clearAnimation();
        button.setEllipsize(standardButtonWidth * CAND_DIV_NUM <= measureButtonLengthByText(text) ? TextUtils.TruncateAt.END : null);
        button.getBackground().setAlpha(Global.getCurrentAlpha());
        button.setPressed(false);
        button.setTextSize(DisplayUtil.px2sp(context, Global.textsizeFactor * standardButtonHeight * TEXTSIZE_RATE));
        return button;
    }

    @SuppressLint("NewApi")
    public void setCandidates(List<String> words) throws Exception, InterruptedException {
        Log.d("输入文字12",words.get(0));
        Map<String,String> params = new HashMap<String,String>();
        String q = words.get(0);
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("from", "zh-CHS");
        params.put("to", "en");
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);
        String signStr = APP_KEY + truncate(q) + salt + curtime + APP_SECRET;
        String sign = getDigest(signStr);
        params.put("appKey", APP_KEY);
        params.put("q", q);
        params.put("salt", salt);
        params.put("sign", sign);
        String a=sendPost_body(YOUDAO_URL,null,params);
        Log.d("输入文字15",a);
        int i = 0;
        for (LinearLayout layout : layerList) {
            layout.removeAllViews();
        }
        layerPointer = 0;

        for (; i < buttonList.size() && i < words.size(); i++) {
            if (i==0){
                Log.d("输入文字16",words.get(i));
                addButtonQ(a, buttonList.get(i));
            }else{
                Log.d("输入文字17",words.get(i));
                addButtonQ(words.get(i), buttonList.get(i));
            }
        }
        for (; i < words.size(); i++) {
            if (i==0){
                Log.d("输入文字18",words.get(i));
            QuickButton button = initNewButton(a);
                button.setVisibility(View.VISIBLE);
                buttonList.add(button);
                addButtonQ(words.get(i), button);
            }
            else{
                Log.d("输入文字19",words.get(i));
                QuickButton button = initNewButton(words.get(i));
                button.setVisibility(View.VISIBLE);
                buttonList.add(button);
                addButtonQ(words.get(i), button);
            }

        }
        for (; i < buttonList.size(); i++) {
            buttonList.get(i).setText("");//clear button
        }

        int j = 0;
        for (; j < layerPointer; j++) {
            layerList.get(j).setVisibility(View.VISIBLE);
        }
        if (j < MIN_LAYER_SHOWNUM) j = MIN_LAYER_SHOWNUM;
        for (; j < layerList.size(); j++) {
            layerList.get(j).setVisibility(View.GONE);
        }
        touched = false;
    }

    private int measureButtonLengthByText(CharSequence text) {
        Paint paint = new Paint();
        Log.d("输入文字11",text.toString());
        int measuredTextLength = (int) (Global.textsizeFactor * TEXT_LENGTH_FACTOR * paint.measureText((String) text));
        int divNum = Math.min(measuredTextLength / standardButtonWidth + 1, CAND_DIV_NUM);
        return divNum * standardButtonWidth;
    }

    public void updateSkin() {
        if (Global.currentKeyboard == Global.KEYBOARD_QK || Global.currentKeyboard == Global.KEYBOARD_EN) {
            mBackColor = softKeyboard.skinInfoManager.skinData.backcolor_candidate_qk;
            mTextColor = softKeyboard.skinInfoManager.skinData.textcolor_candidate_qk;
        } else {
            mBackColor = softKeyboard.skinInfoManager.skinData.backcolor_candidate_t9;
            mTextColor = softKeyboard.skinInfoManager.skinData.textcolor_candidate_t9;
        }
        super.updateSkin(mTextColor, mBackColor);
    }

    public void largeTheCandidate() {
        softKeyboard.functionViewGroup.setVisibility(View.GONE);
        softKeyboard.mInputViewGG.setVisibility(View.GONE);
        softKeyboard.secondLayerLayout.setVisibility(View.GONE);
        int largeheight = softKeyboard.keyboardParams.height - softKeyboard.bottomBarViewGroup.getHeight() - softKeyboard.standardVerticalGapDistance;
        if (getHeight() != largeheight) {
            setHeight(largeheight);
        }
    }

    public void smallTheCandidate() {
        softKeyboard.viewSizeUpdate.UpdateCandidateSize();
        if (Global.currentKeyboard == Global.KEYBOARD_QK || Global.currentKeyboard == Global.KEYBOARD_EN) {
            softKeyboard.functionViewGroup.setVisibility(View.VISIBLE);
        } else {
            softKeyboard.secondLayerLayout.setVisibility(View.VISIBLE);
        }
        softKeyboard.mInputViewGG.setVisibility(View.VISIBLE);
    }

    private void commitQKCandidate(View v) {
        String text = Kernel.getWordSelectedWord(buttonList.indexOf(v));
        scrollView.fullScroll(ScrollView.FOCUS_UP);
        InputConnection ic = softKeyboard.getCurrentInputConnection();
        if (ic != null && text != null) {
            try {
                mQKEmojiManager.commitEmoji(softKeyboard, text);
            } catch (Exception e) {
                Log.d("WIVE", "expception" + e.toString());
            }
        }
    }

    private void commitT9Candidate(View v) {
        String text = Kernel.getWordSelectedWord(buttonList.indexOf(v));
        if (text != null) {
            softKeyboard.commitText(text);
        }
    }

    private void onTouchEffect(View v, MotionEvent event) {
        softKeyboard.transparencyHandle.handleAlpha(event.getAction());
        softKeyboard.keyboardTouchEffect.onTouchEffect(v, event.getAction(),
                skinInfoManager.skinData.backcolor_touchdown,
                mBackColor
        );
    }

    private float downX, downY;
    private boolean touched = false;

    /**
     * Event listener for touching a candidate
     */

    protected boolean onCandidateTouchEvent(View v, MotionEvent event) throws Exception {
        onTouchEffect(v, event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!touched) {
                    displayCandidates();
                    touched = true;
                }
                downX = event.getX();
                downY = event.getY();
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getY() - downY) + Math.abs(event.getX() - downX) < 50) break;
                if (!Global.inLarge) {
                    largeTheCandidate();
                    softKeyboard.bottomBarViewGroup.intoReturnState();
                    Global.inLarge = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!Global.isInView(v, event)) break;
                if (Global.inLarge) {
                    smallTheCandidate();
                    softKeyboard.bottomBarViewGroup.backReturnState();
                    Global.inLarge = false;
                }
                if (Global.currentKeyboard == Global.KEYBOARD_SYM) {
                    CharSequence text = ((TextView) v).getText();
                    if (!softKeyboard.quickSymbolViewGroup.isLock()) {
                        int inputKeyboard = PreferenceManager.getDefaultSharedPreferences(context).getString("KEYBOARD_SELECTOR", "2").equals("1") ?
                                Global.KEYBOARD_T9 : Global.KEYBOARD_QK;
                        softKeyboard.switchKeyboardTo(inputKeyboard, true);
                    }
                    softKeyboard.commitText(text);
                } else if (mQKOrEmoji.equals(Global.SYMBOL)) {
                    CharSequence text = ((TextView) v).getText();
                    softKeyboard.commitText(text);
                    softKeyboard.refreshDisplay();
                } else if (mQKOrEmoji.equals(Global.QUANPIN)) {
                    commitQKCandidate(v);
                    softKeyboard.refreshDisplay();
                } else {
                    commitT9Candidate(v);
                    softKeyboard.refreshDisplay();
                }
                touched = false;
                break;
        }
        return true;
    }

    private OnTouchListener mCandidateOnTouch = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            boolean a=false;
            try {
                a= onCandidateTouchEvent(v, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return  a;
        }
    };

    protected void onScrollOnTouchEvent(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (v.getScrollY() == 0
                    && event.getY() > v.getHeight() * SCROLL_LARGE_HEIGHT_RATE
                    && !Global.inLarge) {

                largeTheCandidate();
                softKeyboard.bottomBarViewGroup.intoReturnState();
                Global.inLarge = true;

            } else if (v.getScrollY() + v.getHeight() == scrollView.getChildAt(0).getMeasuredHeight()
                    && event.getY() < 0
                    && Global.inLarge) {

                smallTheCandidate();
                softKeyboard.bottomBarViewGroup.backReturnState();
                Global.inLarge = false;

                softKeyboard.functionViewGroup.refreshState(false);
                softKeyboard.functionsC.refreshStateForSecondLayout();
                softKeyboard.prefixViewGroup.refreshState();
                softKeyboard.quickSymbolViewGroup.refreshState();
//                    scrollView.fullScroll(ScrollView.FOCUS_UP);
            }
        }
    }

    private OnTouchListener scrollOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            onScrollOnTouchEvent(v, event);
            return scrollView.onTouchEvent(event);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String sendPost_body(String url, Map<String, String> map_header, Map<String, String> map_body) throws Exception, InterruptedException {
        final String[] a = new String[1];
        Runnable requestTask = new Runnable() {
            @Override
            public void run() {
                //添加header信息
                Headers.Builder builder_header = new Headers.Builder();
//        for (String key : map_header.keySet()) {
//            builder_header.add(key, Objects.requireNonNull(map_header.get(key)));
//        }
                Headers headers = builder_header.build();
                //添加参数
                FormBody.Builder builder_body = new FormBody.Builder();
                for (String key : map_body.keySet()) {
                    builder_body.add(key, Objects.requireNonNull(map_body.get(key)));
                }
                FormBody formBody = builder_body.build();

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).headers(headers).post(formBody).build();
//        LocalTime startTime = LocalTime.now();//响应开始时间
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    // bean和String相互转化需要的类
                    ObjectMapper ojbectMapper = new ObjectMapper();
                    //String 转bean
                    try {
                        JsonRootBean jrb=ojbectMapper.readValue(response.body().string(),JsonRootBean.class);
                         a[0] =jrb.getTranslation().get(0);
                        Log.d("输入文字14",jrb.getTranslation().get(0));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
//        LocalTime endTime = LocalTime.now();//响应结束时间
//        int code = response.code();
//        if (code != 200) {
//            Log.e("输入文字13", url + "\n" + response);
//        }
//        Map<String, Object> map = new HashMap<>();
//        map.put("code", code);
//        map.put("entity", Objects.requireNonNull(response.body()).string());
//        map.put("responseTime", Duration.between(startTime, endTime).toMillis());
//        response.close();

            }
    };

        Thread requestThread = new Thread(requestTask);
        requestThread.start();
        requestThread.join();
        return a[0];
    }
    /**
     * 生成加密字段
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getDigest(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     *
     * @param result 音频字节流
     * @param file 存储路径
     */
    private static void byte2File(byte[] result, String file) {
        File audioFile = new File(file);
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(audioFile);
            fos.write(result);

        }catch (Exception e){

        }finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        String result;
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }
}
