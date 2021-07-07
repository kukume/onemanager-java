package me.kuku.onemanager.logic;

import act.inject.AutoBind;
import me.kuku.onemanager.pojo.OnedriveItemList;
import me.kuku.onemanager.pojo.OnedriveItemPojo;
import me.kuku.onemanager.pojo.OnedrivePojo;

import java.io.File;
import java.io.IOException;

@AutoBind
public interface OnedriveLogic {
	String authorizationUrl(OnedrivePojo oneDrivePojo, String state);
	OnedrivePojo token(OnedrivePojo onedrivePojo, String code) throws IOException;
	OnedrivePojo refreshToken(OnedrivePojo onedrivePojo) throws IOException;
	OnedriveItemList listFile(OnedrivePojo onedrivePojo, String...path) throws IOException;
	OnedriveItemList listFileByUrl(OnedrivePojo onedrivePojo, String url) throws IOException;
	boolean upload(OnedrivePojo onedrivePojo, File file, String...path) throws IOException;
	boolean upload(OnedrivePojo onedrivePojo, byte[] bytes, String...path) throws IOException;
	String uploadBigFile(OnedrivePojo onedrivePojo, String...path) throws IOException;
	String download(OnedrivePojo onedrivePojo, String...path) throws IOException;
	OnedriveItemPojo source(OnedrivePojo onedrivePojo, String...path) throws IOException;
	String size(OnedrivePojo onedrivePojo) throws IOException;
	boolean delete(OnedrivePojo onedrivePojo, String itemId) throws IOException;
	boolean rename(OnedrivePojo onedrivePojo, String itemId, String newName) throws IOException;
}