package com.hezy.guide.phone.net;

/**
 * Created by whatisjava on 17-1-3.
 */

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.entities.RespStatus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpUtil {

    private static OkHttpUtil okHttpUtil;
    private static OkHttpClient okHttpClient;
    private Handler mHandler;

    public static Gson getGson() {
        return gson;
    }

    private static Gson gson;

    private OkHttpUtil() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        clientBuilder.connectTimeout(10, TimeUnit.SECONDS);
        clientBuilder.readTimeout(10, TimeUnit.SECONDS);
        clientBuilder.writeTimeout(30, TimeUnit.SECONDS);

        MyHttpLoggingInterceptor httpLoggingInterceptor = new MyHttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(MyHttpLoggingInterceptor.Level.BODY);
        clientBuilder.addInterceptor(httpLoggingInterceptor);

        okHttpClient = clientBuilder.build();

        mHandler = new Handler(Looper.getMainLooper());
    }

    public static OkHttpUtil getInstance() {
        if (okHttpUtil == null) {
            synchronized (OkHttpUtil.class) {
                if (okHttpUtil == null) {
                    okHttpUtil = new OkHttpUtil();
                    gson = new Gson();
                }
            }
        }
        return okHttpUtil;
    }

    private Call call;

    private void request(final Request request, final OkHttpCallback callback) {

        callback.onStart();

        call = okHttpClient.newCall(request);
        if (call.isExecuted() || call.isCanceled()){
            return;
        }
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callbackFailure(request, callback, new BaseException(e.getMessage(), e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callbackError(response, callback, new BaseException("服务器异常,请稍后重试"));
                    return;
                }
                if (response.body() != null) {
                    String resString = response.body().string();
                    if (callback.mType == String.class) {
                        callbackSuccess(response, resString, callback);
                    } else {
                        try {
                            Object object = gson.fromJson(resString, callback.mType);
                            callbackSuccess(response, object, callback);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                            try {
                                RespStatus respStatus = gson.fromJson(resString, RespStatus.class);
                                callbackError(response, callback, new BaseException(respStatus.getErrmsg(), respStatus.getErrcode()));
                            } catch (JsonSyntaxException ee) {
                                ee.printStackTrace();
                                callbackError(response, callback, new BaseException(ee.getMessage(), ee));
                            }

                        }
                    }
                } else {
                    callbackError(response, callback, null);
                }
            }
        });
    }

    public void cancelRequest(Request request){
        cancelRequest(okHttpClient.newCall(request));
    }

    private void cancelRequest(Call call){
        call.cancel();
    }

    private void callbackSuccess(final Response response, final Object o, final OkHttpCallback callback) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(o);
                callback.onFinish();
            }
        });
    }

    private void callbackError(final Response response, final OkHttpCallback callback, final BaseException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onError(response.code(), e);
                callback.onFinish();
            }
        });
    }

    private void callbackFailure(final Request request, final OkHttpCallback callback, final BaseException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(e);
                callback.onFinish();
            }
        });
    }

    private void runOnUiThread(Runnable task) {
        mHandler.post(task);
    }



    public void get(String url, Map<String, String> headers, Map<String, String> params, OkHttpCallback callback) {
        Request request = buildRequest(jointUrl(url, params), headers, null, null, HttpMethodType.GET);
        request(request, callback);
    }

    public void post(String url, Map<String, String> headers, Map<String, String> params, OkHttpCallback callback) {
        Request request = buildRequest(url, headers, params,null, HttpMethodType.POST);
        request(request, callback);
    }

    public void postJson(String url, Map<String, String> headers,String jsonStr, OkHttpCallback callback,Object tag) {
        Request request = buildRequest(url, headers, null, jsonStr, HttpMethodType.POST_JSON);
        request(request, callback);
    }

    public void put(String url, Map<String, String> headers, Map<String, String> params, OkHttpCallback callback){
        Request request = buildRequest(url, headers, params, null,HttpMethodType.PUT);
        request(request, callback);
    }

    public void delete(String url, Map<String, String> headers, Map<String, String> params, OkHttpCallback callback){
        Request request = buildRequest(url, headers, params, null,HttpMethodType.DELETE);
        request(request, callback);
    }

    //-----------------------------------------  get请求 -----------------------------------------

    /**
     * get请求
     */
    public void get(String url, Object tag, OkHttpCallback callback) {
        Request request = buildRequest(url, null, null,null, HttpMethodType.GET, tag);
        request(request, callback);
    }

    public void putJson(String url, Map<String, String> headers, String jsonStr, OkHttpCallback callback,Object tag){
        Request request = buildRequest(url, headers, null,jsonStr, HttpMethodType.PUT_JSON,tag);
        request(request, callback);
    }

    /**
     * get请求_使用map设置请求
     */
    public void get(String url, Map<String, String> params, Object tag, OkHttpCallback callback) {
        get(generateUrlString(url, params), tag, callback);
    }



    /**
     * 用来拼接get请求的url地址,做了URLENCODE
     *
     * @param url    请求的链接
     * @param params 各种参数
     * @return 拼接完成的链接
     */
    public static String generateUrlString(String url, Map<String, String> params) {
        if (params != null && params.size() > 0) {
            Uri uri = Uri.parse(url);
            Uri.Builder b = uri.buildUpon();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                b.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            return b.build().toString();
        }
        return url;
    }


    private Request buildRequest(String url, Map<String, String> headers, Map<String, String> params,String jsonStr, HttpMethodType type) {

        return buildRequest(url,headers,params,jsonStr,type,null);
    }

    private Request buildRequest(String url, Map<String, String> headers, Map<String, String> params,String jsonStr,HttpMethodType type,Object tag) {
        Request.Builder builder = new Request.Builder();
        addHeader(headers, builder);
        builder.url(url);
        if (tag != null) {
            builder.tag(tag);
        }
        if (type == HttpMethodType.GET) {
            builder.get();
        } else if (type == HttpMethodType.POST) {
            builder.post(buildRequestBody(params));
        } else if (type == HttpMethodType.POST_JSON) {
            builder.post(buildRequestBody(jsonStr));
        } else if (type == HttpMethodType.POST_FILE) {
            builder.post(buildMultipartRequestBody(params));
        } else if (type == HttpMethodType.PUT) {
            builder.put(buildRequestBody(params));
        } else if (type == HttpMethodType.PUT_JSON) {
            builder.put(buildRequestBody(jsonStr));
        } else if (type == HttpMethodType.DELETE) {
            builder.delete(buildRequestBody(params));
        } else if (type == HttpMethodType.DELETE_JSON) {
            builder.delete(buildRequestBody(gson.toJson(params)));
        }
        return builder.build();
    }

    public static String jointUrl(String url, Map<String, String> params) {
        if (params != null && params.size() > 0) {
            Uri uri = Uri.parse(url);
            Uri.Builder b = uri.buildUpon();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                b.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            return b.build().toString();
        }
        return url;
    }

    private Request.Builder addHeader(Map<String, String> headers, Request.Builder builder) {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
            return builder;
        }
        return builder;
    }

    /**
     * 通过Map的键值对构建请求对象的body
     *
     * @param params Map
     * @return RequestBody
     */
    private RequestBody buildRequestBody(Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entity : params.entrySet()) {
                builder.add(entity.getKey(), entity.getValue());
            }
        }
        return builder.build();
    }

    /**
     * 通过json字符串创建请求对象的body
     *
     * @param jsonString String
     * @return RequestBody
     */
    private RequestBody buildRequestBody(String jsonString) {
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonString);
    }

    private MultipartBody buildMultipartRequestBody(Map<String, String> params) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (params != null) {
            for (Map.Entry<String, String> entity : params.entrySet()) {
                multipartBuilder.addFormDataPart(entity.getKey(), entity.getValue());
            }
        }
        return multipartBuilder.build();
    }

    /**
     * 这个枚举用于指明是哪一种提交方式
     */
    private enum HttpMethodType {
        GET,
        POST,
        POST_JSON,
        POST_FILE,
        PUT,
        PUT_JSON,
        DELETE,
        DELETE_JSON
    }

    /**
     * 根据Tag取消请求
     *
     * @param tag
     */
    public void cancelTag(Object tag) {
        for (Call call : okHttpClient.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : okHttpClient.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }

    /**
     * 取消所有请求请求
     */
    public void cancelAll() {
        for (Call call : okHttpClient.dispatcher().queuedCalls()) {
            call.cancel();
        }
        for (Call call : okHttpClient.dispatcher().runningCalls()) {
            call.cancel();
        }
    }

}
