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
import org.osgl.http.H;
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

	@GetAction("/{name}/...")
	@CacheFor
	public Object index(String name, String __path, H.Request<?> req) throws IOException {
		List<DriveEntity> driveEntityList = driveService.findAll();
		List<DriveEntity> resultList = driveEntityList.stream().filter(it -> it.getName().equals(name)).collect(Collectors.toList());
		if (resultList.size() != 0){
			OnedrivePojo onedrivePojo = resultList.get(0).getConfigParse(OnedrivePojo.class);
			String[] paths = __path.split("/");
			OnedriveItemPojo pojo = onedriveLogic.source(onedrivePojo, paths);
			if (pojo.getIsFile()) return moved(pojo.getUrl());
			List<OnedriveItemPojo> list = onedriveLogic.listFile(onedrivePojo, paths);
			Map<String, Object> map = new HashMap<>();
			map.put("list", list);
			map.put("url", "/" + name + "/" + __path);
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
			map.put("name", name);
			map.put("path", __path);
			String contextPath = req.path();
			if (!contextPath.equals("/" + name + "/")){
				String prePath = contextPath.substring(0, contextPath.lastIndexOf('/') + 1);
				map.put("prePath", prePath);
			}
			SystemConfigEntity entity = systemConfigService.findByType(SystemConfigType.SITE_NAME);
			String siteName = entity == null ? "OneManager": entity.getContent();
			map.put("siteName", siteName);
			List<Map<String, String>> driveList = driveEntityList.stream().map(it -> {
				Map<String, String> resultMap = new HashMap<>();
				resultMap.put("name", it.getName());
				resultMap.put("url", "/" + it.getName());
				return resultMap;
			}).collect(Collectors.toList());
			map.put("driveList", driveList);
			return map;
		}
		return new ArrayList<>();
	}

}
