package me.kuku.onemanager.controller;

import act.inject.HeaderVariable;
import act.util.Global;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.pojo.SystemConfigType;
import me.kuku.onemanager.service.SystemConfigService;
import org.osgl.mvc.annotation.Before;

import javax.inject.Inject;

import static act.controller.Controller.Util.forbidden;

public class GlobalController {

	@Inject
	private SystemConfigService systemConfigService;

	@Global
	@Before
	public void globalBefore(@HeaderVariable String userAgent){
		if (userAgent == null) {
			forbidden("ua都没有，你不是机器人？");
			return;
		}
		if (userAgent.toUpperCase().contains("QQ")) {
			forbidden("请不要在QQ中打开！！请复制链接到浏览器打开！！");
			return;
		}
		SystemConfigEntity entity = systemConfigService.findByType(SystemConfigType.DISABLED_UA);
		if (entity != null){
			String[] arr = entity.getContent().split("\\|");
			for (String uaKey : arr) {
				if ("".equals(uaKey)) continue;
				if (userAgent.contains(uaKey)) forbidden("禁止的UA");
			}
		}
	}
}
