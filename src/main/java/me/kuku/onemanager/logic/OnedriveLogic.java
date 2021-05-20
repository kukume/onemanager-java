package me.kuku.onemanager.logic;

import act.inject.AutoBind;
import me.kuku.onemanager.pojo.OnedriveItemPojo;
import me.kuku.onemanager.pojo.OnedrivePojo;

import java.io.File;
import java.io.IOException;
import java.util.List;

@AutoBind
public interface OnedriveLogic {
	String authorizationUrl(OnedrivePojo oneDrivePojo, String state);
	OnedrivePojo token(OnedrivePojo onedrivePojo, String code) throws IOException;
	OnedrivePojo refreshToken(OnedrivePojo onedrivePojo) throws IOException;
	List<OnedriveItemPojo> listFile(OnedrivePojo onedrivePojo, String...path) throws IOException;
	boolean upload(OnedrivePojo onedrivePojo, File file, String...path) throws IOException;
	String uploadBigFile(OnedrivePojo onedrivePojo, String...path) throws IOException;
	String download(OnedrivePojo onedrivePojo, String...path) throws IOException;
}