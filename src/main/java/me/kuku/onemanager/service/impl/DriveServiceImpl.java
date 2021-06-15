package me.kuku.onemanager.service.impl;

import me.kuku.onemanager.entity.DriveEntity;
import me.kuku.onemanager.service.DriveService;

import javax.inject.Inject;
import java.util.List;

public class DriveServiceImpl implements DriveService {
	@Inject
	private DriveEntity.Dao dao;

	@Override
	public DriveEntity findByName(String name) {
		return dao.findOneBy("name", name);
	}

	@Override
	public DriveEntity save(DriveEntity driveEntity) {
		return dao.save(driveEntity);
	}

	@Override
	public void deleteByName(String name) {
		dao.deleteBy("name", name);
	}

	@Override
	public List<DriveEntity> findAll() {
		return dao.findAllAsList();
	}
}
