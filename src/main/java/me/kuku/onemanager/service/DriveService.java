package me.kuku.onemanager.service;

import act.inject.AutoBind;
import me.kuku.onemanager.entity.DriveEntity;

import java.util.List;

@AutoBind
public interface DriveService {
	DriveEntity findByName(String name);
	DriveEntity save(DriveEntity driveEntity);
	void deleteByName(String name);
	List<DriveEntity> findAll();
}
