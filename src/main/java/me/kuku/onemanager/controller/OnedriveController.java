package me.kuku.onemanager.controller;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.db.sql.tx.Transactional;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.*;
import me.kuku.onemanager.service.DriveService;
import me.kuku.onemanager.service.SystemConfigService;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static act.controller.Controller.Util.found;
import static act.controller.Controller.Util.renderJson;

@UrlContext("/onedrive")
public class OnedriveController {
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private DriveService driveService;
	@Inject
	private SystemConfigService systemConfigService;

	private final String REDIRECT_URL = "http://localhost:5460/onedrive/token";

	@GetAction("callback")
	public void callback(){}

	@PostAction("authUrl")
	public Result<?> authUrl(String name, String clientId, String clientSecret){
		if (driveService.findByName(name) != null) return Result.failure(ResultStatus.DATA_EXISTS);
		OnedrivePojo onedrivePojo = new OnedrivePojo();
		onedrivePojo.setClientId(clientId);
		onedrivePojo.setClientSecret(clientSecret);
		onedrivePojo.setRedirectUrl(REDIRECT_URL);
		DriveEntity entity = new DriveEntity();
		entity.setName(name);
		entity.setConfigParse(onedrivePojo);
		entity.setDriveType(DriveType.ONEDRIVE);
		driveService.save(entity);
		return Result.success(new HashMap<String, String>(){{
			put("url", onedriveLogic.authorizationUrl(onedrivePojo, name));
		}});
	}

	@Transactional
	private void save(String name, String code) throws IOException {
		DriveEntity driveEntity = driveService.findByName(name);
		if (driveEntity == null) return;
		OnedrivePojo pojo = driveEntity.getConfigParse(OnedrivePojo.class);
		OnedrivePojo newPojo = onedriveLogic.token(pojo, code);
		driveEntity.setConfigParse(newPojo);
		driveService.save(driveEntity);
	}

	@GetAction("token")
	public void token(String state, String code) throws IOException {
		save(state, code);
		found("/admin");
	}

	@Before(only = {"upload", "remove", "rename", "lock"})
	public void before(String name, ActionContext actionContext){
		if (name != null){
			DriveEntity driveEntity = driveService.findByName(name);
			if (driveEntity == null) {
				renderJson(Result.failure("呵呵"));
				return;
			}
			OnedrivePojo onedrivePojo = driveEntity.getConfigParse(OnedrivePojo.class);
			actionContext.renderArg("onedrivePojo", onedrivePojo);
		}
	}

	@PostAction("upload")
	public Result<?> upload(OnedrivePojo onedrivePojo, String path) throws IOException {
		String s = onedriveLogic.uploadBigFile(onedrivePojo, path.split("/"));
		return Result.success("成功", new HashMap<String, String>(){{
			put("url", s);
		}});
	}

	@PostAction("remove")
	public Result<?> delete(OnedrivePojo onedrivePojo, String itemId) throws IOException {
		boolean delete = onedriveLogic.delete(onedrivePojo, itemId);
		return delete ? Result.success() : Result.failure("删除失败！");
	}

	@PostAction("rename")
	public Result<?> rename(OnedrivePojo onedrivePojo, String itemId, String newName) throws IOException {
		boolean status = onedriveLogic.rename(onedrivePojo, itemId, newName);
		return status ? Result.success() : Result.failure("重命名失败！");
	}

	@PostAction("lock")
	public Result<?> lock(OnedrivePojo onedrivePojo, String path, String password) throws IOException {
		SystemConfigEntity entity = systemConfigService.findByType(SystemConfigType.PASSWORD_FILE);
		if (entity == null) return Result.failure("您没有配置密码文件，操作失败！");
		boolean upload = onedriveLogic.upload(onedrivePojo, password.getBytes(StandardCharsets.UTF_8),
				path + "/" + entity.getContent());
		return upload ? Result.success() : Result.failure("加密失败！");
	}
}
