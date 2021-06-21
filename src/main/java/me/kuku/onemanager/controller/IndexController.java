package me.kuku.onemanager.controller;

import act.db.sql.tx.Transactional;
import act.util.CacheFor;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.OnedriveItemPojo;
import me.kuku.onemanager.pojo.OnedrivePojo;
import me.kuku.onemanager.pojo.SystemConfigType;
import me.kuku.onemanager.service.DriveService;
import me.kuku.onemanager.service.SystemConfigService;
import me.kuku.onemanager.utils.MD5Utils;
import me.kuku.onemanager.utils.OkHttpUtils;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static act.controller.Controller.Util.moved;

public class IndexController {
	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private SystemConfigService systemConfigService;

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
	                    String password, H.Cookie darkModeCookie) throws IOException {
		List<DriveEntity> driveEntityList = driveService.findAll();
		List<DriveEntity> resultList = driveEntityList.stream().filter(it -> it.getName().equals(name)).collect(Collectors.toList());
		Map<String, Object> map = new HashMap<>();
		if (darkModeCookie == null) map.put("darkMode", false);
		else map.put("darkMode", darkModeCookie.value().equals("true"));
		if (resultList.size() != 0){
			OnedrivePojo onedrivePojo = resultList.get(0).getConfigParse(OnedrivePojo.class);
			String[] paths = __path.split("/");
			OnedriveItemPojo pojo = onedriveLogic.source(onedrivePojo, paths);
			if (pojo.getIsFile()) return moved(pojo.getUrl());
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
			Map<SystemConfigType, SystemConfigEntity> typeMap = systemConfigService.findByTypeIn(SystemConfigType.SITE_NAME, SystemConfigType.PASSWORD_FILE);
			SystemConfigEntity entity = typeMap.get(SystemConfigType.SITE_NAME);
			String siteName = entity == null ? "OneManager": entity.getContent();
			map.put("siteName", siteName);
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
			list = list.stream().filter(it-> !filterNameList.contains(it.getName())).collect(Collectors.toList());
			map.put("list", list);
			if (contextPath.charAt(contextPath.length() - 1) != '/')
				contextPath += "/";
			map.put("url", contextPath);
			map.put("path", __path);
			if (!contextPath.equals("/" + name) && !contextPath.equals("/" + name + "/")){
				if (contextPath.charAt(contextPath.length() - 1) == '/')
					contextPath = contextPath.substring(0, contextPath.length() - 2);
				String prePath = contextPath.substring(0, contextPath.lastIndexOf('/') + 1);
				map.put("prePath", prePath);
			}
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

}
