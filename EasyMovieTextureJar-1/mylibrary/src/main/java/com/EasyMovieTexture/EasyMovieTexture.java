package com.EasyMovieTexture;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.android.vending.expansion.zipfile.ZipResourceFile.ZipEntryRO;
import com.google.android.exoplayer2.ExoPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.DOWNLOAD_SERVICE;

public class EasyMovieTexture implements MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, OnFrameAvailableListener {
	private Activity 		m_UnityActivity = null;
	private MediaPlayer 	m_MediaPlayer = null;
	
	private int				m_iUnityTextureID = -1;
	private int				m_iSurfaceTextureID = -1;
	private SurfaceTexture	m_SurfaceTexture = null;
	private Surface			m_Surface = null;
	private int 			m_iCurrentSeekPercent = 0;
	private int				m_iCurrentSeekPosition = 0;
	public int 				m_iNativeMgrID;
	private String 			m_strFileName;
	private int 			m_iErrorCode;
	private int				m_iErrorCodeExtra;
	private boolean			m_bRockchip = true;
	private boolean 		m_bSplitOBB = false;
	private String 			m_strOBBName;
	public boolean 			m_bUpdate= false;

	private MyMediaCodec m_mediaCodec;

	private MyExoplayer m_Exoplayer;

	//ForDownload
//	private DownloadManager downloadManager ;
//	private long  mReference = 0 ;
//	private CompleteReceiver cr;
	//ForDownload
	
	public static ArrayList<EasyMovieTexture> m_objCtrl = new ArrayList<EasyMovieTexture>();
	
	public static EasyMovieTexture GetObject(int iID)
	{
		for(int i = 0; i < m_objCtrl.size(); i++)
		{
			if(m_objCtrl.get(i).m_iNativeMgrID == iID)
			{
				return m_objCtrl.get(i);
			}
		}
		
		return null;
		
	}


	private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
	
	
	public native int InitNDK(Object obj);
	
	public native void SetAssetManager(AssetManager assetManager);
	public native int InitApplication();
	public native void QuitApplication();
	public native void SetWindowSize(int iWidth,int iHeight,int iUnityTextureID,boolean bRockchip);
	public native void RenderScene(float [] fValue, int iTextureID,int iUnityTextureID);
	public native void SetManagerID(int iID);
	public native int GetManagerID();
	public native int InitExtTexture();
	
	public native void SetUnityTextureID(int iTextureID);

	
	static
	{
		 System.loadLibrary("BlueDoveMediaRender");
	}
	
	MEDIAPLAYER_STATE m_iCurrentState = MEDIAPLAYER_STATE.NOT_READY;
	
	public void Destroy()
	{
		System.out.println("Destroy");
		if(m_iSurfaceTextureID != -1)
		{
			int [] textures = new int[1];
			textures[0] = m_iSurfaceTextureID;
			GLES20.glDeleteTextures(1, textures, 0);
			m_iSurfaceTextureID = -1;
		}
		
		SetManagerID(m_iNativeMgrID);
		QuitApplication();
		
		m_objCtrl.remove(this);
	
		
		
	}
	
