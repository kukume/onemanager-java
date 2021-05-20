package me.kuku.onemanager.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnedrivePojo {
	private String clientId;
	private String redirectUrl;
	private String clientSecret;
	private String accessToken;
	private String refreshToken;
	private Integer expires;
}
