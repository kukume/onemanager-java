package me.kuku.onemanager;

import act.Act;
import osgl.version.Versioned;

/**
 * A simple hello world app entry
 *
 * Run this app, try to update some of the code, then
 * press F5 in the browser to watch the immediate change
 * in the browser!
 */
@Versioned
public class AppEntry {

	public static void main(String[] args) throws Exception {
		Act.start();
	}

}