package me.kuku.onemanager.controller;

import act.controller.annotation.UrlContext;
import act.db.sql.tx.Transactional;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.logic.OnedriveLogic;
import me.kuku.onemanager.pojo.DriveType;
import me.kuku.onemanager.pojo.OnedrivePojo;
import me.kuku.onemanager.pojo.Result;
import me.kuku.onemanager.pojo.ResultStatus;
import me.kuku.onemanager.service.DriveService;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;

import static act.controller.Controller.Util.found;

@UrlContext("/onedrive")
public class OnedriveController {
	@Inject
	private OnedriveLogic onedriveLogic;
	@Inject
	private DriveService driveService;

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
}
