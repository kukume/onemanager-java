package me.kuku.onemanager.controller;

import act.db.DbBind;
import act.db.sql.tx.Transactional;
import act.util.CacheFor;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.*;
import me.kuku.onemanager.service.DriveService;
import me.kuku.onemanager.service.SystemConfigService;
import me.kuku.onemanager.utils.MD5Utils;
import me.kuku.onemanager.utils.OkHttpUtils;
import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.util.StringUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static act.controller.Controller.Util.moved;
import static act.controller.Controller.Util.render;

public class IndexController {
	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private SystemConfigService systemConfigService;

	@Catch
	public void error(Exception e){
		String errMsg = e.getMessage();
		render("error", errMsg);
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

	@GetAction("/")
	public Object find() throws IOException {
		List<DriveEntity> list = driveService.findAll();
		DriveEntity driveEntity;
		if (list != null && list.size() != 0) {
			driveEntity = list.get(0);
			return moved("/" + driveEntity.getName());
		}
		return new ArrayList<>();
	}

	@Action(value = "/{name}/...", methods = {H.Method.GET, H.Method.POST})
	@CacheFor
	public Object index(String name, String __path, H.Request<?> req, H.Cookie passwordCookie, H.Response<?> resp,
	                    String password, H.Cookie darkModeCookie, String preview, H.Session session) throws IOException {
		List<DriveEntity> driveEntityList = driveService.findAll();
		List<DriveEntity> resultList = driveEntityList.stream().filter(it -> it.getName().equals(name)).collect(Collectors.toList());
		Map<String, Object> map = new HashMap<>();
		Map<SystemConfigType, SystemConfigEntity> typeMap = systemConfigService.findByTypeIn(SystemConfigType.SITE_NAME,
				SystemConfigType.PASSWORD_FILE, SystemConfigType.PASSWORD, SystemConfigType.CUSTOM_CSS, SystemConfigType.CUSTOM_SCRIPT);
		SystemConfigEntity adminPasswordEntity = typeMap.get(SystemConfigType.PASSWORD);
		map.put("admin", adminPasswordEntity.getContent().equals(session.get("admin")));
		SystemConfigEntity cssEntity = typeMap.get(SystemConfigType.CUSTOM_CSS);
		if (cssEntity != null) map.put("css", cssEntity.getContent());
		SystemConfigEntity scriptEntity = typeMap.get(SystemConfigType.CUSTOM_SCRIPT);
		if (scriptEntity != null) map.put("script", scriptEntity.getContent());
		if (darkModeCookie == null) map.put("darkMode", false);
		else map.put("darkMode", darkModeCookie.value().equals("true"));
		if (resultList.size() != 0){
			DriveEntity driveEntity = resultList.get(0);
			OnedrivePojo onedrivePojo = driveEntity.getConfigParse(OnedrivePojo.class);
			DriveConfig driveConfig = driveEntity.getOtherConfigParse(DriveConfig.class);
			String[] paths = __path.split("/");
			OnedriveItemPojo pojo = onedriveLogic.source(onedrivePojo, paths);
			List<Map<String, String>> driveList = driveEntityList.stream().map(it -> {
				Map<String, String> resultMap = new HashMap<>();
				resultMap.put("name", it.getName());
				resultMap.put("url", "/" + it.getName());
				return resultMap;
			}).collect(Collectors.toList());
			map.put("driveList", driveList);
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
			SystemConfigEntity entity = typeMap.get(SystemConfigType.SITE_NAME);
			String siteName = entity == null ? "OneManager": entity.getContent();
			map.put("siteName", siteName);
			if (!contextPath.equals("/" + name) && !contextPath.equals("/" + name + "/")){
				if (contextPath.charAt(contextPath.length() - 1) == '/')
					contextPath = contextPath.substring(0, contextPath.length() - 1);
				String prePath = contextPath.substring(0, contextPath.lastIndexOf('/') + 1);
				map.put("prePath", prePath);
			}
			if (pojo.getIsFile()) {
				if (preview == null) return moved(pojo.getUrl());
				map.put("url", pojo.getUrl());
				String mimeType = pojo.getMimeType();
				if (pojo.getName().endsWith(".flac")) mimeType = "audio/flac";
				if (mimeType.startsWith("image") || mimeType.startsWith("video") || mimeType.startsWith("audio")) {
					map.put("mimeType", mimeType);
					render("/preview");
					return map;
				}else return moved(pojo.getUrl());
			}
			List<OnedriveItemPojo> list = onedriveLogic.listFile(onedrivePojo, paths);
			SystemConfigEntity passwordFileEntity = typeMap.get(SystemConfigType.PASSWORD_FILE);
			List<String> filterNameList = new ArrayList<>();
			if (passwordFileEntity != null){
				String passwordFileName = passwordFileEntity.getContent();
				OnedriveItemPojo passwordItemPojo = find(list, passwordFileName);
				if (passwordItemPojo != null){
					String itemPassword = OkHttpUtils.getStr(passwordItemPojo.getUrl());
					boolean needPassword = true;
					if (password != null){
						if (password.equals(itemPassword)){
							needPassword = false;
							resp.addCookie(new H.Cookie("password", MD5Utils.toMD5(password), contextPath));
						}
					}else {
						if (passwordCookie != null && passwordCookie.value().equals(MD5Utils.toMD5(itemPassword)))
							needPassword = false;
					}
					map.put("needPassword", needPassword);
					if (needPassword) return map;
					filterNameList.add(passwordFileName);
				}
			}
			OnedriveItemPojo readmeItemPojo = find(list, "readme.md");
			if (readmeItemPojo != null) {
				map.put("readme", OkHttpUtils.getStr(readmeItemPojo.getUrl()));
				filterNameList.add("readme.md");
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