	public void UnLoad()
	{

		System.out.println("UnLoad");
		if(m_MediaPlayer!=null)
		{
			if(m_iCurrentState != MEDIAPLAYER_STATE.NOT_READY )
			{
				try {
					m_MediaPlayer.stop();
					m_MediaPlayer.release();
					
					
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				m_MediaPlayer = null;
				
			}
			else
			{
				try {
					m_MediaPlayer.release();
					
					
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				m_MediaPlayer = null;
			}
			
			if(m_Surface != null)
			{
				m_Surface.release();
				m_Surface = null;
			}
			
			if(m_SurfaceTexture != null)
			{
				m_SurfaceTexture.release();
				m_SurfaceTexture = null;
			}
			
			if(m_iSurfaceTextureID != -1)
			{
				int [] textures = new int[1];
				textures[0] = m_iSurfaceTextureID;
				GLES20.glDeleteTextures(1, textures, 0);
				m_iSurfaceTextureID = -1;
			}
		}
	}

	public boolean Load() throws SecurityException, IllegalStateException, IOException
	{
		System.out.println("Load");
		UnLoad();
		m_iCurrentState = MEDIAPLAYER_STATE.NOT_READY;

		System.out.println("Load 2");
//		m_MediaPlayer = new MediaPlayer();
//		m_MediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//		m_mediaCodec = new MyMediaCodec();
		m_Exoplayer = new MyExoplayer(m_UnityActivity);
//		cr = new CompleteReceiver();
//		System.out.println("Load 4");
//		m_UnityActivity.registerReceiver(cr,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));//注册广播
//		System.out.println("Load 3");
//		UseDownloadManager();
//		System.out.println("Load 4");
		m_bUpdate = false;
		System.out.println("m_strFileName : " + m_strFileName);
		if(m_strFileName.contains("file://") == true)
		{
               File sourceFile = new File(m_strFileName.replace("file://", ""));

               if ( sourceFile.exists() )
               {
				   System.out.println("setDataSource 1");
				   try{
					   FileInputStream fs = new FileInputStream(sourceFile);
//				       m_MediaPlayer.setDataSource(fs.getFD());
//					   m_mediaCodec.extractor.setDataSource(fs.getFD());
					   fs.close();
				   }catch (IOException e)
				   {
					   System.out.println("catch catch e");
					   e.printStackTrace();
				   }
               }
        }
		else if(m_strFileName.contains("://") == true)
		{
			//暂时注释下
//			try {
//				Map<String, String> headers = new HashMap<String, String>();
//				headers.put("rtsp_transport", "tcp") ;
//				headers.put("max_analyze_duration", "500") ;
//
//				System.out.println("setDataSource 2");
////				m_MediaPlayer.setDataSource(m_UnityActivity, Uri.parse(m_strFileName), headers);
////				m_mediaCodec.extractor.setDataSource(m_UnityActivity, Uri.parse(m_strFileName), headers);
//				//m_MediaPlayer.setDataSource(m_strFileName);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				Log.e("Unity","Error m_MediaPlayer.setDataSource() : " + m_strFileName);
//				e.printStackTrace();
//
//				m_iCurrentState = MEDIAPLAYER_STATE.ERROR;
//
//				return false;
//			}
			//暂时注释上
		}
		else
		{

			if(m_bSplitOBB)
			{
				try {
			        ZipResourceFile expansionFile = new ZipResourceFile(m_strOBBName);

			        Log.e("unity", m_strOBBName + " " + m_strFileName);
			        AssetFileDescriptor afd = expansionFile.getAssetFileDescriptor("assets/" + m_strFileName);

			        ZipEntryRO[] data =expansionFile.getAllEntries();

			        for(int i = 0; i <data.length; i++)
			        {
			        	Log.e("unity", data[i].mFileName);
			        }

			        Log.e("unity", afd + " " );
					System.out.println("setDataSource 3");
//			        m_MediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
//					m_mediaCodec.extractor.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());

			    } catch (IOException e) {
			    	m_iCurrentState = MEDIAPLAYER_STATE.ERROR;
			        e.printStackTrace();
			        return false;
			    }
			}
			else
			{
				AssetFileDescriptor afd;
				try {
					afd = m_UnityActivity.getAssets().openFd(m_strFileName);
					System.out.println("setDataSource 4");
//					m_MediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
//					m_mediaCodec.extractor.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
			        afd.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e("Unity","Error m_MediaPlayer.setDataSource() : " + m_strFileName);
					e.printStackTrace();
					m_iCurrentState = MEDIAPLAYER_STATE.ERROR;
					return false;
				}
			}
		}
	
		
		if(m_iSurfaceTextureID == -1)
		{
			m_iSurfaceTextureID = InitExtTexture();	
		}

		m_SurfaceTexture = new SurfaceTexture(m_iSurfaceTextureID);
		m_SurfaceTexture.setOnFrameAvailableListener(this);
		m_Surface = new Surface(m_SurfaceTexture);
		System.out.println("setSurface");

		m_Exoplayer.initializePlayer(m_Surface);
		System.out.println("initializePlayer End");


//		m_mediaCodec.setSurface(m_Surface);


		//onPrepared
		m_iCurrentState = MEDIAPLAYER_STATE.READY;
		SetManagerID(m_iNativeMgrID);
		m_iCurrentSeekPercent = 0;
		//onPrepared
		//onFrameAvailable
		m_bUpdate = true;
		//onFrameAvailable
		//下面暂时注释为了DownloadManager
//		m_mediaCodec.play();

//		m_MediaPlayer.setSurface(m_Surface);
//		m_MediaPlayer.setOnPreparedListener(this);
//		m_MediaPlayer.setOnCompletionListener(this);
//		m_MediaPlayer.setOnErrorListener(this);
//
//		m_MediaPlayer.prepareAsync();

		return true;
	}
	
	
	synchronized public void onFrameAvailable(SurfaceTexture surface) {
		System.out.println("onFrameAvailable m_bUpdate = true");
		System.out.println("m_mediaCodec.play();");
		m_bUpdate = true;
	}

	
	public void UpdateVideoTexture()
	{
		m_iCurrentState = MEDIAPLAYER_STATE.PLAYING;
//		System.out.println("UpdateVideoTexture");
		if(m_bUpdate == false)
			return;
			
		if(m_Exoplayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.PLAYING || m_iCurrentState == MEDIAPLAYER_STATE.PAUSED)
			{
			
				SetManagerID(m_iNativeMgrID);
				
			
				boolean [] abValue = new boolean[1];
				GLES20.glGetBooleanv(GLES20.GL_DEPTH_TEST, abValue,0);
				GLES20.glDisable(GLES20.GL_DEPTH_TEST);
				m_SurfaceTexture.updateTexImage();
				
				
				
				
		
				float [] mMat = new float[16];
	
				
				m_SurfaceTexture.getTransformMatrix(mMat);
				
				RenderScene(mMat,m_iSurfaceTextureID,m_iUnityTextureID);
				
				
				if(abValue[0])
				{
					GLES20.glEnable(GLES20.GL_DEPTH_TEST);
				}
				else
				{
					
				}
				
				abValue = null;
				
			}
		}
	}
	
	
	public void SetRockchip(boolean bValue)
	{
		m_bRockchip = bValue;
	}

	
	public void SetLooping(boolean bLoop)
	{
		System.out.println("SetLooping");
		if(m_MediaPlayer != null)
			m_MediaPlayer.setLooping(bLoop);
	}
	
	public void SetVolume(float fVolume)
	{
		System.out.println("SetVolume");
		if(m_MediaPlayer != null)
		{
			m_MediaPlayer.setVolume(fVolume, fVolume);
		}
		
		
	}
	
	
	public void SetSeekPosition(int iSeek)
	{
//		System.out.println("SetSeekPosition");
		if(m_MediaPlayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.READY || m_iCurrentState == MEDIAPLAYER_STATE.PLAYING || m_iCurrentState == MEDIAPLAYER_STATE.PAUSED)
			{
				m_MediaPlayer.seekTo(iSeek);
			}
		}
	}
	
