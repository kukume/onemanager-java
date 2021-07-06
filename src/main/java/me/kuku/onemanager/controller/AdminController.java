package me.kuku.onemanager.controller;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.db.DbBind;
import act.db.sql.tx.Transactional;
import act.util.CacheFor;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.exception.VerifyFailedException;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.*;
import me.kuku.onemanager.service.DriveService;
import me.kuku.onemanager.service.SystemConfigService;
import org.osgl.cache.CacheService;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static act.controller.Controller.Util.*;

@UrlContext("/admin")
public class AdminController {

	@Inject
	private DriveService driveService;
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private SystemConfigService systemConfigService;
	@Inject
	@Named("indexCache")
	private CacheService indexCache;

	@PostAction("login")
	public Result<?> login(String password, H.Session session){
		SystemConfigEntity entity = systemConfigService.findByType(SystemConfigType.PASSWORD);
		if (entity == null) {
			entity = new SystemConfigEntity(SystemConfigType.PASSWORD);
			entity.setContent(password);
			systemConfigService.save(entity);
			session.put("admin", password);
			return Result.failure(201, "设置密码成功！", null);
		}else {
			if (password.equals(entity.getContent())) {
				session.put("admin", password);
				return Result.success();
			} else return Result.failure("登陆失败，密码错误！");
		}
	}

	@PostAction("logout")
	public Result<?> logout(H.Session session){
		session.remove("admin");
		return Result.success();
	}

	@Before(except = {"login", "logout"})
	public void before(H.Session session){
		SystemConfigEntity entity = systemConfigService.findByType(SystemConfigType.PASSWORD);
		if (entity == null)
			renderText("未配置密码，请先去首页进行登录。");
		else if (!entity.getContent().equals(session.get("admin")))
			renderText("未授权，不允许访问！");
	}

	@Before(only = {"index", "var", "drive"})
	public void htmlBefore(ActionContext context){
		List<DriveEntity> findList = driveService.findAll();
		List<JSONObject> list = findList.stream().map(JSON::toJSONString).map(JSON::parseObject).collect(Collectors.toList());
		context.renderArg("list", list);
	}

	@GetAction
	public void index(ActionContext context){
		context.renderArg("curName", "index");
	}

	@GetAction("var")
	public void var(ActionContext context){
		context.renderArg("curName", "var");
		List<SystemConfigEntity> list = systemConfigService.findAll();
		JSONObject jsonObject = new JSONObject();
		for (SystemConfigEntity entity : list) {
			if (entity.getSystemConfigType() == SystemConfigType.PASSWORD) continue;
			jsonObject.put(entity.getSystemConfigType().getType(), entity.getContent());
		}
		context.renderArg("jsonObject", jsonObject);
	}

	@GetAction("{name}")
	public void drive(String name, List<JSONObject> list, ActionContext context){
		context.renderArg("curName", name);
		boolean status = false;
		for (JSONObject jsonObject : list) {
			if (jsonObject.getString("name").equals(name)){
				status = true;
				String config = jsonObject.getString("config");
				JSONObject configJsonObject = JSON.parseObject(config);
				String size;
				try {
					size = onedriveLogic.size(new OnedrivePojo(configJsonObject.getString("accessToken")));
				} catch (Exception e) {
					size = "AccessToken已失效，重新进入一遍首页对应的盘会刷新";
				}
				jsonObject.put("size", size);
				context.renderArg("jsonObject", jsonObject);
			}
		}
		if (!status) renderText("没有找到这个名字的盘");
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

	@PostAction("changePassword")
	@Transactional
	public Object changePassword(String oldPassword, String newPassword, H.Session session){
		SystemConfigEntity systemConfigEntity = systemConfigService.findByType(SystemConfigType.PASSWORD);
		String password = systemConfigEntity.getContent();
		if (oldPassword.equals(password)){
			systemConfigEntity.setContent(newPassword);
			systemConfigService.save(systemConfigEntity);
			session.remove("admin");
			return Result.success();
		}else return Result.failure("旧密码输入错误，请重试！");
	}

	@PostAction("clearCache")
	public Result<?> clearCache(){
		indexCache.clear();
		return Result.success();
	}
}