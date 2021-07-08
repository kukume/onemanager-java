package me.kuku.onemanager.utils;

import me.kuku.utils.OkHttpUtils;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderBinary;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ActUtils {

	public static RenderBinary renderImage(byte[] bytes){
		return new RenderBinary(bytes).contentType("image/png");
	}

	public static RenderBinary renderImage(InputStream is){
		return new RenderBinary(is, "", "image/png", true);
	}

	public static String cookieToString(List<H.Cookie> list){
		StringBuilder sb = new StringBuilder();
		for (H.Cookie cookie : list) {
			sb.append(cookie.name()).append("=").append(cookie.value()).append("; ");
		}
		return sb.toString();
	}

	public static void addCookie(H.Response<?> resp, Map<String, String> map){
		for (Map.Entry<String, String> entry: map.entrySet()){
			resp.addCookie(new H.Cookie(entry.getKey(), entry.getValue()));
		}
	}

	public static void addCookie(H.Response<?> resp, String cookie){
		Map<String, String> map = OkHttpUtils.cookieToMap(cookie);
		addCookie(resp, map);
	}

	public static String getCookie(H.Request<?> req, String...names){
		StringBuilder sb = new StringBuilder();
		Arrays.stream(names).forEach(name -> sb.append(name).append("=").append(req.cookie(name).value()).append("; "));
		return sb.toString();
	}
}
