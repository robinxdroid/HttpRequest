package com.app.core.example.bean;

import java.io.Serializable;

/**
 * Created by Robin on 2016/5/12 10:08.
 */
public class BeautyBean implements Serializable{


    /**
     * title : 台大女神各种校花自拍照片合集
     * description : 台大女神各种校花自拍照片合集
     * picUrl : http://img9.tu11.com:8080/uploads/allimg/150418/1_041Q024134405.jpg
     * url : http://www.yixiuba.com/shenghuomeinvzipai/2015/6371.html
     */
    private String title;
    private String description;
    private String picUrl;
    private String url;

    public BeautyBean() {
        super();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "BeautyBean{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", picUrl='" + picUrl + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
