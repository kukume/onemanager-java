package me.kuku.onemanager.pojo;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum SystemConfigType {
	PASSWORD_FILE("passwordFile"),
	CUSTOM_CSS("customCss"),
	CUSTOM_SCRIPT("customScript"),
	SITE_NAME("siteName"),
	PASSWORD("password"),
	FAVICON("favicon"),
	KEYWORD("keyword"),
	DESCRIPTION("description"),
	DISABLED_UA("disabledUa");

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
