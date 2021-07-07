package me.kuku.onemanager.logic.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.kuku.onemanager.exception.VerifyFailedException;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.OnedriveItemList;
import me.kuku.onemanager.pojo.OnedriveItemPojo;
import me.kuku.onemanager.pojo.OnedrivePojo;
import me.kuku.onemanager.utils.ApiUtils;
import me.kuku.onemanager.utils.OkHttpUtils;
import okhttp3.Headers;
import okhttp3.Response;
import org.osgl.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnedriveLogicImpl implements OnedriveLogic {

	private Headers authorizationHeaders(String accessToken){
		return OkHttpUtils.addSingleHeader("Authorization", "Bearer " + accessToken);
	}

	private OnedrivePojo update(OnedrivePojo onedrivePojo, JSONObject jsonObject){
		if (jsonObject.containsKey("error"))
			throw new VerifyFailedException(jsonObject.getString("error_description"));
		onedrivePojo.setAccessToken(jsonObject.getString("access_token"));
		onedrivePojo.setRefreshToken(jsonObject.getString("refresh_token"));
		long ee = System.currentTimeMillis() + (jsonObject.getInteger("expires_in") * 1000L);
		onedrivePojo.setExpires(ee);
		return onedrivePojo;
	}

	@Override
	public String authorizationUrl(OnedrivePojo onedrivePojo, String state) {
		return String.format("https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=%s&scope=%s&response_type=code&redirect_uri=%s&state=%s",
				onedrivePojo.getClientId(), "files.readwrite.all files.readwrite offline_access", onedrivePojo.getRedirectUrl(), state);
	}

	@Override
	public OnedrivePojo token(OnedrivePojo oneDrivePojo, String code) throws IOException {
		Map<String, String> params = new HashMap<String, String>(){{
			put("client_id", oneDrivePojo.getClientId());
			put("redirect_uri", oneDrivePojo.getRedirectUrl());
			put("client_secret", oneDrivePojo.getClientSecret());
			put("code", code);
			put("grant_type", "authorization_code");
		}};
		JSONObject jsonObject = OkHttpUtils.postJson("https://login.microsoftonline.com/common/oauth2/v2.0/token",
				params);
		return update(oneDrivePojo, jsonObject);
	}

	@Override
	public OnedrivePojo refreshToken(OnedrivePojo oneDrivePojo) throws IOException {
		Map<String, String> params = new HashMap<String, String>(){{
			put("client_id", oneDrivePojo.getClientId());
			put("redirect_uri", oneDrivePojo.getRedirectUrl());
			put("client_secret", oneDrivePojo.getClientSecret());
			put("refresh_token", oneDrivePojo.getRefreshToken());
			put("grant_type", "refresh_token");
		}};
		JSONObject jsonObject = OkHttpUtils.postJson("https://login.microsoftonline.com/common/oauth2/v2.0/token",
				params);
		return update(oneDrivePojo, jsonObject);
	}

	private String path(String...path){
		if (path.length == 0 || (path.length == 1 && "".equals(path[0]))) return "root";
		String ss = StringUtil.join("/", path);
		return "root:/" + ss + ":";
	}

	@Override
	public OnedriveItemList listFile(OnedrivePojo oneDrivePojo, String...path) throws IOException {
		return listFileByUrl(oneDrivePojo, "https://graph.microsoft.com/v1.0/me/drive/" + path(path) + "/children");
	}

	@Override
	public OnedriveItemList listFileByUrl(OnedrivePojo onedrivePojo, String url) throws IOException {
		JSONObject jsonObject = OkHttpUtils.getJson(url,
				authorizationHeaders(onedrivePojo.getAccessToken()));
		if (jsonObject.containsKey("error")){
			JSONObject errorJsonObject = jsonObject.getJSONObject("error");
			throw new VerifyFailedException(errorJsonObject.getString("code") + "：" + errorJsonObject.getString("message"));
		}
		// @odata.nextLink  下一页url
		List<OnedriveItemPojo> list = new ArrayList<>();
		JSONArray jsonArray = jsonObject.getJSONArray("value");
		jsonArray.stream().map(it -> (JSONObject) it).forEach(it -> {
			OnedriveItemPojo pojo = new OnedriveItemPojo(it.getString("id"), it.getString("name"),
					it.getString("createdDateTime"),
					it.getString("lastModifiedDateTime"), it.getLong("size"));
			boolean isFile = it.containsKey("file");
			pojo.setIsFile(isFile);
			if (isFile){
				pojo.setUrl(it.getString("@microsoft.graph.downloadUrl"));
				pojo.setMimeType(it.getJSONObject("file").getString("mimeType"));
			}
			list.add(pojo);
		});
		OnedriveItemList itemList = new OnedriveItemList();
		itemList.setList(list);
		itemList.setNextLink(jsonObject.getString("@odata.nextLink"));
		return itemList;
	}

	@Override
	public boolean upload(OnedrivePojo onedrivePojo, File file, String... path) throws IOException {
		Response response = OkHttpUtils.put("https://graph.microsoft.com/v1.0/me/drive/" + path(path) + "/content",
				OkHttpUtils.getStreamBody(file), authorizationHeaders(onedrivePojo.getAccessToken()));
		JSONObject jsonObject = OkHttpUtils.getJson(response);
		return !jsonObject.containsKey("error");
	}

	@Override
	public boolean upload(OnedrivePojo onedrivePojo, byte[] bytes, String... path) throws IOException {
		Response response = OkHttpUtils.put("https://graph.microsoft.com/v1.0/me/drive/" + path(path) + "/content",
				OkHttpUtils.getStreamBody(bytes), authorizationHeaders(onedrivePojo.getAccessToken()));
		JSONObject jsonObject = OkHttpUtils.getJson(response);
		return !jsonObject.containsKey("error");
	}

	@Override
	public String uploadBigFile(OnedrivePojo onedrivePojo, String... path) throws IOException {
		JSONObject jsonObject = OkHttpUtils.postJson("https://graph.microsoft.com/v1.0/me/drive/" + path(path) + "/createUploadSession",
				OkHttpUtils.addJson("{\"item\": { \"@microsoft.graph.conflictBehavior\": \"fail\" }}"),
				authorizationHeaders(onedrivePojo.getAccessToken()));
		if (jsonObject.containsKey("error")){
			JSONObject errorJsonObject = jsonObject.getJSONObject("error");
			throw new VerifyFailedException(errorJsonObject.getString("code") + "：" + errorJsonObject.getString("message"));
		}
		return jsonObject.getString("uploadUrl");
	}

	@Override
	public String download(OnedrivePojo onedrivePojo, String... path) throws IOException {
		Response response = OkHttpUtils.get("https://graph.microsoft.com/v1.0/drive/" + path(path) + "/content",
				authorizationHeaders(onedrivePojo.getAccessToken()));
		if (response.code() == 302){

		}
		return "";
	}

	@Override
	public OnedriveItemPojo source(OnedrivePojo onedrivePojo, String... path) throws IOException {
		JSONObject jsonObject = OkHttpUtils.getJson("https://graph.microsoft.com/v1.0/me/drive/" + path(path),
				authorizationHeaders(onedrivePojo.getAccessToken()));
		if (jsonObject.containsKey("error")){
			JSONObject errorJsonObject = jsonObject.getJSONObject("error");
			throw new VerifyFailedException(errorJsonObject.getString("code") + "：" + errorJsonObject.getString("message"));
		}
		String id = jsonObject.getString("id");
		boolean isFile = jsonObject.containsKey("file");
		OnedriveItemPojo onedriveItemPojo = new OnedriveItemPojo(id, jsonObject.getString("name"),
				jsonObject.getString("createdDateTime"), jsonObject.getString("lastModifiedDateTime"),
				jsonObject.getLong("size"), jsonObject.getString("@microsoft.graph.downloadUrl"),
				isFile);
		if (isFile){
			onedriveItemPojo.setMimeType(jsonObject.getJSONObject("file").getString("mimeType"));
		}
		return onedriveItemPojo;
	}

	@Override
	public String size(OnedrivePojo onedrivePojo) throws IOException {
		JSONObject jsonObject = OkHttpUtils.getJson("https://graph.microsoft.com/v1.0/me/drive",
				authorizationHeaders(onedrivePojo.getAccessToken()));
		if (jsonObject.containsKey("error")){
			JSONObject errorJsonObject = jsonObject.getJSONObject("error");
			throw new VerifyFailedException(errorJsonObject.getString("code") + "：" + errorJsonObject.getString("message"));
		}
		JSONObject quota = jsonObject.getJSONObject("quota");
		Long total = quota.getLong("total");
		Long used = quota.getLong("used");
		return ApiUtils.parseSize(used) + " / " + ApiUtils.parseSize(total);
	}

	@Override
	public boolean delete(OnedrivePojo onedrivePojo, String itemId) throws IOException {
		Response response = OkHttpUtils.delete("https://graph.microsoft.com/v1.0/me/drive/items/" + itemId,
				new HashMap<>(), authorizationHeaders(onedrivePojo.getAccessToken()));
		response.close();
		return response.code() == 204;
	}

	@Override
	public boolean rename(OnedrivePojo onedrivePojo, String itemId, String newName) throws IOException {
		JSONObject params = new JSONObject();
		params.put("name", newName);
		Response response = OkHttpUtils.patch("https://graph.microsoft.com/v1.0/me/drive/items/" + itemId,
				OkHttpUtils.addJson(params.toJSONString()), authorizationHeaders(onedrivePojo.getAccessToken()));
		JSONObject jsonObject = OkHttpUtils.getJson(response);
		return jsonObject.containsKey("id");
	}
}
