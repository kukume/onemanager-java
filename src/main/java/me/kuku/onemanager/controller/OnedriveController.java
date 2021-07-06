package me.kuku.onemanager.controller;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.db.sql.tx.Transactional;
import act.inject.SessionVariable;
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
import java.util.Map;

import static act.controller.Controller.Util.*;

@UrlContext("/onedrive")
public class OnedriveController {
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private DriveService driveService;
	@Inject
	private SystemConfigService systemConfigService;

	@Before
	public void gloBefore(@SessionVariable String admin){
		SystemConfigEntity entity = systemConfigService.findByType(SystemConfigType.PASSWORD);
		if (entity == null || !entity.getContent().equals(admin))
			renderJson(Result.failure("未授权，禁止访问！"));
	}

	@GetAction("callback")
	public void callback(){}

	@PostAction("authUrl")
	public Result<?> authUrl(String name, String clientId, String clientSecret, String host){
		if (driveService.findByName(name) != null) return Result.failure(ResultStatus.DATA_EXISTS);
		String redirectUrl = host + "/onedrive/token";
		String state = name;
		if (clientId == null || clientSecret == null){
			clientId = "17cd12b4-60a6-4109-9353-3380c77c89c0";
			clientSecret = "~V_v_p~bkejviTLTgIx75SA05~kOY-kA0I";
			state = redirectUrl + "?state=" + name;
			redirectUrl = "https://api.kuku.me/tool/onedrive";
		}
		OnedrivePojo onedrivePojo = new OnedrivePojo();
		onedrivePojo.setClientId(clientId);
		onedrivePojo.setClientSecret(clientSecret);
		onedrivePojo.setRedirectUrl(redirectUrl);
		DriveEntity entity = new DriveEntity();
		entity.setName(name);
		entity.setConfigParse(onedrivePojo);
		entity.setDriveType(DriveType.ONEDRIVE);
		driveService.save(entity);
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("url", onedriveLogic.authorizationUrl(onedrivePojo, state));
		return Result.success(resultMap);
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
		moved("/admin");
	}

	@Before(except = {"callback", "authUrl", "token"})
	public void before(String name, ActionContext actionContext){
		if (name != null){
			DriveEntity driveEntity = driveService.findByName(name);
			if (driveEntity == null) {
				renderJson(Result.failure("没有这个驱动器！"));
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

	@PostAction("uploadMd")
	public Result<?> uploadMd(OnedrivePojo onedrivePojo, String path, String value, String mdName) throws IOException {
		boolean upload = onedriveLogic.upload(onedrivePojo, value.getBytes(StandardCharsets.UTF_8),
				path + "/" + mdName);
		return upload ? Result.success() : Result.failure("上传失败！");
	}
}
