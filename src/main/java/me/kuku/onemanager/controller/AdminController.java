package me.kuku.onemanager.controller;

import act.controller.annotation.UrlContext;
import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.service.DriveService;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;
import java.util.List;

import static act.controller.Controller.Util.found;
import static act.controller.Controller.Util.render;

@UrlContext("/admin")
public class AdminController {

	@Inject
	private DriveService driveService;

	@GetAction
	public void index(){
		List<DriveEntity> list = driveService.findAll();
		render(list);
	}

	@PostAction("delpan")
	public void deletePan(String name){
		driveService.deleteByName(name);
	}
}
