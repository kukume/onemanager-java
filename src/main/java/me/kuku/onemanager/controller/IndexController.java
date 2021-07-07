package me.kuku.onemanager.controller;

import act.app.ActionContext;
import act.db.DbBind;
import act.db.sql.tx.Transactional;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.exception.VerifyFailedException;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.*;
import me.kuku.onemanager.service.DriveService;
import me.kuku.onemanager.service.SystemConfigService;
import me.kuku.onemanager.utils.DateTimeFormatterUtils;
import me.kuku.onemanager.utils.MD5Utils;
import me.kuku.onemanager.utils.OkHttpUtils;
import org.osgl.cache.CacheService;
import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.util.StringUtil;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static act.controller.Controller.Util.*;

public class IndexController {
	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private SystemConfigService systemConfigService;
	@Inject
	@Named("indexCache")
	private CacheService indexCache;

	@Catch(value = VerifyFailedException.class)
	public void error(Exception e){
		String errMsg = e.getMessage();
		template("error", errMsg);
	}

	@Before
	@Transactional
	public void before(String name) throws IOException {
		if (name != null){
			DriveEntity driveEntity = driveService.findByName(name);
			if (driveEntity != null){
				OnedrivePojo onedrivePojo = driveEntity.getConfigParse(OnedrivePojo.class);
				if (System.currentTimeMillis() > onedrivePojo.getExpires()){
					OnedrivePojo newPojo = onedriveLogic.refreshToken(onedrivePojo);
					driveEntity.setConfigParse(newPojo);
					driveService.save(driveEntity);
				}
			}
		}
	}

	@GetAction("/favicon.ico")
	public void toIco(){
		moved("/asset/favicon.ico");
	}

	@Before(only = {"defaultIndex", "index"}, priority = 10)
	public void paramsBefore(ActionContext context, H.Session session, H.Cookie darkModeCookie){
		Map<SystemConfigType, SystemConfigEntity> typeMap = systemConfigService.findAllAsMap();
		SystemConfigEntity passwordEntity = typeMap.get(SystemConfigType.PASSWORD);
		context.renderArg("admin", passwordEntity != null && passwordEntity.getContent().equals(session.get("admin")));
		if (darkModeCookie == null) context.renderArg("darkMode", false);
		else context.renderArg("darkMode", darkModeCookie.value().equals("true"));
		SystemConfigEntity siteNameEntity = typeMap.get(SystemConfigType.SITE_NAME);
		context.renderArg("siteName", siteNameEntity == null ? "OneManager": siteNameEntity.getContent());
		addConfig(context, typeMap.get(SystemConfigType.FAVICON));
		addConfig(context, typeMap.get(SystemConfigType.CUSTOM_SCRIPT));
		addConfig(context, typeMap.get(SystemConfigType.CUSTOM_CSS));
		SystemConfigEntity descriptionEntity = typeMap.get(SystemConfigType.DESCRIPTION);
		context.renderArg("description", descriptionEntity == null || "".equals(descriptionEntity.getContent()) ? "An index & manager of Onedrive." : descriptionEntity.getContent());
		SystemConfigEntity keywordEntity = typeMap.get(SystemConfigType.KEYWORDS);
		context.renderArg("keywords", keywordEntity == null || "".equals(keywordEntity.getContent()) ? "OneManager" : keywordEntity.getContent());
	}

	@GetAction
	public void defaultIndex(ActionContext context) {
		List<DriveEntity> list = driveService.findAll();
		List<OnedriveItemPojo> itemList = new ArrayList<>();
		for (DriveEntity driveEntity : list) {
			OnedriveItemPojo pojo = new OnedriveItemPojo(null, driveEntity.getName(), "",
					DateTimeFormatterUtils.formatNow("yyyy-MM-dd"), 0L, "/" + driveEntity.getName(), false);
			itemList.add(pojo);
		}
		context.renderArg("list", itemList);
		List<DriveEntity> driveEntityList = driveService.findAll();
		addDriveList(context.renderArgs(), driveEntityList);
		render("index");
	}

	private void addConfig(ActionContext context, SystemConfigEntity entity){
		if (entity != null) {
			String name = entity.getSystemConfigType().getType();
			context.renderArg(name, entity.getContent());
		}
	}

	private void addDriveList(Map<String, Object> map, List<DriveEntity> driveEntityList){
		List<Map<String, String>> driveList = driveEntityList.stream().map(it -> {
			Map<String, String> resultMap = new HashMap<>();
			resultMap.put("name", it.getName());
			resultMap.put("url", "/" + it.getName());
			return resultMap;
		}).collect(Collectors.toList());
		map.put("driveList", driveList);
	}

