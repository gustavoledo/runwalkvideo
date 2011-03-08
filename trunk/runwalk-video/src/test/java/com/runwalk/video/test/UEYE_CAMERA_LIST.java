package com.runwalk.video.test;

import com.ochafik.lang.jnaerator.runtime.Structure;
/**
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class UEYE_CAMERA_LIST extends Structure<UEYE_CAMERA_LIST, UEYE_CAMERA_LIST.ByValue, UEYE_CAMERA_LIST.ByReference > {
	/// Conversion Error : ULONG
	public int dwCount;
	/// C type : UEYE_CAMERA_INFO[1]
	public UEYE_CAMERA_INFO[] uci = new UEYE_CAMERA_INFO[(1)];
	public UEYE_CAMERA_LIST() {
		super();
	}
	/// @param uci C type : UEYE_CAMERA_INFO[1]
	public UEYE_CAMERA_LIST(UEYE_CAMERA_INFO uci[]) {
		super();
		if (uci.length != this.uci.length) 
			throw new java.lang.IllegalArgumentException("Wrong array size !");
		this.uci = uci;
	}
	protected ByReference newByReference() { return new ByReference(); }
	protected ByValue newByValue() { return new ByValue(); }
	protected UEYE_CAMERA_LIST newInstance() { return new UEYE_CAMERA_LIST(); }
	public static UEYE_CAMERA_LIST[] newArray(int arrayLength) {
		return Structure.newArray(UEYE_CAMERA_LIST.class, arrayLength);
	}
	public static class ByReference extends UEYE_CAMERA_LIST implements Structure.ByReference {
		
	};
	public static class ByValue extends UEYE_CAMERA_LIST implements Structure.ByValue {
		
	};
}
