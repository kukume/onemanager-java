package me.kuku.onemanager.controller;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("/admin")
public class AdminController {

	@GetAction
	public void index(){}
}
