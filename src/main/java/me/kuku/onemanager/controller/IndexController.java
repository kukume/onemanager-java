package me.kuku.onemanager.controller;

import act.db.sql.tx.Transactional;
import act.util.CacheFor;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.OnedriveItemPojo;
import me.kuku.onemanager.pojo.OnedrivePojo;
import me.kuku.onemanager.service.DriveService;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static act.controller.Controller.Util.moved;

public class IndexController {
	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;

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
		if (list != null) {
			driveEntity = list.get(0);
			return moved("/" + driveEntity.getName());
		}
		return new ArrayList<>();
	}

	@GetAction("/{name}/...")
	@CacheFor
	public Object index(String name, String __path) throws IOException {
		DriveEntity driveEntity = driveService.findByName(name);
		if (driveEntity != null){
			OnedrivePojo onedrivePojo = driveEntity.getConfigParse(OnedrivePojo.class);
			String[] paths = __path.split("/");
			OnedriveItemPojo pojo = onedriveLogic.source(onedrivePojo, paths);
			if (pojo.getIsFile()) return moved(pojo.getUrl());
			List<OnedriveItemPojo> list = onedriveLogic.listFile(onedrivePojo, paths);
			Map<String, Object> map = new HashMap<>();
			map.put("list", list);
			map.put("url", name + "/" + __path);
			List<Map<String, String>> hrefList = new ArrayList<>();
			Map<String, String> hrefMap = new HashMap<>();
			hrefMap.put("name", name);
			hrefMap.put("url", "/" + name);
			hrefList.add(hrefMap);
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
			return map;
		}
		return new ArrayList<>();
	}

}