	public int GetSeekPosition()
	{
//		System.out.println("GetSeekPosition");
		if(m_MediaPlayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.READY || m_iCurrentState == MEDIAPLAYER_STATE.PLAYING  || m_iCurrentState == MEDIAPLAYER_STATE.PAUSED)
			{
				try {
					m_iCurrentSeekPosition =  m_MediaPlayer.getCurrentPosition();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return m_iCurrentSeekPosition;
	}
	
	public int GetCurrentSeekPercent()
	{
		return m_iCurrentSeekPercent;
	}
	
	
	public void Play(int iSeek)
	{
		System.out.println("Play");
		if(m_MediaPlayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.READY || m_iCurrentState == MEDIAPLAYER_STATE.PAUSED || m_iCurrentState == MEDIAPLAYER_STATE.END )
			{
					
				//m_MediaPlayer.seekTo(iSeek);
				m_MediaPlayer.start();
				m_iCurrentState = MEDIAPLAYER_STATE.PLAYING;
				
			}
		}
	}
	
	public void Reset()
	{
		System.out.println("Reset");
		if(m_MediaPlayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.PLAYING)
			{
				m_MediaPlayer.reset();
				
			}
			
		}
		m_iCurrentState = MEDIAPLAYER_STATE.NOT_READY;
	}
	
