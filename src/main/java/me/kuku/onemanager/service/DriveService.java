package me.kuku.onemanager.service;

import act.inject.AutoBind;
import me.kuku.onemanager.entity.DriveEntity;

@AutoBind
public interface DriveService {
	DriveEntity findByName(String name);
	DriveEntity save(DriveEntity driveEntity);
	void deleteByName(String name);
}
