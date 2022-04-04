package com.hit.wi.bean;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.List;

/**
 * Auto-generated: 2022-04-04 15:37:24
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRootBean {

    private List<String> returnPhrase;
    private String query;
    private String errorCode;
    private String l;
    private String tSpeakUrl;
    private List<Web> web;
    private String requestId;
    private List<String> translation;
    private Dict dict;
    private Webdict webdict;
    private Basic basic;
    private boolean isWord;
    private String speakUrl;
    public void setReturnPhrase(List<String> returnPhrase) {
        this.returnPhrase = returnPhrase;
    }
    public List<String> getReturnPhrase() {
        return returnPhrase;
    }

    public void setQuery(String query) {
        this.query = query;
    }
    public String getQuery() {
        return query;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    public String getErrorCode() {
        return errorCode;
    }

    public void setL(String l) {
        this.l = l;
    }
    public String getL() {
        return l;
    }

    public void setTSpeakUrl(String tSpeakUrl) {
        this.tSpeakUrl = tSpeakUrl;
    }
    public String getTSpeakUrl() {
        return tSpeakUrl;
    }

    public void setWeb(List<Web> web) {
        this.web = web;
    }
    public List<Web> getWeb() {
        return web;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    public String getRequestId() {
        return requestId;
    }

    public void setTranslation(List<String> translation) {
        this.translation = translation;
    }
    public List<String> getTranslation() {
        return translation;
    }

    public void setDict(Dict dict) {
        this.dict = dict;
    }
    public Dict getDict() {
        return dict;
    }

    public void setWebdict(Webdict webdict) {
        this.webdict = webdict;
    }
    public Webdict getWebdict() {
        return webdict;
    }

    public void setBasic(Basic basic) {
        this.basic = basic;
    }
    public Basic getBasic() {
        return basic;
    }

    public void setIsWord(boolean isWord) {
        this.isWord = isWord;
    }
    public boolean getIsWord() {
        return isWord;
    }

    public void setSpeakUrl(String speakUrl) {
        this.speakUrl = speakUrl;
    }
    public String getSpeakUrl() {
        return speakUrl;
    }

}