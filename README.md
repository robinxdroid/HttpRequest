 ## HttpRequest ##

重构更新，高度解耦，支持了OKHttp，可自由选择，多种JSON解析器可选（GSON,Fastjson,JackSon），并能自己再拓展，对缓存模块进行了重构，以及其他模块代码优化。

### Provide ###

```bash    
    1 移除HttpClient相关API
    2 支持OKHttp,自由选择
    3 8种网络请求方式 GET，POST，PUT，DELETE，HEAD，OPTIONS，TRACE，PATCH
    4 请求的优先级设置，优先级高的将先于优先级低的发送请求
    5 取消请求，可以取消当前已发送的请求(可自定义取消请求的依据条件)，也可以取消请求队列中还未发送的请求
    6 多请求并发，多个请求同时发送,底层使用固定数量线程池,可设置线程池的大小
    7 支持重复请求的判断，当有重复的请求将挂起，等待第一个请求完成后，挂起的请求使用已经请求完毕的缓存，如果未开启缓存，则会继续请求网络
    8 请求失败重试，默认重试2次，重试超时时间会递增，递增速率可设置，默认为1倍递增，提供重试回调监听
    9 多文件与大文件上传，可以与参数一起发送至服务器,提供上传进度回调
    10 大文件下载，提供下载进度回调
    11 支持发送JSON数据
    12 自动网络判定，可设置此时是否显示缓存数据
    13 结果自动解析，可泛型任何JAVA BEAN
    14 多种解析器Converters可选，默认实现了GSON解析，拓展性强
    15 多种错误类型判定
    16 扩展性强，可自定义发送请求方式与解析请求结果
    17 强大的缓存策略，适应多种场景
```

### About cache ###
二级缓存，内存缓存与磁盘缓存，内存缓存使用LruCache,磁盘缓存使用DiskLruCache，均为LRU策略
 
框架中会有个RequestCacheOptions对象，此对象包含所有的请求缓存相关配置，每个请求对应一个，在在发起请求时传入，此对象包含如下配置：
   
```java
setShouldCache(true) //这个开关控制是否使用缓存的功能

setUseCacheDataAnyway(false) //是否总是使用缓存，这个开关开启后，将每次首先从内存和本地查找缓存，有的话直接使用缓存，请求会在后台执行，完成后会更新缓存。如果没有缓存将直接进行网络请求获取，完成后会更新缓存.
  
setUseCacheDataWhenRequestFailed(true) //是否在请求失败后使用缓存数据，无网络属于请求失败，可以保证即使没有网络，或者请求失败也有数据展示.
  
setUseCacheDataWhenTimeout(true) //是否在请求超时后直接使用缓存，这里的超时时间并不是网络请求的超时时间，而是我们设定一个时间，超过这个时间后，不管请求有没有完成都直接使用缓存，后台的请求完成后会自动更新缓存
  
setUseCacheDataWhenUnexpired(true) //是否使用缓存当缓存未过期的时候，这个开关也是经常开启的开关，每个缓存都会对应一个过期时间，先从内存查找缓存，没有的话再从磁盘查找，有缓存且未过期的话，将直接使用缓存数据，当过期之后会进行网络请求，请求完成后会更新内存缓存与磁盘。没有缓存将直接进行网络请求，请求完成后会更新内存与磁盘缓存

setRetryWhenRequestFailed(true) //是否进行重试，当请求失败的时候，默认开启，重试2次，不需要重试功能的话可关闭
   
setNeverExpired(false) //设置缓存是否永不过期
    
setTimeController //设置时间控制器
```

### Example ###

