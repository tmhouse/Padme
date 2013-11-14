/*
 * Copyright 2013 tmhouse@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.padme.android.lib.utils.Log;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

/**
 * Loggerの便利クラス.
 * 機能
 * - debuggerble==trueのときだけ出力する。
 * - ログの直前行に、VERBOSEで"at location"という形式で吐くので、
 *   LogCatでダブルクリックするとその行へ飛べて便利。
 *   LogCatの出力レベルを普段はDEBUGにしておき、出力行が見たいときは
 *   VERBOSEに切り替えて使用する(といいよ)。
 * - tagにはクラス名とメソッド名が自動で入る。
 * 
 * @author mutoh
 *
 */
public class PdLog {
	private static boolean s_enable = false;
	private static boolean s_isFirst = true;
	
	// セマフォ
	private static Object s_sema = new Object();

	/**
	 * enable/disable.
	 * よく使うサンプル.
	 * PdLog.enable(PdLog.isDebuggable(context));
	 * @param bEnable
	 */
	public static void enable(boolean bEnable) {
		s_enable = bEnable;
	}
	
	/**
	 * call stackを得るためのThrowableクラス.
	 * @author mutoh
	 *
	 */
	private static class PdThrowableForLog extends Throwable {
		private static final long serialVersionUID = 1L;
		private StackTraceElement	m_caller;
		
		public PdThrowableForLog() {
		}
		
		/**
		 * 呼び出し元のファイル名と行番号を返す.
		 * @return
		 */
		public String createCallerFileAndLine() {
			StackTraceElement s = getCaller();
			if( s != null ) {
				String str = "at (" + 
					getFilePath() + ":" + s.getLineNumber() + ")";
				return(str);
			}
			return(null);
		}
		
		/**
		 * 呼び出し元のファイル名のフルパスを返す.
		 * @return
		 */
		public String getFilePath() {
			StackTraceElement s = getCaller();
			if( s != null ) {
				String fullClassName = s.getClassName();
				// inner classから呼ばれた場合は'$n'が付くので消す
				// TODO これ以外のパターンてないのか？
				fullClassName = fullClassName.replaceAll("\\$.*$", "");
				return(fullClassName + ".java");
			}
			return(null);
		}
		
		/**
		 * 呼び出し元のクラス名とメソッド名を返す.
		 * @return
		 */
		public String createCallerMethodName() {
			StackTraceElement s = getCaller();
			if( s != null ) {
				String str = getSimpleClassName(s) + "." +
						s.getMethodName();
				return(str);
			}
			return(null);
		}

		/**
		 * クラス名だけを返す.
		 * @param s
		 * @return
		 */
		private String getSimpleClassName(StackTraceElement s) {
			String fullClassName = s.getClassName();
			String[] arr = fullClassName.split("\\.");
			if( arr != null && arr.length > 0 ) {
				return(arr[arr.length - 1]);
			}
			return(null);
		}

		/**
		 * 呼び出し元のスタックフレーム情報を返す.
		 * @return
		 */
		private StackTraceElement getCaller() {
			if( m_caller == null ) {
				StackTraceElement[] stacks = getStackTrace();
				String logClassName = PdLog.class.getName();
				for( StackTraceElement s : stacks ) {
					String className = s.getClassName();
					// PdLogクラスのstackは除外
					if( className.equals(logClassName) ) {
						continue;
					}
					m_caller = s;
					break;
				}
			}
			return(m_caller);
		}
	}
	
	/**
	 * パッケージがdebuggerble=trueか否か.
	 * @param context
	 * @return
	 */
	public static boolean isDebuggable(Context context) {
		try {
			PackageManager manager = context.getPackageManager();
			ApplicationInfo appInfo = manager.getApplicationInfo(
					context.getPackageName(), 0);
			if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
				return(true);
			}
		} catch (NameNotFoundException e) {
		}
		return(false);
	}
	
	/**
	 * d.
	 * @param msg
	 */
	public static void d(String msg) {
		callLogMethod(Log.DEBUG, msg);
	}

	/**
	 * w.
	 * @param msg
	 */
	public static void w(String msg) {
		callLogMethod(Log.WARN, msg);
	}

	/**
	 * e.
	 * @param msg
	 */
	public static void e(String msg) {
		callLogMethod(Log.ERROR, msg);
	}

	/**
	 * i.
	 * @param msg
	 */
	public static void i(String msg) {
		callLogMethod(Log.INFO, msg);
	}

	/**
	 * v.
	 * @param msg
	 */
	public static void v(String msg) {
		callLogMethod(Log.VERBOSE, msg);
	}
	
	/**
	 * a.
	 * @param msg
	 */
	public static void a(String msg) {
		callLogMethod(Log.ASSERT, msg);
	}
	
	/**
	 * ログを吐くメソッド.
	 * @param kind
	 * @param tag
	 * @param msg
	 */
	private static void callLogMethod(int kind, String msg) {
		if( !s_enable ) {
			return;
		}
		if( s_isFirst ) {
			s_isFirst = false;
			// Log.eで一発何か出しておかないとLogCatのフィルターが有効
			// にならないための処置。
			Log.e(PdLog.class.getSimpleName(), "start.(this is not an error message)");
		}
		
		PdThrowableForLog tmth = new PdThrowableForLog();
		String locStr = tmth.createCallerFileAndLine();
		String methodStr = tmth.createCallerMethodName();
		// 以下の2行はPdLogを使う限り、連続させるため排他する
		synchronized (s_sema) {
			Log.v(PdLog.class.getSimpleName(), locStr);
			Log.println(kind, methodStr, msg);
		}
	}
}
