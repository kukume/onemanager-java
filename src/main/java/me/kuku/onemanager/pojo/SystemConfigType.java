package me.kuku.onemanager.pojo;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum SystemConfigType {
	PASSWORD_FILE("passwordFile"),
	ADMIN_LOGIN_PAGE("adminLoginPage"),
	BACKGROUND("background"),
	BACKGROUND_M("backgroundM"),
	CUSTOM_CSS("customCss"),
	CUSTOM_SCRIPT("customScript"),
	REFERRER("referrer"),
	SITE_NAME("siteName"),
	PASSWORD("password");

	private final String type;
	SystemConfigType(String type){
		this.type = type;
	}

	public static SystemConfigType parse(String type){
		SystemConfigType[] arr = values();
		for (SystemConfigType systemConfigType : arr) {
			if (systemConfigType.getType().equals(type)){
				return systemConfigType;
			}
		}
		return null;
	}
}