	@Before(only = "index", priority = 30)
	public void indexBefore(String name, String __path, ActionContext context, H.Request<?> req){
		String method = req.method().name();
		Object admin = context.renderArg("admin");
		if (method.equals("GET") && admin != null) {
			String key = name + __path + admin;
			Object o = indexCache.get(key);
			if (o != null) {
				IndexCache pojo = (IndexCache) o;
				Map<String, Object> map = pojo.getMap();
				map.forEach((k, v) -> {
					context.renderArg(k, v);
				});
				template(pojo.getPath());
			}
		}
	}

	@Action(value = "{name}/...", methods = {H.Method.GET, H.Method.POST})
	public Object index(String name, String __path, H.Request<?> req, H.Cookie passwordCookie, H.Response<?> resp,
	                    String password, String preview, ActionContext context, String url) throws IOException {
		List<DriveEntity> driveEntityList = driveService.findAll();
		List<DriveEntity> resultList = driveEntityList.stream().filter(it -> it.getName().equals(name)).collect(Collectors.toList());
		Map<String, Object> map = context.renderArgs();
		if (resultList.size() != 0){
			DriveEntity driveEntity = resultList.get(0);
			OnedrivePojo onedrivePojo = driveEntity.getConfigParse(OnedrivePojo.class);
			DriveConfig driveConfig = driveEntity.getOtherConfigParse(DriveConfig.class);
			String[] paths = __path.split("/");
			OnedriveItemPojo pojo = onedriveLogic.source(onedrivePojo, paths);
			addDriveList(map, driveEntityList);
			map.put("name", name);
			List<Map<String, String>> hrefList = new ArrayList<>();
			StringBuilder sb = new StringBuilder("/").append(name).append("/");
			for (String path: paths){
				if ("".equals(path)) continue;
				sb.append(path).append("/");
				Map<String, String> href = new HashMap<>();
				href.put("name", path);
				href.put("url", sb.toString());
				hrefList.add(href);
			}
			map.put("href", hrefList);
			String contextPath = req.path();
			if (contextPath.charAt(contextPath.length() - 1) == '/')
				contextPath = contextPath.substring(0, contextPath.length() - 1);
			String prePath = contextPath.substring(0, contextPath.lastIndexOf('/') + 1);
			map.put("prePath", prePath);
			if (pojo.getIsFile()) {
				if (preview == null) return moved(pojo.getUrl());
				map.put("url", pojo.getUrl());
				String mimeType = pojo.getMimeType();
				if (pojo.getName().endsWith(".flac")) mimeType = "audio/flac";
				if (mimeType.startsWith("image") || mimeType.startsWith("video") || mimeType.startsWith("audio") ||
						mimeType.startsWith("application/vnd.openxmlformats") || mimeType.startsWith("text")) {
					map.put("mimeType", mimeType);
					map.put("encodeUrl", URLEncoder.encode(pojo.getUrl(), "utf-8"));
					if (mimeType.startsWith("text")) map.put("text", OkHttpUtils.getStr(pojo.getUrl()));
					return render("/preview");
				}else return moved(pojo.getUrl());
			}
			OnedriveItemList onedriveItemList;
			if (url == null) onedriveItemList = onedriveLogic.listFile(onedrivePojo, paths);
			else onedriveItemList = onedriveLogic.listFileByUrl(onedrivePojo, url);
			map.put("next", onedriveItemList.getNextLink());
			List<OnedriveItemPojo> list = onedriveItemList.getList();
			SystemConfigEntity passwordFileEntity = systemConfigService.findByType(SystemConfigType.PASSWORD_FILE);
			List<String> filterNameList = new ArrayList<>();
			if (passwordFileEntity != null){
				String passwordFileName = passwordFileEntity.getContent();
				OnedriveItemPojo passwordItemPojo = find(list, passwordFileName);
				if (passwordItemPojo != null){
					context.renderArg("needPassword", true);
					String itemPassword = OkHttpUtils.getStr(passwordItemPojo.getUrl());
					boolean needPassword = true;
					if (password != null){
						if (password.equals(itemPassword)){
							needPassword = false;
							resp.addCookie(new H.Cookie("password", MD5Utils.toMD5(password), URLEncoder.encode(contextPath, "utf-8")));
						}
					}else {
						if (passwordCookie != null && passwordCookie.value().equals(MD5Utils.toMD5(itemPassword)))
							needPassword = false;
					}
					if (needPassword) {
						return render("/needPassword");
					}
					filterNameList.add(passwordFileName);
				}
			}
			OnedriveItemPojo readmeItemPojo = find(list, "readme.md");
			if (readmeItemPojo != null) {
				map.put("readme", OkHttpUtils.getStr(readmeItemPojo.getUrl()));
				filterNameList.add("readme.md");
			}
			OnedriveItemPojo headItemPojo = find(list, "head.md");
			if (headItemPojo != null) {
				map.put("head", OkHttpUtils.getStr(headItemPojo.getUrl()));
				filterNameList.add("head.md");
			}
			String hides = driveConfig.getHide();
			if (StringUtil.isNotEmpty(hides)) {
				String[] hideArr = hides.split("\\|");
				for (String hide : hideArr) {
					String nowPath = removeSlash(__path);
					String hidePath = removeSlash(hide);
					list.forEach(it -> {
						if ((nowPath + "/" + it.getName()).equals(hidePath)) filterNameList.add(it.getName());
					});
				}
			}
			list = list.stream().filter(it-> !filterNameList.contains(it.getName())).collect(Collectors.toList());
			map.put("list", list);
			if (contextPath.charAt(contextPath.length() - 1) != '/')
				contextPath += "/";
			map.put("url", contextPath);
			map.put("path", __path);
		}
		return map;
	}

