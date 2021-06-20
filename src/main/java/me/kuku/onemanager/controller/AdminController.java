package me.kuku.onemanager.controller;

import act.controller.annotation.UrlContext;
import act.db.sql.tx.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.exception.VerifyFailedException;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.OnedrivePojo;
import me.kuku.onemanager.pojo.Result;
import me.kuku.onemanager.pojo.SystemConfigType;
import me.kuku.onemanager.service.DriveService;
import me.kuku.onemanager.service.SystemConfigService;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static act.controller.Controller.Util.render;

@UrlContext("/admin")
public class AdminController {

	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private SystemConfigService systemConfigService;

	@GetAction
	public void index(){
		List<DriveEntity> findList = driveService.findAll();
		List<JSONObject> list = findList.stream().map(JSON::toJSONString).map(JSON::parseObject).peek(it -> {
			try {
				String config = it.getString("config");
				JSONObject jsonObject = JSON.parseObject(config);
				String size;
				try {
					size = onedriveLogic.size(new OnedrivePojo(jsonObject.getString("accessToken")));
				} catch (VerifyFailedException e) {
					e.printStackTrace();
					size = "0 / 0";
				}
				it.put("size", size);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).collect(Collectors.toList());
		Map<String, SystemConfigEntity> systemConfigMap = new HashMap<>();
		systemConfigService.findAll().forEach(it -> {
			systemConfigMap.put(it.getSystemConfigType().getType(), it);
		});
		render(list, systemConfigMap);
	}

	@PostAction("delpan")
	public void deletePan(String name){
		driveService.deleteByName(name);
	}

	@PostAction("systemConfigSetting")
	@Transactional
	public Result<Void> systemConfigSetting(Map<String, String> params){
		for (Map.Entry<String, String> entry: params.entrySet()){
			String key = entry.getKey();
			String value = entry.getValue();
			if (!"".equals(value)){
				SystemConfigType type = SystemConfigType.parse(key);
				if (type != null){
					SystemConfigEntity entity = systemConfigService.findByType(type);
					if (entity == null) entity = new SystemConfigEntity(type);
					entity.setContent(value);
					systemConfigService.save(entity);
				}
			}
		}
		return Result.success();
	}
}