	public void Stop()
	{
		System.out.println("Stop");
		if(m_MediaPlayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.PLAYING)
			{
				m_MediaPlayer.stop();
				
			}
			
		}
		m_iCurrentState = MEDIAPLAYER_STATE.NOT_READY;
	}
	
	public void RePlay()
	{
		System.out.println("RePlay");
		if(m_MediaPlayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.PAUSED)
			{
				m_MediaPlayer.start();
				m_iCurrentState = MEDIAPLAYER_STATE.PLAYING;
				
			}
		}
	}
	
	public void Pause()
	{
//		System.out.println("Pause");
		if(m_MediaPlayer != null)
		{
			if(m_iCurrentState == MEDIAPLAYER_STATE.PLAYING)
			{
				m_MediaPlayer.pause();
				m_iCurrentState = MEDIAPLAYER_STATE.PAUSED;
			}
		}
	}
	
	public int GetVideoWidth()
	{
//		System.out.println("GetVideoWidth");
		return 4096;
//		if(m_MediaPlayer != null)
//		{
//			System.out.println("getVideoWidth : "+m_MediaPlayer.getVideoWidth());
//			return m_MediaPlayer.getVideoWidth();
//
//		}
//		return 0;
	}
	
	public int GetVideoHeight()
	{
//		System.out.println("GetVideoHeight");
		return 2048;
//		if(m_MediaPlayer != null)
//		{
//			System.out.println("getVideoHeight : "+m_MediaPlayer.getVideoHeight());
//			return m_MediaPlayer.getVideoHeight();
//
//		}
//		return 0;
	}
	
	public boolean IsUpdateFrame()
	{
//		System.out.println("IsUpdateFrame");
		if(m_bUpdate == true)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void SetUnityTexture(int iTextureID)
	{
		System.out.println("SetUnityTexture");
		m_iUnityTextureID = iTextureID;
		SetManagerID(m_iNativeMgrID);
		SetUnityTextureID(m_iUnityTextureID);
		
	}
	public void SetUnityTextureID(Object texturePtr)
	{
		
	}
	
	
	public void SetSplitOBB( boolean bValue,String strOBBName)
	{
		System.out.println("SetSplitOBB");
		m_bSplitOBB = bValue;
		m_strOBBName = strOBBName;
	}
	
	public int GetDuration()
	{
		System.out.println("GetDuration");
		if(m_MediaPlayer != null)
		{
			return m_MediaPlayer.getDuration();
		}
		
		return -1;
	}
	
	
	public int InitNative(EasyMovieTexture obj) 
	{
		System.out.println("InitNative");
		
		m_iNativeMgrID = InitNDK(obj);
		m_objCtrl.add(this);
		
		return m_iNativeMgrID;
		
	}
	
	public void SetUnityActivity(Activity unityActivity)
    {
		System.out.println("SetUnityActivity");
		SetManagerID(m_iNativeMgrID);
		m_UnityActivity = unityActivity;
		SetAssetManager(m_UnityActivity.getAssets());
    }
	
	
	public void NDK_SetFileName(String strFileName)
	{
		m_strFileName = strFileName;
	}
	
	
	public void InitJniManager()
	{
		System.out.println("InitJniManager");
		SetManagerID(m_iNativeMgrID);
		InitApplication();
	}
	
	
	

	public int GetStatus()
	{
		return m_iCurrentState.GetValue();
	}
	
	public void SetNotReady()
	{
		m_iCurrentState = MEDIAPLAYER_STATE.NOT_READY;
	}
	
	public void SetWindowSize()
	{
		System.out.println("SetWindowSize");
		SetManagerID(m_iNativeMgrID);
		SetWindowSize(GetVideoWidth(),GetVideoHeight(),m_iUnityTextureID ,m_bRockchip);
		
		
	}
	
	public int GetError()
	{
		return m_iErrorCode;
	}
	
	public int GetErrorExtra()
	{
		return m_iErrorCodeExtra;
	}
	
	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		System.out.println("onError");

		if (arg0 == m_MediaPlayer)
        {
            String strError;

            switch (arg1)
            {
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                	strError = "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";
                    break;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                	strError = "MEDIA_ERROR_SERVER_DIED";
                	break;
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                	strError = "MEDIA_ERROR_UNKNOWN";
                    break;
                default:
                	strError = "Unknown error " + arg1;
            }
            
            m_iErrorCode = arg1;
            m_iErrorCodeExtra = arg2;
            
            



            m_iCurrentState = MEDIAPLAYER_STATE.ERROR;

            return true;
        }

        return false;
        
	}


	
	@Override
	public void onCompletion(MediaPlayer arg0) {
		System.out.println("onCompletion");
		// TODO Auto-generated method stub
		if (arg0 == m_MediaPlayer)
			m_iCurrentState = MEDIAPLAYER_STATE.END;
		
	}

	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
		// TODO Auto-generated method stub
		System.out.println("onBufferingUpdate");
		
	
		if (arg0 == m_MediaPlayer)
			m_iCurrentSeekPercent = arg1;
		
		
		
        
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		System.out.println("onPrepared");
		// TODO Auto-generated method stub
		if (arg0 == m_MediaPlayer)
		{
			m_iCurrentState = MEDIAPLAYER_STATE.READY;
			
			SetManagerID(m_iNativeMgrID);
			m_iCurrentSeekPercent = 0;
			//m_MediaPlayer.setOnBufferingUpdateListener(this);
			
		}
		
	}
	
	
	public enum MEDIAPLAYER_STATE
    {
		NOT_READY       (0),
		READY           (1),
        END     		(2),
        PLAYING         (3),
        PAUSED          (4),
        STOPPED         (5),
        ERROR           (6);

        private int iValue;
        MEDIAPLAYER_STATE (int i)
        {
            iValue = i;
        }
        public int GetValue()
        {
            return iValue;
        }
    }

	public void setDirection(long direction)
	{
//		System.out.println("setDirection java : " + String.valueOf(direction));
//		m_Exoplayer.SeekTo(direction);
		m_Exoplayer.ChangedMoveState(direction);
	}

    //Download