	@After(only = "index")
	public void cacheIndex(String name, String __path, ActionContext context, H.Request<?> req){
		if (Boolean.TRUE.equals(context.renderArg("needPassword"))) return;
		String method = req.method().name();
		Object admin = context.renderArg("admin");
		String key = name + __path + admin;
		if (method.equals("GET") && admin != null && indexCache.get(key) == null) {
			String tempPath = context.templatePath();
			if (tempPath.contains("needPassword")) return;
			Map<String, Object> map = new HashMap<>();
			context.renderArgs().forEach(map::put);
			indexCache.put(key, new IndexCache(tempPath, map));
		}
	}

	private OnedriveItemPojo find(List<OnedriveItemPojo> list, String name){
		for (OnedriveItemPojo onedriveItemPojo : list) {
			if (onedriveItemPojo.getIsFile() && onedriveItemPojo.getName().equalsIgnoreCase(name))
				return onedriveItemPojo;
		}
		return null;
	}

	private String removeSlash(String str){
		int l = str.length();
		if (l == 0) return str;
		if (str.charAt(l - 1) == '/')
			str = str.substring(0, l - 1);
		if (str.charAt(0) != '/')
			str = "/" + str;
		return str;
	}

	@GetAction("upload")
	public Map<String, Object> upload(){
		Map<String, Object> map = new HashMap<>();
		List<String> nameList = new ArrayList<>();
		List<DriveEntity> list = driveService.findAll();
		for (DriveEntity driveEntity : list) {
			DriveConfig driveConfig = driveEntity.getOtherConfigParse(DriveConfig.class);
			String path = driveConfig.getTouristUploadPath();
			if (StringUtil.isNotEmpty(path)) nameList.add(driveEntity.getName());
		}
		map.put("nameList", nameList);
		return map;
	}

	@PostAction("upload")
	public Result<?> uploadResult(@DbBind(value = "name", field = "name") DriveEntity driveEntity, String filename, String url) throws IOException {
		if (driveEntity == null) return Result.failure(ResultStatus.DATA_NOT_EXISTS);
		DriveConfig driveConfig = driveEntity.getOtherConfigParse(DriveConfig.class);
		String path = driveConfig.getTouristUploadPath();
		if (StringUtil.isEmpty(path)) return Result.failure("该存储没有配置游客上传路径！");
		LocalDateTime now = LocalDateTime.now();
		int year = now.getYear();
		int month = now.getMonth().getValue();
		int day = now.getDayOfMonth();
		OnedrivePojo onedrivePojo = driveEntity.getConfigParse(OnedrivePojo.class);
		String[] arr = path.split("/");
		long num = Arrays.stream(arr).filter(it -> !"".equals(it)).count();
		String[] newArr = new String[Math.toIntExact(num + 4)];
		AtomicInteger i = new AtomicInteger(0);
		Arrays.stream(arr).filter(it -> !"".equals(it)).forEach(it -> newArr[i.getAndIncrement()] = it);
		newArr[i.getAndIncrement()] = String.valueOf(year);
		newArr[i.getAndIncrement()] = String.valueOf(month);
		newArr[i.getAndIncrement()] = String.valueOf(day);
		newArr[i.getAndIncrement()] = filename;
		String uploadUrl = onedriveLogic.uploadBigFile(onedrivePojo, newArr);
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("uploadUrl", uploadUrl);
		String uu = url + "/" + driveEntity.getName() + "/" + StringUtil.join("/", newArr);
		resultMap.put("url", uu);
		return Result.success(resultMap);
	}

}
