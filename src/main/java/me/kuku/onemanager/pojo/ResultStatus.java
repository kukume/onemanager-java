package me.kuku.onemanager.pojo;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum ResultStatus {
	SUCCESS(200, "成功"),
	FAIL(500, "失败"),
	PARAM_ERROR(501, "参数异常"),
	DATA_EXISTS(502, "数据已存在"),
	DATA_NOT_EXISTS(503, "数据不存在");


	private final Integer code;
	private final String message;

	ResultStatus(Integer code, String message){
		this.code = code;
		this.message = message;
	}
}
