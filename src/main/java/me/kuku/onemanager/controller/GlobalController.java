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
		if (userAgent.toUpperCase().contains("QQ")) throw forbidden();
		SystemConfigEntity entity = systemConfigService.findByType(SystemConfigType.DISABLED_UA);
		if (entity != null){
			String[] arr = entity.getContent().split("\\|");
			for (String uaKey : arr) {
				if ("".equals(uaKey)) continue;
				if (userAgent.contains(uaKey)) throw forbidden();
			}
		}
	}
}