//	private void UseDownloadManager()
//	{
//		//发送Http请求下载
//////				downloadFile("http://192.168.199.218:8080/Kodak.mp4",Environment.getExternalStorageDirectory()+"/Kodak.mp4");
//		String urlStr = m_mediaCodec.myNginxIp + m_mediaCodec.myDownloadFileName;
//		// uri 是你的下载地址，可以使用Uri.parse("http://")包装成Uri对象
//		DownloadManager.Request req = new DownloadManager.Request(Uri.parse(urlStr));
//		// 此方法表示在下载过程中通知栏会一直显示该下载，在下载完成后仍然会显示，
//		// 直到用户点击该通知或者消除该通知。还有其他参数可供选择
//		req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//		// 设置下载文件存放的路径，同样你可以选择以下方法存放在你想要的位置。
//		req.setDestinationInExternalPublicDir("/DownloadManager/",m_mediaCodec.myDownloadFileName);
//		/*
//		在默认的情况下，通过Download Manager下载的文件是不能被Media Scanner扫描到的 。
//        进而这些下载的文件（音乐、视频等）就不会在Gallery 和  Music Player这样的应用中看到。
//        为了让下载的音乐文件可以被其他应用扫描到，我们需要调用Request对象的
//        */
//		req.allowScanningByMediaScanner();
//		/*
//		如果我们希望下载的文件可以被系统的Downloads应用扫描到并管理，
//        我们需要调用Request对象的setVisibleInDownloadsUi方法，传递参数true。
//        */
//		req.setVisibleInDownloadsUi(true);
//		//开始下载
//		downloadManager = (DownloadManager)m_UnityActivity.getSystemService(DOWNLOAD_SERVICE);
//		mReference = downloadManager.enqueue(req);
//		/*
//        下载管理器中有很多下载项，怎么知道一个资源已经下载过，避免重复下载呢？
//        我的项目中的需求就是apk更新下载，用户点击更新确定按钮，第一次是直接下载，
//        后面如果用户连续点击更新确定按钮，就不要重复下载了。
//        可以看出来查询和操作数据库查询一样的
//        */
//		Query query = new Query();
//		query.setFilterById( mReference );
//		Cursor cursor = downloadManager.query(query);
//		if(!cursor.moveToFirst()){// 没有记录
//
//		} else {
//			//有记录
//		}
//	}
//
//	//DataManager注册广播回调
//	class CompleteReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			System.out.println("Download Finished");
//			m_mediaCodec.play();
////			m_mediaCodec.play();
//		}
//	}
	//Download


}
