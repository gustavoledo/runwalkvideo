package com.runwalk.video.test;
import java.nio.ByteBuffer;

import com.ochafik.lang.jnaerator.runtime.CharByReference;
import com.ochafik.lang.jnaerator.runtime.LibraryExtractor;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
/**
 * JNA Wrapper for library <b>NativeIdsCapturer</b><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class CuEyeCapturerLibrary implements StdCallLibrary {
	public static final java.lang.String JNA_LIBRARY_NAME = LibraryExtractor.getLibraryPath("CuEyeCapturer", true, CuEyeCapturerLibrary.class);
	public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(CuEyeCapturerLibrary.JNA_LIBRARY_NAME, com.ochafik.lang.jnaerator.runtime.MangledFunctionMapper.DEFAULT_OPTIONS);
	
	//CuEyeCapturer INSTANCE = (CuEyeCapturer) Native.loadLibrary("CuEyeCapturer", CuEyeCapturer.class);

	
	public static final int INIT_SUCCESS = 0;
	public static final int IS_INVALID_HANDLE = 1;
	
	static {
		Native.register(CuEyeCapturerLibrary.JNA_LIBRARY_NAME);
	}

	/**
	 * Original signature : <code>int* GetCameraNames()</code><br>
	 * <i>native declaration : line 14</i>
	 */
	public static native UEYE_CAMERA_LIST.ByReference GetCameraNames();
	/**
	 * <i>native declaration : line 15</i><br>
	 * Conversion Error : LPMSG
	 */
	public static native int InitializeCamera(IntByReference cameraHandle, String settingsFile);
	
	public static native int StartRunning(IntByReference cameraHandle);
	
	public static native int StopRunning(IntByReference cameraHandle);
	
}
