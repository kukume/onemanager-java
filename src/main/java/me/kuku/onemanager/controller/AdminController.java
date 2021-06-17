package me.kuku.onemanager.controller;

import act.controller.annotation.UrlContext;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.OnedrivePojo;
import me.kuku.onemanager.service.DriveService;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static act.controller.Controller.Util.found;
import static act.controller.Controller.Util.render;

@UrlContext("/admin")
public class AdminController {

	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;

	@GetAction
	public void index(){
		List<DriveEntity> findList = driveService.findAll();
		List<JSONObject> list = findList.stream().map(JSON::toJSONString).map(JSON::parseObject).peek(it -> {
			try {
				String config = it.getString("config");
				JSONObject jsonObject = JSON.parseObject(config);
				String size = onedriveLogic.size(new OnedrivePojo(jsonObject.getString("accessToken")));
				it.put("size", size);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).collect(Collectors.toList());
		render(list);
	}

	@PostAction("delpan")
	public void deletePan(String name){
		driveService.deleteByName(name);
	}
}
