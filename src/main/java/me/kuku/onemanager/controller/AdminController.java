package me.kuku.onemanager.controller;

import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.sql.tx.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.exception.VerifyFailedException;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.*;
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

@UrlContext("/admin")
public class AdminController {

	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private SystemConfigService systemConfigService;

	@GetAction
	public Map<String, Object> index(){
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
					size = "AccessToken已失效";
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
		Map<String, Object> map = new HashMap<>();
		map.put("list", list);
		map.put("systemConfigMap", systemConfigMap);
		return map;
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
			SystemConfigType type = SystemConfigType.parse(key);
			if (type != null){
				SystemConfigEntity entity = systemConfigService.findByType(type);
				if (entity == null) entity = new SystemConfigEntity(type);
				entity.setContent(value);
				systemConfigService.save(entity);
			}
		}
		return Result.success();
	}

	@PostAction("editName")
	@Transactional
	public Result<?> editName(@DbBind(value = "name", field = "name") DriveEntity driveEntity, String value){
		if (driveEntity != null){
			DriveEntity newEntity = driveService.findByName(value);
			if (newEntity != null) return Result.failure("要更改的名称已存在，更改失败！");
			driveEntity.setName(value);
			driveService.save(driveEntity);
			return Result.success("修改成功！", null);
		}else return Result.failure(ResultStatus.DATA_NOT_EXISTS);
	}

	@PostAction("reAuth")
	public Result<?> reAuth(@DbBind(value = "name", field = "name") DriveEntity driveEntity){
		if (driveEntity != null){
			OnedrivePojo onedrivePojo = driveEntity.getConfigParse(OnedrivePojo.class);
			String url = onedriveLogic.authorizationUrl(onedrivePojo, driveEntity.getName());
			return Result.success(Result.map("url", url));
		}else return Result.failure(ResultStatus.DATA_NOT_EXISTS);
	}

	@PostAction("copy")
	public Result<?> copy(@DbBind(value = "name", field = "name") DriveEntity driveEntity, String newName){
		if (driveEntity == null) return Result.failure(ResultStatus.DATA_NOT_EXISTS);
		if (driveService.findByName(newName) != null) return Result.failure(ResultStatus.DATA_EXISTS);
		DriveEntity newEntity = new DriveEntity(null, newName, driveEntity.getDriveType(), driveEntity.getConfig(), driveEntity.getOtherConfig());
		driveService.save(newEntity);
		return Result.success("复制成功", null);
	}

	@PostAction("driveConfig")
	public Result<?> driveConfig(@DbBind(value = "name", field = "name") DriveEntity driveEntity,
	                             String proxy, String touristUploadPath, String onlyDic, String hide){
		if (driveEntity == null) return Result.failure(ResultStatus.DATA_NOT_EXISTS);
		DriveConfig driveConfig = driveEntity.getOtherConfigParse(DriveConfig.class);
		if (proxy != null) driveConfig.setProxy(proxy);
		if (touristUploadPath != null) driveConfig.setTouristUploadPath(touristUploadPath);
		if (onlyDic != null) driveConfig.setOnlyPic(onlyDic);
		if (hide != null) driveConfig.setHide(hide);
		driveEntity.setOtherConfigParse(driveConfig);
		driveService.save(driveEntity);
		return Result.success("保存成功！", null);
	}
}