[Download demo.apk](https://github.com/robinxdroid/HttpRequest/blob/master/XDroidRequestExample.apk?raw=true)

### Screenshot ###

![](https://github.com/robinxdroid/HttpRequest/blob/master/1.png) 
![](https://github.com/robinxdroid/HttpRequest/blob/master/2.png) 

### Usage ###

Gradle:
```bash
compile 'net.robinx:lib.http:1.0.2'
compile 'com.google.code.gson:gson:2.6.2'  //默认解析使用的gson
```

需要使用OKHTTP的话添加下面的依赖：
```bash
compile 'net.robinx:lib.http.okhttpsupport:1.0.0'
compile 'com.squareup.okhttp3:okhttp:3.2.0'  //okhttp3
```

需要自定义解析方式添加下面的依赖：
```bash
compile 'net.robinx:lib.http.converters:1.0.0'
compile 'com.fasterxml.jackson.core:jackson-databind:2.7.4'  //jackson
compile 'com.alibaba:fastjson:1.2.11' //fastjson
```

**1.初始化，应用启动的时候进行，主要初始化缓存的路径上下文等信息。**

```java
XRequest.initXRequest(getApplicationContext());
```   
其他配置：   
```CacheConfig.DISK_CACHE_MAX_SIZE = DISK_CACHE_MAX_SIZE;``` 磁盘缓存路径       
```CacheConfig.DISK_CACHE_DIRECTORY = DISK_CACHE_DIR_PATH;``` 磁盘缓存最大值

**2.发起请求**

① 请求
```java
		RequestParams params = new RequestParams();
        params.putHeaders("apikey", "ae75f7350ede43701ce8a5ad8a161ff9");
        params.putParams("city", "hefei");

        MultipartRequest.Builder<String> builder = new MultipartRequest.Builder<>();
        HttpRequest<String> request = builder
                .requestParams(params)
                .httpMethod(HttpMethod.GET) //请求方法
                .url("http://apis.baidu.com/heweather/weather/free") //url
                .cacheKey("http://apis.baidu.com/heweather/weather/free") //不设置时默认为url
                .tag(mRequestTag) //tag,用于取消请求的标识
                .onRequestListener(getOnPostRequestListener()) //回调
                .body(new HurlRequestBody()) //请求体，不设置时默认为HurlRequestBody(),可设置为okhttp new OkRequestBody()
                .build()
                .execute();
```


**3.请求回调**：
  回调接口```OnRequestListener```,可选回调```OnRequestListenerAdapter```
  
```java
XRequest.getInstance().sendGet(mRequestTag, url, cacheKey, params, new OnRequestListener<String>() {

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
				Toast.makeText(context, "获取信息失败，系统已经为您重试" + currentRetryCount+"次", Toast.LENGTH_SHORT).show();
				
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
				CLog.i("onRequestDownloadProgress current：%d , total : %d" ,transferredBytesSize,totalSize);
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
				CLog.i("onRequestUploadProgress current：%d , total : %d" ,transferredBytesSize,totalSize);
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
			 * @return 是否允许缓存，“true”允许缓存 “fale”反之，默认为true，请求解析完成后，可以根据情况自己指定缓存条件
			 */
			@Override
			public boolean onParseNetworkResponse(Request<?> request, NetworkResponse networkResponse, String result) {
				CLog.i("GET请求网络数据解析完成");
			        return true
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

		});
```

**4.缓存配置**：


(2)查找当前缓存数据占用的空间
```java
long diskCacheCurrentSize = DiskCache.INSTANCE.getCurrentSize();
```

(3)查找缓存路径
```java
String diskCacheDir = DiskCache.INSTANCE.getDirectory().getPath();
```

(4)查询当前缓存最大值
```java
long diskCacheMaxSize = DiskCache.INSTANCE.getMaxSize();
```

(5)清除所有缓存
```java
DiskCache.INSTANCE.clear();
```

**4.请求配置**：

在发送请求的时候，有的重载函数需要传入一个RequestCacheOptions对象（见Demo项目），不需要传入此对象的重载函数内部传入的是默认的
RequestCacheOptions对象，通过RequestCacheOptions对象控制缓存于网络数据等 
     
```java
HttpRequest request = new MultipartRequest.Builder()
        .requestCacheOptions(RequestCacheOptions.buildAllCloseOptions())
        ...
        .build();
```

每次请求如果需要重新指定配置，自己构造这样一个对象传入即可   

RequestCacheOptions默认配置供参考

```java
public static RequestCacheOptions buildDefaultCacheOptions() {
		RequestCacheOptions options=new RequestCacheOptions.Builder()
                .shouldCache(true) //开启缓存
                .useCacheDataAnyway(false) //关闭总是优先使用缓存
                .useCacheDataWhenRequestFailed(true)  //开启请求失败使用缓存
                .useCacheDataWhenTimeout(false)  //关闭超时使用缓存
                .useCacheDataWhenUnexpired(true)  //开启当缓存未过期时使用缓存
                .retryWhenRequestFailed(true)  //开启请求失败重试
                .neverExpired(false)   //关闭缓存永不过期
                .timeController(new TimeController().setExpirationTime(DEFAULT_EXPIRATION_TIME).setTimeout(DEFAULT_TIMEOUT))
                .build();

		return options;
	}
	
```

**5.自定义解析方式**：

如果需要对请求的结果的解析方式进行自定义拓展，只需实现Converter<T>接口,重写```fromJSONObject```与```fromJSONArray```两个函数即可，参考项目中```GsonConverter```,```FastjsonConverter```<br> 
  
```java
public class FastjsonConverter<T> implements Converter<T> {


    public FastjsonConverter() {
        CLog.w("Create Fastjson");
    }

    @Override
    public T fromJSONObject(String json, Type cls) {
        CLog.w("From JSON Object");
        return fromObject(json, cls);
    }

    @Override
    public T fromJSONArray(String json, Type cls) {
        CLog.w("From JSON Array");
        return (T) fromList(json, cls);
    }

    public T fromObject(String json, Type cls) {
        T bean = JSON.parseObject(json,cls);
        return bean;
    }

    public ArrayList<T> fromList(String json, Type cls) {
        ArrayList<T> list= (ArrayList<T>) JSON.parseObject(json, new TypeReference<List<T>>(){});
        return list;
    }
}
```


**6.自定义请求方式**：

如有需要，参考```OKHttpStack```，```OKRequestBody```

**7.其他设置**：


(1).取消请求
```java
// 取消指定请求(两种方式都可以)
XRequest.INSTANCE.cancelRequest(request);

// 取消队列中的所有相同tag请求
XRequest.INSTANCE.cancelAllRequestInQueueByTag(mRequestTag);
```

(2).框架Log控制

开启Log:
```java
Clog.openLog();
```
关闭Log:
```java
Clog.closeLog();
```

(3).设置请求优先级
```java
HttpRequest request = new MultipartRequest.Builder()
                .retryPolicy(new DefaultRetryPolicyImpl(DefaultRetryPolicyImpl.DEFAULT_TIMEOUT_MS, DefaultRetryPolicyImpl.DEFAULT_MAX_RETRIES, DefaultRetryPolicyImpl.DEFAULT_BACKOFF_MULT))
                ...
                .build();
```
(3).切换OKHttp
```java
XRequest.INSTANCE.setStack(new OkHttpStack());
HttpRequest request = new MultipartRequest.Builder()
                .body(new OkRequestBody())
                ...
                .build();
```
(4).切换解析器
```java
XRequest.INSTANCE.setStack(new OkHttpStack());
HttpRequest request = new MultipartRequest.Builder()
                .addConverter(new FastjsonConverter()) //new JackSonConverter()
                ...
                .build();
```
**8.其他请求示例（列举两个，其他请查看Demo）**：

(1)同步请求
```java
		SyncRequest<String> syncRequest = SyncRequest.newSyncRequest();
        HttpRequest<String> request = new MultipartRequest.Builder<String>()
                ...
                .execute();

        try {
            String result = syncRequest.get();
            CLog.i("Sync Request result: %s",result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
```

(3)上传文件
```java
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
```
测试了上传百兆以上文件无压力，如果你想测试多文件上传，下面的PHP多文件上传代码供参考。要注意的是PHP默认上传2M以内文件，需要自己改下
配置文件，网上很多，搜索即可

```java
<?php
 foreach($_FILES['file']['error'] as $k=>$v)
 {
    $uploadfile = './upload/'. basename($_FILES['file']['name'][$k]);
    if (move_uploaded_file($_FILES['file']['tmp_name'][$k], $uploadfile)) 
    {
        echo "File : ", $_FILES['file']['name'][$k] ," is valid, and was successfully uploaded.\n";
    }

    else 
    {
        echo "Possible file : ", $_FILES['file']['name'][$k], " upload attack!\n";
    }   

 }

 echo "成功接收附加字段:". $_POST['file_name'];

?>
```
(3)下载文件
```java
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
```

**9.更多请查看demo和阅读源码**：


#Thanks
[DiskLruCache](https://github.com/JakeWharton/DiskLruCache)<br>
[android-volley](https://github.com/mcxiaoke/android-volley)
#About me
Email:735506404@robinx.net<br>
Blog:[www.robinx.net](http://www.robinx.net)


