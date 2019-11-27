package com.zhongyou.meet.mobile.ameeting.network;


import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Created by jie on 2016/12/8.
 */

public interface ApiService {


	/*@POST("/pick/sign/in")
		//返回结果也是JSONObject  参数是以JSONObject的方式传递的
	Observable<JSONObject> login(@Body JSONObject jsonObject);

	@GET("/wap/index.json")
	Observable<JSONObject> index();

	*//**
	 * 首页点击加入购物车弹窗
	 *
	 * @param commonId
	 * @param clientType
	 *//*
	@GET("/wap/goods/spec.json")
	Observable<JSONObject> getGoodsSpec(@Query("commonId") String commonId, @Query("clientType") String clientType);


	*//**
	 * 商品详情页
	 *//*
	@GET("/wap/goods/detail.json")
	Observable<JSONObject> getGoodsDetail(@QueryMap HashMap<String, String> map);*/
}
