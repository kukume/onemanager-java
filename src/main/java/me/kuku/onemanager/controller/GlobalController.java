package me.kuku.onemanager.controller;

import act.inject.HeaderVariable;
import act.util.Global;
import org.osgl.mvc.annotation.Before;

import static act.controller.Controller.Util.renderText;

public class GlobalController {

	@Global
	@Before
	public void globalBefore(@HeaderVariable String userAgent){
		if (userAgent.contains("QQ")) renderText("不允许在qq中打开！");
	}
}
