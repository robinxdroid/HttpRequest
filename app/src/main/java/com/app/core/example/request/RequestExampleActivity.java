package com.app.core.example.request;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.app.core.example.ILogProcessor;
import com.app.core.example.LogFormattedString;
import com.app.core.example.LogProcessor;
import com.app.core.example.R;
import com.app.core.example.bean.BeautyBean;
import com.app.core.example.bean.CityRootBean;
import net.robinx.lib.http.XRequest;
import net.robinx.lib.http.base.Request;
import net.robinx.lib.http.cache.disk.DiskCache;
import net.robinx.lib.http.config.DataType;
import net.robinx.lib.http.config.HttpMethod;
import net.robinx.lib.http.config.RequestCacheOptions;
import net.robinx.lib.http.converters.JackSonConverter;
import net.robinx.lib.http.callback.OnRequestListenerAdapter;
import net.robinx.lib.http.callback.OnRequestListener;
import net.robinx.lib.http.network.HttpError;
import net.robinx.lib.http.network.HttpException;
import net.robinx.lib.http.network.ex.DownloadRequest;
import net.robinx.lib.http.network.ex.MultipartRequest;
import net.robinx.lib.http.network.ex.HttpRequest;
import net.robinx.lib.http.network.ex.ImageRequest;
import net.robinx.lib.http.network.ex.SyncRequest;
import net.robinx.lib.http.network.ex.RequestParams;
import net.robinx.lib.http.network.ex.hurl.HurlRequestBody;
import net.robinx.lib.http.network.ex.hurl.HurlStack;
import net.robinx.lib.http.response.NetworkResponse;
import net.robinx.lib.http.utils.CLog;
import net.robinx.lib.http.okhttp.OkHttpStack;
import net.robinx.lib.http.okhttp.OkRequestBody;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class RequestExampleActivity extends Activity {

    private Context context = this;

    /**
     * 请求的tag，可根据此tag取消请求
     */
    private Object mRequestTag = this;

    private ProgressBar mUploadProgressBar;
    private ProgressBar mDownloadProgressBar;

    private ImageView mLoadImageView;

    private Button mUploadButton;

    private RadioButton mOkHttpRadioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_example);

        CLog.printTagPrefixOnly = true; // 日志只打印前缀，不打印方法名与行号

        initView();

        initLog();

    }

    /**
     * 初始化View
     */
    private void initView() {
        RadioGroup stackRadioGroup = (RadioGroup) findViewById(R.id.rg_stack);
        RadioButton hurlRadioButton = (RadioButton) findViewById(R.id.rb_hurl);
        mOkHttpRadioButton = (RadioButton) findViewById(R.id.rb_okhttp);

        stackRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_hurl:
                        XRequest.INSTANCE.setStack(new HurlStack());
                        break;
                    case R.id.rb_okhttp:
                        XRequest.INSTANCE.setStack(new OkHttpStack());
                        break;
                }
            }
        });

        mOkHttpRadioButton.setChecked(true);


        int[] ids = new int[]{R.id.btn_post, R.id.btn_get, R.id.btn_json_post, R.id.btn_custom, R.id.btn_custom_list, R.id.btn_file_download, R.id.btn_load_image, R.id.btn_sync_request};
        for (int i = 0; i < ids.length; i++) {
            findViewById(ids[i]).setOnClickListener(getOnClickListener());
        }

        mUploadButton = (Button) findViewById(R.id.btn_file_upload);
        mUploadButton.setOnClickListener(getOnClickListener());

        final EditText diskCacheMaxSizeEditText = (EditText) findViewById(R.id.et_disk_cache_max_size);
        final EditText diskCacheDirEditText = (EditText) findViewById(R.id.et_disk_cache_dir);
        final TextView diskCacheCurrentSizeTextView = (TextView) findViewById(R.id.tv_disk_cache_current_size);
        Button refreshButton = (Button) findViewById(R.id.btn_refresh);
        Button deleteButton = (Button) findViewById(R.id.btn_delete);
        mUploadProgressBar = (ProgressBar) findViewById(R.id.pb_upload);
        mDownloadProgressBar = (ProgressBar) findViewById(R.id.pb_download);
        mLoadImageView = (ImageView) findViewById(R.id.img);

        //刷新当前缓存数据
        refreshButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                diskCacheCurrentSizeTextView.setText(DiskCache.INSTANCE.getCurrentSize() / (1024) + "KB");
            }
        });

        //删除所有缓存
        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DiskCache.INSTANCE.clear();

                long diskCacheMaxSize = DiskCache.INSTANCE.getMaxSize();
                String diskCacheDir = DiskCache.INSTANCE.getDirectory().getPath();
                long diskCacheCurrentSize = DiskCache.INSTANCE.getCurrentSize();

                diskCacheMaxSizeEditText.setText(diskCacheMaxSize / (1024 * 1024) + "M");
                diskCacheDirEditText.setText(diskCacheDir);
                diskCacheCurrentSizeTextView.setText(diskCacheCurrentSize / (1024) + "KB");
            }
        });


        //读取缓存相关数据
        long diskCacheMaxSize = DiskCache.INSTANCE.getMaxSize();
        String diskCacheDir = DiskCache.INSTANCE.getDirectory().getPath();
        long diskCacheCurrentSize = DiskCache.INSTANCE.getCurrentSize();

        diskCacheMaxSizeEditText.setText(diskCacheMaxSize / (1024 * 1024) + "M");
        diskCacheDirEditText.setText(diskCacheDir);
        diskCacheCurrentSizeTextView.setText(diskCacheCurrentSize / (1024) + "KB");
    }

    public OnClickListener getOnClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_post:
                        post();
                        break;
                    case R.id.btn_get:
                        get();
                        break;
                    case R.id.btn_json_post:
                        jsonPost();
                        break;
                    case R.id.btn_file_upload:
                        uploadFile();
                        break;
                    case R.id.btn_file_download:
                        downloadFile();
                        break;
                    case R.id.btn_custom:
                        convertToBean();
                        break;
                    case R.id.btn_custom_list:
                        convertToBeanList();
                        break;
                    case R.id.btn_sync_request:
                        new Thread(){
                            @Override
                            public void run() {
                                syncRequest();
                            }
                        }.start();

                        break;
                    case R.id.btn_load_image:
                        String url = "https://raw.githubusercontent.com/robinxdroid/XDroidCache/master/screenshoot3.png";
                        ImageRequest request = new ImageRequest(url, new OnRequestListenerAdapter<Bitmap>() {

                            public void onRequestDownloadProgress(Request<?> request, long transferredBytesSize, long totalSize) {
                                CLog.i("正在加载图片， 当前进度：%d , 总大小 : %d", transferredBytesSize, totalSize);
                            }

                            @Override
                            public void onDone(Request<?> request, Map<String, String> headers, Bitmap result, DataType dataType) {
                                super.onDone(request, headers, result, dataType);
                                mLoadImageView.setImageBitmap(result);
                                CLog.i(request.getRequestCacheOptions().toString());
                            }
                        });
                        XRequest.INSTANCE.addToRequestQueue(request);
                        break;

                }
            }

        };
    }

	/*
     * ========================================================================
	 * 以下为请求示例
	 * ========================================================================
	 */

    /**
     * 带参数的POST 请求
     */
    private void post() {
        RequestParams params = new RequestParams();
        params.putHeaders("apikey", "ae75f7350ede43701ce8a5ad8a161ff9");
        params.putParams("city", "hefei");

        MultipartRequest.Builder<String> builder = new MultipartRequest.Builder<>();
        HttpRequest<String> request = builder
                .requestParams(params)
                .httpMethod(HttpMethod.POST)
                .url("http://apis.baidu.com/heweather/weather/free")
                .cacheKey("http://apis.baidu.com/heweather/weather/free" + "post") //与GET请求的URL一样，为了避免同样的缓存key、这里重新指定缓存key
                .tag(mRequestTag)
                .onRequestListener(getOnPostRequestListener())
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .build()
                .execute();
    }

    public OnRequestListener getOnPostRequestListener() {
        return new OnRequestListenerAdapter<String>() {
            @Override
            public void onRequestFailed(Request<?> request, HttpException httpException) {
                super.onRequestFailed(request, httpException);
                switch (httpException.getHttpErrorCode()) {
                    case HttpError.ERROR_NOT_NETWORK:
                        Toast.makeText(context, "网络未连接，请检查", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onRequestRetry(Request<?> request, int currentRetryCount, HttpException previousError) {
                Toast.makeText(context, "获取信息失败，系统已经为您重试" + currentRetryCount + "次", Toast.LENGTH_SHORT).show();

                CLog.i("POST请求结果失败，正在重试,当前重试次数：" + currentRetryCount);
            }

            @Override
            public void onRequestDownloadProgress(Request<?> request, long transferredBytesSize, long totalSize) {
                CLog.i("onRequestDownloadProgress current：%d , total : %d", transferredBytesSize, totalSize);
            }

            @Override
            public void onRequestUploadProgress(Request<?> request, long transferredBytesSize, long totalSize, int currentFileIndex,
                                                File currentFile) {
                CLog.i("onRequestUploadProgress current：%d , total : %d", transferredBytesSize, totalSize);
            }

            @Override
            public void onDone(Request<?> request, Map<String, String> headers, String result, DataType dataType) {
                super.onDone(request, headers, result, dataType);
            }
        };
    }


    /**
     * 带参数的GET 请求
     */
    private void get() {
        RequestParams params = new RequestParams();
        params.putHeaders("apikey", "ae75f7350ede43701ce8a5ad8a161ff9");
        params.putParams("city", "hefei");

        HttpRequest<String> request = new MultipartRequest.Builder<String>()
                .requestParams(params)
                .httpMethod(HttpMethod.GET)
                .url("http://apis.baidu.com/heweather/weather/free")
                .cacheKey("http://apis.baidu.com/heweather/weather/free" + "get") //与POST请求的URL一样，为了避免同样的缓存key、这里重新指定缓存key
                .tag(mRequestTag)
                .onRequestListener(getOnGetRequestListener())
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .build()
                .execute();
    }

    public OnRequestListener getOnGetRequestListener() {
        return new OnRequestListener<String>() {

            /**
             * 请求前准备回调
             * 运行线程：主线程
             * @param request 当前请求对象
             */
            @Override
            public void onRequestPrepare(Request<?> request) {
                Toast.makeText(context, "GET请求准备", Toast.LENGTH_SHORT).show();

                CLog.i("GET请求准备");
            }

            /**
             * 请求完成回调
             * 运行线程：主线程
             * @param request 当前请求对象
             * @param headers 请求结果头文件Map集合
             * @param result 请求结果泛型对象
             */
            @Override
            public void onRequestFinish(Request<?> request, Map<String, String> headers, String result) {
                Toast.makeText(context, "GET请求结果获取成功", Toast.LENGTH_SHORT).show();
                CLog.i("GET请求结果获取成功");
            }

            /**
             * 请求失败回调
             * 运行线程：主线程
             * @param request 当前请求对象
             * @param httpException 错误类对象，包含错误码与错误描述
             */
            @Override
            public void onRequestFailed(Request<?> request, HttpException httpException) {
                Toast.makeText(context, "GET请求结果失败", Toast.LENGTH_SHORT).show();
                CLog.i("GET请求结果失败");
            }

            /**
             * 请求失败重试回调
             * 运行线程：主线程
             * @param request 当前请求对象
             * @param currentRetryCount 当前重试次数
             * @param previousError 上一个错误类对象，包含错误码与错误描述
             */
            @Override
            public void onRequestRetry(Request<?> request, int currentRetryCount, HttpException previousError) {
                Toast.makeText(context, "获取信息失败，系统已经为您重试" + currentRetryCount + "次", Toast.LENGTH_SHORT).show();

                CLog.i("GET请求结果失败，正在重试,当前重试次数：" + currentRetryCount);
            }

            /**
             * 下载进度回调
             * 运行线程：子线程
             * @param request 当前请求对象
             * @param transferredBytesSize 当前下载大小
             * @param totalSize 总大小
             *
             */
            @Override
            public void onRequestDownloadProgress(Request<?> request, long transferredBytesSize, long totalSize) {
                CLog.i("onRequestDownloadProgress current：%d , total : %d", transferredBytesSize, totalSize);
            }

            /**
             * 上传进度回调
             * 运行线程：子线程
             * @param request 当前请求对象
             * @param transferredBytesSize 当前写入进度
             * @param totalSize 总进度
             * @param currentFileIndex 当前正在上传的是第几个文件
             * @param currentFile 当前正在上传的文件对象
             *
             */
            @Override
            public void onRequestUploadProgress(Request<?> request, long transferredBytesSize, long totalSize, int currentFileIndex,
                                                File currentFile) {
                CLog.i("onRequestUploadProgress current：%d , total : %d", transferredBytesSize, totalSize);
            }

            /**
             * 缓存数据加载完成回调
             * 运行线程：主线程
             * @param request 当前请求对象
             * @param headers 缓存的头信息Map集合
             * @param result 缓存的数据结果对象
             */
            @Override
            public void onCacheDataLoadFinish(Request<?> request, Map<String, String> headers, String result) {
                Toast.makeText(context, "GET请求缓存加载成功", Toast.LENGTH_SHORT).show();
                CLog.i("GET请求缓存加载成功");
            }

            /**
             * 解析网络数据回调，请求完成后，如果需要做耗时操作（比如写入数据库）可在此回调中进行，不会阻塞UI
             * 运行线程：子线程
             * @param request 当前请求对象
             * @param networkResponse 网络请求结果对象，包含byte数据流与头信息等
             * @param result 解析byte数据流构建的对象
             * @return 是否允许缓存
             */
            @Override
            public boolean onParseNetworkResponse(Request<?> request, NetworkResponse networkResponse, String result) {
                CLog.i("GET请求网络数据解析完成");
                return true;
            }

            /**
             * 此请求最终完成回调，每次请求只会调用一次，无论此请求走的缓存数据还是网络数据，最后交付的结果走此回调
             * 运行线程：主线程
             * @param request 当前请求对象
             * @param headers 最终交付数据的头信息
             * @param result 最终交付的请求结果对象
             * @param dataType 最终交付的数据类型枚举，网络数据/缓存数据
             */
            @Override
            public void onDone(Request<?> request, Map<String, String> headers, String result, DataType dataType) {
                Toast.makeText(context, "GET请求完成", Toast.LENGTH_SHORT).show();
            }

        };
    }

    /**
     * POST 方式提交JSON字符串参数
     */
    private void jsonPost() {
        String url = "http://www.oschina.net/action/api/team_stickynote_batch_update";
        RequestParams params = new RequestParams();
        params.putParams(
                "{\"uid\":863548,\"stickys\":[{\"id\":29058,\"iid\":0,\"content\":\"你好\",\"color\":\"green\",\"createtime\":\"2015-04-16 16:26:17\",\"updatetime\":\"2015-04-16 16:26:17\"}]}");


        HttpRequest<String> request = new MultipartRequest.Builder<String>()
                .requestParams(params)
                .httpMethod(HttpMethod.POST)
                .url("http://www.oschina.net/action/api/team_stickynote_batch_update")
                .tag(mRequestTag)
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .build()
                .execute();
    }


    /**
     * 上传文件
     */
    private void uploadFile() {
        RequestParams params = new RequestParams();
        params.put("file[0]", new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "HTextView.apk"));
        params.put("file[1]", new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "photoview.apk"));
        params.putParams("file_name", "上传的文件名称");

        HttpRequest<String> request = new MultipartRequest.Builder<String>()
                .requestCacheOptions(RequestCacheOptions.buildAllCloseOptions())
                .requestParams(params)
                .httpMethod(HttpMethod.POST)
                .url("http://192.168.1.71/upload_multi.php")
                .tag(mRequestTag)
                .onRequestListener(getOnUploadListener())
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .build()
                .execute();

    }

    public OnRequestListener getOnUploadListener() {
        return new OnRequestListenerAdapter<String>() {

            @Override
            public void onRequestPrepare(Request<?> request) {
                Toast.makeText(context, "请求准备", Toast.LENGTH_SHORT).show();
                CLog.i("请求准备");
            }

            @Override
            public void onRequestFinish(Request<?> request, Map<String, String> headers, String response) {
                Toast.makeText(context, "请求完成", Toast.LENGTH_SHORT).show();
                mUploadButton.setText("UploadFile");
                mUploadButton.setOnClickListener(getOnClickListener());
            }

            @Override
            public void onRequestFailed(Request<?> request, HttpException httpException) {
                Toast.makeText(context, "请求结果失败", Toast.LENGTH_SHORT).show();
                CLog.i("请求结果失败");
            }

            @Override
            public void onRequestUploadProgress(Request<?> request, final long transferredBytesSize, final long totalSize, final int currentFileIndex,
                                                File currentFile) {
                CLog.i("正在上传第%s个文件,当前进度：%d , 总大小 : %d", currentFileIndex, transferredBytesSize, totalSize);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUploadButton.setOnClickListener(null);
                        mUploadButton.setText(String.format("正在上传第%s个文件", currentFileIndex));
                    }
                });

                mUploadProgressBar.setMax((int) totalSize);
                mUploadProgressBar.setProgress((int) transferredBytesSize);
            }
        };
    }

    /**
     * 下载文件
     */
    @SuppressLint("SdCardPath")
    public void downloadFile() {
        String downloadPath = "/sdcard/xrequest/download";
        String fileName = "test.apk";

        HttpRequest request = new DownloadRequest.Builder(downloadPath, fileName)
                .requestCacheOptions(RequestCacheOptions.buildAllCloseOptions())
                .httpMethod(HttpMethod.POST)
                .url("http://192.168.1.71/upload/animate.apk")
                .tag(mRequestTag)
                .onRequestListener(getOnDownloadListener())
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .build()
                .execute();
    }

    public OnRequestListener getOnDownloadListener() {
        return new OnRequestListenerAdapter<File>() {
            @Override
            public void onRequestRetry(Request<?> request, int currentRetryCount, HttpException previousError) {
                Toast.makeText(context, "下载失败，系统已经为您重试" + currentRetryCount + "次", Toast.LENGTH_SHORT).show();

                CLog.i("下载失败，正在重试,当前重试次数：" + currentRetryCount);
            }

            @Override
            public void onRequestDownloadProgress(Request<?> request, long transferredBytesSize, long totalSize) {
                CLog.i("正在下载， 当前进度：%d , 总大小 : %d", transferredBytesSize, totalSize);
                mDownloadProgressBar.setMax((int) totalSize);
                mDownloadProgressBar.setProgress((int) transferredBytesSize);
            }

            @Override
            public void onDone(Request<?> request, Map<String, String> headers, File result, DataType dataType) {
                CLog.i("下载完成 : %s", result != null ? result.toString() : "获取File为空");
            }
        };
    }

    /**
     * JSON自动转换为实体
     */
    private void convertToBean() {
        RequestParams params = new RequestParams();
        params.putHeaders("apikey", "ae75f7350ede43701ce8a5ad8a161ff9");

        HttpRequest<CityRootBean<CityRootBean.CityBean>> request = new MultipartRequest.Builder<CityRootBean<CityRootBean.CityBean>>()
                .requestParams(params)
                .httpMethod(HttpMethod.POST)
                .url("http://apis.baidu.com/apistore/aqiservice/citylist")
                .cacheKey("http://apis.baidu.com/apistore/aqiservice/citylist") //与POST请求的URL一样，为了避免同样的缓存key、这里重新指定缓存key
                .tag(mRequestTag)
                .onRequestListener(getOnConvertToBeanListener())
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .addConverter(new JackSonConverter())
                //.addConverter(new FastjsonConverter())
                .build()
                .execute();
    }

    public OnRequestListener getOnConvertToBeanListener() {
        return new OnRequestListenerAdapter<CityRootBean<CityRootBean.CityBean>>() {

            @Override
            public void onRequestDownloadProgress(Request<?> request, long transferredBytesSize, long totalSize) {
                CLog.i("onRequestDownloadProgress current：%d , total : %d", transferredBytesSize, totalSize);
            }

            @Override
            public void onRequestUploadProgress(Request<?> request, long transferredBytesSize, long totalSize, int currentFileIndex,
                                                File currentFile) {
                CLog.i("onRequestUploadProgress current：%d , total : %d", transferredBytesSize, totalSize);
            }

            @Override
            public void onDone(Request<?> request, Map<String, String> headers, CityRootBean<CityRootBean.CityBean> result,
                               DataType dataType) {
                CLog.i("Bean信息:" + (result == null ? "null" : result.toString()));
            }

        };
    }

    /**
     * JSON自动转换为List
     */
    private void convertToBeanList() {

        HttpRequest<List<BeautyBean>> request = new MultipartRequest.Builder<List<BeautyBean>>()
                .httpMethod(HttpMethod.GET)
                .url("http://git.oschina.net/robinxdroid/API/raw/master/Beauty.txt?dir=0&filepath=Beauty.txt")
                .cacheKey("http://git.oschina.net/robinxdroid/API/raw/master/Beauty.txt?dir=0&filepath=Beauty.txt")
                .tag(mRequestTag)
                .onRequestListener(getOnConvertToBeanListListener())
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .addConverter(new JackSonConverter())
                //.addConverter(new FastjsonConverter())
                .build()
                .execute();
    }

    public OnRequestListener getOnConvertToBeanListListener() {
        return new OnRequestListenerAdapter<List<BeautyBean>>() {

            @Override
            public void onRequestUploadProgress(Request<?> request, long transferredBytesSize, long totalSize, int currentFileIndex,
                                                File currentFile) {
                CLog.i("onRequestUploadProgress current：%d , total : %d", transferredBytesSize, totalSize);
            }

            @Override
            public void onDone(Request<?> request, Map<String, String> headers, List<BeautyBean> result,
                               DataType dataType) {
                super.onDone(request, headers, result, dataType);
                CLog.i("Bean信息:" + (result == null ? "null" : result.toString()));
            }
        };
    }

    private void syncRequest() {
        SyncRequest<String> syncRequest = SyncRequest.newSyncRequest();
        HttpRequest<String> request = new MultipartRequest.Builder<String>()
                .httpMethod(HttpMethod.GET)
                .url("http://git.oschina.net/robinxdroid/API/raw/master/Beauty.txt?dir=0&filepath=Beauty.txt")
                .cacheKey("sync")
                .tag(mRequestTag)
                .onRequestListener(syncRequest)
                .body(mOkHttpRadioButton.isChecked()?new OkRequestBody():new HurlRequestBody())
                .parseBeanType(String.class)
                .build()
                .execute();

        try {
            String result = syncRequest.get();
            CLog.i("Sync Request result: %s",result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        // 取消指定请求
        // request.cancel();
        // XRequest.INSTANCE.cancelRequest(request);

        // 取消队列中的所有相同tag请求
        //request.getRequestQueue().cancelAll(mRequestTag);
        XRequest.INSTANCE.cancelAllRequestInQueueByTag(mRequestTag);

        //彻底关闭请求，可在程序退出时调用
        XRequest.INSTANCE.shutdown();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

	/*
     * =====================================================================
	 * 以下所有用于日志打印模块，与请求无关，可忽略
	 * =====================================================================
	 */

    LoggerListAdapter mAdapter;
    ListView listView;

    private ILogProcessor mService;
    private int mLogType = 0;
    @SuppressWarnings("unused")
    private boolean mServiceRunning = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ILogProcessor.Stub.asInterface((IBinder) service);
            LogProcessor.setHandler(mHandler);

            try {
                mService.run(mLogType);
                mServiceRunning = true;
            } catch (RemoteException e) {
                Log.e("Logger", "Could not start logging");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i("Logger", "onServiceDisconnected has been called");
            mService = null;
        }
    };

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LogProcessor.MSG_READ_FAIL:
                    Log.d("Logger", "MSG_READ_FAIL");
                    break;
                case LogProcessor.MSG_LOG_FAIL:
                    Log.d("Logger", "MSG_LOG_FAIL");
                    break;
                case LogProcessor.MSG_NEW_LINE:
                    mAdapter.addLine((String) msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    /**
     * 初始化日志模块
     */
    private void initLog() {
        // Set Log
        listView = (ListView) findViewById(R.id.lv);
        listView.setStackFromBottom(true);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        listView.setDividerHeight(0);

        mAdapter = new LoggerListAdapter(this);
        listView.setAdapter(mAdapter);
    }

    // 用于日志
    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, LogProcessor.class), mConnection, Context.BIND_AUTO_CREATE);

    }

    // 用于日志
    @Override
    public void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

	/*
     * =====================================================================
	 * Adapter内部类，用于日志
	 * =====================================================================
	 */

    public class LoggerListAdapter extends BaseAdapter {
        @SuppressWarnings("unused")
        private Context mContext;
        private int mFilter = -1;
        private String mFilterTag = "system.out";
        private ArrayList<String> mLines;
        private LayoutInflater mInflater;

        final char[] mFilters = {'D', 'E', 'I', 'V', 'W'};

        public LoggerListAdapter(Context c) {
            mContext = c;
            mLines = new ArrayList<String>();
            mInflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return mLines.size();
        }

        public long getItemId(int pos) {
            return pos;
        }

        public Object getItem(int pos) {
            return mLines.get(pos);
        }

        public View getView(int pos, View convertView, ViewGroup parent) {
            TextView holder;
            String line = mLines.get(pos);

            if (convertView == null) {
                // inflate the view here because there's no existing view
                // object.
                convertView = mInflater.inflate(R.layout.log_item, parent, false);

                holder = (TextView) convertView.findViewById(R.id.log_line);
                holder.setTypeface(Typeface.MONOSPACE);

                convertView.setTag(holder);
            } else {
                holder = (TextView) convertView.getTag();
            }

            if (mLogType == 0) {
                holder.setText(new LogFormattedString(line));
            } else {
                holder.setText(line);
            }

            final boolean autoscroll = (listView.getScrollY() + listView.getHeight() >= listView.getBottom()) ? true
                    : false;

            if (autoscroll) {
                listView.setSelection(mLines.size() - 1);
            }

            return convertView;
        }

        public void addLine(String line) {
            if (mFilter != -1 && line.charAt(0) != mFilters[mFilter]) {
                return;
            }

            if (!mFilterTag.equals("")) {
                if (line.indexOf("(") > 0) {
                    String tag = line.substring(2, line.indexOf("("));

                    if (!mFilterTag.toLowerCase().equals(tag.toLowerCase().trim())) {
                        return;
                    }
                }

            }

            mLines.add(line);
            notifyDataSetChanged();
        }

        public void resetLines() {
            mLines.clear();
            notifyDataSetChanged();
        }

        public void updateView() {
            notifyDataSetChanged();
        }

    }
}
