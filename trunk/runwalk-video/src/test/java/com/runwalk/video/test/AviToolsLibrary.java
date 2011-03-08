package com.runwalk.video.test;

import com.ochafik.lang.jnaerator.runtime.LibraryExtractor;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.ptr.IntByReference;
/**
 * JNA Wrapper for library <b>AviTools</b><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class AviToolsLibrary implements Library {
	public static final java.lang.String JNA_LIBRARY_NAME = LibraryExtractor.getLibraryPath("uEye_tools", true, AviToolsLibrary.class);
	public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(AviToolsLibrary.JNA_LIBRARY_NAME, com.ochafik.lang.jnaerator.runtime.MangledFunctionMapper.DEFAULT_OPTIONS);
	static {
		Native.register(AviToolsLibrary.JNA_LIBRARY_NAME);
	}
	public static final int IS_AVI_CM_BAYER = 11;
	public static final int IS_AVI_CM_RGB24 = 1;
	public static final int IS_AVI_CM_Y8 = 6;
	public static final int IS_AVI_CM_RGB32 = 0;
	public static final int IS_AVI_NO_ERR = 0;
	public static final int IS_AVI_SET_EVENT_FRAME_SAVED = 1;
	public static final int ISAVIERRBASE = 300;

	public static native int isavi_InitAVI(IntByReference pnAviID, int cameraHandle);
	
	public static native int isavi_OpenAVI(int nAviID, String strFileName);
	
	public static native int isavi_StartAVI(int nAviID);
	
	public static native int isavi_StopAVI(int nAviID);
	
	public static native int isavi_ExitAVI(int nAviID);
	
	public static native int isavi_SetImageSize(int nAviID, int cMode, int Width, int Height, int PosX, int PosY, int LineOffset);
	
	/*! \brief	Sets the frame rate of the video. The frame rate can be changed at any time if the avi file is 
	*			already created.
	*
	*
	*  \param   nAviID:			Instance ID returned by isavi_InitAVI()
	*  \param	pcImageMem:		Pointer to data image
	*
	*  \return	IS_AVI_NO_ERR			No error, initialisation was successful
	*  \return	IS_AVI_ERR_INVALID_ID	The specified instance could not be found. The ID is either invalid or the 
	*									specified interface has already been destroyed by a previous call to 
	*									isavi_ExitAVI().
	*  \return	IS_AVI_ERR_WRITE_INFO	The AVI file could not be modified
	***********************************************************************************************************/
	public static native int isavi_SetFrameRate(int nAviID,double fr);
	
	/*! \brief	Sets the quality of the actual image that is going to be compressed and added to the video stream. 
	*			The quality can be changed at any time. The best image quality is 100 (bigger avi file size) and 
	*			the worst is 1.
	*
	*
	*  \param   nAviID:	Instance ID returned by isavi_InitAVI()
	*  \param	q:		Quality of compression [1…100] 
	*
	*  \return	IS_AVI_NO_ERR				No error, initialisation was successful
	*  \return	IS_AVI_ERR_INVALID_ID		The specified instance could not be found. The ID is either invalid 
	*										for the specified interface has already been destroyed by a previous 
	*										call to isavi_ExitAVI().
	*  \return	IS_AVI_ERR_INVALID_VALUE	The parameter q is bigger than 100 or less than 1
	***********************************************************************************************************/
	public static native int isavi_SetImageQuality(int nAviID, int q);
	
	
}
